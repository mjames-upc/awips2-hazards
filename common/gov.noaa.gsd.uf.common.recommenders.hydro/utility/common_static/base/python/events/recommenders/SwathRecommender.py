'''
Swath recommender for probabilistic hazard types.
'''
import datetime, math
import EventFactory, EventSetFactory, GeometryFactory
import RecommenderTemplate
from VisualFeatures import VisualFeatures
import logging, UFStatusHandler

import math, time
from math import *
import shapely
import shapely.ops as so
import shapely.geometry as sg
import shapely.affinity as sa
from inspect import currentframe, getframeinfo
import os, sys
from java.util import Date
import numpy as np


import JUtil     

#===============================================================================
# Users wanting to add new storm paths should enter a new method in this class
# with the same name as one of the choices found in 
# CommonMetaData.py:_getConvectiveSwathPresets()
# and using the same arguments to the new method in this class as the existing
# methods.
#
# For example, if I want to add a new path named 'foo', I would add 'foo'
# as a choice in CommonMetaData.py:_getConvectiveSwathPresets()
# and a method below as:
#
# def foo(self, speedVal, dirVal, spdUVal, dirUVal, step, numIvals):
#
# which would do some calculations based on the values passed in and
# then return a dictionary:
# 
#        returnDict = {
#                      'speedVal':speedVal, 
#                      'dirVal':dirVal, 
#                      'spdUVal':spdUVal,
#                      'dirUVal':dirUVal
#                      }
#
# 
#===============================================================================
class SwathPreset(object):
    def __init__(self):
        pass
    
    def NoPreset(self, speedVal, dirVal, spdUVal, dirUVal, step, numIvals):
        returnDict = {
                      'speedVal':speedVal,
                      'dirVal':dirVal, 
                      'spdUVal':spdUVal,
                      'dirUVal':dirUVal
                      }
        return returnDict
    
    def RightTurningSupercell(self, speedVal, dirVal, spdUVal, dirUVal, step, numIvals): 
        spdWt = 0.75
        dirWtTup = (0.,30.)
        dirWt = dirWtTup[1] * step / numIvals
        dirVal = dirVal + dirWt
        speedVal =  speedVal * spdWt
      
        returnDict = {
                      'speedVal':speedVal, 
                      'dirVal':dirVal, 
                      'spdUVal':spdUVal,
                      'dirUVal':dirUVal
                      }
        return returnDict
    
    def LeftTurningSupercell(self, speedVal, dirVal, spdUVal, dirUVal, step, numIvals): 
        spdWt = 0.75
        dirWtTup = (0.,30.)
        dirWt = -1. * dirWtTup[1] * step / numIvals
        dirVal = dirVal + dirWt
        speedVal = speedVal * spdWt
        
        returnDict = {
                      'speedVal':speedVal, 
                      'dirVal':dirVal, 
                      'spdUVal':spdUVal,
                      'dirUVal':dirUVal
                      }
        return returnDict
    
    def BroadSwath(self, speedVal, dirVal, spdUVal, dirUVal, step, numIvals):
        spdUValWtTup = (0.,15)
        dirUValWtTup = (0.,40)
        spdUValWt = spdUValWtTup[1] * step / numIvals
        spdUVal = spdUVal + (spdUValWtTup[1] - spdUValWt)
        dirUValWt = dirUValWtTup[1] * step / numIvals
        dirUVal = dirUVal + (dirUValWtTup[1] - dirUValWt)

        returnDict = {
                      'speedVal':speedVal, 
                      'dirVal':dirVal, 
                      'spdUVal':spdUVal,
                      'dirUVal':dirUVal
                      }
        return returnDict
    
    def LightBulbSwath(self, speedVal, dirVal, spdUVal, dirUVal, step, numIvals):
        spdUValWtTup = (0.,7)
        dirUValWtTup = (0.,20)
        spdUValWt = spdUValWtTup[1] * step / numIvals
        spdUVal = spdUVal + (spdUValWtTup[0] + spdUValWt)
        dirUValWt = dirUValWtTup[1] * step / numIvals
        dirUVal = dirUVal + (dirUValWtTup[0] + dirUValWt)

        returnDict = {
                      'speedVal':speedVal, 
                      'dirVal':dirVal, 
                      'spdUVal':spdUVal,
                      'dirUVal':dirUVal
                      }
        return returnDict
    
class PolygonFeature:
    def __init__(self, polygon, start, end, prob):
        pass
    
         
