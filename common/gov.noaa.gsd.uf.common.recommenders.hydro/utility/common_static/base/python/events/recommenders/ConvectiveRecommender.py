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
from collections import defaultdict
from shapely.wkt import loads
from shapely.geometry import Polygon, Point

import TimeUtils, LogUtils
from ProbUtils import ProbUtils

from HazardConstants import *
import HazardDataAccess

from com.raytheon.uf.common.time import SimulatedTime
from edu.wisc.ssec.cimss.common.dataplugin.probsevere import ProbSevereRecord
from SwathRecommender import Recommender as SwathRecommender 
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

AUTOMATION_LEVELS = ['userOwned','attributesOnly','attributesAndMechanics','automated']

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
        metaDict['background'] = True
        metaDict['includeDataLayerTimes'] = True
        return metaDict

    def defineDialog(self, eventSet):
        """
        @return: A dialog definition to solicit user input before running tool
        """        
        return None

    def initialize(self):
        self.probUtils = ProbUtils()
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
                         str(eventSet.getAttribute("eventIdentifier")) + "\n    attribute:  " +
                         str(eventSet.getAttribute("attributeIdentifiers")) + "\n")
        self.initialize()
        sessionAttributes = eventSet.getAttributes()
        if sessionAttributes:
            sessionMap = JUtil.pyDictToJavaMap(sessionAttributes)
            
        st = time.time()

        currentEvents = self.getCurrentEvents(eventSet)        

        LogUtils.logMessage('Finnished ', 'getCurrentEvent',' Took Seconds', time.time()-st)
        st = time.time()
            
        self.currentTime = datetime.datetime.utcfromtimestamp(long(sessionAttributes["currentTime"])/1000)
        
        dlt = sessionAttributes.get("dataLayerTimes", [])
        
        self.dataLayerTime = sorted(dlt)[-1] if len(dlt) else self.currentTime
        self.latestDLTDT = datetime.datetime.fromtimestamp(self.dataLayerTime/1000)
        print '\n\n\tDATALAYERTIME:', self.latestDLTDT
        
        latestCurrentEventTime = self.getLatestTimestampOfCurrentEvents(eventSet, currentEvents)

        LogUtils.logMessage('Finnished ', 'getLatestTimestampOfCurrentEvents',' Took Seconds', time.time()-st)
        st = time.time()
        
        recommendedEventsDict = self.getRecommendedEventsDict(self.currentTime, latestCurrentEventTime)

        LogUtils.logMessage('Finnished ', 'getRecommendedEventsDict',' Took Seconds', time.time()-st)
        
        
        st = time.time()
        identifiersOfEventsToSaveToHistory, identifiersOfEventsToSaveToDatabase, mergedEventSet = self.mergeHazardEventsNew2(currentEvents, recommendedEventsDict)

        LogUtils.logMessage('Finnished ', 'mergeHazardEvent',' Took Seconds', time.time()-st)
        
        
        if len(mergedEventSet.events) > 0:
            st = time.time()
            swathRec = SwathRecommender()
            mergedEventSet = swathRec.execute(mergedEventSet, None, None)
            LogUtils.logMessage('Finnished ', 'swathRec.execute',' Took Seconds', time.time()-st)
        
        for e in mergedEventSet:
            print ')))) ', e.get('objectID'), e.getStatus()

        ### Ensure that any resulting events are saved to the history list or the database
        ### (the latter as latest versions of those events).  If one or both categories have
        ### no identifiers, set them to nothing in case the Swath Recommender set that
        ### attribute, as this recommender knows which ones should be saved in which category.
        if (identifiersOfEventsToSaveToHistory):
            mergedEventSet.addAttribute("saveToHistory", identifiersOfEventsToSaveToHistory)
        else:
            mergedEventSet.addAttribute("saveToHistory", None)
        if (identifiersOfEventsToSaveToDatabase):
            mergedEventSet.addAttribute("saveToDatabase", identifiersOfEventsToSaveToDatabase)
        else:
            mergedEventSet.addAttribute("saveToDatabase", None)
        return mergedEventSet
    

    def getRecommendedEventsDict(self, currentTime, latestDatetime):
        #hdfFilesList = self.getLatestProbSevereDataHDFFileList(currentTime)
        hdfFilesList = self.getLatestProbSevereDataHDFFileList(latestDatetime=None)
        #eventsDict = self.eventSetFromHDFFile(hdfFilesList, currentTime, latestDatetime)
        eventsDict = self.latestEventSetFromHDFFile(hdfFilesList, currentTime)
        return eventsDict

    def uvToSpdDir(self, eastMotions, southMotions):
        if eastMotions is None or southMotions is None:
            wdir = self.defaultWindDir()
            wspd = self.defaultWindSpeed() #kts
        else:
            #u = float(eastMotions)
            #v = -1.*float(southMotions)
            u = -1.*float(eastMotions)
            v = -1.*float(southMotions)
            wspd = int(round(math.sqrt(u**2 + v**2) * 1.94384)) # to knots
            wdir = int(round(math.degrees(math.atan2(-u, -v))))
            if wdir < 0:
                wdir += 360
                
        return {'wdir':wdir, 'wspd':wspd}



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
        
        #print 'HHHHHHHHHHHHHHHHHHH\n\n'
        #print '\thdfFilenameList', hdfFilenameList, hdfFilenameList[:2]
        ##pprint.pprint(valuesDict)
        #print '\tself.latestDLTDT:', self.latestDLTDT
        #print '\tsorted(valuesDict.keys()', sorted(valuesDict.keys())
        
        ### BUG ALERT :: Rounding to zero in datetime comparison
        groupDTList = [t for t in sorted(valuesDict.keys()) if t.replace(second=0) <= self.latestDLTDT.replace(second=0)]
        
        ### No ProbSevere objects older than Latest Data Layer
        if len(groupDTList) == 0:
            return {}
        #print '\tgroupDTList', groupDTList
        latestGroupDT = max(groupDTList)# if len(groupDTList) else self.latestDLTDT
        #print'\tgroupDTList', groupDTList
        #print '\tlatestGroupDT', latestGroupDT
        #print'====================='
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
                print 'CR: Inavlid polygon, skipping...', row.get('objectids')
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

        #=======================================================================
        # for event in eventSet:
        #     eventCreationTime = event.getCreationTime()
        #     if eventCreationTime > latestDatetime:
        #        latestDatetime =  eventCreationTime
        #=======================================================================
               
        return latestDatetime
    
    def getCurrentEvents(self, eventSet):
        siteID = eventSet.getAttributes().get('siteID')        
        mode = eventSet.getAttributes().get('hazardMode', 'PRACTICE').upper()
        # Get current events from Session Manager (could include pending / potential)
        currentEvents = [event for event in eventSet]
        eventIDs = [event.getEventID() for event in eventSet]

        # Add in those from the Database
        databaseEvents = HazardDataAccess.getHazardEventsBySite(siteID, mode) 
        for event in databaseEvents:
            if event.getEventID() not in eventIDs:
                currentEvents.append(event)
                eventIDs.append(event.getEventID())
        return currentEvents

    def makeHazardEvent(self, ID, values):
        
        print '\n========= MAKING HAZARD EVENT ========'
        print '\t>>>>', ID, '<<<<\n'
        
        if values.get('belowThreshold'):
            print '\t', ID, 'Below Threshold', self.lowThreshold, 'returning None'
            return None
        
        sys.stdout.flush()
        probSevereTime = values.get('startTime', self.dataLayerTime)
        #dataLayerTimeMS = int(currentTime.strftime('%s'))*1000
        hazardEvent = EventFactory.createEvent()
        hazardEvent.setCreationTime(probSevereTime)
        self.setEventTimes(hazardEvent, values)
        
        hazardEvent.setHazardStatus("pending")
        hazardEvent.setHazardMode("O")        
        
        hazardEvent.setPhenomenon("Prob_Severe")
        
        polygon = loads(values.pop('polygons'))
        hazardEvent.setGeometry(polygon)
        hazardEvent.set('convectiveObjectDir', values.get('wdir'))
        hazardEvent.set('convectiveObjectSpdKts', values.get('wspd'))
        hazardEvent.set('probSeverAttrs',values)
        hazardEvent.set('objectID', ID)
        hazardEvent.setStatus('ISSUED')
        
        hazardEvent.set('automationLevel', 'automated')
        
        graphProbs = self.probUtils.getGraphProbs(hazardEvent, int(probSevereTime.strftime('%s'))*1000)
        hazardEvent.set('convectiveProbTrendGraph', graphProbs)
        
        return hazardEvent

    def setEventTimes(self, event, values):
        psStartTime = values.get('startTime', self.dataLayerTime)
        event.set('probSevereStartTime', TimeUtils.datetimeToEpochTimeMillis(psStartTime))
        psEndTime = psStartTime + datetime.timedelta(seconds=DEFAULT_DURATION_IN_SECS)
        event.set('probSevereEndTime', TimeUtils.datetimeToEpochTimeMillis(psEndTime)) 
        
        ### Per request from Greg
        event.setStartTime(self.latestDLTDT)
        
        endTime = event.getStartTime() + datetime.timedelta(seconds=DEFAULT_DURATION_IN_SECS)
        event.setEndTime(endTime)
        


    def updateEventGeometry(self, event, recommendedDict):
        try:
            newShape = loads(recommendedDict.get('polygons'))
            event.setGeometry(newShape)
        except:
            print 'ConvectiveRecommender: WHAT\'S WRONG WITH THIS POLYGON?', currID, type(recommended.get('polygons')), recommended.get('polygons')
            sys.stdout.flush()
        


    def updateUserOwned(self, event, recommended, dataLayerTimeMS):
        pass


    def updateAttributesAndMechanics(self, event, recommended, dataLayerTime):
        self.updateEventGeometry(event, recommended)
        self.updateAttributesOnly(event, recommended, dataLayerTime)


    def updateAttributesOnly(self, event, recommended, probSevereTime):
        manualAttrs = event.get('manualAttributes', [])
        ##################################################################
        ##  Need to handle special cases in computing updatedAttrs
        ##################################################################
        updatedAttrs = {k:v for k,v in recommended.iteritems() if k not in manualAttrs}
        probSevereAttrs = event.get('probSeverAttrs')
        
        #print '\n=================='
        #print 'CR - ManualAttrs:', manualAttrs
        #print 'CR - UpdatedAttrs:', updatedAttrs
        #print 'CR - probSevereAttrs1:', probSevereAttrs
        
        for k,v in updatedAttrs.iteritems():
                probSevereAttrs[k] = v

        #print 'CR - probSevereAttrs2:', probSevereAttrs
        #print '*******'
        
        event.set('probSeverAttrs',probSevereAttrs)
        
        
        ### Special Cases
        if 'convectiveObjectDir' not in manualAttrs:
            event.set('convectiveObjectDir', recommended.get('wdir'))
            
        if 'convectiveObjectSpdKts' not in manualAttrs:
            event.set('convectiveObjectSpdKts', recommended.get('wspd'))
            
        if 'convectiveProbTrendGraph' not in manualAttrs:
            graphProbs = self.probUtils.getGraphProbs(event, probSevereTime)
            event.set('convectiveProbTrendGraph', graphProbs)
            
        event.setStartTime(self.latestDLTDT)
        endTime = event.getStartTime() + datetime.timedelta(seconds=DEFAULT_DURATION_IN_SECS)
        event.setEndTime(endTime)

    def updateAutomated(self, event, recommended, probSevereTime):
        self.updateEventGeometry(event, recommended)
            
        event.set('convectiveObjectDir', recommended.get('wdir'))
        event.set('convectiveObjectSpdKts', recommended.get('wspd'))
        event.set('probSeverAttrs',recommended)
        
        if recommended.get('belowThreshold'):
            event.setStatus('PROPOSED')
        else:
            event.setStatus('ISSUED')
        
        graphProbs = self.probUtils.getGraphProbs(event, probSevereTime)
        event.set('convectiveProbTrendGraph', graphProbs)
        
        ### Per request from Greg
