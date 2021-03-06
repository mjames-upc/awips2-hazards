'''
Ingests convective cell identification and attributes from automated source

November 2015 :: starting with ingesting ProbSevere data from 
http://cimss.ssec.wisc.edu/severe_conv/probsev.html

Assumes data feed from CIMSS via LDM into /awips2/edex/data/manual

probsevere data plugin for edex should be part of baseline and should ingest
to datastore

'''
import sys
import datetime
import EventFactory
import EventSetFactory
import GeometryFactory
import AdvancedGeometry
import RecommenderTemplate
import numpy
import JUtil
import time
import re
from EventSet import EventSet

import h5py
import numpy as np
import glob, os, time, datetime
import pprint
from collections import defaultdict, deque
from shapely.wkt import loads
from shapely.geometry import Polygon, Point

import TimeUtils, LogUtils
from ProbUtils import ProbUtils

from HazardConstants import *
import HazardDataAccess

from com.raytheon.uf.common.time import SimulatedTime
from edu.wisc.ssec.cimss.common.dataplugin.probsevere import ProbSevereRecord
from SwathRecommender import Recommender as SwathRecommender 
from HazardEventLockUtils import HazardEventLockUtils
from org.opengis.filter.temporal import EndedBy
from org.apache.cxf.common.i18n import UncheckedException
#
# The size of the buffer for default flood polygons.
DEFAULT_POLYGON_BUFFER = 0.05

#
# Keys to values in the attributes dictionary produced
# by the flood recommender.
OBJECT_ID = "objectids"
MISSING_VALUE = -9999
MILLIS_PER_SECOND = 1000

### FIXME
DEFAULT_DURATION_IN_SECS = 3600 # 60 minutes


sysTime=SimulatedTime.getSystemTime().getMillis()/1000
print "SYSTIME:", sysTime
if sysTime < 1478186880:
    SOURCEPATH='/awips2/edex/data/hdf5/probsevere'
else:
    SOURCEPATH = '/realtime-a2/hdf5/probsevere'

AUTOMATION_LEVELS = ['userOwned','attributesOnly','attributesAndGeometry','automated']