class Recommender(RecommenderTemplate.Recommender):
    
    def __init__(self):
        self.logger = logging.getLogger('SwathRecommender')
        self.logger.addHandler(UFStatusHandler.UFStatusHandler(
            'gov.noaa.gsd.common.utilities', 'SwathRecommender', level=logging.INFO))
        self.logger.setLevel(logging.INFO)
        

    def defineScriptMetadata(self):
        '''
        @return: A dictionary containing information about this
                 tool
        '''
        metadata = {}
        metadata['toolName'] = 'Swath Recommender'
        metadata['author'] = 'GSD'
        metadata['version'] = '1.0';
        metadata['description'] = '''
        '''
        metadata['eventState'] = 'Pending'
        metadata['onlyIncludeTriggerEvent'] = True
        
        # This tells Hazard Services to not notify the user when the recommender
        # creates no hazard events. Since this recommender is to be run in response
        # to hazard event changes, etc. it would be extremely annoying for the user
        # to be constantly dismissing the warning message dialog if no hazard events
        # were being created. 
        metadata['background'] = True
        
        return metadata

    def defineDialog(self, eventSet):
        '''
        @return: A dialog definition to solicit user input before running tool
        '''   
        return None
            
    def execute(self, eventSet, dialogInputMap, spatialInputMap):
        '''
        Runs the Swath Recommender tool
        
        @param eventSet: A set of events which include session
                         attributes
        @param dialogInputMap: A map of information retrieved from
                               a user's interaction with a dialog.
        @param spatialInputMap:   A map of information retrieved
                                  from the user's interaction with the
                                  spatial display.
        
        @return: A list of potential probabilistic hazard events. 
        '''


        '''  
        Event set attributes that recommenders have access to:
          - currentTime (millis?)
          - selectedTime
          - framesInfo (dictionary)
          - siteID
          - hazardMode (practice, operational, etc.)

          - trigger: one of the strings "none", "hazardTypeFirst", "hazardEventModification", 
             "hazardEventVisualFeatureChange", or "timeInterval"
          - eventType: gives event type if trigger is "hazardTypeFirst"
          - eventIdentifier: only supplied if trigger is "hazardEventModification" or "hazardEventVisualFeatureChange"
          - attributeIdentifiers: only supplied if trigger is 
               "hazardEventModification" -- one or more hazard attributes.  
                   Could be: "timeRange", "geometry", "visualFeature", and "status" 
                        (indicated under "modifyRecommenders" in HazardTypes.py)
                   Plus generic hazard attributes that have been marked with "modifyRecommender" in the MetaData 
               "hazardEventVisualFeatureChange" -- a set of visual feature identifiers
           - origin: one of strings "user" or "other"
           
        
        The SwathRecommender is called in the following ways:
           - "hazardEventModification" with origin of User or Other
               There will be exactly one event in the eventSet to process
           - "hazardVisualFeatureChange" with origin of User
               There will be exactly one event in the eventSet to process
           - "timeInterval" with origin of PHI_GridRecommender  
               There can be multiple events in the eventSet to process  
               
        Data structures kept for each hazard event :  parallel lists of [poly] and [startTime, endTime]
          (NOTE: parallel lists are necessary because of a restriction in storing of hazard attributes.)
            downstreamPolys, downstreamTimes - current time polygon plus downstream polygons.     
            historyPolys, historyTimes - former downstream polygons that are now past the current time     
            motionVectorPolys, motionVectorTimes - those polygons that are to be included in 
                    the calculation of motion vector.  This includes the initial polygon
                    plus any that the user has nudged
            On creation of the hazard event, the hazard geometry is included in 
                 BOTH the downstreamPolys and the motionVectorPolys.
                 However, as time marches forward, the current time polygon will only be added to 
                 the motionVectorPolys if it is nudged by the user.
            
        All times are in millis past the epoch.
        The current time polygon is also stored in first class event geometry. 
  
        '''
                
        self._setPrintFlags()
        self._printEventSet("\nSwathRecommender", eventSet, eventLevel=1)
                
        self.sp = SwathPreset()        
        eventSetAttrs = eventSet.getAttributes()
        trigger = eventSetAttrs.get('trigger')
        self._currentTime = long(eventSetAttrs.get("currentTime"))

        resultEventSet = EventSetFactory.createEventSet(None)
        
        for event in eventSet:  
            # FIXME kludge since can't figure out why product generation is not setting the issueTime
            #  Need this set in order to Issue                               
            event.set('issueTime', self._currentTime)
             
            # If called from PHI_GridRecommender cycle through all events, 
            #  otherwise we are just processing one event
            if trigger in ['hazardEventModification', 'hazardEventVisualFeatureChange']:
                eventIdentifier = eventSetAttrs.get('eventIdentifier')            
                if eventIdentifier and eventIdentifier != event.getEventID():
                    continue
                
            if event.getStatus() in ['ELAPSED', 'ENDED']:
                continue
                        
            # Set userOwned and round start / end times to nearest minute
            #   This event will subsequently be ignored by the ConvectiveRecommender
            if eventSetAttrs.get('origin') == 'user':
                event.set('userOwned', True)                
            self._roundEventTimes(event, self._currentTime)
            
            self._printEvent("Before Update", event) 

            self._initializeMotionVectorPolys(event)
            self._advanceDownstreamPolys(event)                       
            # Make updates to motion vector, duration due to user adjustments to 
            #  hazard attributes or 'nudging'
            if trigger == 'hazardEventModification' and not self._needUpdate(event): 
                continue
            if trigger == 'hazardEventVisualFeatureChange':                 
                self._adjustForVisualFeatureChange(event, eventSetAttrs)
                                                
            # Create Interval Polygons -- Input is motion vector and current time polygon
            self._createIntervalPolys(event, eventSetAttrs, timeDirection="downstream")
            if self._firstTimeHazard:
                self._createIntervalPolys(event, eventSetAttrs, timeDirection="upstream")
                                            
            self._setVisualFeatures(event)
            resultEventSet.add(event)                    
            self._printEvent("After Update", event)  
                                                                        
        return resultEventSet


    ##############################################
    # Downstream and current polygon bookkeeping #
    ##############################################
    
    def _initializeMotionVectorPolys(self, event):
        # Initialize with current time polygon
        mvPolys = event.get('motionVectorPolys')
        if not mvPolys:
            self._firstTimeHazard = True
            mvPolys = [event.getGeometry()]
            st = self._convertFeatureTime(self._currentTime, 0)
            et = self._convertFeatureTime(self._currentTime, self._timeStep())
            mvTimes = [(st, et)]
            event.set('motionVectorPolys', mvPolys)
            event.set('motionVectorTimes', mvTimes)
        else:
            self._firstTimeHazard = False
            
    def _advanceDownstreamPolys(self, event):
        ''' 
        As currentTime progresses, move downstreamPolys to historyPolys
        Then update the event geometry with new initial polygon
        
        '''
        downstreamPolys = event.get('downstreamPolys', [])
        downstreamTimes = event.get('downstreamTimes', [])
        if not downstreamPolys or not downstreamTimes:
            return
        index = 0
        print "downstreamPolys", downstreamPolys
        print "downstreamTimes", downstreamTimes
        self.flush()
        for i in range(len(downstreamTimes)):
            st, et = downstreamTimes[i]
            if st >= self._currentTime:
                index = i
                break
        event.set('downstreamPolys', downstreamPolys[index:])
        event.set('downstreamTimes', downstreamTimes[index:])
        self._setEventGeometry(event, downstreamPolys[0])
        historyPolys = event.get('historyPolys', [])
        historyTimes = event.get('historyTimes', [])
        newHistPolys = historyPolys + downstreamPolys[:index]
        newHistTimes = historyTimes + downstreamTimes[:index]
        event.set('historyPolys', newHistPolys)
        event.set('historyTimes', newHistTimes)
         
    def _setEventGeometry(self, event, polygon):
        polygon = self._reducePolygon(polygon)
        event.setGeometry(polygon)
        
    def _reducePolygon(self, initialPoly):
        numPoints = self._hazardPointLimit()
        tolerance = 0.001
        newPoly = initialPoly.simplify(tolerance, preserve_topology=True)
        while len(newPoly.exterior.coords) > numPoints:
            tolerance += 0.001
            newPoly = initialPoly.simplify(tolerance, preserve_topology=True)

        return newPoly    

    def _relocatePolygon(self, newCentroid, initialPoly):
        if isinstance(initialPoly, shapely.geometry.collection.GeometryCollection):
            initialPoly = initialPoly.geoms[0]
        initialPoly_gglGeom = so.transform(self._c4326t3857, initialPoly)
        newCentroid_gglGeom = so.transform(self._c4326t3857, newCentroid)
        xdis = newCentroid_gglGeom.x-initialPoly_gglGeom.centroid.x
        ydis = newCentroid_gglGeom.y-initialPoly_gglGeom.centroid.y
        newPoly_gglGeom = sa.translate(initialPoly_gglGeom,\
                                                xoff=xdis, yoff=ydis)
        newPoly = so.transform(self._c3857t4326, newPoly_gglGeom)
        return newPoly

    ##################################
    # Check for Update of Attributes #
    ##################################

    def _needUpdate(self, event):
        update = False
        
        # Compare the previous values to the new values to determine if any changes are necessary.  
        # Update the previous values along the way.
        triggerCheckList = ['convectiveObjectSpdKtsUnc', 'convectiveObjectDirUnc', 
                            'convectiveObjectDir', 'convectiveObjectSpdKts', 'convectiveSwathPresets', 
                            'duration']
        attrs = event.getHazardAttributes()
        
        newTriggerAttrs = {t:attrs.get(t) for t in triggerCheckList}
        newTriggerAttrs['duration'] = self._calcEventDuration(event)
        
        # Get the values from the MetaData. These should supercede and update the ones stored in the event
        self._updateConvectiveAttrs(event) 
                
        for t in triggerCheckList:
            prevName = "prev_"+t
            prevVal = event.get(prevName)
            newVal = newTriggerAttrs.get(t)
            # Test to see if there is a change
            if prevVal != newVal:
                update = True
            # Update previous value
            event.set(prevName, newVal)
                    
        return update

    def _calcEventDuration(self, event):
        startTime = event.getStartTime()
        endTime = event.getEndTime()
        durationMinutes = int(round((endTime-startTime).total_seconds() / 60))
        return durationMinutes
    
    def _updateConvectiveAttrs(self, event):
        convectiveAttrs = event.get('convectiveAttrs')
        if not convectiveAttrs:
            return
        if 'wdir' in convectiveAttrs:
            event.set('convectiveObjectDir', convectiveAttrs['wdir'])
        if 'wspd' in convectiveAttrs:
            event.set('convectiveObjectSpdKts', convectiveAttrs['wspd'])

    ###############################
    # Compute Motion Vector       #
    ###############################

    def computeMotionVector(self, polygonTuples, currentTime, defaultSpeed=32, defaultDir=270):
        '''
        polygonTuples is a list of tuples expected as:
        [(poly1, startTime1), (poly2, startTime2),,,,(polyN, startTimeN)]
        '''
        meanU = None
        meanV = None
        uList = []
        vList = []

        ### Sort polygonTuples by startTime
        sortedPolys = sorted(polygonTuples, key=lambda tup: tup[1])

        ### Get create sorted list of u's & v's
        for i in range(len(sortedPolys)):
            if i == 0:
                ### Use default motionVector. 
                ### Note, need to invert dir since Meteorological winds
                ### by definition are entered in as *from*
                u, v = self.get_uv(defaultSpeed, (180+defaultDir)%360)
                speed = defaultSpeed
            else: ### use i, i-1 pair
                p1 = sortedPolys[i-1][0]
                t1 = sortedPolys[i-1][1]
                p2 = sortedPolys[i][0]
                t2 = sortedPolys[i][1]
                dist = self.getHaversineDistance(p1, p2)
                speed = dist/ ((t2-t1)/ 1000)
                bearing = self.getBearing(p1, p2)
                u, v = self.get_uv(speed, bearing)

            uList.append(u)
            vList.append(v)


        uStatsDict = self.weightedAvgAndStdDev(uList)
        vStatsDict = self.weightedAvgAndStdDev(vList)

        meanU = uStatsDict['weightedAverage']
        stdU = uStatsDict['stdDev']

        meanV = vStatsDict['weightedAverage']
        stdV = vStatsDict['stdDev']

        meanDir = 270 - (atan2(meanV,meanU) * (180 / math.pi))
        meanSpd = math.sqrt(meanU**2 + meanV**2)

        stdDir = atan2(stdV,stdU) * (180 / math.pi)
        stdSpd = math.sqrt(stdU**2 + stdV**2)

        return {
                'convectiveObjectDir' : meanDir,
                'convectiveObjectDirUnc' : stdDir,
                'convectiveObjectSpdKts' : meanSpd*1.94384,
                'convectiveObjectSpdKtsUnc' : stdSpd*1.94384
                }


         
    def XX_computeMotionVector(self, polygonTuples, currentTime, defaultSpeed=32, defaultDir=270):
        '''
        polygonTuples is a list of tuples expected as:
        [(poly1, startTime1), (poly2, startTime2),,,,(polyN, startTimeN)]
        '''
        meanU = None
        meanV = None
        uList = []
        vList = []

        ### Sort polygonTuples by startTime
        sortedPolys = sorted(polygonTuples, key=lambda tup: tup[1])

        ### Get create sorted list of u's & v's
        for i in range(len(sortedPolys)):
            if i == 0:
                ### Use default motionVector. 
                ### Note, need to invert dir since Meteorological winds
                ### by definition are entered in as *from*
                speed = defaultSpeed*0.514444
                bearing = (180+defaultDir)%360
                u, v = self.get_uv(speed, bearing)
            else: ### use i, i-1 pair
                p1 = sortedPolys[i-1][0]
                t1 = sortedPolys[i-1][1]
                p2 = sortedPolys[i][0]
                t2 = sortedPolys[i][1]
                dist = self.getHaversineDistance(p1, p2)
                speed = dist/((t2-t1)/1000)
                bearing = self.getBearing(p1, p2)
                u, v = self.get_uv(speed, bearing)

            uList.append(u)
            vList.append(v)

        uStatsDict = self.weightedAvgAndStdDev(uList)
        vStatsDict = self.weightedAvgAndStdDev(vList)

        meanU = uStatsDict['weightedAverage']
        stdU = uStatsDict['stdDev']

        meanV = vStatsDict['weightedAverage']
        stdV = vStatsDict['stdDev']

        meanDir = 270 - (atan2(meanV,meanU) * (180 / math.pi))
        meanSpd = math.sqrt(meanU**2 + meanV**2)

        stdDir = atan2(stdV,stdU) * (180 / math.pi)
        stdSpd = math.sqrt(stdU**2 + stdV**2)

        return {
                'convectiveObjectDir' : meanDir,
                'convectiveObjectDirUnc' : stdDir,
                'convectiveObjectSpdKts' : meanSpd*1.94384,
                'convectiveObjectSpdKtsUnc' : stdSpd*1.94384
                }    

    def weightedAvgAndStdDev(self, xList):
        arr = np.array(xList)
        weights = range(1,len(arr)+1)
        weightedAvg = np.average(arr, weights=weights)
        weightedVar = np.average((arr-weightedAvg)**2, weights=weights)
        return {'weightedAverage':weightedAvg, 'stdDev': math.sqrt(weightedVar)}
        return (180+bearing) % 360 ### Wind comes *from*

    def get_uv(self, Spd, DirGeo):
        '''
        from https://www.eol.ucar.edu/content/wind-direction-quick-reference
        '''
        RperD = (math.pi / 180)
        Ugeo = (-1*Spd) * sin(DirGeo * RperD)
        Vgeo = (-1*Spd) * cos(DirGeo * RperD)
        return Ugeo, Vgeo

    def weightedMean(self, xList):
        numerator = 0
        denomenator = 0
        for i, val in enumerate(xList):
            numerator += ((i+1)*val)
            #print '\tnum:', numerator, val
            denomenator += (i+1)
        #print 'Num:', numerator
        #print 'Den:', denomenator
        return numerator/denomenator


    def getHaversineDistance(self, poly1, poly2):
        """
        Calculate the great circle distance between two points 
        on the earth (specified in decimal degrees)
        """
        lat1 = poly1.centroid.y
        lon1 = poly1.centroid.x
        lat2 = poly2.centroid.y
        lon2 = poly2.centroid.x
        # convert decimal degrees to radians 
        lon1, lat1, lon2, lat2 = map(math.radians, [lon1, lat1, lon2, lat2])
        # haversine formula 
        dlon = lon2 - lon1
        dlat = lat2 - lat1
        a = math.sin(dlat/2)**2 + math.cos(lat1) * math.cos(lat2) * math.sin(dlon/2)**2
        c = 2 * math.asin(math.sqrt(a))
        meters = 6367000 * c # 6367000 Earth's radius in meters
        return meters

    def getBearing(self, poly1, poly2):
        lat1 = poly1.centroid.y
        lon1 = poly1.centroid.x
        lat2 = poly2.centroid.y
        lon2 = poly2.centroid.x
        # convert decimal degrees to radians 
        lon1, lat1, lon2, lat2 = map(math.radians, [lon1, lat1, lon2, lat2])

        bearing = atan2(sin(lon2-lon1)*cos(lat2), cos(lat1)*sin(lat2)-sin(lat1)*cos(lat2)*cos(lon2-lon1))
        bearing = degrees(bearing)
        bearing = (bearing + 360) % 360
        return bearing

    def get_uv(self, speed, direction):
        """
        Given speed and direction (degrees oceanographic),
        return u (zonal) and v (meridional) components.
        """
        u = speed * self.sind(direction)
        v = speed * self.cosd(direction)
        return u, v

    def sind(self, x):
        return sin(radians(x))

    def cosd(self, x):
        return cos(radians(x))
    

    ######################################################
    # Compute Interval (downstream and upstream Polygons #
    ######################################################
            
    def _createIntervalPolys(self, event, eventSetAttrs, timeDirection='downstream'):
        '''
        This method creates the downstream or upstream polygons given 
          -- current time polygon 
          -- a direction and direction uncertainty
          -- a speed and a speed uncertainty
          -- a Preset Choice
                
        From the downstreamPolys, the visualFeatures (swath, trackpoints, and upstream polygons)
        can be derived.
        
        '''
        attrs = event.getHazardAttributes()

        # Set up direction and speed values
        ### get dir
        dirVal = attrs.get('convectiveObjectDir')
        if not dirVal:
            dirVal = self._defaultWindDir()
        dirVal = int(dirVal)
        ### get dirUncertainty (degrees)
        dirUVal = attrs.get('convectiveObjectDirUnc')
        if dirUVal:
            dirUVal = int(dirUVal)
        else:
            dirUVal = 10
        ### get speed
        speedVal = attrs.get('convectiveObjectSpdKts')
        if not speedVal:
            speedVal = self._defaultWindSpeed()
        speedVal = int(speedVal)
        # get speedUncertainty
        spdUVal = attrs.get('convectiveObjectSpdKtsUnc')
        if spdUVal:
            spdUVal = int(spdUVal)
        else:
            spdUVal = 10
                            
        ### Get initial polygon.  
        # This represents the polygon at the current time resulting from the last nudge.
        poly = event.getGeometry()
        
        ### Check if single polygon or iterable -- no longer need this because swath is a
        # visual feature.
        if hasattr(poly,'__iter__'):
            poly = poly[0]
            
        presetChoice = attrs.get('convectiveSwathPresets') if attrs.get('convectiveSwathPresets') is not None else 'NoPreset'
        presetMethod = getattr(self.sp,presetChoice )
                
        ### convert poly to Google Coords to make use of Karstens' code
        fi_filename = os.path.basename(getframeinfo(currentframe()).filename)
        total = time.time()
        gglPoly = so.transform(self._c4326t3857,poly)
        
        ### calc for intervals (default time step) over duration
        if timeDirection == 'downstream':
            ### get duration (in seconds)
            durationSecs = int(round((event.getEndTime()-event.getStartTime()).total_seconds()))
            numIvals = int(durationSecs/self._timeStep())
        else:
            numIvals = self._upstreamTimeSteps()
            
        intervalPolys = []
        intervalTimes = []
        self.flush()
        for step in range(numIvals):
            if timeDirection == 'upstream':
                increment = -1*(step + 1)
            else:
                increment = step
            origDirVal = dirVal
            presetResults = presetMethod(speedVal, dirVal, spdUVal, dirUVal, step, numIvals)
            secs = increment * self._timeStep()
            gglDownstream = self._downstream(secs,
                                            presetResults['speedVal'],
                                            presetResults['dirVal'],
                                            presetResults['spdUVal'],
                                            presetResults['dirUVal'],
                                            origDirVal,
                                            gglPoly)
            intervalPoly = so.transform(self._c3857t4326, gglDownstream)
            intervalPoly = self._reducePolygon(intervalPoly)
            intervalPolys.append(intervalPoly)
            st = self._convertFeatureTime(self._currentTime, secs)
            et = self._convertFeatureTime(self._currentTime, secs+self._timeStep())
            intervalTimes.append((st, et))
                    
        if timeDirection == 'downstream':
            event.addHazardAttribute('downstreamPolys',intervalPolys)       
            event.addHazardAttribute('downstreamTimes',intervalTimes) 
        else:
            self._upstreamPolys = intervalPolys
            self._upstreamTimes = intervalTimes      
        #print '[', fi_filename, getframeinfo(currentframe()).lineno,'] === FINAL took ', time.time()-total, 'seconds for ', event.get('objectID'), ' ==='

    def _downstream(self, secs, speedVal, dirVal, spdUVal, dirUVal, origDirVal, threat):
        speedVal = float(speedVal)
        dirVal = float(dirVal)
        dis = secs * speedVal * 0.514444444
        defaultDir = float(self._defaultWindDir())
        xDis = dis * math.cos(math.radians(defaultDir - dirVal))
        yDis = dis * math.sin(math.radians(defaultDir - dirVal))
        xDis2 = secs * spdUVal * 0.514444444
        yDis2 = dis * math.tan(math.radians(dirUVal))
        threat = sa.translate(threat,xDis,yDis)
    
        if origDirVal:
            rot = origDirVal - dirVal
            threat = sa.rotate(threat,rot,origin='centroid')
            #rotVal = -1 * (270 - dirVal)
            #if rotVal > 0:
            #    rotVal = -1 * (360 - rotVal)
            #print '\tRotVal1:', rotVal
            #threat = sa.rotate(threat,rotVal,origin='centroid')
        coords = threat.exterior.coords
        center = threat.centroid
        newCoords = []
        for c in coords:
                dir = math.atan2(c[1] - center.y,c[0] - center.x)
                x = math.cos(dir) * xDis2
                y = math.sin(dir) * yDis2
                p = sg.Point(c)
                c2 = sa.translate(p,x,y)
                newCoords.append((c2.x,c2.y))
        threat = sg.Polygon(newCoords)
        #if origDirVal:
        #    rotVal = 270 - origDirVal
        #    if rotVal < 0:
        #        rotVal = rotVal + 360
        #    print '\tRotVal2:', rotVal
        #    threat = sa.rotate(threat,rotVal,origin='centroid')
        return threat
    
    
    def _c4326t3857(self, lon, lat):
        """
        Pure python 4326 -> 3857 transform. About 8x faster than pyproj.
        """
        lat_rad = math.radians(lat)
        xtile = lon * 111319.49079327358
        ytile = math.log(math.tan(lat_rad) + (1 / math.cos(lat_rad))) / \
            math.pi * 20037508.342789244
        return(xtile, ytile)
    
    
    def _c3857t4326(self, lon, lat):
        """
        Pure python 3857 -> 4326 transform. About 12x faster than pyproj.
        """
        xtile = lon / 111319.49079327358
        ytile = math.degrees(
            math.asin(math.tanh(lat / 20037508.342789244 * math.pi)))
        return(xtile, ytile)


    ###############################
    # Visual Features and Nudging #
    ###############################
        '''
        Overall rules for display and editing:
       
        Hazard Event selected:
            Swath / track points appear 
            If selected time is at current time:
                Current time polygon is displayed. Editable: dragged or re-shaped to add to history.
            If selected time is prior to current time:
                Relocated polygon is displayed. Editable: dragged to add to the history.
            If selected time is past current time:
                Downstream polygon displayed. Not editable.
                Relocated polygon. Not editable.
        Hazard Event NOT selected:
            Swath / track do not appear
            If selected time is at or past current time, current time or downstream polygon is displayed
            If selected time is prior to current time, upstream relocated polygon is displayed
        '''
    
    def _adjustForVisualFeatureChange(self, event, eventSetAttrs):
        '''
        Based on the visual feature change, update the motion vector
          
        attributeIdentifiers for edited features could be:
            upstream"+str(st))  (Dragged upstream) OR
            downstream_ (Dragged and/or re-shaped current time polygon)
            
        Event geometry contains current time polygon        
        '''
        # Add the visualFeature change to the history list
        motionVectorPolys = event.get('motionVectorPolys', []) 
        motionVectorTimes = event.get('motionVectorTimes', [])
        # Convert to list of tuples (poly, st, et) for processing 
        motionVectorTuples = []
        for i in range(len(motionVectorPolys)):
            poly = motionVectorPolys[i]
            st, et = motionVectorTimes[i]
            motionVectorTuples.append((poly, st, et))
            
        selectedFeatures = event.getSelectedVisualFeatures()
        baseFeatures = event.getBaseVisualFeatures()
        features = selectedFeatures + baseFeatures
        # We are only changing one attribute per SwathRecommender execution
        featureChanged = list(eventSetAttrs.get('attributeIdentifiers'))[0]

        eventSt_ms = long(self._datetimeToMs(event.getStartTime()))
        newMotionVector = []
        for feature in features:
            featureIdentifier = feature.get('identifier')
            # Find the feature that has changed
            if featureIdentifier == featureChanged:
                newMotionVector = []
                # Get feature polygon
                polyDict = feature["geometry"]
                #  This will work because we only have one polygon in our features
                #  TODO refactor to have multiple polygons per feature
                for timeBounds, geometry in polyDict.iteritems():
                    featureSt, featureEt = timeBounds
                    featureSt = long(featureSt)
                    featurePoly = geometry
                # Look for a motionVectorPoly that has the same start time as the 
                #  changed feature and replace it 
                found = False
                for histPoly, histSt, histEt in motionVectorTuples:
                    if abs(histSt-featureSt) <= 50*1000:                        
                        newMotionVector.append((featurePoly, histSt, histEt))
                        found = True
                        if histSt == eventSt_ms:
                            # Store the current time polygon in the event geometry
                            event.setGeometry(featurePoly)
                    if not found: 
                        newMotionVector.append((histPoly, histSt, histEt))
                # Otherwise, add a new motionVectorPoly 
                if not found:                    
                    newMotionVector.append((featurePoly, featureSt, featureEt))
        
        if not newMotionVector:
            print "Expecting feature change -- feature not found" 
            self.flush()
            return 
        
        # Make sure they are in time order
        newMotionVector.sort(self._sortPolys)
        print "SR adjust new motionVector, found", found
        for poly, st, et, in newMotionVector:
            print "    ", st, et
        self.flush()
        # Convert back to parallel lists for storing in event attributes
        motionVectorPolys = [poly for poly, st, et in newMotionVector]
        motionVectorTimes = [(st,et) for poly, st, et in newMotionVector]            
        event.set('motionVectorPolys', motionVectorPolys)
        event.set('motionVectorTimes', motionVectorTimes)
        
        # Re-compute the motion vector and uncertainty
        newMotion = self.computeMotionVector(newMotionVector, self._currentTime,                     
                   event.get('convectiveObjectSpdKts', self._defaultWindSpeed()),
                   event.get('convectiveObjectDir',self._defaultWindDir())) 
        for attr in ['convectiveObjectDir', 'convectiveObjectSpdKts',
                      'convectiveObjectDirUnc', 'convectiveObjectSpdKtsUnc']:
            event.set(attr, int(newMotion.get(attr)))

    def _setVisualFeatures(self, event):
        downstreamPolys = event.get('downstreamPolys')
        if not downstreamPolys:
            return
        downstreamTimes = event.get('downstreamTimes')
    
        baseVisualFeatures = [] 
        selectedVisualFeatures = [] 
        geometry = event.getGeometry() 
        eventSt_ms = long(self._datetimeToMs(event.getStartTime()))
        upstreamSt_ms = eventSt_ms - (self._timeStep() * 1000 * self._upstreamTimeSteps())

        #print 'SR eventSt_ms', eventSt_ms
        #self.flush()
        
        # Downstream Polygons, Track Points, Relocated Downstream 
        numIntervals = len(downstreamPolys)        
        for i in range(numIntervals):
            poly = downstreamPolys[i]
            polySt_ms, polyEt_ms = downstreamTimes[i]
            polyEnd = polyEt_ms - 60*1000 # Take a minute off
   
            # First downstream polygon is at current time and always editable
            if polySt_ms == eventSt_ms:
                dragCapability = "all"
            else:
                dragCapability = "none"
              
            downstreamFeature = {
              "identifier": "downstream_"+str(polySt_ms),
              "borderColor": "eventType",
              "borderThickness": "eventType",
              "borderStyle": "eventType",
              "textSize": "eventType",
              "label": str(event.get('objectID')) + " " + event.getHazardType(),
              "textColor": "eventType",
              "dragCapability": dragCapability, 
              "geometry": {
                   (polySt_ms, polyEnd): poly
                  }
                }
            baseVisualFeatures.append(downstreamFeature)
               
            # Track Points 
            centroid = poly.centroid
            color = self._getProbTrendColor(i, numIntervals)
            trackPointFeature = {
              "identifier": "trackPoint_"+str(polySt_ms),
              "borderColor": color,
              "borderThickness": 2,
              "geometry": {
                  (upstreamSt_ms,
                   VisualFeatures.datetimeToEpochTimeMillis(event.getEndTime())):
                   centroid
               }
            }
            selectedVisualFeatures.append(trackPointFeature)
             
            # Relocated initial going downstream
            if polySt_ms > eventSt_ms:
                relocatedPoly = self._relocatePolygon(centroid, geometry)
                relocatedFeature = {
                  "identifier": "relocated_"+str(polySt_ms),
                  "borderColor": "eventType",
                  "borderThickness": "eventType",
                  "borderStyle": "dashed",
                  "textSize": "eventType",
                  "geometry": {
                      (polySt_ms, polyEnd): relocatedPoly
                       }
                }
                selectedVisualFeatures.append(relocatedFeature)
 
        # History Polygons
        historyPolys = event.get('historyPolys', [])
        historyTimes = event.get('historyTimes', [])
        numIntervals = len(historyPolys)        
        for i in range(numIntervals):
            poly = historyPolys[i]
            polySt_ms, polyEt_ms = historyTimes[i]
            polyEnd = polyEt_ms - 60*1000 # Take a minute off
                 
            historyFeature = {
              "identifier": "history_"+str(polySt_ms),
              "borderColor": "eventType",
              "borderThickness": "eventType",
              "borderStyle": "eventType",
              "textSize": "eventType",
              "label": str(event.get('objectID')) + " " + event.getHazardType(),
              "textColor": "eventType",
              "dragCapability": dragCapability, 
              "geometry": {
                   (polySt_ms, polyEnd): poly
                  }
                }
            baseVisualFeatures.append(historyFeature)
        
        envelope = shapely.ops.cascaded_union(downstreamPolys)
        swath = {
              "identifier": "swath",
              "borderColor": { "red": 1, "green": 1, "blue": 0 },
              "borderThickness": 3,
              "borderStyle": "dotted",
              "geometry": {
                  (upstreamSt_ms,
                   VisualFeatures.datetimeToEpochTimeMillis(event.getEndTime())):
                   envelope
               }
              }
        selectedVisualFeatures.append(swath)        
               
        # Upstream polygons -- prior to current time
        # Only shown for initial brand new hazard
        if self._firstTimeHazard:
            #print "\nDoing Upstream polys"
            for i in range(self._upstreamTimeSteps()):
                poly = self._upstreamPolys[i]
                polySt_ms, polyEt_ms = self._upstreamTimes[i]
                polyEt_ms = polySt_ms + ((self._timeStep()-60)*1000)
                
                # TODO Remove this once we're sure it's not needed i.e. can only nudge upstream for brand new only
    #             for i in range(len(motionVectorPolys)):
    #                 histPoly = motionVectorPolys[i]
    #                 histSt_ms, histEt_ms = motionVectorTimes[i]
    #                 if polySt_ms == histSt_ms:
    #                     #print "SR Found previous motionVector"
    #                     poly = histPoly
                        
                upstreamFeature = {              
                  "identifier": "upstream_"+str(polySt_ms),
                  "borderColor":  { "red": 1, "green": 1, "blue": 0 }, #"eventType", 
                  "borderThickness": "eventType",
                  "borderStyle": "eventType",
                  "dragCapability": "all",
                  "textSize": "eventType",
                  "label": str(event.get('objectID')) + " " + event.getHazardType(),
                  "textColor": "eventType",
                  "geometry": {
                      (polySt_ms, polyEt_ms): poly
                   }
                  }
                selectedVisualFeatures.append(upstreamFeature)

        #print ' ---- BASE VISUAL FEATURES FOR ', event.getEventID(), " = ", str(len(baseVisualFeatures)), ' ----'
        #print ' ---- SELECTED VISUAL FEATURES FOR ', event.getEventID(), " = ", str(len(selectedVisualFeatures)), ' ----'
                            
        if baseVisualFeatures:
            event.setBaseVisualFeatures(VisualFeatures(baseVisualFeatures))
        if selectedVisualFeatures:
            event.setSelectedVisualFeatures(VisualFeatures(selectedVisualFeatures))
            
