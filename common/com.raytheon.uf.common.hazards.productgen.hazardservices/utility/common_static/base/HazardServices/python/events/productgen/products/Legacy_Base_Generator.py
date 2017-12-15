'''
    Description: The base class for all PGFv3 Product Generators to
                 inherit from.

    SOFTWARE HISTORY
    Date         Ticket#    Engineer    Description
    ------------ ---------- ----------- --------------------------
    Oct 24, 2014    4933    Robert.Blum Initial creation
    Jan 12, 2015    4937    Robert.Blum Refactored to support PGFv3.
    Jan 31, 2015    4937    Robert.Blum General cleanup along with add floodTimeStr
                                        business logic from v2.
    Mar 16, 2015    6951    Robert.Blum Formated floodTimeStr as a 12 hour clock.
    Mar 19, 2015    7094    Robert.Blum Updated CTA KeyInfo to contain eventIDs/UGCs.
    Mar 20, 2015    7149    Robert.Blum Made CTAs a String so it is a textBox megaWidget
                                        on the Product Editor.
    Mar 23, 2015    7165    Robert.Blum Added _createSectionDictionary() and retrieving
                                        previously edited raw data values from the productText
    Apr 10, 2015    7399    Robert.Blum Checking for empty strings in the additional info
                                        to prevent the megawidget error.
                                        table using setVal() in TextProductCommon.
    Apr 15  2015    6469    hansen      Cleaning up city list, locations affected, impacted locations
    Mar 27, 2015    6959    Robert.Blum Changes for Partial cancellations.
    Apr 07, 2015    6690    Robert.Blum List of drainages contents now matches WarnGen.
    Apr 16, 2015    7579    Robert.Blum Updates for amended Product Editor.
    Apr 28, 2015    7914    Robert.Blum Fixed error cause by latest merge.
    Apr 30, 2015    7579    Robert.Blum Changes for multiple hazards per section.
    May 05, 2015    7141    Robert.Blum Changes so RVS can call _createHazardEventDictionary().
    May 12, 2015    7729    Robert.Blum Added the floodSeverity to the dictionary as is along with
                                        the productString for it.
    May 07, 2015    6979    Robert.Blum Added a updateDataList() which removes the need for 
                                        the entire generator to be called twice. It takes
                                        the previously generated dictionaries and updates the
                                        required fields.
    May 21, 2015    8181    Robert.Blum Added phen and sig to the hazard Dictionary.
    May 26, 2015    7447    Robert.Blum Changes to create accurate dictionaries for EXA/EXB and
                                        resulting segments.
    Jun 02, 2015    7138    Robert.Blum Not calling the VTEC Engine for RVS.
    Jun 04, 2015    8492    Chris.Cody  Error issuing new FA.A with issued FA.A also selected
    Jun 05, 2015    8530    Robert.Blum Removing None check as it causes megawidget errors.
    Jun 17, 2015    7636    Robert.Blum Fixed _prepareLocationsAffected to use WarnGen's locations table.
    Aug 11, 2015    9920    Robert.Blum Additional fix for cityList query errors.
    Aug 13, 2015    8836    Chris.Cody  Changes for a configurable Event Id
    Aug 19, 2015   10224    Robert.Blum Adjusted additionalRainFalll to handle more user error cases.
    Aug 25, 2015    9992    Robert.Blum Fixed Product Level CTAs not correctly transferring from staging dialog.
    Aug 25, 2015    9626    Robert.Blum Added immediate cause for product level metadata.
    Sep 01, 2015    9590    Robert.Blum Removed metadata from the product dictionary.
    Sep 09, 2015   10263    Robert.Blum Changes to set the previousForecastCategory correctly.
    Oct 01, 2015   11739    Robert.Blum Removed decimal place from additional rain if it is not needed.
    Oct 05, 2015   11753    Robert.Blum Fixed a few product generation bugs causing intermittent errors.
    Oct 06, 2015   11832    Robert.Blum Misc. bug fixes.
    Oct 20, 2015   11865    Robert.Blum Updated issueTime when updateProductDictionaries is called.
    Nov 09, 2015    7532    Robert.Blum Moved CTAs from segment to section level and fixed logic in 
                                        checkForCancel method which would cause entire HID metadata
                                        to be incorrectly added to the staging dialog. 
    Nov 09, 2015   13111    Robert.Blum Fix for additional rainfall when less than 1 inch.
    Dec 10, 2015   12940    mduff       Default description var and check before use
    Dec 21, 2015   14042    Robert.Blum Fixed grammar issue with additional rainfall.
    Jan 05, 2016   11854    Robert.Blum CTAs no longer use a Set to remove duplicates since the
                                        order of the CTAs needs to be maintained.
    Jan 28, 2016   13012    Robert.Blum Added endingOption productStrings to the hazard dictionary.
    Feb 05, 2016   14784    Kevin.Bisanz Update _prepareToCreateHazardEventDictionary()
                                         to not use the prev event if proposed or
                                         pending status.
    Mar 01, 2016   14032    Ben.Phillippe Reworked spatial query
    Mar 31, 2016    8837    Robert.Blum  Changes for Service Backup.
    Apr 27, 2016   17742    Roger.Ferrel Added user name to hazard event dictionary
    May 06, 2016   18202    Robert.Blum  Changes for operational mode.
    May 09, 2016   18278    Ben.Phillippe CAN/CON products no longer calculate cancelled geometry
    May 19, 2016   16545    Robert.Blum  Updated _setProductInformation to correctly set the 
                                         previous flood categories.
    Jun 08, 2016    9620    Robert.Blum  Updates for editable expiration time.
    Jun 21, 2016    9620    Robert.Blum  Fixed python times to be in UTC.
    Jun 23, 2016   19537    Chris.Golden Changed to use UTC when converting epoch time to datetime.
    Jun 29, 2016   18209    Robert.Blum  Fixed retrieving user edits on followups where the pil changes.
    Jul 06, 2016   18257    Kevin.Bisanz Set issueTime when correcting product
    Jul 15, 2016   18244    Robert.Blum  Fixed Flood Moving time to be in local time.
    Jul 20, 2016   18244    Robert.Blum  Adding missing import.
    Jul 20, 2016   18257    Kevin.Bisanz correctionsCheck(..) returns false if no eventIDs or UGCs.
    Jul 22, 2016   19214    Kevin.Bisanz Add _prepareLocationsAffectedFallBack(..).
    Aug 09, 2016   17067    Robert.Blum  Fixed RVS products.
    Aug 08, 2016   21056    Robert.Blum  Implemented pathcast.
    Aug 11, 2016   20654    Kevin.Bisanz Rename _prepareLocationsAffectedFallBackUpdate
                                         to _prepareLocationDicts and move
                                         formatting logic into formatter.
    Aug 26, 2016   21458    Robert.Blum  Fixed framing code.
    Sep 06, 2016   19202    Sara.Stewart Added attributes to endingOption 
    Sep 12, 2016   19147    Robert.Blum  Use TimeUtils.py and default TZ to gmt.
    Sep 16, 2016   15934    Chris.Golden Changed to work with new advanced geometries in hazard events.
    Oct 07, 2016   21777    Robert.Blum  Fixed issue with multiple logger handlers.
    Oct 21, 2016   22489    Robert.Blum  Fixed _getCityList() to handle GFE's accurateCities flag.
    Oct 27, 2016   22963    Robert.Blum  Fixed query error related to pathcast code.
    Nov 10, 2016   22119    Kevin.Bisanz Add siteID argument to
                                         ProductTextUtil.createOrUpdateProductText(..)
    Nov 30, 2016   26598    dgilling     Save previousFloodSeverity for FL.W followups.
'''

import ProductTemplate
from Bridge import Bridge
import math, datetime, copy, json
from TextProductCommon import  TextProductCommon
from com.raytheon.uf.common.time import SimulatedTime
import ProductTextUtil
from ufpy.dataaccess import DataAccessLayer
from shapely.geometry import Polygon 
import logging, UFStatusHandler
from VTECEngineWrapper import VTECEngineWrapper
import HazardConstants
from HazardEvent import HazardEvent
from shapely import geometry
import HazardDataAccess
from KeyInfo import KeyInfo
import os, collections
import GeometryFactory
import SpatialQuery
import JUtil
from dateutil import tz
import PathCastUtil
import TimeUtils
import pwd

from abc import *

class ProdSegment(object):
    def __init__(self, segment, vtecRecords):
        # 'segment' is (frozenset([list of ugcs or points]), frozenset([list of eventIDs])        
        self.segment = segment
        self.vtecRecords = vtecRecords
        self.vtecRecords_ms = vtecRecords

    def str(self):
        print '  segment', self.segment 
        print '  vtecRecords: ', self.vtecRecords