class Recommender(RecommenderTemplate.Recommender):

    def __init__(self):
        """
        We'll see if anything needs to be initialized
        """
        
        description="""
            first line is always, "Valid: yyyymmdd_HHMMSS" (in UTC). Each line after than gives information for a unique storm.
            
            A colon separates each piece of info. 
            
            Column1: Type of object (string). Right now, it is always "RAD". It could potentially be "SAT" or "LTG" in the future.
            Column2: The probability. Integer from 0 - 100.
            Column3: String for MUCAPE info. May sometimes be "N/A"
            Column4: String for effective shear info. May sometimes be "N/A"
            Column5: String for MESH info, giving time and size. May sometimes be "N/A"
            Column6: String for satellite growth predictor1. Can be "N/A"
            Column7: String for satellite growth predictor2. Can be "N/A"
            Column8: Comma separated list of lats and lons. "Lat1, Lon1,...,Lat(n),Lon(n),Lat1,Lon1. Lat1 and Lon1 pair are at the end of the list, too, in order to close the polygon
            Column9: ObjectID (long)
            Column10: mean motion east, in m/s (float)
            Column11: mean motion south, in m/s (float)
        """
    
    def defineScriptMetadata(self):
        """
        @return: A dictionary containing information about this
                 tool
        """
        metaDict = {}
        metaDict["toolName"] = "ConvectiveRecommender"
        metaDict["author"] = "GSD"
        metaDict["version"] = "1.0"
        metaDict["description"] = "Ingests convective cell identification and attributes from automated source"
        metaDict["eventState"] = "Pending"
        metaDict['includeEventTypes'] = [ "Prob_Severe", "Prob_Tornado" ]
        metaDict['includeDataLayerTimes'] = True
        metaDict["getDialogInfoNeeded"] = False
        metaDict["getSpatialInfoNeeded"] = False
        return metaDict

    def defineDialog(self, eventSet):
        """
        @return: A dialog definition to solicit user input before running tool
        """        
        return None

    def initialize(self, practice):
        self.probUtils = ProbUtils(practice=practice)
        lats = self.probUtils.lats
        ulLat = lats[0]
        lrLat = lats[-1]
        lons = self.probUtils.lons
        ulLon = lons[0]
        lrLon = lons[-1]
        self.domainPolygon = Polygon([(ulLon, ulLat), (lrLon, ulLat), (lrLon, lrLat), (ulLon, lrLat), (ulLon, ulLat)])
        self.lowThreshold = self.probUtils.lowThreshold
    

    def execute(self, eventSet, dialogInputMap, visualFeatures):
        """
        Runs the River Flood Recommender tool
        
        @param eventSet: A set of events which include session
                         attributes
        @param dialogInputMap: A map of information retrieved from
                               a user's interaction with a dialog.
        @param visualFeatures:   A list of visual features provided
                                 by the defineSpatialInput() method;
                                 ignored for this recommender.
        
        @return: A list of potential events. 
        """

        import sys
        sys.stderr.write("Running convective recommender.\n    trigger:    " +
                         str(eventSet.getAttribute("trigger")) + "\n    event type: " + 
                         str(eventSet.getAttribute("eventType")) + "\n    origin:     " + 
                         str(eventSet.getAttribute("origin")) + "\n    hazard ID:  " +
                         str(eventSet.getAttribute("eventIdentifiers")) + "\n    attribute:  " +
                         str(eventSet.getAttribute("attributeIdentifiers")) + "\n")


        ### Make sure we are getting the latest PHIConfig info each time we run.
        caveMode = eventSet.getAttributes().get('runMode','PRACTICE').upper()
        self.practice = (False if caveMode == 'OPERATIONAL' else True)
        self.initialize(self.practice)

        sessionAttributes = eventSet.getAttributes()
        if sessionAttributes:
            sessionMap = JUtil.pyDictToJavaMap(sessionAttributes)
            
        st = time.time()

        currentEvents = self.getCurrentEvents(eventSet)        

        LogUtils.logMessage('Finnished ', 'getCurrentEvent',' Took Seconds', time.time()-st)
        st = time.time()
            
        self.currentTime = sessionAttributes["currentTime"]
        
        dlt = sessionAttributes.get("dataLayerTimes", [])
        
        self.dataLayerTime = sorted(dlt)[-1] if len(dlt) else self.currentTime
        self.latestDLTDT = datetime.datetime.fromtimestamp(self.dataLayerTime/1000)
        print '\n\n\t[CR] DATALAYERTIME:', self.latestDLTDT
        
        latestCurrentEventTime = self.getLatestTimestampOfCurrentEvents(eventSet, currentEvents)

        LogUtils.logMessage('Finnished ', 'getLatestTimestampOfCurrentEvents',' Took Seconds', time.time()-st)
        st = time.time()
        
        recommendedEventsDict = self.getRecommendedEventsDict(self.currentTime, latestCurrentEventTime)

        LogUtils.logMessage('Finnished ', 'getRecommendedEventsDict',' Took Seconds', time.time()-st)
        
        
        st = time.time()
        identifiersOfEventsToSaveToHistory, mergedEventSet = self.mergeHazardEvents(currentEvents, recommendedEventsDict)

        LogUtils.logMessage('Finnished ', 'mergeHazardEvent',' Took Seconds', time.time()-st)
        
        returnMergedEventSet = EventSetFactory.createEventSet(None)
        
        if len(mergedEventSet.events) > 0:
            
            st = time.time()
            swathRec = SwathRecommender()
            returnMergedEventSet.setAttributes(mergedEventSet.getAttributes())
            resultEventSet = swathRec.execute(mergedEventSet, None, None)
            returnMergedEventSet.addAll(resultEventSet.getEvents())
            LogUtils.logMessage('Finnished ', 'swathRec.execute',' Took Seconds', time.time()-st)
                    
        for e in returnMergedEventSet:
            e.setIssuanceCount(e.getIssuanceCount() + 1)
            print '[CR-1] )))) ', e.get('objectID'), e.getStatus()

        ### Ensure that any resulting events are saved to the history list or the database
        ### (the latter as latest versions of those events).  If one or both categories have
        ### no identifiers, set them to nothing in case the Swath Recommender set that
        ### attribute, as this recommender knows which ones should be saved in which category.
        if (identifiersOfEventsToSaveToHistory):
            returnMergedEventSet.addAttribute(SAVE_TO_HISTORY_KEY, identifiersOfEventsToSaveToHistory)
        else:
            returnMergedEventSet.addAttribute(SAVE_TO_HISTORY_KEY, None)

        returnMergedEventSet.addAttribute(SAVE_TO_DATABASE_KEY, True)
        returnMergedEventSet.addAttribute(TREAT_AS_ISSUANCE_KEY, True)
        returnMergedEventSet.addAttribute(KEEP_SAVED_TO_DATABASE_LOCKED_KEY, False)
        return returnMergedEventSet

    def getRecommendedEventsDict(self, currentTime, latestDatetime):
        hdfFilesList = self.getLatestProbSevereDataHDFFileList(latestDatetime=None)
        eventsDict = self.latestEventSetFromHDFFile(hdfFilesList, currentTime)
        return eventsDict

    def uvToSpdDir(self, eastMotions, southMotions):
        if eastMotions is None or southMotions is None:
            wdir = self.defaultWindDir()
            wspd = self.defaultWindSpeed() #kts
        else:
            ### Note: traditionally u, v calculations have v > 0 = Northward
            ### For some reason, ProbSevere has "southMotions" where
            ### > 0 = Southward.  Multiplying by -1. here to correct this.
            wspd, wdir = self.probUtils.UVToMagDir(float(eastMotions), -1.*float(southMotions))
            wspd = self.probUtils.convertMsecToKts(wspd)
                
        return {'wdir':int(round(wdir)), 'wspd':int(round(wspd))}



    def dumpHDFContents(self, hFile, latestN=1):
        contents = defaultdict(dict)
        def saveItems(name, obj):
            if isinstance(obj, h5py.Dataset):
                group, ds = name.split('/')
                groupDT = self.parseGroupName(group)
                contents[groupDT][ds] = obj.value.tolist()
                
        hFile.visititems(saveItems)
        
        ### Do we need to sort?
        
        return dict(contents)
                
    def makeValid(self, polyString):
        poly = loads(polyString)
        #newPoly = loads(polyString)
        tolerance = 0.01
        valid = False
        while not valid:
           newPoly = poly.simplify(tolerance)
           valid = newPoly.is_valid
           tolerance+=0.01
           if tolerance > 1:
               break
           
        return str(newPoly)
        
        

    def latestEventSetFromHDFFile(self, hdfFilenameList, currentTime):
        ### Should be a single file with latest timestamp
        hFile = None
        valuesDict = {}
        for hdfFilename in hdfFilenameList[:2]: # should grab at most 2, but should still pass if len(hdfFilenameList) == 1
            try:
                hFile = h5py.File(hdfFilename,'r')
            except:
                print 'Convective Recommender Warning: Unable to open', hdfFilename, ' in h5py. Skipping...'
                if len(hdfFilenameList) == 1:
                    return

            valuesDict.update(self.dumpHDFContents(hFile))
            hFile.close()
        
        ### BUG ALERT :: Rounding to zero in datetime comparison
        groupDTList = [t for t in sorted(valuesDict.keys()) if t.replace(second=0) <= self.latestDLTDT.replace(second=0)]
        
        ### No ProbSevere objects older than Latest Data Layer
        if len(groupDTList) == 0:
            return {}

        latestGroupDT = max(groupDTList)# if len(groupDTList) else self.latestDLTDT
        latestGroup = valuesDict.get(latestGroupDT)

        returnDict = {}
        for i in range(len(latestGroup.values()[0])):
            row = {k:v[i] for k,v in latestGroup.iteritems()}

            thisPolyString = row.get('polygons')
            if thisPolyString is None:
                continue
            elif not loads(thisPolyString).centroid.within(self.domainPolygon):
                continue
            elif not loads(thisPolyString).is_valid:
                print 'CR: Invalid polygon, Attempting to validate...', row.get('objectids')
                validPoly = self.makeValid(thisPolyString)
                if loads(validPoly).is_valid:
                    row['polygons'] = validPoly
                else:
                    print '\tSTILL INVALID, SKIPPING'
                    continue
            
           
            if row.get('probabilities') < self.lowThreshold:
                row['belowThreshold'] = True
            else:
                row['belowThreshold'] = False
            
            ### Current CONVECTPROB feed has objectids like "653830; Flash Rate 0 fl/min"
            objIds = row.get('objectids')
            if ';' in objIds:
                row['objectids'] =  objIds.split(';')[0]
            
            
            row['startTime'] = latestGroupDT
            vectorDict = self.uvToSpdDir(row.get('eastMotions'),row.get('southMotions'))
            row['wdir'] = vectorDict.get('wdir')
            row['wspd'] = vectorDict.get('wspd')
            row['hazardType'] = 'Prob_Severe'
            
            ### Needed for now - refactor
            returnDict[row['objectids']] = row

        return returnDict
        
                           
                    
    def parseGroupName(self, rootName):
        name,dtString = rootName.split('::')
        dt = datetime.datetime.strptime(dtString,"%Y-%m-%d_%H:%M:%S.0")
        return dt

    
    def getLatestProbSevereDataHDFFileList(self, latestDatetime=None):
        fileList = None
        try:
            fileList = sorted(glob.glob(os.path.join(SOURCEPATH,'*.h5')), reverse=True)
        except:
            print 'Convective Recommender Warning: Could not obtain list of probsevere*.h5 files at:', os.path.join(SOURCEPATH,'*.h5')
            print 'Returning:', fileList
            return fileList
        
        
        if latestDatetime:
            ### Use filename to make datetime and return ONLY the latest
            regex = "probsevere-%Y-%m-%d-%H.h5"
                
            fileDict = {datetime.datetime.strptime(os.path.basename(x),regex):x for x in fileList}
            
            # see https://bytes.com/topic/python/answers/765154-find-nearest-time-datetime-list
            returnFileList = [fileDict.get(min(fileDict.keys(), key=lambda date : abs(latestDatetime-date)))]
            
            return returnFileList
        else:
            return fileList
                
    
    def toString(self):
        return "ConvectiveRecommender"
    

    def getLatestTimestampOfCurrentEvents(self, eventSet, currentEvents):
        ### Initialize latestDatetime
        latestDatetime = datetime.datetime.min

        for event in currentEvents:
            eventCreationTime = event.getCreationTime()
            if eventCreationTime > latestDatetime:
               latestDatetime =  eventCreationTime

        return latestDatetime

    
    def getCurrentEvents(self, eventSet):
        siteID = eventSet.getAttributes().get('siteID')        

        caveMode = eventSet.getAttributes().get('runMode','PRACTICE').upper()
        practice = True
        if caveMode == 'OPERATIONAL':
            practice = False
        # Get current events from Session Manager (could include pending / potential)
        currentEvents = [event for event in eventSet]
        eventIDs = [event.getEventID() for event in eventSet]

        # Add in those from the Database
        databaseEvents = HazardDataAccess.getHazardEventsBySite(siteID, practice) 
        for event in databaseEvents:
            if event.getEventID() not in eventIDs:
                currentEvents.append(event)
                eventIDs.append(event.getEventID())
        return currentEvents


    def makeHazardEvent(self, ID, values):
        
        #print '\tMaking New Event?', ID
        if values.get('belowThreshold'):
            #print '\t\tBelow Threshold', self.lowThreshold, 'returning None'
            return None

        try:        
            polygon = loads(values.get('polygons'))
        except:
            print '\t\tPOLYPOLYPOLY  Unable to load polygons. Returning None.  POLYPOLYPOLY'
            return None
        
        sys.stdout.flush()
        probSevereTime = values.get('startTime', self.dataLayerTime)
        hazardEvent = EventFactory.createEvent(self.practice)
        hazardEvent.setCreationTime(probSevereTime)
        self.setEventTimes(hazardEvent, values)
        
        hazardEvent.setHazardStatus("pending")
        
        hazardEvent.setPhenomenon("Prob_Severe")
        
        hazardEvent.setGeometry(polygon)
        hazardEvent.set('convectiveObjectDir', values.get('wdir'))
        hazardEvent.set('convectiveObjectSpdKts', values.get('wspd'))
        hazardEvent.set('probSeverAttrs',values)
        hazardEvent.set('objectID', ID)
        hazardEvent.set('visibleGeometry', 'highResolutionGeometryIsVisible')
        hazardEvent.setStatus('ISSUED')
        hazardEvent.set('statusForHiddenField', 'ISSUED')

        # fill in the fields for newly created event
        hazardEvent.set('convectiveObjectDirUnc', 12)
        hazardEvent.set('convectiveObjectSpdKtsUnc', 4) 
        self.storeLastEvent(hazardEvent)
        hazardEvent.set('probSevereGeomList', [(AdvancedGeometry.createShapelyWrapper(polygon, 0) ,long(TimeUtils.datetimeToEpochTimeMillis(self.latestDLTDT)))])
        
        hazardEvent.set('manuallyCreated', False)
        hazardEvent.set('geometryAutomated', True)
        hazardEvent.set('motionAutomated', True)
        hazardEvent.set('durationAutomated', True)
        hazardEvent.set('probTrendAutomated', True)
        
        graphProbs = self.probUtils.getGraphProbs(hazardEvent, int(probSevereTime.strftime('%s'))*1000)
        hazardEvent.set('convectiveProbTrendGraph', graphProbs)
        
        return hazardEvent


    def setEventTimes(self, event, values=None):
        
        if event.getEndTime() is None or event.get('durationAutomated'):
            durSecs = DEFAULT_DURATION_IN_SECS
        else:
            durSecs = self.probUtils.getDurationSecs(event)
        
        if values is not None:
            psStartTime = values.get('startTime', self.dataLayerTime)
            event.set('probSevereStartTime', TimeUtils.datetimeToEpochTimeMillis(psStartTime))
            psEndTime = psStartTime + datetime.timedelta(seconds=durSecs)
            event.set('probSevereEndTime', TimeUtils.datetimeToEpochTimeMillis(psEndTime)) 
        
        ### Per request from Greg
        event.setStartTime(self.latestDLTDT)
        endTime = event.getStartTime() + datetime.timedelta(seconds=durSecs)
        event.setEndTime(endTime)
        

    def storeLastEvent(self, event):
        checkList = ['convectiveObjectSpdKtsUnc', 'convectiveObjectDirUnc', 'convectiveProbTrendGraph',
                            'convectiveObjectDir', 'convectiveObjectSpdKts']
        
        for c in checkList:
            event.set('convRecPast'+c, event.get(c))


    def storeNextGeometry(self, event, recommendedDict):
        geomList = deque(event.get('probSevereGeomList', []), maxlen=5)
        
        st = long(TimeUtils.datetimeToEpochTimeMillis(self.latestDLTDT))
        geom = AdvancedGeometry.createShapelyWrapper(loads(recommendedDict.get('polygons')), 0)
        
        ### Only want to store if new startTime 
        times = sorted([t[1] for t in geomList])
        if len(times) == 0:
            geomList.append((geom, st))
        elif st > times[-1]:
            geomList.append((geom, st))
        
        event.set('probSevereGeomList', list(geomList))
        
        
    def updateEvent(self, event, recommended, mergedEvents):
        dataLayerTimeMS = int(self.dataLayerTime )
        
        # Store last update
        self.storeLastEvent(event)
        self.storeNextGeometry(event, recommended)
        
        if event.get('motionAutomated', False):
            pastProbSeverePolys = event.get('probSevereGeomList', [])
            if len(pastProbSeverePolys) > 1:
                self.probUtils.updateMotionVector(event, pastProbSeverePolys)
            else:
                event.set('convectiveObjectDir', recommended.get('wdir'))
                event.set('convectiveObjectSpdKts', recommended.get('wspd'))
                
            ### Requested by Greg (HWT 2018) to restrict uncertainty for automated events
            event.set('convectiveObjectDirUnc', 12)
            event.set('convectiveObjectSpdKtsUnc', 4)

            
        if event.get('probTrendAutomated', False):
            ### BUG ALERT: do we want DataLayerTime or ProbSevereTime?
            graphProbs = self.probUtils.getGraphProbs(event, dataLayerTimeMS)
            event.set('convectiveProbTrendGraph', graphProbs)