#         if self._printVisualFeatures:
#             self._printVisualFeatures("\nBase Visual Features", baseVisualFeatures)
#             self._printVisualFeatures("\nSelected Visual Features", selectedVisualFeatures)

    ###############################
    # Helper methods              #
    ###############################
    
    ## TO DO -- Refactor these into their own module shared by Recommenders
    def _parseIdentifier(self, baseTime_ms, featureIdentifier):
        featureSt = featureIdentifier.split('_')[1]
        return int(featureSt)
                
    def _roundEventTimes(self, event, currentTime):
        # Current time rounded to nearest minute
        currentDt = datetime.datetime.fromtimestamp(currentTime/1000)
        startTime = self._roundTime(currentDt) 
        endTime = self._roundTime(event.getEndTime())        
        event.setStartTime(startTime)
        event.setEndTime(endTime)

    def _datetimeToMs(self, datetime):
        return float(time.mktime(datetime.timetuple())) * 1000
    
    def _convertFeatureTime(self, baseTime_ms, secs):
        # Return millis given the baseTime and secs offset
        # TODO: Round to minutes
        return long(baseTime_ms + secs * 1000 )

    def _convertMsToSecsOffset(self, time_ms, baseTime_ms=0):
        result = time_ms - baseTime_ms
        return int(result / 1000)
    
    def _roundTime(self, dt=None, dateDelta=datetime.timedelta(minutes=1)):
        """Round a datetime object to a multiple of a timedelta
        dt : datetime.datetime object, default now.
        dateDelta : timedelta object, we round to a multiple of this, default 1 minute.
        Author: Thierry Husson 2012 - Use it as you want but don't blame me.
                Stijn Nevens 2014 - Changed to use only datetime objects as variables
        """
        roundTo = dateDelta.total_seconds()    
        if dt == None : dt = datetime.datetime.now()
        seconds = (dt - dt.min).seconds
        # // is a floor division, not a comment on following line:
        rounding = (seconds+roundTo/2) // roundTo * roundTo
        return dt + datetime.timedelta(0,rounding-seconds,-dt.microsecond)
    
    def _getProbTrendColor(self, interval, numIvals):
        '''
        (range: color) e.g. ((0,20), { "red": 0, "green": 1, "blue": 0 } ), 
                          ((20,40), { "red": 1, "green": 1, "blue": 0 }),
        
        '''

        if interval >= numIvals:
            print "Invalid interval greater than numIntervals"
            return None

        ### FIXME: will want probTrend list from probTrend megawidget
        probTrend = []
        for i in range(numIvals):
            probTrend.append(100-(i*100/numIvals))


        prob = probTrend[interval]

        ### Should match PHI Prototype Tool
        colors =  {
            (0,20): { "red": 102/255.0, "green": 224/255.0, "blue": 102/255.0 }, 
            (20,40): { "red": 255/255.0, "green": 255/255.0, "blue": 102/255.0 }, 
            (40,60): { "red": 255/255.0, "green": 179/255.0, "blue": 102/255.0 }, 
            (60,80): { "red": 255/255.0, "green": 102/255.0, "blue": 102/255.0 }, 
            (80,101): { "red": 255/255.0, "green": 102/255.0, "blue": 255/255.0 }
        }


        for k, v in colors.iteritems():
            if prob in range(k[0], k[1]):
                return v
            
