'''
Swath recommender for probabilistic hazard types.
'''
import datetime, math
import EventFactory, EventSetFactory, GeometryFactory
import RecommenderTemplate
import TimeUtils
import AdvancedGeometry
from VisualFeatures import VisualFeatures
from ProbUtils import ProbUtils
import logging, UFStatusHandler
import matplotlib.pyplot as plt
import LogUtils

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
# def foo(self, speedVal, dirVal, spdUVal, dirUVal, secs, totalSecs):
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
    
    def NoPreset(self, speedVal, dirVal, spdUVal, dirUVal, secs, totalSecs):
        returnDict = {
                      'speedVal':speedVal,
                      'dirVal':dirVal, 
                      'spdUVal':spdUVal,
                      'dirUVal':dirUVal
                      }
        return returnDict
    
    def RightTurningSupercell(self, speedVal, dirVal, spdUVal, dirUVal, secs, totalSecs): 
        spdWt = 0.75
        dirWtTup = (0.,30.)
        dirWt = dirWtTup[1] * secs / totalSecs
        dirVal = dirVal + dirWt
        speedVal =  speedVal * spdWt
      
        returnDict = {
                      'speedVal':speedVal, 
                      'dirVal':dirVal, 
                      'spdUVal':spdUVal,
                      'dirUVal':dirUVal
                      }
        return returnDict
    
    def LeftTurningSupercell(self, speedVal, dirVal, spdUVal, dirUVal, secs, totalSecs): 
        spdWt = 0.75
        dirWtTup = (0.,30.)
        dirWt = -1. * dirWtTup[1] * secs / totalSecs
        dirVal = dirVal + dirWt
        speedVal = speedVal * spdWt
        
        returnDict = {
                      'speedVal':speedVal, 
                      'dirVal':dirVal, 
                      'spdUVal':spdUVal,
                      'dirUVal':dirUVal
                      }
        return returnDict
    
    def BroadSwath(self, speedVal, dirVal, spdUVal, dirUVal, secs, totalSecs):
        spdUValWtTup = (0.,15)
        dirUValWtTup = (0.,40)
        spdUValWt = spdUValWtTup[1] * secs / totalSecs
        spdUVal = spdUVal + (spdUValWtTup[1] - spdUValWt)
        dirUValWt = dirUValWtTup[1] * secs / totalSecs
        dirUVal = dirUVal + (dirUValWtTup[1] - dirUValWt)

        returnDict = {
                      'speedVal':speedVal, 
                      'dirVal':dirVal, 
                      'spdUVal':spdUVal,
                      'dirUVal':dirUVal
                      }
        return returnDict
    
    def LightBulbSwath(self, speedVal, dirVal, spdUVal, dirUVal, secs, totalSecs):
        spdUValWtTup = (0.,7)
        dirUValWtTup = (0.,20)
        spdUValWt = spdUValWtTup[1] * secs / totalSecs
        spdUVal = spdUVal + (spdUValWtTup[0] + spdUValWt)
        dirUValWt = dirUValWtTup[1] * secs / totalSecs
        dirUVal = dirUVal + (dirUValWtTup[0] + dirUValWt)

        returnDict = {
                      'speedVal':speedVal, 
                      'dirVal':dirVal, 
                      'spdUVal':spdUVal,
                      'dirUVal':dirUVal
                      }
        return returnDict
         
