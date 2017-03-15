"""
Burn Scar Flood Recommender
Initially patterned after the Dam Break Flood Recommender

@since: October 2014
@author: GSD Hazard Services Team
"""
import RecommenderTemplate
import logging, UFStatusHandler
import os, sys

from ConfigurationUtils import ConfigUtils

DEFAULTPHIGRIDOUTPUTPATH = '/scratch/PHIGridTesting'
LOWTHRESHSOURCE = "phiConfigLowThreshold"
ULLONSOURCE = "phiConfigUpperLeftLon"
ULLATSOURCE = "phiConfigUpperLeftLat"
LRLONSOURCE = "phiConfigLowerRightLon"
LRLATSOURCE = "phiConfigLowerRightLat"
NUMLONPOINTSSOURCE = "phiConfigNumLonPoints"
NUMLATPOINTSSOURCE = "phiConfigNumLatPoints"
BUFFERSOURCE = "phiConfigDomainBuffer"
OUTDIRSOURCE = "phiConfigPHIOutputGridLocation"

LONMAX = -67.5
LONMIN = -129.0
LATMAX = 50.0
LATMIN = 27.0

 
class Recommender(RecommenderTemplate.Recommender):

    def __init__(self):
        self.logger = logging.getLogger('LineAndPointTool')
        self.logger.addHandler(UFStatusHandler.UFStatusHandler(
            'gov.noaa.gsd.common.utilities', 'LineAndPointTool', level=logging.INFO))
        self.logger.setLevel(logging.INFO)
        self._configUtils = ConfigUtils()

        
    def defineScriptMetadata(self):
        """
        @return: JSON string containing information about this
                 tool
        """
        metaDict = {}
        metaDict["toolName"] = "PHI Configuration Tool"
        metaDict["author"] = "GSD"
        metaDict["version"] = "1.0"
        metaDict["description"] = "Set Hazard Services wide configuration information for PHI Processing"
        metaDict["eventState"] = "Pending"
        
                
        # This tells Hazard Services to not notify the user when the recommender
        # creates no hazard events. Since this recommender is to be run in response
        # to hazard event changes, etc. it would be extremely annoying for the user
        # to be constantly dismissing the warning message dialog if no hazard events
        # were being created. 
        metaDict['background'] = True
        
        return metaDict

    def defineDialog(self, eventSet):
        """
        @param eventSet: A set of event objects that the user can use to help determine 
        new objects to return 
        @return: MegaWidget dialog definition to solicit user input before running tool
        """        
        dialogDict = {"title": "PHI Configuration Tool"}
        fieldDictList = []
        valueDict = {}
        
        lowThresholdDict = {}
        lowThresholdDict["fieldName"] = LOWTHRESHSOURCE
        lowThresholdDict["label"] = "Set Low Threshold Value"
        lowThresholdDict["fieldType"] = "IntegerSpinner"
        lowThresholdDict["minValue"] = 0
        lowThresholdDict["maxValue"] = 101
        lowThresholdDict["values"] = self._configUtils.getLowThreshold()
        lowThresholdDict["showScale"] = True
        lowThresholdDict["incrementDelta"] = 10
        valueDict[LOWTHRESHSOURCE] = lowThresholdDict["values"]
        fieldDictList.append(lowThresholdDict)
        
        upperLeftLonDict = {}
        upperLeftLonDict["fieldName"] = ULLONSOURCE
        upperLeftLonDict["label"] = "Set Upper Left LONGitude"
        upperLeftLonDict["fieldType"] = "FractionSpinner"
        upperLeftLonDict["minValue"] = LONMIN
        upperLeftLonDict["maxValue"] = LONMAX
        upperLeftLonDict["values"] = self._configUtils.getDomainULLon()
        upperLeftLonDict["showScale"] = True
        upperLeftLonDict["incrementDelta"] = 0.1
        valueDict[ULLONSOURCE] = upperLeftLonDict["values"]
        fieldDictList.append(upperLeftLonDict)
        
        upperLeftLatDict = {}
        upperLeftLatDict["fieldName"] = ULLATSOURCE
        upperLeftLatDict["label"] = "Set Upper Left LATitude"
        upperLeftLatDict["fieldType"] = "FractionSpinner"
        upperLeftLatDict["minValue"] = LATMIN
        upperLeftLatDict["maxValue"] = LATMAX
        upperLeftLatDict["values"] = self._configUtils.getDomainULLat()
        upperLeftLatDict["showScale"] = True
        upperLeftLatDict["incrementDelta"] = 0.1
        valueDict[ULLATSOURCE] = upperLeftLatDict["values"]
        fieldDictList.append(upperLeftLatDict)
        
        lowerRightLonDict = {}
        lowerRightLonDict["fieldName"] = LRLONSOURCE
        lowerRightLonDict["label"] = "Set Lower Right LONGitude"
        lowerRightLonDict["fieldType"] = "FractionSpinner"
        lowerRightLonDict["minValue"] = LONMIN
        lowerRightLonDict["maxValue"] = LONMAX
        lowerRightLonDict["values"] = self._configUtils.getDomainLRLon()
        lowerRightLonDict["showScale"] = True
        lowerRightLonDict["incrementDelta"] = 0.1
        valueDict[LRLONSOURCE] = lowerRightLonDict["values"]
        fieldDictList.append(lowerRightLonDict)
        
        lowerRightLatDict = {}
        lowerRightLatDict["fieldName"] = LRLATSOURCE
        lowerRightLatDict["label"] = "Set Lower Right LATitude"
        lowerRightLatDict["fieldType"] = "FractionSpinner"
        lowerRightLatDict["minValue"] = LATMIN
        lowerRightLatDict["maxValue"] = LATMAX
        lowerRightLatDict["values"] = self._configUtils.getDomainLRLat()
        lowerRightLatDict["showScale"] = True
        lowerRightLatDict["incrementDelta"] = 0.1
        valueDict[LRLATSOURCE] = lowerRightLatDict["values"]
        fieldDictList.append(lowerRightLatDict)
        
        bufferDict = {}
        bufferDict["fieldName"] = BUFFERSOURCE
        bufferDict["label"] = "Set Buffer Around Domain (in Degrees Lon/Lat)"
        bufferDict["fieldType"] = "FractionSpinner"
        bufferDict["minValue"] = 0.25
        bufferDict["maxValue"] = 3.0
        bufferDict["values"] = self._configUtils.getDomainBuffer()
        bufferDict["showScale"] = True
        bufferDict["incrementDelta"] = 0.25
        valueDict[BUFFERSOURCE] = bufferDict["values"]
        fieldDictList.append(bufferDict)
        
        outputDirDict = {}
        outputDirDict["fieldName"] = OUTDIRSOURCE
        outputDirDict["label"] = "Set Location for PHI Output Grids"
        outputDirDict["fieldType"] = "Text"
        outputDirDict["lines"] = 1
        outputDirDict["values"] = self._configUtils.getOutputDir()
        outputDirDict["visibleChars"] = 40
        valueDict[OUTDIRSOURCE] = outputDirDict["values"]
        fieldDictList.append(outputDirDict)
        
        
        dialogDict["fields"] = fieldDictList
        dialogDict["valueDict"] = valueDict
        
        return dialogDict
    
    def execute(self, eventSet, dialogInputMap, visualFeatures):
        """
        @eventSet: List of objects that was converted from Java IEvent objects
        @param dialogInputMap: A map containing user selections from the dialog
        created by the defineDialog() method.
        @param visualFeatures: Visual features as defined by the defineSpatialInfo()
        method and modified by the user to provide spatial input; ignored.
        @return: List of objects that will be later converted to Java IEvent
        objects
        """
        self._configUtils.setConfigDict(
                                          lowThresh = dialogInputMap.get(LOWTHRESHSOURCE),
                                          initial_ulLon = dialogInputMap.get(ULLONSOURCE),
                                          initial_ulLat = dialogInputMap.get(ULLATSOURCE),
                                          initial_lrLon = dialogInputMap.get(LRLONSOURCE),
                                          initial_lrLat = dialogInputMap.get(LRLATSOURCE),
                                          OUTPUTDIR = dialogInputMap.get(OUTDIRSOURCE),
                                          buff = dialogInputMap.get(BUFFERSOURCE),
                                          )
        

        return None

    def toString(self):
        return "PHIConfigurationTool"
    
     