#     def _getProbTrendColor(self, i, intervals):
#         '''
#         (range: color) e.g. ((0,20): { "red": 0, "green": 1, "blue": 0 } ), 
#                           ((20,40), { "red": 1, "green": 1, "blue": 0 }), 
# 
#         '''
# #         for range, color in self._probTrendColors():
# #             min, max = range
# #             if t_ms >= min and t_ms < max:
# #                 return color
# #         # default
#         return { "red": 0, "green": 1, "blue": 0 }
    
    def _sortPolys(self, p1, p2):
        # Sort polygon tuples by start time
        poly1, st1, et1 = p1
        poly2, st2, et2 = p2
        if st1 < st2:
            return -1
        if st2 < st1:
            return 1
        return 0
            
    def _printVisualFeatures(self, label, features):
        print label
        for feature in features:
            print feature

    def _printEventSet(self, label, eventSet, eventLevel=1):            
        print label
        if self._printEventSetAttributes:
            # TODO Kevin can we just use print?
            eventSetAttrs = eventSet.getAttributes()
            print "trigger: ", eventSetAttrs.get("trigger")
            print "eventIdentifier: ", eventSetAttrs.get("eventIdentifier")
            print "origin: ", eventSetAttrs.get("origin")
            print "attributeIdentifiers: ", eventSetAttrs.get("attributeIdentifiers")
            #print "eventType: ", eventSetAttrs.get("eventType")
        if eventLevel:
            print "Events:"
            for event in eventSet:
                self._printEvent("============", event, eventLevel)