#        event.setStartTime(recommended.get('startTime', self.dataLayerTime))
        event.setStartTime(self.latestDLTDT)
        endTime = event.getStartTime() + datetime.timedelta(seconds=DEFAULT_DURATION_IN_SECS)
        event.setEndTime(endTime)
      
    ### Update the current events, and return a list of identifiers of
    ### events that are to be saved to the database.  
    def updateCurrentEvents(self, intersectionDict, mergedEvents):
        dataLayerTimeMS = int(self.dataLayerTime )
        
        identifiersOfEventsToSaveToDatabase = []
        
        for ID, vals in intersectionDict.iteritems():
            currentEvent = vals['currentEvent']

            print '\n!!!!!!!  ID[1]: ', ID, '>>>>', currentEvent.getStatus()
            if currentEvent.getStatus() == 'ELAPSED':
                print '\tMerging elapsed event'
                mergedEvents.add(currentEvent)
                continue
            
            ### DISCOVERED ON 20170218:
            ### If UI machine selects a hazard event AND hits Modify button,  
            ### then PROCESSOR machine receives activateModify=0.
            ### IF UI machine deselects hazard event, activateModify=True.
            ### I understand that the flags don't necessarily make sense,
            ### but it appears to be consistent and something we can use to
            ### tell the Convective Recommedner to bypass THIS event
            ### if THIS event activateModify=0
            if currentEvent.get('activateModify') == 0:
                print '\tNot updating this hazard event in Convective Recommender...'
                continue
            
            
            
            ### if we want to ensure a selected HE is not updated when the Conv Rec
            ### runs, we need to make sure this is working correctly.
            ### Right now, it will not recognize a HE that is selected on the UI machine
            ### (selected and activate are both always seen as False from the Conv Rec) 
            #===================================================================
            # editableHazard = self.isEditableSelected(currentEvent)
            # print '\n!!!!!!!  ID[1]: ', ID, editableHazard
            # if editableHazard:# and selectedHazard:
            #     print '\tSkipping Update!'
            #     continue
            #===================================================================

            
            recommendedAttrs = vals['recommendedAttrs']
            automationLevel = currentEvent.get('automationLevel')  ### TODO: determine default value
            
            if automationLevel == 'attributesOnly':
                self.updateAttributesOnly(currentEvent, recommendedAttrs, dataLayerTimeMS)
            
            if automationLevel == 'attributesAndMechanics':
                self.updateAttributesAndMechanics(currentEvent, recommendedAttrs, dataLayerTimeMS)
            
            if automationLevel == 'automated':
                
                ### Update the automated event, and if its status changed,
                ### add it to the list of events to be saved.
                self.updateAutomated(currentEvent, recommendedAttrs, dataLayerTimeMS)
                identifiersOfEventsToSaveToDatabase.append(currentEvent.getEventID())
            
            if automationLevel == 'userOwned':
                self.updateUserOwned(currentEvent, recommendedAttrs, dataLayerTimeMS)
                
            mergedEvents.add(currentEvent)
            
            return identifiersOfEventsToSaveToDatabase
            
            

    # Create an event set of new hazard events to be merged, together with
    # existing events that are to be elapsed. Return a tuple of three elements,
    # the first being a list of identifiers of events that are to be saved to
    # history, the second being a list of identifiers of events that are to be
    # saved to the database as latest versions, and the third being the event
    # set itself.
    def mergeHazardEventsNew2(self, currentEventsList, recommendedEventsDict):
        intersectionDict = {}
        recommendedObjectIDsList = sorted(recommendedEventsDict.keys())
        unmatchedEvents = []
        manualEventsList = []
        mergedEvents = EventSet(None)

        currentTimeMS = int(self.currentTime.strftime('%s'))*1000
        mergedEvents.addAttribute('currentTime', currentTimeMS)
        mergedEvents.addAttribute('trigger', 'autoUpdate')
        
        for currentEvent in currentEventsList:
            print '\n\t[1]:', currentEvent.get('objectID')

            if currentEvent.get('automationLevel') == 'userOwned':
                print 'Manual Event.  Storing and mocing on...'
                manualEventsList.append(currentEvent)
                continue
            
            currentEventObjectID = currentEvent.get('objectID')
            print '######### currentEventObjectID ######', currentEventObjectID
            #if currentEventObjectID in recommendedObjectIDsList:
            ### Account for prepended 'M' to automated events that are level 3 or 2 automation.
            if str(currentEventObjectID).endswith(tuple([str(z) for z in recommendedObjectIDsList])):
                ### If current event has match in rec event, add to dict for later processing
                ### should avoid 'userOwned' since they are filtered out with previous if statement
                rawRecommendedID = currentEventObjectID[1:] if str(currentEventObjectID).startswith('M') else currentEventObjectID
                print "\t#### rawRecommendedID", rawRecommendedID
                #pprint.pprint(recommendedEventsDict)
                intersectionDict[currentEventObjectID] = {'currentEvent': currentEvent, 'recommendedAttrs': recommendedEventsDict[rawRecommendedID]}
                
                ### Remove ID from rec list so remaining list is "newOnly"
                recommendedObjectIDsList.remove(rawRecommendedID)
            else:
                print '\t!!!!!!!  ELAPSING   !!!!!'
                currentEvent.setStatus('ELAPSED')

            mergedEvents.add(currentEvent)

        ### Update the current events with the attributes of the recommended events.
        ### This returns a list of identifiers of events that are to be saved to the
        ### database (not history list).  
        #identifiersOfEventsToSaveToDatabase = self.updateCurrentEvents(intersectionDict, mergedEvents, currentTime)
        identifiersOfEventsToSaveToDatabase = self.updateCurrentEvents(intersectionDict, mergedEvents)
        
        # Create a list of hazard event identifiers that are to be saved
        # to the history list.
        identifiersOfEventsToSaveToHistory = []
        
        ### Loop through remaining/unmatched recommendedEvents
        ### if recommended geometry overlaps an existing *manual* geometry
        ### ignore it. 
        for recID in recommendedObjectIDsList:
            recommendedValues = recommendedEventsDict[recID]
            recommendedEvent = None

            if len(manualEventsList) == 0:
                
                ### If an event is created, add it to the event set and add
                ### it to the list of events to be saved to history.
                print '1111111: Calling makeHazardEvent for:', recID
                recommendedEvent = self.makeHazardEvent(recID, recommendedValues)
                
            else:
                ### Get recommended geometry
                recGeom = loads(recommendedValues.get('polygons'))
                for event in manualEventsList:
                    mergedEvents.add(event)
                    evtGeom = event.getGeometry().asShapely()
                    ### if the geometries DO NOT intersect, add recommended
                    if not evtGeom.intersects(recGeom):
                	print '2222222: Calling makeHazardEvent for:', recID
                    recommendedEvent = self.makeHazardEvent(recID, recommendedValues)

            if recommendedEvent:
                mergedEvents.add(recommendedEvent)
                identifiersOfEventsToSaveToHistory.append(recommendedEvent.getEventID())

        for e in mergedEvents:
           print '%%%%%:', e.get('objectID'), e.getStatus()
                    
        return identifiersOfEventsToSaveToHistory, identifiersOfEventsToSaveToDatabase, mergedEvents
    