def applyInterdependencies(triggerIdentifiers, mutableProperties):
    returnDict = {}
    if triggerIdentifiers == None:
        return returnDict
    
    if 'phiConfigUpperLeftLat' in triggerIdentifiers:
        ulLatVal = mutableProperties['phiConfigUpperLeftLat']['values']
        lrLatVal = mutableProperties['phiConfigLowerRightLat']['values']
        if ulLatVal <= lrLatVal + 1:
            lrLatVal =  ulLatVal - 1.0
        returnDict['phiConfigLowerRightLat'] = {'values': lrLatVal}

    if 'phiConfigLowerRightLat' in triggerIdentifiers:
        ulLatVal = mutableProperties['phiConfigUpperLeftLat']['values']
        lrLatVal = mutableProperties['phiConfigLowerRightLat']['values']
        if ulLatVal <= lrLatVal + 1:
            ulLatVal =  lrLatVal + 1.0
        returnDict['phiConfigUpperLeftLat'] = {'values': ulLatVal}

    if 'phiConfigUpperLeftLon' in triggerIdentifiers:
        ulLonVal = mutableProperties['phiConfigUpperLeftLon']['values']
        lrLonVal = mutableProperties['phiConfigLowerRightLon']['values']
        if ulLonVal >= lrLonVal - 1:
            lrLonVal =  ulLonVal + 1.0
        returnDict['phiConfigLowerRightLon'] = {'values': lrLonVal}

    if 'phiConfigLowerRightLon' in triggerIdentifiers:
        ulLonVal = mutableProperties['phiConfigUpperLeftLon']['values']
        lrLonVal = mutableProperties['phiConfigLowerRightLon']['values']
        if ulLonVal >= lrLonVal - 1:
            ulLonVal =  lrLonVal - 1.0
        returnDict['phiConfigUpperLeftLon'] = {'values': ulLonVal}



    return returnDict
      