class ProductSegmentGroup(object):
    def __init__(self, productID, productName, geoType, vtecEngine, mapType, segmented, productSegments, etn=None, formatPolygon=None, actions=[]):
        self.productID = productID
        self.productName = productName
        self.geoType = geoType
        self.vtecEngine = vtecEngine
        self.mapType = mapType
        self.segmented = segmented
        self.productSegments = productSegments
        self.etn = etn
        self.formatPolygon = formatPolygon
        self.actions = actions
        self.determineSegmentInfo()
        self.productLabel = self.determineProductLabel()

    def addProductSegment(self, productSegment):
        self.productSegments.append(productSegment)

    def setProductParts(self, productParts):
        self.productParts = productParts

    def determineSegmentInfo(self):
        '''
        Return a list of information for each segment:
           Each segment will have a dictionary of  
               {'act': action, 'eventIDs': eventID, 'type':hazardType, 'etn':etn)
               
        NOTE: most segments have only one action, eventID, hazardType
            but some long-fused products can have multiple
        '''
        self.segmentInfoList = []
        self.actions = []
        self.eventIDs = []
        self.hazardTypes = []
        self.etns = []
        for productSegment in self.productSegments:
            actionEventList = []
            for vtecRecord in productSegment.vtecRecords:
                action = vtecRecord.get('act')
                eventIDs = vtecRecord.get('eventID')
                hazardType = vtecRecord.get('key')
                etn = vtecRecord.get('etn')
                info = {
                    'action': action,
                    'eventIDs': eventIDs,
                    'type': hazardType,
                    'etn': etn,
                }
                actionEventList.append(info)
                self.actions.append(action)
                self.eventIDs += list(eventIDs)
                self.hazardTypes.append(hazardType)
                self.etns.append(etn)
            self.segmentInfoList.append(actionEventList)

    def determineProductLabel(self):
        ''' 
          The Product Label is a unique identifier for use in the Product Staging dialog
          It could also be used in the Product Editor and if included in the Product Dictionary
             
            FFA_area, FFA_point
            FLW_point, FLS_point_advisory, FLS_point_warning
            FLW_area_eventID_hazardType, FLS_area_eventID_hazardType
              Note: The hazard type is added for clarity, not needed for uniqueness
        
        '''
        # FFA_area, FFA_point, FLW_point
        productLabel = self.productID + '_' + self.geoType
        if self.geoType == 'point' and self.productID in ['FLS']:
            # FLS_point_advisory, FLS_point_warning
            if self.productName.find('Advisory') >= 0:
                productLabel += '_advisory'
            else:
                productLabel += '_warning'                
        elif self.geoType == 'area' and self.productID in ['FLW', 'FLS']:
            # FLW_area_eventID_hazardType, FLS_area_eventID_hazardType
            eventID = self.eventIDs[0]
            hazardType = self.hazardTypes[0].replace('.','_')
            productLabel += '_' + eventID + '_' + hazardType
        return productLabel

    def str(self):
        print 'productID: ', self.productID, self.productName, self.productLabel
        print 'geoType: ', self.geoType, 'mapType: ', self.mapType, 'segmented: ', self.segmented, 'etn: ', self.etn, 'actions: ', self.actions
        print 'productSegments: ', len(self.productSegments)
        for productSegment in self.productSegments:
            productSegment.str()