#    def mergeHazardEventsNew(self, currentEventsList, recommendedEventsDict):
#        intersectionDict = {}
#        recommendedObjectIDsList = sorted(recommendedEventsDict.keys())
#        automatedCurrentOnlyList = []
#        manualList = []
#        mergedEvents = EventSet(None)
#
#        currentTimeMS = int(self.currentTime.strftime('%s'))*1000
#        mergedEvents.addAttribute('currentTime', currentTimeMS)
#        mergedEvents.addAttribute('trigger', 'autoUpdate')
#        
#        
#        ### First, find the overlap between currentEvents and recommended events
#        for currentEvent in currentEventsList:
#
#            if currentEvent.get('automationLevel') == 'userOwned':
#                manualList.append(currentEvent)
#                continue
#            
#            currentEventObjectID = currentEvent.get('objectID')
#            print '######### currentEventObjectID ######', currentEventObjectID
#            #if currentEventObjectID in recommendedObjectIDsList:
#            ### Account for prepended 'M' to automated events that are level 3 or 2 automation.
#            if str(currentEventObjectID).endswith(tuple([str(z) for z in recommendedObjectIDsList])):
#                ### If current event has match in rec event, add to dict for later processing
#                ### should avoid 'userOwned' since they are filtered out with previous if statement
#                rawRecommendedID = currentEventObjectID[1:] if str(currentEventObjectID).startswith('M') else currentEventObjectID
#                print "\t#### rawRecommendedID", rawRecommendedID
#                #pprint.pprint(recommendedEventsDict)
#                intersectionDict[currentEventObjectID] = {'currentEvent': currentEvent, 'recommendedAttrs': recommendedEventsDict[rawRecommendedID]}
#                
#                ### Remove ID from rec list so remaining list is "newOnly"
#                recommendedObjectIDsList.remove(rawRecommendedID)
#            else:
#                automatedCurrentOnlyList.append(currentEvent)
#                
#
#        
#        
#        ### Loop through remaining/unmatched recommendedEvents
#        ### if recommended geometry overlaps an existing *manual* geometry
#        ### ignore it. 
#        for recID in recommendedObjectIDsList:
#            recommendedValues = recommendedEventsDict[recID]
#            ### Get recommended geometry
#            recGeom = loads(recommendedValues.get('polygons'))
#            
#
#            if len(automatedCurrentOnlyList) == 0:
#                recommendedEvent = self.makeHazardEvent(recID, recommendedValues)
#                if recommendedEvent:
#                    mergedEvents.add(recommendedEvent)
#                
#            else:
#                ### The only events left in this list should be
#                ###  1) those that are full manual
#                ###  2) formerly automated at some level but no longer have
#                ###     a corresponding ProbSevere ID and should be "removed".
#                for event in automatedCurrentOnlyList:
#                    print 'vvvvvv ', event.get('objectID'), ' vvvvvvvv'
#		    print '\t', event.get('probabilities'), event.get('automationLevel'), 'userOwned' not in event.get('automationLevel')
#                    print '\tSETTING TO ELAPSED'
#                    event.setStatus('ELAPSED')
#                    print '\t\tstatus update 1',event.getStatus()
#                        ### userOwned events get precedent over automated
#                    else:
#                        ### Add the userOwned geometry to the EventSet
#                        evtGeom = event.getGeometry().asShapely()
#                        ### if the geometries DO NOT intersect, add recommended
#                        if not evtGeom.intersects(recGeom):
#                            recommendedEvent = self.makeHazardEvent(recID, recommendedValues)
#                            if recommendedEvent:
#                                mergedEvents.add(recommendedEvent)
#                    print '\t\tUPDATED STATUS',event.getStatus()
#                    mergedEvents.add(event)
#                    
#        return mergedEvents
        
    def isEditableSelected(self, event):
        selected = event.get('selected', False)
        #print 'CR [FARNSWORTH] ID:', event.get('objectID')
        #print "CR [FARNSWORTH] isEditable  selected, activate", selected, event.get('activate', False)
        #print "CR [FARNSWORTH] isEditable automationLevel, status", event.get('automationLevel'), event.getStatus()
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