#        graphProbs = self.probUtils.getGraphProbs(event, dataLayerTimeMS)
#        event.set('convectiveProbTrendGraph', graphProbs)
            
        self.updateEventGeometry(event, recommended)
            
        probSevereAttrs = event.get('probSeverAttrs')
        for k,v in recommended.iteritems():
            probSevereAttrs[k] = v
            

        event.set('probSeverAttrs',probSevereAttrs)
        
        self.setEventTimes(event)
        
        mergedEvents.add(event)



    def updateEventGeometry(self, event, recommendedDict):
        
        if event.get('geometryAutomated', False):
            try:
                newShape = loads(recommendedDict.get('polygons'))
                event.setGeometry(newShape)
            except:
                print 'ConvectiveRecommender: WHAT\'S WRONG WITH THIS POLYGON?', currID, type(recommended.get('polygons')), recommended.get('polygons')
                sys.stdout.flush()
        ### We want to get the nearest location of forecast (relocated inner) poly nearest the given time and use that
        else:
            recommendedStartTimeDT = recommendedDict.get('startTime')
            recommendedEpochMillis = TimeUtils.datetimeToEpochTimeMillis(recommendedStartTimeDT)
            latestDLTMillis = TimeUtils.datetimeToEpochTimeMillis(self.latestDLTDT)
            polyGeom = self.getNearestPolygon(event, currentTimeMS=latestDLTMillis, featureType="swathRec_relocated_")
            #===================================================================
            # polyGeom = None
            # timeMin = sys.maxint
            # features = event.getVisualFeatures()
            # for feature in features:
            #     featureIdentifier = feature.get('identifier')
            #     if featureIdentifier.startswith("swathRec_relocated_"):
            #         geomDict = feature.get('geometry')
            #         if geomDict is None:
            #             continue
            #         #"geometry": {(polySt_ms, polyEt_ms): relocatedShape}
            #         polySt_ms = int(geomDict.keys()[0][0])
            #         polyShape = geomDict.values()[0]
            #             
            #         timeDiff = np.abs(recommendedEpochMillis-polySt_ms)
            #         
            #         if timeDiff < timeMin:
            #             timeMin = timeDiff
            #             polyGeom = polyShape
            #===================================================================

            if polyGeom is not None:
                event.setGeometry(polyGeom)
                    

      
    def doubleCheckEnded(self, currentEventsList, recommendedEventsDict):
        recoverAutomatedList = []
        for evt in currentEventsList:
            if evt.getStatus() in ['ENDED', 'ENDING', 'ELAPSED']:
                objectID = evt.get('objectID')
                if objectID in recommendedEventsDict.keys():
                   recoverAutomatedList.append(int(objectID))
        return recoverAutomatedList


    def getNearestPolygon(self, event, currentTimeMS=None, featureType="swathRec_forecast_"):

        if currentTimeMS is None:
            currentTimeMS=self.currentTime
        polyGeom = None
        timeMin = sys.maxint
        features = event.getVisualFeatures()
        for feature in features:
            featureIdentifier = feature.get('identifier')
            if featureIdentifier.startswith(featureType):
                geomDict = feature.get('geometry')
                if geomDict is None:
                    continue
                    #"geometry": {(polySt_ms, polyEt_ms): relocatedShape}
                polySt_ms = int(geomDict.keys()[0][0])
                polyShape = geomDict.values()[0]
                        
                timeDiff = np.abs(currentTimeMS-polySt_ms)
                    
                if timeDiff < timeMin:
                    timeMin = timeDiff
                    polyGeom = polyShape

        return polyGeom

        
        
    def overlaps(self, existingEvent, recommendedDict):
        #existingGeom = existingEvent.getGeometry().asShapely()
        existingGeom = self.getNearestPolygon(existingEvent)
        if existingGeom is None:
            sys.stderr.write('\nCR (overlaps): existingEvent returns NONE geometry.\nFailing intersection test. Might result in overlapping Hazard Events\n\n')
            return False
        recommendedGeom = loads(recommendedDict.get('polygons'))
        return existingGeom.asShapely().intersects(recommendedGeom)



    def mergeHazardEvents(self, existingEventSet, recommendedEventsDict):
        
        '''
        function to merge recommended Hazard events with current valid events
        lockedEvents will not be touched
        
        # recommendedEventsDict  
        #
        #     { objectID : {hazardType: xxx, objectids: xxx, polygon: xxxx, ...}}
        
        '''
        
        
        mergedEvents = EventSet(None)
        currentTimeMS = self.currentTime
        mergedEvents.addAttribute('currentTime', currentTimeMS)
        mergedEvents.addAttribute('trigger', 'autoUpdate')

        identifiersOfEventsToSaveToHistory = []

        # Need to retain proposed events because we might need to recover them to issued
        for existingEvent in existingEventSet:
            if self.probUtils.isFullyAuto(existingEvent):
                if existingEvent.get('probSeverAttrs').get('probabilities') < self.lowThreshold:
                    existingEvent.set('threshold', 'below')
                else:
                    if existingEvent.get('threshold') != 'at or above':
                        existingEvent.set('threshold', 'at or above')
            
        
        # recommende hazartType_objectIDs_tuples -- example: [ (Prob_Severe, M334), (Prob_Tor, 5557)]
        recommended_hazardType_objectIDs_tuples = [(xx.get('hazardType'), xx.get('objectids', None)) for xx in recommendedEventsDict.values()]
        
        # First pass:  Eliminate existing events that can be merged through
        for existingEvent in existingEventSet:
            rawExistingEventID = re.findall('\d+', str(existingEvent.get('objectID')))[0]
            if existingEvent.get('manuallyCreated'):
                mergedEvents.add(existingEvent)
                continue
            if (existingEvent.getHazardType(), rawExistingEventID) not in recommended_hazardType_objectIDs_tuples:
                if self.probUtils.isFullyAuto(existingEvent):
                    print '\n\n\t\t$$$$$$$  CR -- ENDED event',  rawExistingEventID, '\n\n'
                    existingEvent.setStatus('ENDED')
                    mergedEvents.add(existingEvent)
                    identifiersOfEventsToSaveToHistory.append(existingEvent.getEventID())
                    continue
                #if partially or fully taken over
                else:
                    # Convert it to be fully taken over i.e. "M" and all automated boxes unchecked
                    existingEvent.set('durationAutomated', False)
                    existingEvent.set('geometryAutomated', False)
                    existingEvent.set('motionAutomated', False)
                    existingEvent.set('probTrendAutomated', False)
                    existingEvent.set('objectID',  'M' + existingEvent.getDisplayEventID())
                    existingEvent.set('manuallyCreated', True)
                    mergedEvents.add(existingEvent)
                    continue
                
        # Second pass:  For each reommended event, see if we can update an existing one and if not, we create a new one 
        for recommendedObjectID in recommendedEventsDict:
            found = False
            overlap = False
            endedEvent = None
            nonEndedEvent = None
            recommendedDict = recommendedEventsDict.get(recommendedObjectID)
            recommendedHazardType = recommendedDict.get('hazardType')
            
            if len(existingEventSet) == 0:
                newRec = self.makeHazardEvent(recommendedObjectID, recommendedDict)
                if newRec is not None:
                    mergedEvents.add(newRec)
                    identifiersOfEventsToSaveToHistory.append(newRec.getEventID())
            else:
                for existingEvent in existingEventSet:
                    rawExistingEventID = re.findall('\d+', str(existingEvent.get('objectID')))[0]
                    existingHazardType = existingEvent.getHazardType()
                    
                    if existingHazardType == recommendedHazardType:
                        if rawExistingEventID == recommendedObjectID:
                            found = True
                            if existingEvent.getStatus() in ['ENDED', 'ENDING', 'ELAPSED']:
                                endedEvent = existingEvent
                            else:
                                nonEndedEvent = existingEvent
                        else: 
                            if not self.probUtils.isFullyAuto(existingEvent) and existingEvent.getStatus() in ['ISSUED', 'PENDING'] and self.overlaps(existingEvent, recommendedDict):
                                overlap = True
                                
                if not found and not overlap:
                    newRec = self.makeHazardEvent(recommendedObjectID, recommendedDict)
                    if newRec is not None:
                        mergedEvents.add(newRec)
                        identifiersOfEventsToSaveToHistory.append(newRec.getEventID())
                else:
                    if nonEndedEvent is not None:
                        recoveredEvents = self.updateExistingFromRecommended(nonEndedEvent, recommendedDict, mergedEvents)
                        identifiersOfEventsToSaveToHistory.extend(recoveredEvents)
                    elif endedEvent is not None:
                        recoveredEvents = self.updateExistingFromRecommended(endedEvent, recommendedDict, mergedEvents)
                        identifiersOfEventsToSaveToHistory.extend(recoveredEvents)
                    
        return identifiersOfEventsToSaveToHistory, mergedEvents
                    
    def updateExistingFromRecommended(self, existingEvent, recommendedDict, mergedEvents):
        # Resurrect recommended object ID if it was ended
        
        identifiersOfEventsToSaveToHistory = []
        
        recommendedObjectID = recommendedDict.get('objectids')
        if existingEvent.getStatus() in ['ENDED', 'ENDING', 'ELAPSED']:
            newRec = self.makeHazardEvent(recommendedObjectID, recommendedDict)
            if newRec is not None:
                mergedEvents.add(newRec)
                identifiersOfEventsToSaveToHistory.append(newRec.getEventID())
        # Update existingEvent from recommendedDict
        else:
            self.updateEvent(existingEvent, recommendedDict, mergedEvents)
        
        return identifiersOfEventsToSaveToHistory
                   


    def isEditableSelected(self, event):
        selected = event.get('selected', False)
        if selected == 0: selected = False  
        return selected and event.get('activate', False), selected        

                                
    def flush(self):
        import os
        os.sys.__stdout__.flush()
        
        
    #########################################
    ### OVERRIDES
    
    def defaultWindSpeed(self):
        return 32
    
    def defaultWindDir(self):
        return 270