class Recommender(RecommenderTemplate.Recommender):
    
    def __init__(self):
        self.logger = logging.getLogger('SwathRecommender')
        self.logger.addHandler(UFStatusHandler.UFStatusHandler(
            'gov.noaa.gsd.common.utilities', 'SwathRecommender', level=logging.INFO))
        self.logger.setLevel(logging.INFO)
        self.probUtils = ProbUtils()
        
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
        metadata['includeEventTypes'] = [ "Prob_Severe", "Prob_Tornado" ]
        metadata['onlyIncludeTriggerEvent'] = True
        metadata['includeDataLayerTimes'] = True
        
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
            
    def execute(self, eventSet, dialogInputMap, visualFeatures):
        '''
        
        Please see this Google Doc for the SwathRecommender Design:
        https://docs.google.com/document/d/1Ry9Es0bZheazBnkTkpKUbnzpHejKomwrv_z-fIFji1k/edit
        
        Runs the Swath Recommender tool
        
        @param eventSet: A set of events which include session
                         attributes
        @param dialogInputMap: A map of information retrieved from
                               a user's interaction with a dialog.
        @param visualFeatures: Visual features as defined by the
                               defineSpatialInfo() method and
                               modified by the user to provide
                               spatial input; ignored.
        
        @return: A list of potential probabilistic hazard events. 
        '''                
        self.setPrintFlags()
        self.printEventSet("\n*********************\nRunning SwathRecommender", eventSet, eventLevel=1)
                 
        eventSetAttrs = eventSet.getAttributes()
        trigger = eventSetAttrs.get('trigger')
        origin = eventSetAttrs.get('origin')
        self.currentTime = long(eventSetAttrs.get("currentTime")) 
        self.selectedTime = long(eventSetAttrs.get('selectedTime'))
        self.setDataLayerTimes(eventSetAttrs)                
        self.attributeIdentifiers = eventSetAttrs.get('attributeIdentifiers', [])
        if self.attributeIdentifiers is None:
            self.attributeIdentifiers = []

        resultEventSet = EventSetFactory.createEventSet(None)
        
        # IF there are any editableObjects, we do not want to set the selected time ahead for
        #   a timeInterval update
        self.editableObjects = False
        if trigger == 'timeInterval':
            for event in eventSet:
                if event.get('editable'):
                    self.editableObjects = True
                
        for event in eventSet:
             
            if not self.selectEventForProcessing(event, trigger, eventSetAttrs, resultEventSet):
                continue
            
            # Begin Graph Draw
            if self.beginGraphDraw(event, trigger):
                 resultEventSet.add(event)
                 continue

            self.eventBookkeeping(event, origin)
            self.initializeEvent(event)
            
            
            # Origin Database
            #  From another machine: create Visual Features
            if origin == 'database':
                self.setVisualFeatures(event)
                resultEventSet.add(event)
                continue
                       
            # Adjust Hazard Event Attributes
            
            if trigger == 'frameChange':
                if not self.adjustForFrameChange(event, eventSetAttrs):
                    continue
                    
            elif trigger == 'dataLayerUpdate':
                self.visualCueForDataLayerUpdate(event)
                continue
                
            elif trigger == 'timeInterval':
                if not self.editableHazard:
                    self.advanceForecastPolys(event, eventSetAttrs)
                    self.setVisualFeatures(event)
                    resultEventSet.add(event)
                    if not self.editableObjects:
                        resultEventSet.addAttribute('selectedTime', self.latestDataLayerTime)
                        print "SR Setting selected time to latestDataLayer due to time interval", self.editableHazard, self.editableObjects
                        self.flush()
                continue
                
            elif trigger == 'hazardEventModification':
                changes = self.adjustForEventModification(event)
                if not changes:
                    # Handle a status change or "showGrid"
                    if "status" in self.attributeIdentifiers or "showGrid" in self.attributeIdentifiers:
                        self.setVisualFeatures(event)
                        resultEventSet.add(event)
                    continue
                resultEventSet.addAttribute('selectedTime', self.eventSt_ms)
                print "SR Setting selected time to eventSt"
                self.flush()
                    
            elif trigger == 'hazardEventVisualFeatureChange':
                if not self.adjustForVisualFeatureChange(event, eventSetAttrs):
                    continue
                resultEventSet.addAttribute('selectedTime', self.dataLayerTimeToLeft)
                print "SR Setting selected time to dataLayerTimeToLeft"
                self.flush()
                
            elif trigger == 'autoUpdate':
                self.adjustForAutoUpdate(event, eventSetAttrs)

            print self.logMessage("Re-calculating")                                                
            # Re-calculate Motion Vector-related and Probabilistic Information
            self.advanceForecastPolys(event, eventSetAttrs)
            self.getIntervalPolys(event, eventSetAttrs, 'forecast')
            if self.editableHazard:
                self.getIntervalPolys(event, eventSetAttrs, 'upstream')
                    
            if trigger == 'dataLayerUpdate':
                graphProbs = self.probUtils.getGraphProbs(event, self.latestDataLayerTime)
                #LogUtils.logMessage('[0]', graphProbs)
                event.set('convectiveProbTrendGraph', graphProbs)
            
            # Construct Updated Visual Features
            self.setVisualFeatures(event)         
                
            # Add revised event to result
            resultEventSet.add(event)              

        return resultEventSet      
    
    def setDataLayerTimes(self, eventSetAttrs):
        # Data Layer Times are in ms past the epoch
        dlTimes = eventSetAttrs.get("dataLayerTimes")
        if not dlTimes: 
            # Set default data layer times at one minute intervals  
            dlTimes = []
            for i in range(self.upstreamTimeLimit()):
                dlTimes.append(TimeUtils.roundEpochTimeMilliseconds(
                                self.currentTime - i*60*1000, delta=datetime.timedelta(seconds=1)))
            dlTimes.sort()
            
        # Round data layer times to seconds
        self.dataLayerTimes = []
        for dlTime in dlTimes:
            newTime = TimeUtils.roundEpochTimeMilliseconds(int(dlTime), delta=datetime.timedelta(seconds=1))
            # Cut off at upstreamTimeLimit e.g. no earlier than 30 minutes back in time
            if abs(self.currentTime - newTime) / (60*1000) > self.upstreamTimeLimit():
                    continue
            if self.selectedTime >= newTime:
                self.dataLayerTimeToLeft = newTime
            self.dataLayerTimes.append(newTime)
         
        if self.dataLayerTimes:
            self.latestDataLayerTime = self.dataLayerTimes[-1]
        else:
            self.latestDataLayerTime = self.currentTime  

        print 'latestDataLayerTime', self.probUtils.displayMsTime(self.latestDataLayerTime)
        print 'dataLayerTimes'
        for t in self.dataLayerTimes:
            print self.probUtils.displayMsTime(t), 
        print 'currentTime', self.probUtils.displayMsTime(self.currentTime)
        print 'framesInfo', eventSetAttrs.get('framesInfo')
        self.flush()
                
    def selectEventForProcessing(self, event, trigger, eventSetAttrs, resultEventSet):
        ''' 
        Return True if the event needs to be processed
        Otherwise return False
        '''            
        # For event modification or visual feature change, 
        #   we only want to process the one event identified in the eventSetAttrs,
        #   so skip all others           
        if trigger in ['hazardEventModification', 'hazardEventVisualFeatureChange']:
            eventIdentifier = eventSetAttrs.get('eventIdentifier')            
            if eventIdentifier and eventIdentifier != event.getEventID():
                    return False
        # Check for end time < current time and end the event
        eventEndTime_ms = long(TimeUtils.datetimeToEpochTimeMillis(event.getEndTime()))
        if eventEndTime_ms < self.currentTime:
            event.setStatus('ENDED')
            resultEventSet.add(event)
        # Skip ended, elapsed events 
        if event.getStatus() in ['ELAPSED', 'ENDED']:                
            return False
        return True                  
    
    def beginGraphDraw(self, event, trigger):
        ''' 
        Return True if the user has clicked "Draw" on the prob trend graph.
        In this case, we'll want to bypass swath recommender processing
        ''' 
        if trigger == 'hazardEventModification' and len(self.attributeIdentifiers) is 1 and \
          'convectiveProbTrendGraph' in self.attributeIdentifiers and \
          event.get('convectiveProbTrendGraph', []) == []:
            # Save off probTrend so that if the user edits some other attribute before drawing the 
            # graph, we have something to reset to
            event.set('preDraw_convectiveProbTrendGraph', event.get('prev_convectiveProbTrendGraph'))  
            return True
        else:
            return False  
        
    def eventBookkeeping(self, event, origin):  
        '''
        Set up values to be used in swath calculations
        '''      
        self.nudge = False
        self.pendingHazard = event.getStatus() in ["PENDING", "POTENTIAL"]
        self.selectedHazard = event.get('selected')
        #if not self.selectedHazard or 'selected' in self.attributeIdentifiers: # means we are transitioning to unselected
        #    event.set('editable', False)        
        self.editableHazard = event.get('editable', False)
        print "SR Bookkeeping Event status", event.getStatus()
        print "SR Event selected", event.get('selected')
        print "SR Event editable", event.get('editable'), self.editableHazard
        self.flush()

        
    #################################
    # Update of Event Attributes    #
    #################################
    
    def initializeEvent(self, event):
        # Initialize with event polygon
        mvPolys = event.get('motionVectorPolys')
        if not mvPolys:
            # Set start time and set eventSt_ms:
            self.moveStartTime(event, self.latestDataLayerTime)                
            mvPolys = [event.getGeometry()]
            st = self.probUtils.convertFeatureTime(self.eventSt_ms, 0)
            et = self.probUtils.convertFeatureTime(self.eventSt_ms, self.probUtils.timeStep())
            mvTimes = [(st, et)]
            event.set('motionVectorPolys', mvPolys)
            event.set('motionVectorTimes', mvTimes)
        else:
            self.probUtils.roundEventTimes(event)
            self.eventSt_ms = long(TimeUtils.datetimeToEpochTimeMillis(event.getStartTime()))
        print "SR event start, current", self.eventSt_ms, self.currentTime
        self.flush()
        
    def setUserOwned(self, event):         
        event.set('automationLevel', 'userOwned')
        if event.get('objectID') and not event.get('objectID').startswith('M'):
            event.set('objectID',  'M' + event.get('objectID'))
            
    def adjustForFrameChange(self, event, eventSetAttrs):
        # If hazard is editable AND
        #    If selected time is at latest data layer and start time < selected time:
        #       Move start time
        #       return True
        # return False
        if self.editableHazard:
            if self.selectedTime == self.latestDataLayerTime:
                if self.eventSt_ms < self.selectedTime:
                    self.moveStartTime(event, self.latestDataLayerTime)
                    event.set('dataLayerStatus', 'Synced')
                    return True
        return False
        
    def visualCueForDataLayerUpdate(self, event):
        # If editable:
        #    If start time is not at data layer:
        #        Set attribute 'dataLayerStatus': 'Data Layer Updated' (which will give a visual cue)
        if self.eventSt_ms != self.latestDataLayerTime:
            event.set('dataLayerStatus', 'Data Layer Updated')
        
    def adjustForEventModification(self, event):
        changed = False

        if 'selected' in self.attributeIdentifiers:
            return False               
        
        # Handle Reset Motion Vector
        if 'resetMotionVector' in self.attributeIdentifiers: 
            event.set('convectiveObjectDir', 270)
            event.set('convectiveObjectSpdKts', 32) 
            motionVectorPolys = event.get('motionVectorPolys', []) 
            motionVectorTimes = event.get('motionVectorTimes', [])
            if motionVectorPolys:
                motionVectorPolys = [motionVectorPolys[-1]]
                motionVectorTimes = [motionVectorTimes[-1]]
                event.set('motionVectorPolys', motionVectorPolys) 
                event.set('motionVectorTimes', motionVectorTimes) 
            return True
        
        # Handle Move Start Time
        if 'moveStartTime' in self.attributeIdentifiers: 
            #self.moveStartTime(event, self.latestDataLayerTime)
            return True
        
        # Handle Modify Button -- 'editable' changed by Interdependency Script
        if 'editable' in self.attributeIdentifiers: # Hit modify button
            if event.getStatus() == 'ISSUED':         
                self.moveStartTime(event, self.latestDataLayerTime, moveEndTime=True)
                graphProbs = self.probUtils.getGraphProbs(event, self.latestDataLayerTime)
                event.set('convectiveProbTrendGraph', graphProbs)
            return True
        
        if 'autoShape' in self.attributeIdentifiers:
            if not event.get('autoShape'):
                if event.get('automationLevel') in ['automated', 'attributesAndGeometry']:
                    # Adjust automationLevel
                    event.set('automationLevel', 'attributesOnly')
                    return True  
                
        # Get Convective Attributes from the MetaData. 
        # These should supercede and update the ones stored in the event
        self.updateConvectiveAttrs(event) 
     
        # Compare previous values to the new values to determine if any changes were made.  
        # Update the previous values along the way.
        triggerCheckList = ['convectiveObjectSpdKtsUnc', 'convectiveObjectDirUnc', 'convectiveProbTrendGraph',
                            'convectiveObjectDir', 'convectiveObjectSpdKts', 'convectiveSwathPresets', 
                            'duration']
        newTriggerAttrs = {t:event.get(t) for t in triggerCheckList}
        newTriggerAttrs['duration'] = self.probUtils.getDurationMinutes(event)
        
        changedAttrs = []
        manualAttrs = event.get('manualAttributes', [])
        
        for t in triggerCheckList:
            newVal = newTriggerAttrs.get(t)
            prevName = "prev_"+t
            prevVal = event.get(prevName)
            if t == 'convectiveProbTrendGraph':
                if newVal is None: newVal = []
                if prevVal is None: prevVal = [] 
                
            # Test to see if there is a change
            if prevVal != newVal:
                changed = True
                if t not in manualAttrs:
                    changedAttrs.append(t)
                if t == 'duration':
                    graphProbs = self.probUtils.getGraphProbs(event, self.latestDataLayerTime)
                    #LogUtils.logMessage('[1]', graphProbs)
                    event.set('convectiveProbTrendGraph', graphProbs)
                elif t == 'convectiveProbTrendGraph':
                    self.ensureLastGraphProbZeroAndUneditable(event)
                # Update previous value
                event.set(prevName, newVal)

            
        # Ensure that if changes were made, there are valid probability trend graph points ready for calculations. 
        # There could be none currently if the user had previously commenced a "draw points on graph" action, 
        # but did not complete it.
        # TODO -- think about why we need 'preDraw' -- can't we just always rely on 'prev'?
        if changed:
            if not event.get('convectiveProbTrendGraph'):
                event.set('convectiveProbTrendGraph', event.get('preDraw_convectiveProbTrendGraph', []))

            # Update manual attributes
            if self.editableHazard and event.get('automationLevel') != 'userOwned':
                manualAttrs = event.get('manualAttributes', [])  
                manualAttrs += changedAttrs
                event.set('manualAttributes', manualAttrs)                    
            
            return True
        
        return False

    def ensureLastGraphProbZeroAndUneditable(self, event):
        probVals = event.get('convectiveProbTrendGraph', [])
        if len(probVals) == 0:
            return
        lastPoint = probVals[-1]
        if lastPoint["y"] != 0 or lastPoint["editable"]:
            probVals[-1] = { "x": lastPoint["x"], "y": 0, "editable": False }
            
        event.set('convectiveProbTrendGraph', probVals)
    
    def updateConvectiveAttrs(self, event):
        convectiveAttrs = event.get('convectiveAttrs')
        if not convectiveAttrs:
            return
        if 'wdir' in convectiveAttrs:
            event.set('convectiveObjectDir', convectiveAttrs['wdir'])
        if 'wspd' in convectiveAttrs:
            event.set('convectiveObjectSpdKts', convectiveAttrs['wspd'])
            
    def moveStartTime(self, event, startMS, moveEndTime=True):
        newStart = datetime.datetime.utcfromtimestamp(startMS/1000)
        #if abs((newStart - event.getStartTime()).total_seconds()*1000) > self.probUtils.timeDelta_ms():
        #    return False     
        if moveEndTime or self.pendingHazard:
            durationSecs = self.probUtils.getDurationSecs(event)
            endMS = startMS+(durationSecs*1000)
            event.setEndTime(datetime.datetime.utcfromtimestamp(endMS/1000))
        event.setStartTime(newStart)
        self.probUtils.roundEventTimes(event)
        self.eventSt_ms = long(TimeUtils.datetimeToEpochTimeMillis(event.getStartTime()))
        return True
                
    ##############################################
    # Forecast bookkeeping                     #
    ##############################################    
                        
    def advanceForecastPolys(self, event, eventSetAttrs):
        ''' 
        As the current time moves forward, 
            move forecastPolys to pastPolys
        ''' 
        ####### 
        # Forecast Polys
        forecastPolys = event.get('forecastPolys', [])
        forecastTimes = event.get('forecastTimes', [])
        if not forecastPolys or not forecastTimes:
            return
        index = 0
        for i in range(len(forecastTimes)):           
            times = forecastTimes[i]
            if times is None:
                print "SwathRecommender Warning: forecast times None", forecastTimes
                self.flush()
                return
            
            st, et = forecastTimes[i]
            if st >= self.currentTime:    
                index = i
                break
        
        event.set('forecastPolys', forecastPolys[index:])
        event.set('forecastTimes', forecastTimes[index:])
        
        # If trigger is visualFeature, then don't do this
        # otherwise do this
        trigger = eventSetAttrs.get('trigger')
        
        ######
        #  Past Polys        
        pastPolys = event.get('pastPolys', [])
        pastTimes = event.get('pastTimes', [])
        newPastPolys = pastPolys + forecastPolys[:index]
        newPastTimes = pastTimes + forecastTimes[:index]
        
        # Truncate pastPolys if there is a maximum limit set
        maxPast = self.maxPastPolygons()
        if maxPast and len(newPastPolys) > maxPast:
            index = len(newPastPolys) - maxPast
            newPastPolys = newPastPolys[index:]
            newPastTimes = newPastTimes[index:]
        event.set('pastPolys', newPastPolys)
        event.set('pastTimes', newPastTimes)
      
    def setEventGeometry(self, event, shape):
        '''
        @summary Set the event geometry to that specified.
        @param event Event to be set.
        @param shape Shape in advanced geometry form.
        '''
        shape = self.probUtils.reduceShapeIfPolygon(shape)
        event.setGeometry(shape)

    ##############################################
    # Upstream / Forecast set up               #
    ##############################################    
 
    def getIntervalPolys(self, event, eventSetAttrs, timeDirection='forecast'):
        timeIntervals = []
        if timeDirection == 'upstream':
            # Follow dataLayerTimes for upstream polys.
            # Do not include the latest data layer time. 
            # DataLayerTimes are in ms past the epoch
            # Convert to secs relative to eventSt_ms
            startTime_ms = self.eventSt_ms
            for dlTime in self.dataLayerTimes[:-1]:
                secs = int((dlTime - self.eventSt_ms) / 1000)
                timeIntervals.append(secs)
        else:
            # Follow timeStep for forecast polys
            startTime_ms = self.currentTime
            durationSecs = self.probUtils.getDurationSecs(event, truncateAtZero=True)
            timeStep = self.probUtils.timeStep()
            i= 0
            while i <= durationSecs:
                timeIntervals.append(i)
                i += timeStep
        print "SR getIntervalPolys", timeDirection, timeIntervals
        self.flush()
        if timeIntervals:
            self.probUtils.createIntervalPolys(event, eventSetAttrs, self.nudge, SwathPreset(), 
                                             self.eventSt_ms, timeIntervals, timeDirection)

    ###############################
    # Visual Features and Nudging #
    ###############################  
      
    def adjustForAutoUpdate(self, event, eventSetAttrs):
        self.nudge = True
        #=======================================================================
        # # If nudging an issued event, restore the issuedDuration
        # if not self.pendingHazard:
        #     durationSecs = event.get("durationSecsAtIssuance")
        #     if durationSecs is not None:
        #         endTimeMS = TimeUtils.roundEpochTimeMilliseconds(self.eventSt_ms + durationSecs *1000, delta=datetime.timedelta(seconds=1))
        #         event.setEndTime(datetime.datetime.utcfromtimestamp(endTimeMS/1000))
        #         #graphProbs = self.probUtils.getGraphProbsBasedOnDuration(event)
        #         graphProbs = event.get("graphProbsAtIssuance")
        #         event.set('convectiveProbTrendGraph', graphProbs)
        # #=======================================================================
        # # newMotion = self.probUtils.computeMotionVector(motionVectorTuples, self.eventSt_ms, #self.currentTime,                     
        # #            event.get('convectiveObjectSpdKts', self.probUtils.defaultWindSpeed()),
        # #            event.get('convectiveObjectDir',self.probUtils.defaultWindDir())) 
        # # for attr in ['convectiveObjectDir', 'convectiveObjectSpdKts',
        # #               'convectiveObjectDirUnc', 'convectiveObjectSpdKtsUnc']:
        # #     event.set(attr, int(newMotion.get(attr)))
        # #=======================================================================
        #=======================================================================
        
        
        
 
    ###############################
    # Visual Features and Nudging #
    ###############################  
      
    def adjustForVisualFeatureChange(self, event, eventSetAttrs):
        '''
        Based on the visual feature change, update the motion vector
                      
        Update event geometry i.e. polygon at event start time, if changed        
        '''
        
        print "SR AdjustforVF editable", self.editableHazard
        self.flush()
        if not self.editableHazard:
            return False
            
        # Add the visualFeature change to the motion vector polygons       
        motionVectorPolys = event.get('motionVectorPolys', []) 
        motionVectorTimes = event.get('motionVectorTimes', [])
            
        features = event.getVisualFeatures()
        if not features: features = []

        # We are only changing one visual feature per SwathRecommender execution
        changedIdentifier = list(eventSetAttrs.get('attributeIdentifiers'))[0]

        for feature in features:
            featureIdentifier = feature.get('identifier')
            # Find the feature that has changed
            if featureIdentifier == changedIdentifier:
                # Get feature polygon
                polyDict = feature["geometry"]
                #  This will work because we only have one polygon in our features
                #  TODO refactor to have multiple polygons per feature
                for timeBounds, geometry in polyDict.iteritems():
                    featureSt, featureEt = timeBounds
                    featureSt = long(featureSt)
                    featurePoly = geometry
                # Add the feature to the motionVectorPolys 
                motionVectorPolys, motionVectorTimes = self.updatePolys(motionVectorPolys, motionVectorTimes, 
                             featurePoly, featureSt, featureEt, limit=self.maxMotionVectorPolygons())
                event.set('motionVectorPolys', motionVectorPolys)
                event.set('motionVectorTimes', motionVectorTimes)
                                     
                # If the feature is prior to the event start time, 
                #   add it to the past list
                if featureSt < self.eventSt_ms:
                    pastPolys = event.get('pastPolys', []) 
                    pastTimes = event.get('pastTimes', [])
                    pastPolys, pastTimes = self.updatePolys(pastPolys, pastTimes, featurePoly, featureSt, featureEt,
                                   limit=self.maxPastPolygons())
                    event.set('pastPolys', pastPolys)
                    event.set('pastTimes', pastTimes)
                    
                # If the feature is at the event start time, add it to the 
                #  event geometry
                if abs(featureSt-self.eventSt_ms) <= self.probUtils.timeDelta_ms():
                    event.setGeometry(featurePoly)
                    self.nudge = True
                    # If nudging an issued event, restore the issuedDuration
                    if not self.pendingHazard: 
                        durationSecs = event.get("durationSecsAtIssuance")
                        if durationSecs is not None:
                            endTimeMS = TimeUtils.roundEpochTimeMilliseconds(self.eventSt_ms + durationSecs *1000, 
                                                                             delta=datetime.timedelta(seconds=1))
                            event.setEndTime(datetime.datetime.utcfromtimestamp(endTimeMS/1000))
                            #graphProbs = self.probUtils.getGraphProbsBasedOnDuration(event)
                            graphProbs = event.get("graphProbsAtIssuance")
                            #LogUtils.logMessage('[2]', graphProbs)
                            event.set('convectiveProbTrendGraph', graphProbs)
                                        
        #print "SR Feature ST", featureSt, self.probUtils.displayMsTime(featureSt)
        #self.flush()
        
        if len(motionVectorPolys) <= 1:
            return False
                                      
        # Re-compute the motion vector and uncertainty
        motionVectorTuples = []
        for i in range(len(motionVectorPolys)):
            poly = motionVectorPolys[i]
            st, et = motionVectorTimes[i]
            motionVectorTuples.append((poly, st, et))
            print 'SR motionVector Poly, startTime:', poly.asShapely().centroid, self.probUtils.displayMsTime(st)
            self.flush()
            
        #print "SR motionVectorTuples", len(motionVectorTuples)
        #self.flush()
            
        newMotion = self.probUtils.computeMotionVector(motionVectorTuples, self.eventSt_ms, #self.currentTime,                     
                   event.get('convectiveObjectSpdKts', self.probUtils.defaultWindSpeed()),
                   event.get('convectiveObjectDir',self.probUtils.defaultWindDir())) 
        for attr in ['convectiveObjectDir', 'convectiveObjectSpdKts',
                      'convectiveObjectDirUnc', 'convectiveObjectSpdKtsUnc']:
            event.set(attr, int(newMotion.get(attr)))
            
        return True

    def updatePolys(self, polys, times, newPoly, newSt, newEt, limit=None):
        # Add the newPoly to the list of polys and times
        # If one currently exists at the newSt, replace it
        # Keep the lists in time order
        # If limit, then add the new poly and then truncate the list to the 
        #  given limit e.g. we may only want 10 past polygons or 2 motion vector polygons
    
        # Convert to tuples
        tuples = []
        for i in range(len(polys)):
            poly = polys[i]
            st, et = times[i]
            tuples.append((poly, st, et))

        newTuples = []
        found = False
        for poly, st, et in tuples:
            if abs(st-newSt) <= self.probUtils.timeDelta_ms():                        
                newTuples.append((newPoly, st, et))
                found = True
            else: 
                newTuples.append((poly, st, et))
        # Otherwise, add a new motionVectorPoly 
        if not found:                    
            newTuples.append((newPoly, newSt, newEt))
 
        newTuples.sort(self.sortPolys) 
        # CHECK THIS -- for Motion Vector may have to keep last one added to replace it
        if limit and len(newTuples) > limit:
            newTuples = newTuples[:limit]
        newPolys = [poly for poly, st, et in newTuples]
        newTimes = [(st,et) for poly, st, et in newTuples]            
        return newPolys, newTimes         
 
    def setVisualFeatures(self, event):
        print self.logMessage("Setting Visual Features")
        self.flush()

        forecastPolys = event.get('forecastPolys')
        if not forecastPolys:
            return
        
        features = []
        upstreamSt_ms = self.dataLayerTimes[0]
        featuresDisplay = self.featuresDisplay()
        
        # Forecast Features -- polygons, track points, relocated last motionVector
        features += self.forecastVisualFeatures(event, upstreamSt_ms)
        
        # Swath
        if featuresDisplay.get('swath'):
            swathFeature = self.swathFeature(event, upstreamSt_ms, forecastPolys)                
            features.append(swathFeature) 
        
        # Motion vector centroids
        if featuresDisplay.get('motionVectorCentroids'):
            features += self.motionVectorFeatures(event, upstreamSt_ms)     
        
        # Previous Time Features
        features += self.previousTimeVisualFeatures(event)
        
        # Preview Grid
        if featuresDisplay.get('previewGrid'):
            features += self.getPreviewGridFeatures(event, upstreamSt_ms)
                           
        # Replace Visual Features
        if features:
            try:
                event.setVisualFeatures(VisualFeatures(features))
            except:
                self.printFeatures(event, "\n*** [ERROR] - Unable to set Visual Features", features)
            
        if self.printVisualFeatures:
             self.printFeatures(event, "Visual Features", features)
        
    def forecastVisualFeatures(self, event, upstreamSt_ms):
        forecastPolys = event.get('forecastPolys')
        if not forecastPolys:
            return
        forecastTimes = event.get('forecastTimes')
        featuresDisplay = self.featuresDisplay()
        
        geometry = event.getGeometry()
        
        features = []
        # Forecast Polygons, Track Points, Relocated Forecast 
        numIntervals = len(forecastPolys)
        
        # Prob Label
        probTrend = event.get('convectiveProbTrendGraph')[0]
        probStr = ' '+ `probTrend['y']`+'%'
        self.label = str(event.get('objectID')) + " " + event.getHazardType() + probStr
   
        for i in range(numIntervals):
            poly = forecastPolys[i]
            polySt_ms, polyEt_ms = forecastTimes[i]
              
            forecastFeature = {
              "identifier": "swathRec_forecast_"+str(polySt_ms),
              "visibilityConstraints": "always",
              "borderColor": "eventType",
              "borderThickness": "eventType",
              "borderStyle": "eventType",
              "textSize": "eventType",
              "label": self.label,
              "textColor": "eventType",
              "dragCapability": "none", 
              "geometry": {
                   (polySt_ms, polyEt_ms): poly
                  }
                }
            if featuresDisplay.get('forecastPolys'):
                features.append(forecastFeature)
               
            # Track Points 
            centroid = poly.asShapely().centroid
            color = self.probUtils.getInterpolatedProbTrendColor(event, i, numIntervals)
            trackPointFeature = {
              "identifier": "swathRec_trackPoint_"+str(polySt_ms),
              "visibilityConstraints": "selected",
              "borderColor": color,
              "borderThickness": 2,
              "diameter": 5,
              "geometry": {
                  (upstreamSt_ms,
                   TimeUtils.datetimeToEpochTimeMillis(event.getEndTime()) + 1000):
                   AdvancedGeometry.createShapelyWrapper(centroid, 0)
               }
            }
            if featuresDisplay.get('trackPoints'):
                features.append(trackPointFeature)
             
            # Dashed relocated shape and centroid show up if selected, editable if at object start time
            if self.editableHazard and event.get('automationLevel') in ['userOwned', 'attributesOnly']:
                if i == 0:
                    editable = True
                    dragCapability = "whole"
                else:
                    editable = False
                    dragCapability = "none"
                    
                relocatedShape = self.probUtils.reduceShapeIfPolygon(AdvancedGeometry.
                                                                       createRelocatedShape(geometry, centroid))
    
                relocatedFeature = {
                  "identifier": "swathRec_relocated_"+str(polySt_ms),
                  "visibilityConstraints": "selected",
                  "borderColor": "eventType",
                  "borderThickness": "eventType",
                  "borderStyle": "dashed",
                  "textSize": "eventType",
                  "dragCapability": dragCapability,
                  "scaleable": editable,
                  "rotatable": editable,
                  "geometry": {
                      (polySt_ms, polyEt_ms): relocatedShape
                       }
                }
                if featuresDisplay.get('dashedPolys') and self.selectedHazard:
                    features.append(relocatedFeature)
                

                centroidFeature = {
                  "identifier": "swathRec_relocatedCentroid_"+str(polySt_ms),
                  "visibilityConstraints": "selected",
                  "borderColor": { "red": 0, "green": 0, "blue": 0 },
                  "borderThickness": 2,
                  "fillColor": { "red": 1, "green": 1, "blue": 1},
                  "diameter": 6,
                   "geometry": {
                      (polySt_ms, polyEt_ms): AdvancedGeometry.createShapelyWrapper(centroid, 0)
                       }
                }
                if featuresDisplay.get('dashedPolyCentroid') and self.selectedHazard:
                   features.append(centroidFeature)
     
        return features

    def swathFeature(self, event, upstreamSt_ms, forecastPolys):
        advancedGeometries = forecastPolys
        forecastPolys = []
        for geometry in advancedGeometries:
            forecastPolys.append(geometry.asShapely())
        envelope = shapely.ops.cascaded_union(forecastPolys)
        swath = {
              "identifier": "swathRec_swath",
              "visibilityConstraints": "selected",
              "borderColor": { "red": 1, "green": 1, "blue": 0 },
              "borderThickness": 3,
              "borderStyle": "dotted",
              "geometry": {
                  (upstreamSt_ms,
                   TimeUtils.datetimeToEpochTimeMillis(event.getEndTime()) + 1000):
                   AdvancedGeometry.createShapelyWrapper(envelope, 0)
               }
              }
        return swath

    def motionVectorFeatures(self, event, upstreamSt_ms):
        motionVectorPolys = event.get('motionVectorPolys', []) 
        motionVectorTimes = event.get('motionVectorTimes', [])
        features = []
        for i in range(len(motionVectorTimes)):
            st, et = motionVectorTimes[i]
            poly = motionVectorPolys[i].asShapely()                        
            centroid = AdvancedGeometry.createShapelyWrapper(poly.centroid, 0)
            feature = {
              "identifier": "swathRec_motionVector_"+str(st),
              "visibilityConstraints": "selected",
              "borderColor": { "red": 0, "green": 0, "blue": 0 },
              "borderThickness": 2,
              "fillColor": { "red": 1, "green": 1, "blue": 1},
              "diameter": 6,
              "geometry": {
                  (upstreamSt_ms,
                   TimeUtils.datetimeToEpochTimeMillis(event.getEndTime()) + 1000):
                   centroid
               }
            }
            features.append(feature)
        return features

    def getPreviewGridFeatures(self, event, upstreamSt_ms):
        if not event.get('showGrid'): 
            return []
        gridFeatures = []            
        probGrid, lons, lats = self.probUtils.getProbGrid(event)
        polyTupleDict = self.createPolygons(probGrid, lons, lats)
                
        # Generate and add preview-grid-related visual features        
        for key in sorted(polyTupleDict): 
            poly = polyTupleDict[key]
            
                                
            ### Should match PHI Prototype Tool
            colorFill =  {
                '0': { "red": 102/255.0, "green": 224/255.0, "blue": 102/255.0, "alpha": 0.4 }, 
                '20': { "red": 255/255.0, "green": 255/255.0, "blue": 102/255.0, "alpha": 0.4 }, 
                '40': { "red": 255/255.0, "green": 179/255.0, "blue": 102/255.0, "alpha": 0.4 }, 
                '60': { "red": 255/255.0, "green": 102/255.0, "blue": 102/255.0, "alpha": 0.4 }, 
                '80': { "red": 255/255.0, "green": 102/255.0, "blue": 255/255.0, "alpha": 0.4 }
                }
            
            if poly.is_valid:
                gridPreviewPoly = {
                    "identifier": "gridPreview_" + key,
                    "visibilityConstraints": "selected",
                    "borderColor": { "red": 0, "green": 0, "blue": 0 }, # colorFill[key],
                    "fillColor": colorFill[key],
                    "geometry": {
                        (upstreamSt_ms,
                        #(TimeUtils.datetimeToEpochTimeMillis(event.getStartTime()), 
                         TimeUtils.datetimeToEpochTimeMillis(event.getEndTime()) + 1000):
                                 AdvancedGeometry.createShapelyWrapper(poly, 0)
                    }
                }
                gridFeatures.append(gridPreviewPoly)
                            
        return gridFeatures
    
    def createPolygons(self, probGrid, lons, lats):
        polyDict = {}
        
        levels = np.linspace(0,100,6)

        X, Y = np.meshgrid(lons, lats)
        plt.figure()
        
        CS = plt.contour(X, Y, probGrid, levels=levels)

        prob = ['0', '20', '40', '60', '80']
        probIndex = [0.0, 20.0, 40.0, 60.0, 80.0]
        polyTupleDict = {}

        for c in range(0,(len(CS.levels) - 1)):
            contourVal = CS.levels[c]
            coords = CS.collections[c].get_paths()

            polygons = []
            for coord in coords:
                v = coord.vertices
                x = v[:,0]
                y = v[:,1]
                poly = GeometryFactory.createPolygon([(i[0], i[1]) for i in zip(x,y)])
                polygons.append(poly)
                
            mp = GeometryFactory.createMultiPolygon(polygons,'polygons')
            if contourVal in probIndex and len(polygons):    
                    polyTupleDict[prob[c]] = mp
                
        return polyTupleDict
        
    def previousTimeVisualFeatures(self, event):    
        # Previous time polygons -- prior to current time
        #
        # If time has marched forward from the initial creation of the hazard, we could have
        #  motionVectorPolys, pastPolys, and upstreamPolys (IF still pending)
        #
        # Choose to use in this order of preference:
        #      motionVector polygon
        #      then the past polygon
        #      then if the hazard is editable
        #         the upstream polygon
        #
        # All are editable if the hazard has not been issued yet.
        # 
        #print "\nDoing Previous time polys"
        pastPolys = event.get('pastPolys', [])
        pastTimes = event.get('pastTimes', [])
        motionVectorPolys = event.get('motionVectorPolys', []) 
        motionVectorTimes = event.get('motionVectorTimes', [])
        upstreamPolys = event.get('upstreamPolys', []) 
        upstreamTimes = event.get('upstreamTimes', [])
        previousFeatures = []
        
        if self.editableHazard and event.get('automationLevel') in ['userOwned', 'attributesOnly']:
            editableObject = True
        else:
            editableObject = False
        print "SR previousTimeVisualFeatures editableHazard, automationLevel", self.editableHazard, event.get('automationLevel')  
        self.flush()
                        
        # Don't want to show upstream polys for latestDataLayerTime
        for i in range(len(self.dataLayerTimes[:-1])):
            polySt_ms = self.dataLayerTimes[i]
            poly, editablePoly = self.findPreviousPoly(polySt_ms,
                                          motionVectorPolys, motionVectorTimes, 
                                          pastPolys, pastTimes, upstreamPolys, upstreamTimes)
            if not poly:
                continue

            polyEt_ms = self.dataLayerTimes[i + 1]
            #print "SR previous st, et", self.probUtils.displayMsTime(polySt_ms), self.probUtils.displayMsTime(polyEt_ms)
            #self.flush()
            
            if editablePoly and editableObject:
                dragCapability = 'whole'
                color = { "red": 1, "green": 1, "blue": 0 }
            else:
                dragCapability = 'none'
                color = "eventType"
                                     
            previousFeature = {              
              "identifier": "swathRec_previous_" + str(polySt_ms),
              "visibilityConstraints": "selected",
              "borderColor":  color, #{ "red": 1, "green": 1, "blue": 0 }, #"eventType", 
              "borderThickness": "eventType",
              "borderStyle": "eventType",
              "dragCapability": dragCapability,
              "textSize": "eventType",
              "label": self.label, 
              "textColor": "eventType",
              "geometry": {
                  (polySt_ms, polyEt_ms): poly
               }
              }
            previousFeatures.append(previousFeature)
        return previousFeatures


    def findPreviousPoly(self, polySt_ms, motionVectorPolys, motionVectorTimes, pastPolys, pastTimes,
                          upstreamPolys, upstreamTimes):
        featuresDisplay = self.featuresDisplay()
        editable = False
        if featuresDisplay.get('motionVectorPolys') and featuresDisplay.get('motionVectorPolys'):
            for i in range(len(motionVectorTimes)):
                st, et = motionVectorTimes[i]
                if abs(st-polySt_ms) < self.probUtils.timeDelta_ms():
                    print "SR upstream using motion vector", i, self.probUtils.displayMsTime(st)
                    editable = True
                    return motionVectorPolys[i], editable
        if featuresDisplay.get('pastPolys') and featuresDisplay.get('pastPolys'):
            for i in range(len(pastTimes)):
                st, et = pastTimes[i]
                if abs(st-polySt_ms) < self.probUtils.timeDelta_ms():
                    print "SR upstream using past", i, self.probUtils.displayMsTime(st)
                    return pastPolys[i], editable
        if not self.editableHazard:
            return None, False
        if featuresDisplay.get('upstreamPolys') and featuresDisplay.get('upstreamPolys'):
            for i in range(len(upstreamTimes)):
                st, et = upstreamTimes[i]
                if abs(st-polySt_ms) < self.probUtils.timeDelta_ms():
                    print "SR upstream poly", self.probUtils.displayMsTime(st)
                    editable = True
                    return upstreamPolys[i], editable
        return None, False

    ###############################
    # Helper methods              #
    ###############################
        
    def sortPolys(self, p1, p2):
        # Sort polygon tuples by start time
        poly1, st1, et1 = p1
        poly2, st2, et2 = p2
        if st1 < st2:
            return -1
        if st2 < st1:
            return 1
        return 0
            
    def printFeatures(self, event, label, features):
        print label, event.getEventID(), " = ", str(len(features)), ' ----'
        self.flush()
        #for feature in features:
        #    print feature

    def printEventSet(self, label, eventSet, eventLevel=1):            
        print label
        if self.printEventSetAttributes:
            if isinstance(eventSet, list):
                print "No values in EventSet"
                return
                
            eventSetAttrs = eventSet.getAttributes()
            print "trigger: ", eventSetAttrs.get("trigger")
            print "eventIdentifier: ", eventSetAttrs.get("eventIdentifier")
            print "origin: ", eventSetAttrs.get("origin")
            print "attributeIdentifiers: ", eventSetAttrs.get("attributeIdentifiers")
        if eventLevel:
            print "Events:", len(eventSet.getEvents())
            for event in eventSet:
                self.printEvent(None, event, eventLevel)         

    def printEvent(self, label, event, eventLevel=1):
        if self.printEvent:
            import pprint
            if label: print label
            if eventLevel >=1:
                print 'ID:', event.getEventID(), event.get('objectID')
                print "start, end", event.getStartTime(), event.getEndTime()
                print "automationLevel", event.get('automationLevel')
                print 'newManualObject', event.get('newManualObject')
            if eventLevel >= 2:
                print '=== attrs ==='
                pprint.pprint(event.getHazardAttributes())
                print '=== ... ==='
            if eventLevel >= 3:
                print '=== geometry ==='
                pprint.pprint(event.getFlattenedGeometry())
                print '=== ... ==='
            self.flush()
                
    def __str__(self):
        return 'Swath Recommender'

    def flush(self):
        import os
        os.sys.__stdout__.flush()
        
    def logMessage(self, *args):
        import inspect, os
        s = ", ".join(str(x) for x in list(args))
        fName = os.path.basename(inspect.getfile(inspect.currentframe()))
        lineNo = inspect.currentframe().f_back.f_lineno
        return '\t**** [' + str(fName) + ' // Line ' + str(lineNo) + ']:' + s


    #########################################
    ### OVERRIDES
    
    def upstreamTimeLimit(self):
        # Number of minutes backward in time for upstream polygons
        return 30 

    def maxPastPolygons(self):
        # Maximum number of past polygons to store
        # Older polygons are dropped off as time progresses
        return None
        
    def maxMotionVectorPolygons(self):
        # Maximum number of motion vector polygons
        # If None, then unlimited
        return None
                        
    def setPrintFlags(self):
        self.printVisualFeatures = False
        self.printEventSetAttributes = True
        self.printEventSetEvents = True
        self.printEvents = True
        
    def featuresDisplay(self):
        return {
               'upstreamPolys': True,
               'forecastPolys': True,
               'trackPoints': True,
               'dashedPolys': True,
               'dashedPolyCentroids': True,
               'motionVectorCentroids': True,
               'pastPolys': False,
               'swath': True,
               'basePoly': True,
               'previewGrid': True,
               }
        