#             sys.stderr.write("Running swath recommender.\n    trigger: " +
#                          str(eventSet.getAttribute("trigger")) + "\n    event type: " + 
#                          str(eventSet.getAttribute("eventType")) + "\n    hazard ID: " +
#                          str(eventSet.getAttribute("eventIdentifier")) + "\n    attribute: " +
#                          str(eventSet.getAttribute("attributeIdentifiers")) + "\n")
         

    def _printEvent(self, label, event, eventLevel=1):
        if self._printEvent:
            import pprint
            print label
            if eventLevel >=1:
                print event.getEventID(), event.get('objectID')
                print "start, end", event.getStartTime(), event.getEndTime()
                print "userOwned", event.get('userOwned')
            if eventLevel >= 2:
                print '=== attrs ==='
                pprint.pprint(event.getHazardAttributes())
                print '=== ... ==='
            if eventLevel >= 3:
                print '=== geometry ==='
                pprint.pprint(event.getGeometry())
                print '=== ... ==='
            self.flush()
                
    def __str__(self):
        return 'Swath Recommender'

    def flush(self):
        import os
        os.sys.__stdout__.flush()


    #########################################
    ### OVERRIDES
    
    def _defaultWindSpeed(self):
        return 32
    
    def _defaultWindDir(self):
        return 270
    
    def _timeStep(self):
        # Time step for downstream polygons and track points
        return 60 # secs

    def _upstreamTimeSteps(self):
        # Number of time steps for upstream polygons
        return 10 
    
            
    def _probTrendColors(self, interval, numIvals):
        '''
        (range: color) e.g. ((0,20), { "red": 0, "green": 1, "blue": 0 } ), 
                          ((20,40), { "red": 1, "green": 1, "blue": 0 }),
        
        '''
        probTrend = []
        for i in range(numIntervals):
            probTrend.append(100-(i*100/numIvals))
            
        colors =  [
            ((0,20), { "red": 0, "green": 1, "blue": 0 }), 
            ((20,40), { "red": 0, "green": 1, "blue": 0 }), 
            ((40,60), { "red": 0, "green": 1, "blue": 0 }), 
            ((60,80), { "red": 0, "green": 1, "blue": 0 }), 
            ((80,101), { "red": 0, "green": 1, "blue": 0 }), 
            ]
        
    # TO DO:  The Recommender should access HazardTypes.py for this 
    #   information
    def _hazardPointLimit(self):
        return 20
    
    def _setPrintFlags(self):
        self._printVisualFeatures = True
        self._printEventSetAttributes = True
        self._printEventSetEvents = True
        self._printEvents = True


    #########################################