class Product(ProductTemplate.Product):

    def createProductSegment(self, segment, vtecRecords):
        return ProdSegment(segment, vtecRecords)
    def createProductSegmentGroup(self, productID, productName, geoType, vtecEngine, mapType, segmented, productSegments, etn=None, formatPolygon=None):
        return ProductSegmentGroup(productID, productName, geoType, vtecEngine, mapType, segmented, productSegments, etn, formatPolygon)

    def __init__(self):
        self.bridge = Bridge()
        self._areaDictionary = self.bridge.getAreaDictionary()
        self._cityLocation = self.bridge.getCityLocation()

        self_hazardEventManager = None
        self._tpc = TextProductCommon()
        self._tpc.setUp(self._areaDictionary)
        self._siteInfo = self.bridge.getSiteInfo()

        self.logger = logging.getLogger('Legacy_Base_Generator')
        for handler in self.logger.handlers:
            self.logger.removeHandler(handler)
        self.logger.addHandler(UFStatusHandler.UFStatusHandler(
            'com.raytheon.uf.common.hazards.productgen', 'Legacy_Base_Generator', level=logging.INFO))
        self.logger.setLevel(logging.INFO)

        # This needs to set by each v3 product generator
        # to allow VTECEngineWrapper to init correctly.
        self._productGeneratorName = ''

    def _initialize(self):
        # Default is True -- Products which are not VTEC can override and set to False
        self._vtecProduct = True
        self._vtecEngine = None

        # To ensure time calls are based on Zulu
        os.environ['TZ'] = "GMT0"

    @abstractmethod
    def defineDialog(self, eventSet):
        '''
        @return: dialog definition to solicit user input before running tool
        '''  
        pass

    def _previewProductSegmentGroups(self, eventSet):
        self._initialize()
        self._getVariables(eventSet)
        if not self._inputHazardEvents:
            return {}
        segments = []
        if self._productID is not 'RVS':
            # TODO - Why is the VTEC Engine called for ESF products?

            # Important to turn off "issue" so that the ETN number will not be advanced
            # when previewing the VTEC segmentation
            self._issueFlag = False
            segments = self._getSegments(self._inputHazardEvents)
        else:
            self._generatedHazardEvents = self.determineShapeTypesForHazardEvents(self._inputHazardEvents)
        return self._groupSegments(segments)

    def _getProductLevelMetaData(self, inputHazardEvents, metaDataFile, productSegmentGroups):
        # Use product segment groups to determine the products 
        metaDataDict = {}
        self.bridge = Bridge()
        eventIDs = [hazardEvent.getEventID() for hazardEvent in self._generatedHazardEvents]
        for productSegmentGroup in productSegmentGroups:
            productGroup = {
                'productID': productSegmentGroup.productID,
                'productName': productSegmentGroup.productName,
                'geoType': productSegmentGroup.geoType,
                'productLabel': productSegmentGroup.productLabel,
                'productParts': productSegmentGroup.productParts,
                'actions':productSegmentGroup.actions,
                'eventIDs': eventIDs,
                }
            # Just send one group -- we'll have a separate sub-tab for each product           
            metaDict = {'productSegmentGroup': productGroup, 'productCategory': self._productCategory}
            metaData = self.getMetaData(inputHazardEvents, metaDict, metaDataFile) 
            metaDataDict[productSegmentGroup.productLabel] = metaData.get('metadata')
        return metaDataDict

    def _checkForCancel(self, inputHazardEvents, productSegmentGroups):
        ''' Check for partial or automatic cancel
        Partial Cancel -- 
            Look for an eventID with both CAN segments and non-CAN segments
        Automatic Cancel -- 
           Look for an eventID with a CAN segment that is not included in the 
           inputHazardEvents

        @param inputHazardEvents -- hazard events given as input to the product generator
        @param productSegmentGroups
        @return metaDataDict -- organized by productSegmentGroup / productLabel:
           {productLabel: megawidgets to display for cancellations}
        '''
        inputEventIDs = [hazardEvent.getEventID() for hazardEvent in inputHazardEvents]
        # Determine all the actions in a product associated with each eventID
        actionDict = {}
        metaDataDict = {}
        description = None
        
        for productSegmentGroup in productSegmentGroups:
            for productSegment in productSegmentGroup.productSegments:
                vtecRecords = productSegment.vtecRecords
                actions = []
                for vtecRecord in vtecRecords:
                    action = vtecRecord.get('act')
                    vtecEventIDs = vtecRecord.get('eventID')
                    for id in vtecEventIDs:
                        eventActions = actionDict.get(id, [])
                        eventActions.append(action)
                        actionDict[id] = eventActions
                        actions.append(action)
                ids, eventIDs = productSegment.segment
                if 'CAN' in actions:
                    description = self._tpc.formatUGC_names(ids)
            # Look for Partial and Automatic Cancellation
            canceledEventIDs = []
            for eventID in actionDict:
                if eventID not in inputEventIDs:
                    # Automatic Cancellation -- need to retrieve hazard event.
                    hazardEvent = HazardDataAccess.getHazardEvent(eventID, self._practice)
                    if hazardEvent is not None:
                        inputHazardEvents.add(hazardEvent)
                    canceledEventIDs.append(eventID)
                eventActions = actionDict.get(eventID, [])
                if 'CAN' in eventActions:
                    non_CAN = False
                    for eventAction in eventActions:
                        if eventAction not in ['CAN']:
                            # Partial cancellation -- Cancel segments plus non-Cancel segments associated with the same eventID
                            canceledEventIDs.append(eventID)
                            break

            # Get the Meta Data for the canceledEventIDs
            # We will assume that all the eventIDs for a giving productSegmentGroup are in the same segment, 
            # so we only have to get the metaData for the "lowest" eventID (by convention)
            # Note: 
            #    -- the product staging information needs to be unpacked accordingly and 
            #    -- the "sectionHazardEvent" therefore should be the "lowest" eventID so
            #       that this metaData is picked up.
            metaDataList = []
            canceledEventIDs.sort()
            for eventID in canceledEventIDs:
                for hazardEvent in self._inputHazardEvents:
                    if eventID == hazardEvent.getEventID():
                        if not metaDataList:
                            saveStatus = hazardEvent.getStatus()
                            hazardEvent.setStatus('ending')
                            metaDataList += self.getHazardMetaData(hazardEvent).get('metadata', [])
                            hazardEvent.setStatus(saveStatus)

            if metaDataList:
                label = "Enter information for cancellation"
                if description:
                    label += " -- " + description + "."
                else:
                    label += "."
                    
                metaDataList = [{
                    "fieldType": "Label",
                    "fieldName": "label1",
                    "label": label,
                    "bold": True,
                    "italic": True
                    }] + metaDataList
                    
            metaDataDict[productSegmentGroup.productLabel] = metaDataList
        return metaDataDict

    def _organizeByProductLabel(self, productLevelMetaData_dict, cancel_dict, fieldName):
        # Organize by productLabel with a tab for each productLabel
        pages = []
        pageNames = []
        for productLabel in productLevelMetaData_dict:
            productFields = productLevelMetaData_dict.get(productLabel, [])
            cancelFields = cancel_dict.get(productLabel, [])
            if productFields is None: productFields = []
            if cancelFields is None: cancelFields = []
            fields = productFields + cancelFields
            pageNames.append(productLabel)
            # Add a tab for the productLabel
            if fields:
                page = {
                    "pageName": productLabel,
                    "pageFields": fields
                }
                pages.append(page)
        # Add any tabs that we missed
        for productLabel in cancel_dict:
            if productLabel not in pageNames:
                pageNames.append(productLabel)
                fields = cancel_dict.get(productLabel)
                # add a tab for the productLabel
                if fields:
                    page = {
                        "pageName": productLabel,
                        "pageFields": fields
                    }
                    pages.append(page)
        tabs = [{
            "fieldType": "TabbedComposite",
            "fieldName": fieldName,
            "leftMargin": 10,
            "rightMargin": 10,
            "topMargin": 10,
            "bottomMargin": 10,
            "expandHorizontally": True,
            "expandVertically": True,
            "pages": pages,
        }]

        dialogDict = {}
        if pages:
            dialogDict['metadata'] = tabs
            # dialogDict['metadata'] = fields
        return dialogDict

    @abstractmethod
    def execute(self, eventSet, dialogInputMap):
        '''
        Must be overridden by the Product Generator
        '''
        pass

    def _getVariables(self, eventSet, dialogInputMap=None):
        self._inputHazardEvents = eventSet.getEvents()
        metaDict = eventSet.getAttributes()

        # List of vtecEngineWrappers generated for these products
        #  Used at end to save vtec records if issueFlag is on
        self._wrappers = []

        self._issueFlag = metaDict.get('issueFlag')

        if self._issueFlag == 'False':
            self._issueFlag = False
        else:
            self._issueFlag = True

        self._issueTime = int(metaDict.get('currentTime'))
        self._issueTime_secs = self._issueTime / 1000
        self._siteID = metaDict.get('siteID')
        self._tpc.setSiteID(self._siteID)
        self._backupSiteID = metaDict.get('backupSiteID', self._siteID)
        inputFields = metaDict.get('inputFields', {})
        self._overviewHeadline_value = inputFields.get('overviewHeadline', '') 
        self._sessionDict = metaDict.get('sessionDict', {})
        caveMode = self._sessionDict.get('hazardMode','PRACTICE').upper()
        self._practice = True
        if caveMode == 'OPERATIONAL':
            self._practice = False

        # These come from SiteInfo
        # Primary Site
        siteEntry = self._siteInfo.get(self._siteID)
        self._fullStationID = siteEntry.get('fullStationID')  # KBOU
        self._region = siteEntry.get('region')
        self._wfoCity = siteEntry.get('wfoCity')
        self._wfoCityState = siteEntry.get('wfoCityState')
        self._areaName = ''  # siteEntry.get('state')  #  'GEORGIA' 

        # Backup Site
        siteEntry = self._siteInfo.get(self._backupSiteID)
        self._backupWfoCityState = siteEntry.get('wfoCityState')
        self._backupFullStationID = siteEntry.get('fullStationID')

        vtecMode = metaDict.get('vtecMode')
        if vtecMode:
            vtecMode = str(vtecMode)
        self._vtecMode = vtecMode
        self._vtecTestMode = bool(metaDict.get('vtecTestMode'))

        if dialogInputMap:
            if self._productID == 'RVS':
                # Dont save the staging values for RVS.
                self._dialogInputMap = dialogInputMap
            else:
                self._storeDialogInputMap(dialogInputMap)
        else:
            self._dialogInputMap = {}

        self._preProcessHazardEvents(self._inputHazardEvents)

    def _storeDialogInputMap(self, dialogInputMap):
        self._dialogInputMap = dialogInputMap
        eventIDs = [hazardEvent.getEventID() for hazardEvent in self._inputHazardEvents]
        for eventID in eventIDs:
            for key in dialogInputMap:
                value = dialogInputMap.get(key)
                if isinstance(value,set):
                    value = list(value)
                # Some values may be lists e.g. calls to action
                value = json.dumps(value)
                ProductTextUtil.createOrUpdateProductText(key, '', '', '', [eventID], self._siteID, value)
        self.flush()

    def _addDialogInputMapToDict(self, dialogInputMap, productDict):
        '''
        Adds each key from the dialogInputMap to the product dictionary.
        For certain keys it replaces the values which are the metadata
        identifiers with the productStrings from the metaData.

        @param dialogInputMap - dictionary from product staging dialog.
        @param productDict - dictionary which the entries are added to.: 
        '''
        for key in dialogInputMap.keys():
            if key == 'callsToAction_productLevel_' + self._productLabel:
                dictList = self._productLevelMetaData_dict.get(self._productLabel)
                for dict in dictList:
                    if dict.get('fieldName') == key:
                        cta_dict = dict
                        break
                # list of the productStrings to be passed to the formatter
                newCTAs = []
                # Loop over the selected CTAs from the staging dialog
                for value in dialogInputMap.get(key):
                    # Compare value with each identifier to find a match
                    for choice in cta_dict.get('choices'):
                        if value == choice.get('identifier'):
                            # Found a match so grab the productString
                            productString = choice.get('productString')
    
                            # Clean up the productString
                            productString = productString.replace('  ', '')
                            productString = productString.replace('\n', ' ')
                            productString = productString.replace('</br>', '\n')
    
                            # Add it to the list to be passed to the formatter
                            newCTAs.append(productString)
                            break
                productDict[key] = newCTAs
            else:
                # Pass the value as is
                productDict[key] = dialogInputMap[key]

    def _preProcessHazardEvents(self, hazardEvents):
        '''
        Can be overridden to preprocess the hazard events
        For example, the Immediate Cause is derived from the Hydrologic Cause for
        an FF.W.NonConvective and needs to be set prior to VTEC processing
        
        @param hazardEvents: hazard events
        '''
        pass

    def _makeProducts_FromHazardEvents(self, hazardEvents, eventSetAttributes):
        # Determine the list of segments given the hazard events 
        segments = []
        if self._productID is not 'RVS':
            segments = self._getSegments(hazardEvents)
        else:
            self._generatedHazardEvents = self.determineShapeTypesForHazardEvents(hazardEvents)
        self.logger.info('Product Generator --  Number of segments=' + str(len(segments)))

        # Determine the list of products and associated segments given the segments
        productSegmentGroups = self._groupSegments(segments)

        # Create each product dictionary and add to the list of productDicts
        productDicts = []
        for productSegmentGroup in productSegmentGroups:
            # Update these first so they are correct when we init the product dictionary.
            self._productID = productSegmentGroup.productID
            self._productName = productSegmentGroup.productName
            self._productLabel = productSegmentGroup.productLabel

            # Init the productDict
            productDict = collections.OrderedDict()
            self._initializeProductDict(productDict, eventSetAttributes)
            productDict['productLabel'] = self._productLabel
            if self._productID != "RVS":
                metaDataList, hazardEvents = self.getSegmentMetaData(productSegmentGroup.productSegments[0].segment)
                productDict['previousProductLabel'] = hazardEvents[0].get('productLabel', None)

            # Add productParts to the dictionary
            productParts = productSegmentGroup.productParts
            productDict['productParts'] = productParts
        
            self._createProductLevelProductDictionaryData(productDict)
            # Add dialogInputMap entries to the product dictionary
            self._addDialogInputMapToDict(self._dialogInputMap, productDict)

            # Create the segments and add them to the product dictionary
            self._createAndAddSegmentsToDictionary(productDict, productSegmentGroup)

            # Add any remaining info to the productDict
            self._wrapUpProductDict(productDict)

            # Add the productDict to the list
            productDicts.append(productDict)

        # If issuing, save the VTEC records for legacy products       
        self._saveVTEC(self._generatedHazardEvents) 

        return productDicts, self._generatedHazardEvents


    def _createProductLevelProductDictionaryData(self, productDict):
        pass

    def _createAndAddSegmentsToDictionary(self, productDict, productSegmentGroup):
        segmentDicts = []

        for productSegment in productSegmentGroup.productSegments:

            # Create the segment dicitonary
            segmentDict = collections.OrderedDict()
            self._productSegment = productSegment

            # Setup critical info for the segment
            self._setupSegment()

            # Check whether this segment requires any special processing
            self.checkSegment(productSegmentGroup)

            # Create and order the sections for the segment:     
            self._productSegment.sections = self._createSections(self._productSegment.vtecRecords_ms, self._productSegment.metaDataList)

            if not self._productSegment.sections:
                self._setProductInformation(self._productSegment.vtecRecords_ms, self._productSegment.hazardEvents)

            # Add the sections to the segment dictionary
            self._addSectionsToSegment(segmentDict)

            segmentDict['expireTime'] = self._productSegment.expireTime
            segmentDict['vtecRecords'] = self._productSegment.vtecRecords
            segmentDict['ugcs'] = self._productSegment.ugcs
            segmentDict['timeZones'] = self._productSegment.timeZones
            segmentDict['cityList'] = self._productSegment.cityList

            segmentDicts.append(segmentDict)

        # Add the segmentDicts to the productDict
        productDict['segments'] = segmentDicts
        
    def _addSectionsToSegment(self, segmentDict):
        # If no sections add an empty list and return
        if not self._productSegment.sections:
            segmentDict['sections'] = []
            return

        sectionDicts = []
        for section in self._productSegment.sections:
            sectionCTAs = []
            # A section can have multiple events/metaData
            sectionVtecRecord, sectionMetaData, sectionHazardEvents = section
            for hazardEvent in sectionHazardEvents:
                metaData = sectionMetaData.get(hazardEvent.getEventID())

                # Gather the section CTAs so they can be added at both section and segment level.depending on product
                hazardCTAs = self.getCTAsPhrase(hazardEvent, metaData)
                if hazardCTAs:
                    for hazardCTA in hazardCTAs:
                        found = False
                        for sectionCTA in sectionCTAs:
                            if hazardCTA == sectionCTA:
                                found = True
                                break
                        if not found:
                            sectionCTAs.append(hazardCTA)

            sectionDict = self._createSectionDictionary(sectionHazardEvents, sectionVtecRecord, sectionMetaData)
            # Convert the CTAs from a set to a string
            if sectionCTAs:
                sectionDict[HazardConstants.CALLS_TO_ACTION] = '\n\n'.join(list(sectionCTAs))
            else:
                sectionDict[HazardConstants.CALLS_TO_ACTION] = ''
            sectionDicts.append(sectionDict)

        # Add the list of section dictionaries to the segment dictionary
        segmentDict['sections'] = sectionDicts

    def _createSections(self, vtecRecords, metaDataList):
        '''
        Order vtec records and create the sections for the segment

        @param vtecRecords:  vtecRecords for a segment
        @param metaDataList: list of (metaData, hazardEvent) for the segment
        '''
        sections = []
        hList = copy.deepcopy(vtecRecords)

        if len(hList):
            if self._productID in ['CWF', 'NSH', 'OFF', 'GLF']:
                hList.sort(self._tpc.marineSortHazardAlg)
            else:
                hList.sort(self._tpc.regularSortHazardAlg)

        while len(hList) > 0:
            vtecRecord = hList[0]

            # Can't make a section with vtecRecords with no 'hdln' entry 
            if vtecRecord.get('hdln') == '':
                hList.remove(vtecRecord)
                continue

            # make sure the vtecRecord is still in effect or within EXP criteria
            if (vtecRecord.get('act') != 'EXP' and self._issueTime_secs >= vtecRecord.get('endTime')) or \
            (vtecRecord.get('act') == 'EXP' and self._issueTime_secs > 30 * 60 + vtecRecord.get('endTime')):
                hList.remove(vtecRecord)
                continue

            # Add to sections - section can have multiple events
            sectionHazardEvents = []
            sectionMetaData = {}
            for metaData, hazardEvent in metaDataList:
                if hazardEvent.getEventID() in vtecRecord.get('eventID'):
                    sectionHazardEvents.append(hazardEvent)
                    sectionMetaData[hazardEvent.getEventID()] = metaData
            sections.append((vtecRecord, sectionMetaData, sectionHazardEvents))

            # always remove the main vtecRecord from the list
            hList.remove(vtecRecord)
        return sections

    def _createSectionDictionary(self, hazardEvents, vtecRecord, metaDataDictionary):
        '''
        Returns a dictionary that contains all the raw data associated with
        this section.

        @param hazardEvents: hazardEvents associated with the section
        @param vtecRecord:  vtecRecords for the section
        @param metaDataDictionary: metaData for the section - this is a dictionary with
                         eventIDs as keys and the associated metadata as the values.
        '''
        # Dictionary for this section
        section = collections.OrderedDict()

        # Create a list of dictionaries for each hazardEvent in the section
        hazardEventDicts = []
        for hazardEvent in hazardEvents:
            metaData = metaDataDictionary.get(hazardEvent.getEventID())
            if self._partialCAN or self._CONorEXTofEXA_EXB or self._EXA_EXB:
                hazardEventDict = self._prepareToCreateHazardEventDictionary(hazardEvent, vtecRecord, metaData)
            else:
                hazardEventDict = self._createHazardEventDictionary(hazardEvent, vtecRecord, metaData)
            hazardEventDicts.append(hazardEventDict)

        section['vtecRecord'] = vtecRecord
        section['hazardEvents'] = hazardEventDicts
        return section

    def _createHazardEventDictionary(self, hazardEvent, vtecRecord, metaData):
        '''
        Returns a dictionary that contains all the raw data associated with
        this hazardEvent/metaData.

        @param hazardEvent: hazardEvent associated with the section
        @param vtecRecord:  vtecRecord for the section
        @param metaData: metaData for the section - this is a dictionary with
                         eventIDs as keys and the associated metadata as the values.
        '''
        # Dictionary for this section
        hazardDict = collections.OrderedDict()

        attributes = hazardEvent.getHazardAttributes()
        for attribute in attributes:
            # Special case attributes that need additional work before adding to the dictionary
            if attribute == 'additionalInfo':
                additionalInfo = self._prepareAdditionalInfo(attributes[attribute] , hazardEvent, metaData)
                hazardDict['additionalComments'] = additionalInfo
            elif attribute == 'cta':
                # CTAs are gathered and displayed at the segment level, pass here
                pass
            elif attribute == 'floodSeverity':
                hazardDict['floodSeverity'] = attributes.get(attribute, None)
                hazardDict['floodSeverityName'] = self._tpc.getProductStrings(hazardEvent, metaData, 'floodSeverity')
            elif attribute == 'floodRecord':
                hazardDict['floodRecord'] = self._tpc.getProductStrings(hazardEvent, metaData, 'floodRecord')
            elif attribute == 'endingOption':
                hazardDict['endingOption'] = self._tpc.getProductStrings(hazardEvent, metaData, 'endingOption')
                hazardDict[attribute + "_identifiers"] = attributes.get(attribute)
            else:
                hazardDict[attribute] = attributes.get(attribute, None)
                # Add both the identifier and the productString for this attributes.
                # The identifiers are needed for the BasisText module.
                if attribute in ['advisoryType', 'warningType', 'optionalSpecificType']:
                    hazardDict[attribute + '_productString'] = self._tpc.getProductStrings(hazardEvent, metaData, attribute, attributes.get(attribute))

        # Add impacts to the dictionary
        if vtecRecord.get("phen") != "HY" and vtecRecord.get('sig') != 'S':
            # TODO this does not seem to work, causing the placeholder to be in final product.
            hazardDict['impacts'] = self._tpc.getProductStrings(hazardEvent, metaData, 'impacts')
        else:
            hazardDict['impacts'] = ''

        if self._productID != 'RVS':
            hazardDict['locationsAffected'] = self._prepareLocationsAffected(hazardEvent)
            hazardDict['locationDicts'] = self._prepareLocationDicts(hazardEvent)
            hazardDict['timeZones'] = self._productSegment.timeZones
            hazardDict['cityList'] = self._getCityList([hazardEvent])
            hazardDict['endingSynopsis'] = hazardEvent.get('endingSynopsis')
            hazardDict['subType'] = hazardEvent.getSubType()
            hazardDict['replacedBy'] = hazardEvent.get('replacedBy', None)
            hazardDict['replaces'] = hazardEvent.get('replaces', None)
            hazardDict['impactsStringForStageFlowTextArea'] = hazardEvent.get('impactsStringForStageFlowTextArea', None)

        hazardDict['eventID'] = hazardEvent.getEventID()
        hazardDict['phen'] = hazardEvent.getPhenomenon()
        hazardDict['sig'] = hazardEvent.getSignificance()
        hazardDict[HazardConstants.START_TIME] = hazardEvent.getStartTime()
        hazardDict[HazardConstants.END_TIME] = hazardEvent.getEndTime()
        hazardDict[HazardConstants.CREATION_TIME] = hazardEvent.getCreationTime()
        hazardDict['geometry'] = hazardEvent.getProductGeometry()
        # The user generating the product will not always necessarily be the
        # user that last modified the Hazard Event.
        hazardDict['userName'] = pwd.getpwuid(os.getuid()).pw_name

        if hazardEvent.get('pointID'):
            # Add RiverForecastPoint data to the dictionary
            self._prepareRiverForecastPointData(hazardEvent.get('pointID'), hazardDict, hazardEvent)

        if hazardEvent.get("locationsAffectedRadioButton") == "pathcast":
            # Only add the pathcast data if selected in the HID
            pathcastData = PathCastUtil.preparePathCastData(hazardEvent)
            hazardDict['pathcastData'] = pathcastData
            hazardDict['pathcastOtherPoints'] = self._preparePathcastOtherPoints(hazardEvent)

        if vtecRecord:
            self._setProductInformation([vtecRecord], [hazardEvent])
        return hazardDict

    def _showProductParts(self):
        # IF True will label the editable pieces in the Product Editor with product parts
        return False

    ######################################################
    #  Product Segment determination         
    ######################################################
    def _getSegments(self, hazardEvents):
        '''
        Determine the segments for the product
        @param hazardEvents: list of Hazard Events
        @return a list of segments for the hazard events
        '''
        self._generatedHazardEvents = self.determineShapeTypesForHazardEvents(hazardEvents)
        self.getVtecEngine(self._generatedHazardEvents)        
        segments = self._vtecEngine.getSegments()
        return segments

    def _getSegments_ForPointsAndAreas(self, hazardEvents):
        '''
        Gets the segments for point hazards and areal hazards separately
        
        Sets variables 
            self._pointEvents, self._point_productSegments
            self._areaEvents,  self._area_productSegments
            
        @param hazardEvents: list of Hazard Events
        @return a list of segments for the hazard events
                
        Separate the point events from the area event and create a separate VTEC engine for each.  
        This means we will put the area and point segments into different FFA products for now.
        However, if policy determines that they could be in one product, we can change that.
        
        For area events, segments look like this:
          [(frozenset(['FLZ051', 'FLZ048', 'FLZ049']), frozenset([1, 6])), 
           (frozenset(['FLZ030', 'FLZ031', 'FLZ032']), frozenset([2, 3,])]
         which indicates 2 segments:
        1 zones FLZ051, FLZ048, FLZ049,  comprised of eventIDs of 1 and 6
        2 zones FLZ030, FLZ031, FLZ032, comprised of eventIDs of 2 and 3
        Zones are never repeated, but eventIDs may be repeated.
        
        For the point events, segments look like this:
        [(frozenset(['FLC049']), frozenset([26])), 
          (frozenset(['FLC049']), frozenset([25]))]
        '''
        self._pointEvents = []
        self._areaEvents = []
        self._point_productSegments = []
        self._area_productSegments = []
        self._generatedHazardEvents = []
        for hazardEvent in hazardEvents:
            if hazardEvent.get('geoType') == 'point':
                self._pointEvents.append(hazardEvent)
            else:
                self._areaEvents.append(hazardEvent)
        for geoType in ('point', 'area'):
            if geoType == 'point' :   events = self._pointEvents
            else:                     events = self._areaEvents
            if not events: continue
            events = self.determineShapeTypesForHazardEvents(events)
            self._generatedHazardEvents += events
            self.getVtecEngine(events)
            segments = self._vtecEngine.getSegments()
            productSegments = []
            for segment in segments:
                vtecRecords = self.getVtecRecords(segment)
                productSegments.append(self.createProductSegment(segment, vtecRecords))
            if geoType == 'point':  
                self._point_productSegments = productSegments
                self._pointVtecEngine = self._vtecEngine
            else:
                self._area_productSegments = productSegments
                self._areaVtecEngine = self._vtecEngine
        return self._point_productSegments + self._area_productSegments

    @abstractmethod
    def _groupSegments(self, segments):
        '''
        Group the segments into the products
        
        Since the rules for grouping segments into products differs across product
        types, this method must be implemented by the Product Generator class
        @param segments
        @return: productSegmentGroups
           where productSegmentGroup contains specific information for generating the product 
                 including a list of ordered segments included in the product
       '''
        return []

    #### Utility methods
    def getVtecEngine(self, hazardEvents) :
        '''
        Instantiates a VTEC Engine for the given hazardEvents
        Note that more than one VTEC Engine may be instantiated for
        a product generator. For example, point and area hazardEvents
        must have separate VTEC Engines.
        @param hazardEvents -- list of hazard events
        '''
        self._vtecEngineWrapper = VTECEngineWrapper(
               self.bridge, self._productCategory, self._fullStationID,
               hazardEvents, vtecMode=self._vtecMode, issueTime=self._issueTime_secs,
               operationalMode=(not self._practice), testHarnessMode=False, vtecProduct=self._vtecProduct, 
               issueFlag=self._issueFlag, productGeneratorName= self._productGeneratorName)
        try :
            pass
        except :
            msg = 'Constructor for VTECEngineWrapper failed.'
            self.logger.info(msg)
        self._vtecEngine = self._vtecEngineWrapper.engine()
        self._wrappers.append(self._vtecEngineWrapper)

    def getVtecRecords(self, segment, vtecEngine=None):
        if not vtecEngine:
            vtecEngine = self._vtecEngine
        vtecRecords = copy.deepcopy(vtecEngine.getVtecRecords(segment))
        # Change times to milliseconds from seconds
        for vtecRecord in vtecRecords:
            for key in ['startTime', 'endTime', 'issueTime', 'riseAbove', 'crest', 'fallBelow']:
                value = vtecRecord.get(key)
                if value:
                    vtecRecord[key] = value * 1000
        return vtecRecords

    def _saveVTEC(self, hazardEvents):
        '''
        if issuing: 
            For each VTEC Engine generated in the product, save the vtec records 
        '''
        if str(self._issueFlag) == 'True':
            for wrapper in self._wrappers:
                wrapper.mergeResults() 
            self.logger.info(self._productCategory + ' Saving VTEC')
            # Handle Ended eventIDs 
            # Set the state to 'ended' for events that are completely canceled or expired.
            # Note that for some long-fused hazards e.g. FA.A, one eventID could be
            # associated with both a CAN and a NEW and we do not want to change the 
            # state to 'ended'.
            for hazardEvent in hazardEvents:
                vtecCodes = hazardEvent.get('vtecCodes', [])
                if ('CAN' in vtecCodes or 'EXP' in vtecCodes):
                    ended = True
                    for code in ['NEW', 'CON', 'EXA', 'EXT', 'EXB', 'UPG', 'ROU']:
                        if code in vtecCodes:
                            ended = False
                            break
                    if ended:
                        hazardEvent.setStatus('ENDED')

    def determineShapeTypesForHazardEvents(self, hazardEvents):
        '''
        For each hazard event, determine the shapeType
        to associate with its collection of geometries.
        @param hazardEvents -- list of hazard events
        @return newHazardEvents -- list of augmented hazard events with 
            entries added for the shape type and site ID
        '''
        #
        # TODO - This logic needs to be improved or replaced 
        # to reflect the fact that multiple geometries of different shape types may be
        # associated with a single hazard event
        newHazardEvents = []
        for hazardEvent in hazardEvents:
            # VTEC processing expects siteID4 e.g. KOAX instead of OAX
            hazardEvent.set('siteID4', str(self._fullStationID))

            geometryCollection = hazardEvent.getProductGeometry()
            
            for geometry in geometryCollection:
            
                geometryType = geometry.geom_type
            
                if geometryType in [HazardConstants.SHAPELY_POLYGON, HazardConstants.SHAPELY_MULTIPOLYGON]:
                    hazardEvent.set('shapeType', 'polygon')
                elif geometryType == HazardConstants.SHAPELY_LINE:
                    hazardEvent.set('shapeType', 'line')
                else:
                    hazardEvent.set('shapeType', 'point')

                    # Ensure the event has a pointID
                    if hazardEvent.get('pointID') is None:
                        hazardEvent.set('pointID', 'XXXXX')
                        
            newHazardEvents.append(hazardEvent)
 
        return newHazardEvents

    def _initializeProductDict(self, productDict, eventSetAttributes):
        siteID = eventSetAttributes['siteID']
        backupSiteID = eventSetAttributes['backupSiteID']

        productDict['productID'] = self._productID
        productDict['productName'] = self._productName
        productDict['productCategory'] = self._productCategory
        productDict['siteID'] = siteID
        productDict['backupSiteID'] = backupSiteID
        productDict['runMode'] = eventSetAttributes.get('runMode')
        productDict['issueFlag'] = self._issueFlag
        productDict['issueTime'] = self._issueTime
        productDict['purgeHours'] = self._purgeHours


    def _wrapUpProductDict(self, productDict):
        pass

    def _setupSegment(self):
        self._productSegment.metaDataList, self._productSegment.hazardEvents = self.getSegmentMetaData(self._productSegment.segment)
        # Assume that if there are multiple hazard events in 
        # this segment that they all have the same geoType.
        hazardEvent = self._productSegment.hazardEvents[0]
        if hazardEvent.get('geoType') == 'area':
           self._productSegment.ugcs = list(self._productSegment.segment[0])
        else:
            ugcSet = set()
            for hazard in self._productSegment.hazardEvents:
                ugcSet.update(hazard.get('ugcs', []))
            self._productSegment.ugcs = list(ugcSet)
        
        self._productSegment.ugcs.sort()
        self._productSegment.pointID = hazardEvent.get('pointID')
        self._productSegment.cityList = self._getCityList(self._productSegment.hazardEvents)
        self._productSegment.timeZones = self._tpc.hazardTimeZones(self._productSegment.ugcs)
        self._productSegment.expireTime = self._tpc.getExpireTime(self._issueTime, self._purgeHours, 
                                                                    self._productSegment.vtecRecords_ms)

        # Check for special case where a CAN/EXP is paired with a
        # NEW/EXA/EXB/EXT
        #
        # find any 'CAN' with non-CAN for reasons of text capture
        canVtecRecord = None
        for vtecRecord in self._productSegment.vtecRecords_ms:
            if vtecRecord['act'] in ['CAN', 'EXP', 'UPG']:
                canVtecRecord = vtecRecord
                break  # take the first one
        self._productSegment.canVtecRecord = canVtecRecord

    def _preparePathcastOtherPoints(self, hazardEvent):
        otherPointDicts = SpatialQuery.executeConfiguredQuery(hazardEvent['flattenedgeometry'],self._siteID,'PathcastOtherPoints')
        otherPoints = [point.get("name") for point in otherPointDicts]
        return otherPoints

    def _prepareLocationsAffected(self, hazardEvent):
        locationDicts = SpatialQuery.executeConfiguredQuery(hazardEvent['flattenedgeometry'],self._siteID,'LocationsAffected')
        locations = [loc.get("name") for loc in locationDicts]
        return locations

    def _prepareLocationDicts(self, hazardEvent):
        '''
        @param hazardEvent A HazardEvent object
        @return: List of dictionaries containing information about UGCs covered by the hazard
        '''
        hazardEventDict = {}
        hazardEventDict['ugcs'] = set(hazardEvent.getHazardAttributes().get('ugcs'))
        hazardEventDict['ugcPortions'] = hazardEvent.get('ugcPortions', {})
        hazardEventDict['ugcPartsOfState'] = hazardEvent.get('ugcPartsOfState', {})
        hazardEventDicts = [hazardEventDict]

        # Fill in these data structures
        portions = {}           # Map UGC to the part(s) of that UGC covered by the hazard.
        ugcPartsOfState = {}    # Map UGC to the part of the state in which the UGC is located.
        orderedUgcs = []        # Ordered strings containing UGCs
        self._tpc.makeUGCInformation(hazardEventDicts, portions, ugcPartsOfState, orderedUgcs)

        locationDicts = []

        for ougc in orderedUgcs :
            ugc = ougc.split("|")[1]

            entry = {}
            entry['ugc'] = ugc
            entry['entityName'] = self._tpc.getInformationForUGC(ugc, "entityName")
            entry['typeSingular'] = self._tpc.getInformationForUGC(ugc, "typeSingular")
            entry['typePlural'] = self._tpc.getInformationForUGC(ugc, "typePlural")
            entry['ugcPortions'] = portions.get(ugc)
            entry['ugcPartsOfState'] = ugcPartsOfState.get(ugc)
            entry['fullStateName'] = self._tpc.getInformationForUGC(ugc, "fullStateName")

            locationDicts.append(entry)

        return locationDicts

    def _getCityList(self, hazardEvents):
        cityList = []
        if not self._polygonBased: # area based
            # Get all the hazard types in the product
            hazardTypes = set()
            for hazardEvent in hazardEvents:
                hazardTypes.add(hazardEvent.getHazardType())

            # Check if accurateCities is set
            accurateCities = True
            # Check the HazardType entry for each
            for hazardType in hazardTypes:
                hazardTypeEntry = self.bridge.getHazardTypes(hazardType)
                if hazardTypeEntry.get("accurateCities", False) == False:
                    accurateCities = False;
                    break;
            if accurateCities:
                # This uses the cities from CityLocation.py
                cityInfo = self.getCityInfo(self._productSegment.ugcs, returnType='list')
                for city, latLon in cityInfo:
                    # Check if the Point is inside the Geometry
                    point = GeometryFactory.createPoint((latLon[1], latLon[0]))
                    for hazardEvent in hazardEvents:
                        if hazardEvent.getProductGeometry().contains(point):
                            cityList.append(city)
                            break
            else:
                # This uses ugcCities from the Area Dictionary
                for ugc in self._productSegment.ugcs:
                    cityList.extend(self._tpc.getInformationForUGC(ugc, "primaryLocations"))
        else: # polygon-based
            for hazardEvent in hazardEvents:
                cityList.extend(self._getCityListForPolygon(hazardEvent))
        return cityList

    def _getCityListForPolygon(self, hazardEvent):
        geometry = hazardEvent.getProductGeometry()
        columns = ["name", "warngenlev"]
        try :
            cityGeoms = self._tpc.mapDataQuery("city", columns, geometry)
        except :
            return []
        if not isinstance(cityGeoms, list) :
            return []
        names12 = []
        namesOther = []
        for cityGeom in cityGeoms :
            try:
                name = cityGeom.getLocationName()
                if not name:
                    continue
                levData = str(cityGeom.getString('warngenlev'))
                if levData == "1" or levData == "2" :
                      names12.append(name)
                else :
                      namesOther.append(name)
            except :
                pass
        if len(names12) > 0 :
            return names12
        if len(namesOther) > 0 :
            return namesOther
        return []

    def _prepareAdditionalInfo(self, attributeValue, event, metaData):
        additionalInfo = []
        if len(attributeValue) > 0:
            for identifier in attributeValue:
                if identifier:
                    additionalInfoText = ''
                    if identifier == 'listOfDrainages':
                        drainagesDicts = SpatialQuery.executeConfiguredQuery(event['flattenedgeometry'],self._siteID,'ListOfDrainages')
                        drainages = [drainage.get("streamname") for drainage in drainagesDicts]
                        drainages = self._tpc.formatDelimitedList(set(drainages))
                        productString = self._tpc.getProductStrings(event, metaData, 'additionalInfo', choiceIdentifier=identifier)
                        if len(drainages)== 0 or len(productString) == 0:
                            continue
                        additionalInfoText = productString + drainages + "."
                    elif identifier == 'listOfCities':
                        citiesListFlag = True
                    elif identifier == 'floodMoving':
                        additionalInfoText = self._tpc.getProductStrings(event, metaData, 'additionalInfo', choiceIdentifier=identifier,
                                        formatMethod=self.floodTimeStr, formatFramedValues=['additionalInfoFloodMovingTime'])
                    elif identifier == 'addtlRain':
                        additionalInfoText = self._tpc.getProductStrings(event, metaData, 'additionalInfo', choiceIdentifier=identifier)
                        additionalInfoText = self.checkAddtlRainStatement(additionalInfoText, event )
                    else:
                        additionalInfoText = self._tpc.getProductStrings(event, metaData, 'additionalInfo', choiceIdentifier=identifier)

                    # Add the additional info to the list if not None or empty.
                    if additionalInfoText:
                        additionalInfo.append(additionalInfoText)
        return additionalInfo

    def checkAddtlRainStatement(self, addtlRainString, hazardEvent):
        lowerVal = hazardEvent.get('additionalRainLowerBound')
        upperVal = hazardEvent.get('additionalRainUpperBound')
        lowerValStr = "{:2.1f}".format(lowerVal)
        upperValStr = "{:2.1f}".format(upperVal)
        # Remove trailing zeros and decimal place if not needed
        lowerValStr = lowerValStr.rstrip('0').rstrip('.') if '.' in lowerValStr else lowerValStr
        upperValStr = upperValStr.rstrip('0').rstrip('.') if '.' in upperValStr else upperValStr
        inchText = " inches"
        if upperVal == 1.0:
            inchText = " inch"
        if lowerVal == 0.0 and upperVal == 0.0:
            return ''
        elif lowerVal == 0.0:
            addtlRainString = " up to " + upperValStr
        elif upperVal == 0.0:
            addtlRainString = " up to " + lowerValStr
        elif upperVal == lowerVal:
            addtlRainString = " up to " + upperValStr
        else:
            addtlRainString = " of " + lowerValStr + ' to ' + upperValStr
        return "Additional rainfall amounts" + addtlRainString + inchText + " are possible in the warned area."

    def floodTimeStr(self, creationTime, hashTag, flood_time_ms):
        floodTime = flood_time_ms/1000
        # Make creationTime timezone aware
        utcCreationTime = creationTime.replace(tzinfo=tz.tzutc())
        utcFloodTime = datetime.datetime.fromtimestamp(floodTime, tz=tz.tzutc())
        localTimeZone = tz.gettz(self._productSegment.timeZones[0])
        localFloodTime = utcFloodTime.astimezone(localTimeZone)
        localCreationTime = utcCreationTime.astimezone(localTimeZone)
        tdelta = localFloodTime - localCreationTime
        if (tdelta.days == 6 and localFloodTime.date().weekday() == localCreationTime.date().weekday()) or \
            tdelta.days > 6:
            format = '%l%M %p %a %b %d'
        elif localCreationTime.day != localFloodTime.day:
            format = '%l%M %p %a'
        else:
            format = '%l%M %p'
        return localFloodTime.strftime(format).lstrip()

    def _setProductInformation(self, vtecRecords, hazardEvents, hazardEventDicts=[]):
        if self._issueFlag:
            for vtecRecord in vtecRecords:
                for hazardEvent in hazardEvents:
                    if hazardEvent.getEventID() in vtecRecord.get('eventID'):
                        # Update hazardEvent
                        expTime = hazardEvent.get('expirationTime')
                        # Take the earliest expiration time
                        if (expTime and expTime > self._productSegment.expireTime) or not expTime:
                            hazardEvent.set('expirationTime', self._productSegment.expireTime)
                        hazardEvent.set('issueTime', self._issueTime)
                        hazardEvent.addToList('etns', vtecRecord['etn'])
                        hazardEvent.addToList('vtecCodes', vtecRecord['act'])
                        hazardEvent.addToList('pils', vtecRecord['pil'])
                        # Only set the productLabel if it is not already set.
                        if not (hazardEvent.get('productLabel', None)):
                            hazardEvent.set('productLabel', self._productLabel)
                        # Get the value from the hazardEvent dictionary
                        for eventDict in hazardEventDicts:
                            if eventDict.get('eventID') == hazardEvent.getEventID():
                                hazardEvent.set('previousForecastCategory', eventDict.get('maxFcstCategory', None))
                                hazardEvent.set('previousObservedCategory', eventDict.get('observedCategory', None))
                                hazardEvent.set('previousForecastCategoryName', eventDict.get('maxFcstCategoryName', None))
                                hazardEvent.set('previousObservedCategoryName', eventDict.get('observedCategoryName', None))
                                hazardEvent.set('previousFloodSeverity', vtecRecord.get('hvtec', {}).get(HazardConstants.FLOOD_SEVERITY, None))
                                break

    def getSegmentHazardEvents(self, segments, hazardEventList=None):
        '''
        @param segments: List of segments
        @param hazardEventList: Set of hazardEvents 
        @return: Return a list of hazardEvents
        '''
        #  Each segment lists eventIDs, so collect those and then use
        #  getHazardEvents to get the hazardEvents.
        if not hazardEventList:
            hazardEventList = self._generatedHazardEvents

        eventIDs = []
        for segment in segments:
            ugcs, ids = segment
            eventIDs += ids
        hazardEvents = []
        for eventID in eventIDs:
            found = False
            for hazardEvent in hazardEventList:
                if hazardEvent.getEventID() == eventID:
                    hazardEvents.append(hazardEvent)
                    found = True
            if not found:
                # Must retrieve this hazard event for automatic cancellation
                hazardEvent = HazardDataAccess.getHazardEvent(eventID, self._practice)
                if hazardEvent is not None:
                    # Initialize the product-specific information
                    hazardEvent.removeHazardAttribute('expirationTime');
                    hazardEvent.removeHazardAttribute('vtecCodes');
                    hazardEvent.removeHazardAttribute('etns');
                    hazardEvent.removeHazardAttribute('pils');
                    hazardEvents.append(hazardEvent)  
                    hazardEventList.append(hazardEvent)
        return hazardEvents

    def getSegmentMetaData(self, segment) :
        '''
        @param: eventInfo
        '''
        # Get meta data for this segment
        #  May need to get multiple hazardEvents and meta data
        metaDataList = []
        segmentEvents = self.getSegmentHazardEvents([segment])
        for hazardEvent in segmentEvents:
            metaDataList.append((self.getHazardMetaData(hazardEvent), hazardEvent))
        return metaDataList, segmentEvents

    def getMetaData(self, hazardEvents, metaDict, metaDataFileName): 
        eventDicts = []
        for hazardEvent in hazardEvents:
            eventDict = {}
            eventDict['eventID'] = hazardEvent.getEventID()
            eventDict['hazardType'] = hazardEvent.getHazardType()
            eventDict['status'] = hazardEvent.getStatus()
            eventDict['immediateCause'] = hazardEvent.get('immediateCause')
            eventDicts.append(eventDict)
        criteria = {'dataType':'metaData', 'fileName':metaDataFileName, 'site':self._siteID}
        metaData = self.bridge.getData(json.dumps(criteria)) 
        if type(metaData) is not types.ListType:
            metaData = metaData.execute(eventDicts, metaDict) 
        return metaData

    def getHazardMetaData(self, hazardEvent):
        phen = hazardEvent.getPhenomenon()
        sig = hazardEvent.getSignificance()
        subType = hazardEvent.getSubType()
        criteria = {'dataType':'hazardMetaData_filter',
                'filter':{'phen':phen, 'sig':sig, 'subType':subType, "site":self._siteID}
                }
        metaData, filePath = self.bridge.getData(json.dumps(criteria))
        if type(metaData) is not types.ListType:
            metaData = metaData.execute(hazardEvent, {})
        return metaData

    def getCityInfo(self, ugcs, returnType='string'):
        '''
        @param ugcs -- list of ugc codes
        @param returnType -- can be list or string
        @return list or string of cities within the ugc areas
        '''
        cities = ''
        cityList = []
        for ugc in ugcs:
            ugcCities = self._cityLocation.get(ugc)
            if not ugcCities: return ''
            for city in ugcCities:
                cities += city + '...'
                cityList.append((city, ugcCities.get(city)))
        if returnType == 'string':
            return cities
        else:
            return cityList

    def correctProduct(self, dataList, eventSet, keyInfo, correctAllSegments):
        millis = SimulatedTime.getSystemTime().getMillis()
        dt = datetime.datetime.utcfromtimestamp(millis / 1000)
        currentTime = dt.strftime('%d%H%m')

        for hazardEvent in eventSet:
            # Explicitly remove the attribute because if this python
            # HazardEvent is backed by a HazardEvent.java (vs
            # BaseHazardEvent.java), then adding an attribute only adds to the
            # attribute collection.  It does not replace the existing
            # attribute.
            hazardEvent.removeHazardAttribute('issueTime')
            hazardEvent.set('issueTime', millis)

        for i in range(0, len(dataList)):
            data = dataList[i]
            data['startTime'] = currentTime
            data['correction'] = True
            data['issueTime'] = millis
            segments = data.get('segments')
            for j in range(0, len(segments)):
                segment = segments[j]
                if correctAllSegments:
                    segment = self.correctSegment(segment, millis)

                else:
                    if self.correctionsCheck(segment, keyInfo):
                        segment = self.correctSegment(segment, millis)
        return dataList

    def correctSegment(self, segment, millis):
         if 'vtecRecords' in segment:
            vtecRecords = segment.get('vtecRecords')
            for vtecRecord in vtecRecords:
                self.correctVtecRecord(vtecRecord, millis)
         if 'sections' in segment:
            sections = segment.get('sections')
            for section in sections:
                vtecRecord = section.get('vtecRecord')
                self.correctVtecRecord(vtecRecord, millis)
         return segment
    
    def correctVtecRecord(self, vtecRecord, millis):
        action = vtecRecord['act']
        vtecRecord['act'] = 'COR'
        # Do not set prevAct to COR
        # incase 2 corrections occur.
        if action != 'COR':
            vtecRecord['prevAct'] = action
        vtecString = vtecRecord['vtecstr']
        updatedVtecString = vtecString.replace(action, 'COR')
        vtecRecord['vtecstr'] = updatedVtecString
        
        vtecRecord['issueTime'] = millis

    def correctionsCheck(self, segmentDict, keyInfo):
        '''
            Returns True if the correction should be applied to the segment.
            It determines this by comparing the eventIDs UGCs in the keyInfo
            object the eventIDs and UGCs in the VTEC records for the segment.
            If the segment contains all of both the keyInfo object belongs 
            to this segment.
        '''
        # Get the eventIDs and UGC from the KeyInfo object
        eventIDStrings = JUtil.javaObjToPyVal(keyInfo.getEventIDs())
        ugcString = JUtil.javaObjToPyVal(keyInfo.getSegment())

        if (not eventIDStrings) or (not ugcString):
            # No Events or UGCs, so nothing to correct.  This can happen in the
            # case of product correction because a final call to executeFrom
            # (with an empty KeyInfo) is made to refresh the issueTime.
            return False

        # Convert eventIds to strings
        eventIDs = set()
        for eventID in eventIDStrings:
            eventIDs.add(eventID)

        # Make a set out of the ugc string
        ugcs = ugcString.split(',')
        ugcSet = set()
        for ugc in ugcs:
            ugcSet.add(ugc.lstrip())

        # Get the eventIDs and UGCs from the segment dictionary
        segmentEventIDs = set()
        segmentUGCs = set(segmentDict.get('ugcs', []))
        for vtecRecord in segmentDict.get('vtecRecords', []):
            segmentEventIDs.update(set(vtecRecord.get('eventID', [])))

        # If the segment contains both the eventIDs and ugcs from the keyInfo
        # assume that that keyInfo productPart belongs to this segment.
        if eventIDs.issubset(segmentEventIDs) and ugcSet.issubset(segmentUGCs):
            return True
        return False

    def checkSegment(self, productSegmentGroup):
        '''
            The below method checks for special segments that 
            require additional processing to create an accurate
            dictionary. For example the hazard geometries need
            updated since the hazard objects only hold the most
            recent polygon which is incorrect in some cases.

            Checks for:
                EXA/EXB segment
                CAN segment of Partial Cancellation
                CON or EXT segment of EXA/EXB
        '''
        # Always reset flags to false
        self._EXA_EXB = False
        self._partialCAN = False
        self._CONorEXTofEXA_EXB = False
        vtecRecords = self._productSegment.vtecRecords

        for vtecRecord in vtecRecords:
            act = vtecRecord.get('act')
            if act in ['EXA', 'EXB']:
                self._EXA_EXB = True
                return
            eventIDs = vtecRecord.get('eventID')
            if act in ['CAN', 'CON', 'EXT']:
                # Check the other segments for same eventID
                # indicating the same event is in multiple segments.
                for productSegment in productSegmentGroup.productSegments:
                    # Dont check the current segment
                    if productSegment == self._productSegment:
                        continue
                    else:
                        otherVtecRecords = productSegment.vtecRecords
                        for otherVtecRecord in otherVtecRecords:
                            if eventIDs.issubset(otherVtecRecord.get('eventID')):
                                if act == 'CAN':
                                    self._partialCAN =  True
                                elif otherVtecRecord.get('act') in ['EXA', 'EXB']:
                                    self._CONorEXTofEXA_EXB = True
                                return

    def _prepareToCreateHazardEventDictionary(self, hazardEvent, vtecRecord, metaData):
        eventID = hazardEvent.getEventID()

        # Get the previous state of this hazard event
        prevHazardEvent = HazardDataAccess.getHazardEvent(eventID, self._practice)
        if prevHazardEvent is not None:
            prevEventStatus = prevHazardEvent.getStatus()
            if (prevEventStatus != "PROPOSED") and \
                (prevEventStatus != "PENDING"):
                # Events with proposed or pending status do not have enough
                # information available (such as UGCs) to use the below
                # functions.

                # Get the attributes of both hazardEvents]
                prevAttributes = prevHazardEvent.getHazardAttributes()
                attributes = hazardEvent.getHazardAttributes()

                if self._CONorEXTofEXA_EXB:
                    return self._createHazardEventDictionary_forCONorEXTofEXA_EXB(hazardEvent, prevHazardEvent, attributes,
                                                                                  prevAttributes, vtecRecord, metaData)
                elif self._EXA_EXB and prevAttributes is not None:
                    return self._createHazardEventDictionary_forEXA_EXB(hazardEvent, prevHazardEvent, attributes,
                                                                        prevAttributes, vtecRecord, metaData)
                elif self._partialCAN:
                    return self._createHazardEventDictionary_forPartialCancellation(hazardEvent, prevHazardEvent, attributes,
                                                                                    prevAttributes, vtecRecord, metaData)
            else:
                return self._createHazardEventDictionary(hazardEvent, vtecRecord, metaData)
        else:
            return self._createHazardEventDictionary(hazardEvent, vtecRecord, metaData)

    def _createHazardEventDictionary_forEXA_EXB(self, hazardEvent, prevHazardEvent, attributes, prevAttributes, vtecRecord, metaData):
        '''
            Creates a event dictionary for a EXA or EXB segment. It correctly adjusts
            the geometry and ugcs since the current polygon is incorrect for this segment.
        '''
        # Geometry/UGCs for the EXA/EXB hazard
        geometry = hazardEvent.getFlattenedGeometry().difference(prevHazardEvent.getFlattenedGeometry())
        prevUGCs = set(prevAttributes.get('ugcs'))
        currentUGCs = set(attributes.get('ugcs'))
        ugcs = list(currentUGCs.difference(prevUGCs))

        # Determine the portions and partsOfState for the CAN hazard
        ugcPortions = {}
        ugcPartsOfState = {}
        for ugc in ugcs:
            if attributes.get('ugcPortions', None):
                ugcPortions[ugc] = attributes.get('ugcPortions').get(ugc)
            if attributes.get('ugcPartsOfState', None):
                ugcPartsOfState[ugc] = attributes.get('ugcPartsOfState').get(ugc)

        # Update the prevAttributes to reflect the current EXA/EXB segment
        attributes['ugcs'] = ugcs
        attributes['ugcPortions'] = ugcPortions
        attributes['ugcPartsOfState'] = ugcPartsOfState

        # Store the updated attributes back in the hazard object
        hazardEvent.setHazardAttributes(attributes)

        # Update the geometry as well
        hazardEvent.setProductGeometry(GeometryFactory.createCollection([geometry]))

        # Call the original method with the updated hazardEvent to get the hazard dictionary.
        return self._createHazardEventDictionary(hazardEvent, vtecRecord, metaData)

    def _createHazardEventDictionary_forCONorEXTofEXA_EXB(self, hazardEvent, prevHazardEvent, attributes, prevAttributes, vtecRecord, metaData):
        '''
            Creates a event dictionary for a CON or EXT segment resulting from a EXA or EXB. 
            It correctly adjusts the geometry and ugcs since the current polygon is incorrect
            for this segment.
        '''
        # Update the Attributes to reflect the current CON segment
        attributes['ugcs'] = prevAttributes.get('ugcs', [])
        attributes['ugcPortions'] = prevAttributes.get('ugcPortions', {})
        attributes['ugcPartsOfState'] = prevAttributes.get('ugcPartsOfState', {})

        # Store the updated attributes back in the hazard object
        hazardEvent.setHazardAttributes(attributes)

        # Update the geometry as well
        hazardEvent.setProductGeometry(prevHazardEvent.getFlattenedGeometry())

        # Call the original method with the updated hazardEvent to get the hazard dictionary.
        return self._createHazardEventDictionary(hazardEvent, vtecRecord, metaData)

    def _createHazardEventDictionary_forPartialCancellation(self, hazardEvent, prevHazardEvent, attributes, prevAttributes, vtecRecord, metaData):
        '''
            Creates a event dictionary for a CAN segment resulting from a partial cancellation. 
            It correctly adjusts the geometry and ugcs since the current polygon is incorrect
            for this segment.
        '''
        # Geometries/UGCs for the CAN hazard
        # As of DR #18278, this is no longer calculating the difference in geometries for CAN/CONs
        # When WarnGen produces CAN/CON products, it places the same geometry (the CON geometry)
        # in the product for both the CAN and CON.  This was changed to reflect that behavior.
        # If the geometry of the cancelled portion is included and the product is displayed in WarnGen
        # the cancelled portion will erroneously displayed as the continued portion
        geometry = hazardEvent.getProductGeometry()

        prevUGCs = set(prevAttributes.get('ugcs'))
        currentUGCs = set(attributes.get('ugcs'))
        ugcs = list(prevUGCs.difference(currentUGCs))

        # Determine the portions and partsOfState for the CAN hazard
        ugcPortions = {}
        ugcPartsOfState = {}
        for ugc in ugcs:
            if prevAttributes.get('ugcPortions', None):
                ugcPortions[ugc] = prevAttributes.get('ugcPortions').get(ugc)
            if prevAttributes.get('ugcPartsOfState', None):
                ugcPartsOfState[ugc] = prevAttributes.get('ugcPartsOfState').get(ugc)

        # Update the prevAttributes to reflect the current CAN section
        prevAttributes['ugcs'] = ugcs
        prevAttributes['ugcPortions'] = ugcPortions
        prevAttributes['ugcPartsOfState'] = ugcPartsOfState

        # Store the updated attributes back in the hazard object
        prevHazardEvent.setHazardAttributes(prevAttributes)

        # Update the geometry as well
        if geometry:
            prevHazardEvent.setProductGeometry(geometry)

        # Call the original method with the updated prevHazardEvent to get the section dictionary.
        return self._createHazardEventDictionary(prevHazardEvent, vtecRecord, metaData)

    def getCTAsPhrase(self, hazardEvent, metaData):
        ctas = self._tpc.getProductStrings(hazardEvent, metaData, 'cta')
        validCTAs = []
        for cta in ctas:
            if cta:
                validCTAs.append(cta)
        return validCTAs

    def flush(self):
        ''' Flush the print buffer '''
        os.sys.__stdout__.flush()

    def updateDataList(self, dataList, eventSet):
        '''
        Takes the dataList (product dictionaries) from a previous execution of the generator and updates them. 
        '''
        self._initialize()

        # Extract information for update
        self._getVariables(eventSet)

        productDicts, hazardEvents = self.updateProductDictionaries(dataList, self._inputHazardEvents)
        return productDicts, hazardEvents 

    def updateProductDictionaries(self, dataList, hazardEvents):
        '''
        Updates each productDictionary with the updated VTECRecords,
        database values, etc.
        '''
        # Determine the list of segments given the hazard events
        segments = []
        if self._productID is not 'RVS':
            segments = self._getSegments(hazardEvents)
        else:
            self._generatedHazardEvents = self.determineShapeTypesForHazardEvents(hazardEvents)

        # Determine the list of products and associated segments given the segments
        productSegmentGroups = self._groupSegments(segments)

        # Update each product dictionary
        productCounter = 0
        for productSegmentGroup in productSegmentGroups:
            self._productID = productSegmentGroup.productID
            self._productLabel = productSegmentGroup.productLabel
            # Get the corresponding productDictionary from the dataList
            productDictionary = dataList[productCounter]

            # Update the Issue Time/Flag
            productDictionary['issueTime'] = self._issueTime
            productDictionary['issueFlag'] = self._issueFlag
            purgeHours = productDictionary.get('purgeHours', self._purgeHours)

            segmentCounter = 0
            for productSegment in productSegmentGroup.productSegments:
                self._productSegment = productSegment
                # Get the corresponding segmentDict from the dataList
                segmentDict = productDictionary.get('segments')[segmentCounter]

                productSegment.metaDataList, productSegment.hazardEvents = self.getSegmentMetaData(productSegment.segment)
                self._productSegment.expireTime = self._tpc.getExpireTime(self._issueTime, purgeHours, 
                                                                          self._productSegment.vtecRecords_ms)

                # Create and order the sections for the segment:     
                productSegment.sections = self._createSections(productSegment.vtecRecords_ms, productSegment.metaDataList)

                # What segmentDict level info needs updated here?
                segmentDict['vtecRecords'] = productSegment.vtecRecords
                segmentDict['expireTime'] = self._tpc.getExpireTime(self._issueTime, self._purgeHours, 
                                                                productSegment.vtecRecords_ms)

                sectionCounter = 0
                for productSection in productSegment.sections:
                    sectionVtecRecord, sectionMetaData, sectionHazardEvents = productSection
                    # Get the corresponding sectionDict from the dataList
                    sectionDict = segmentDict.get('sections')[sectionCounter]
                    sectionDict['vtecRecord'] = sectionVtecRecord

                    # Update the RFP values for each hazard
                    for hazardDict in sectionDict.get('hazardEvents', []):
                        # Find the corresponding sectionHazardEvent
                        for hazard in sectionHazardEvents:
                            if hazard.getEventID() == hazardDict.get('eventID'):
                                # Update the times only
                                hazardDict[HazardConstants.START_TIME] = hazard.getStartTime()
                                hazardDict[HazardConstants.END_TIME] = hazard.getEndTime()
                                if hazard.get('pointID', None):
                                    self._prepareRiverForecastPointData(hazard.get('pointID'), hazardDict, hazard)
                                break
                    sectionCounter = sectionCounter + 1

                    # Update the Product Information
                    self._setProductInformation([sectionVtecRecord], sectionHazardEvents, sectionDict.get('hazardEvents', None))
                segmentCounter = segmentCounter + 1
            productCounter = productCounter + 1

        # If issuing, save the VTEC records for legacy products
        self._saveVTEC(self._generatedHazardEvents)
        return dataList, self._generatedHazardEvents

    def updateExpireTimes(self, dataList):
        self._initialize()
        for productDictionary in dataList:
            issueTime = productDictionary.get("issueTime")
            purgeHours = productDictionary.get('purgeHours')
            for segment in productDictionary.get("segments", []):
                vtecRecords = segment.get("vtecRecords", [])
                expireTime = self._tpc.getExpireTime(issueTime, purgeHours, vtecRecords)
                segment[HazardConstants.EXPIRATION_TIME] = expireTime