# REMOVE ME when stable    
#         # Old code to go over with Kevin
#         # Set up direction and speed values
#         trigger = eventSetAttrs.get('trigger')
#         if trigger == 'hazardEventModification':
#             ### get dir
#             dirVal = attrs.get('convectiveObjectDir')
#             # What if not dirVal?  Then what would we use?
#             if dirVal:
#                 dirVal = int(dirVal)
#                 ### If the user adjusts the DIR, save that adjustment to be picked up by the 
#                 ### automated call (PHI Grid Recommender)
#                 # Tracy: Why can't the PHI Grid Recommender just use the convectiveObjectDir directly?
#                 if probSevereAttrs:
#                     probSevereAttrs['wdir'] = dirVal
#                 else:
#                     probSevereAttrs = {'wdir':dirVal}
#                 event.set('probSeverAttrs', probSevereAttrs)
#                 
#             # There is potential for a bug in this code such that probSevereAttrs only has one or 
#             # other of the wdir, wspd values and results in a crash somewhere down the road.
#             
#             ### get dirUncertainty (degrees)
#             dirUVal = attrs.get('convectiveObjectDirUnc')
#             if dirUVal:
#                 dirUVal = int(dirUVal)
#             else:
#                 dirUVal = 10
#             ### get speed
#             speedVal = attrs.get('convectiveObjectSpdKts')
#             if speedVal:
#                 speedVal = int(speedVal)
#                 ### If the user adjusts the SPD, save that adjustment to be picked up by the 
#                 ### automated call (PHI Grid Recommender)
#                 #  Tracy: Same question as above
#                 if probSevereAttrs: 
#                     probSevereAttrs['wspd'] = speedVal
#                 else:
#                     probSevereAttrs = {'wspd':speedVal}
#                 event.set('probSeverAttrs', probSevereAttrs)
#                 ### get speedUncertainty (kts)
#             spdUVal = attrs.get('convectiveObjectSpdKtsUnc')
#             if spdUVal:
#                 spdUVal = int(spdUVal)
#             else:
#                 spdUVal = 10
#             #print 'SwathRec HandDrawn...', dirVal, speedVal, event.get('probSeverAttrs')
#             # This is not necessarily handdrawn
# #         else:  # I don't think this will happen -- 
# #             dirVal = probSevereAttrs.get('wdir')
# #             speedVal = probSevereAttrs.get('wspd')
# #             dirUVal = 10
# #             spdUVal = 10
# #             return False
# #             #print 'SwathRec Automated...', dirVal, speedVal, event.get('probSeverAttrs')

                

