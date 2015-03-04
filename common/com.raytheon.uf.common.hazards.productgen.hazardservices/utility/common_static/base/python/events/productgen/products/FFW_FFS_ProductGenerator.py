import collections
from KeyInfo import KeyInfo
import HydroGenerator

'''
Description: Product Generator for the FFW and FFS products.

SOFTWARE HISTORY
Date         Ticket#    Engineer    Description
------------ ---------- ----------- --------------------------
April 5, 2013            Tracy.L.Hansen      Initial creation
Nov      2013  2368      Tracy.L.Hansen      Changing from eventDicts to hazardEvents, simplifying product
                                             dictionary
Oct 24, 2014   4933      Robert.Blum         Implement Product Generation Framework v3
Dec 18, 2014   4933      Robert.Blum         Fixing issue with rebase conflict that was missed in previous checkin.
Jan 12, 2015   4937      Robert.Blum         Refactor to use new generator class hierarchy 
                                             introduced with ticket 4937.
Jan 31, 2015   4937      Robert.Blum         General cleanup and minor bug fixes.

@author Tracy.L.Hansen@noaa.gov
@version 1.0
'''

class Product(HydroGenerator.Product):

    def __init__(self) :
        ''' Hazard Types covered
             ('FF.W.Convective',     'Flood'),
             ('FF.W.NonConvective',  'Flood'), 
             ('FF.W.BurnScar',       'Flood'), 
        '''
        super(Product, self).__init__()

        # Used by the VTECEngineWrapper to access the productGeneratorTable
        self._productGeneratorName = 'FFW_FFS_ProductGenerator'

    def _initialize(self) :
        super(Product, self)._initialize()
        self._productID = 'FFW'
        self._productCategory = 'FFW_FFS'
        self._productName = 'Flash Flood Warning'
        self._purgeHours = -1
        self._FFW_ProductName = 'Flash Flood Warning'
        self._FFS_ProductName = 'Flash Flood Statement'
        self._includeAreaNames = True
        self._includeCityNames = False

    def defineScriptMetadata(self):
        metadata = collections.OrderedDict()
        metadata['author'] = 'Raytheon'
        metadata['description'] = 'Content generator for flash flood warning.'
        metadata['version'] = '1.0'
        return metadata

    def defineDialog(self, eventSet):
        return {}

    def executeFrom(self, dataList, prevDataList=None):
        if prevDataList is not None:
            dataList = self.correctProduct(dataList, prevDataList, True)
        return dataList

    def execute(self, eventSet, dialogInputMap):
        self._initialize()

        self.logger.info('Start ProductGeneratorTemplate:execute FFW_FFS')

        # Extract information for execution
        self._getVariables(eventSet, dialogInputMap)
        eventSetAttributes = eventSet.getAttributes()

        if not self._inputHazardEvents:
            return []

        productDicts, hazardEvents = self._makeProducts_FromHazardEvents(self._inputHazardEvents, eventSetAttributes)

        return productDicts, hazardEvents

    def _preProcessHazardEvents(self, hazardEvents):
        '''
        Set Immediate Cause for FF.W.NonConvective prior to VTEC processing
        '''
        for hazardEvent in hazardEvents:
            if hazardEvent.getHazardType() == 'FF.W.NonConvective':
                immediateCause = self.hydrologicCauseMapping(hazardEvent.get('hydrologicCause'))
                hazardEvent.set('immediateCause', immediateCause)

    def _prepareSection(self, event, vtecRecord, metaData):
        attributes = event.getHazardAttributes()

        # This creates a list of ints for the eventIDs and also formats the UGCs correctly.
        eventIDs, ugcList = self.parameterSetupForKeyInfo(list(vtecRecord.get('eventID', None)), attributes.get('ugcs', None))

        # Attributes that get skipped. They get added to the dictionary indirectly.
        noOpAttributes = [] # Needed for attribution / firstBullet ['ugcs', 'ugcPortions', 'ugcPartsOfState']

        section = collections.OrderedDict()
        for attribute in attributes:
            # Special case attributes that need additional work before adding to the dictionary
            if attribute == 'additionalInfo':
                additionalInfo, citiesListFlag = self._prepareAdditionalInfo(attributes[attribute] , event, metaData)
                additionalCommentsKey = KeyInfo('additionalComments', self._productCategory, self._productID, eventIDs, ugcList, editable=True, label='Additional Comments')
                section[additionalCommentsKey] = additionalInfo
                section['citiesListFlag'] = citiesListFlag
            elif attribute == 'cta':
                # These are now added at the segment level. Do we want to add here as well?
                callsToActionValue = self._tpc.getProductStrings(event, metaData, 'cta')
                section['callsToAction'] = callsToActionValue
            elif attribute in noOpAttributes:
                continue
            else:
                section[attribute] = attributes.get(attribute, None)

        impactedLocationsKey = KeyInfo('impactedLocations', self._productCategory, self._productID, eventIDs, ugcList,True,label='Impacted Locations')
        impactedLocationsValue = self._prepareImpactedLocations(event.getGeometry())
        section[impactedLocationsKey] = impactedLocationsValue

        section['impactedAreas'] = self._prepareImpactedAreas(attributes)
        section['geometry'] = event.getGeometry()
        section['subType'] = event.getSubType()
        section['timeZones'] = self._productSegment.timeZones
        section['vtecRecord'] = vtecRecord
        section['startTime'] = event.getStartTime()
        section['endTime'] = event.getEndTime()
        section['metaData'] = metaData
        section['creationTime'] = event.getCreationTime()

        self._setProductInformation(vtecRecord, event)
        return section

    def _groupSegments(self, segments):
        '''
         Group the segments into the products
            return a list of productSegmentGroup dictionaries
        
         Check the pil 
          IF FFW -- make a new FFW -- there can only be one segment per FFW
          Group the segments into FFS products with same ETN
        '''
        # For short fused areal products, 
        #   we can safely make the assumption of only one hazard/action per segment.
        productSegmentGroups = []
        for segment in segments:
            vtecRecords = self.getVtecRecords(segment)
            for vtecRecord in vtecRecords:  # NOTE there is only one vtecRecord / hazard to process
                pil = vtecRecord.get('pil')
                etn = vtecRecord.get('etn')
                if pil == 'FFW':
                    # Create new FFW
                    productSegmentGroup = self.createProductSegmentGroup(pil, self._FFW_ProductName, 'area', self._vtecEngine, 'counties', False,
                                            [self.createProductSegment(segment, vtecRecords)], etn=etn, formatPolygon=True)
                    productSegmentGroups.append(productSegmentGroup)
                else:  # FFS
                    # See if this record matches the ETN of an existing FFS
                    found = False
                    for productSegmentGroup in productSegmentGroups:
                        if productSegmentGroup.productID == 'FFS' and productSegmentGroup.etn == etn:
                            productSegmentGroup.addProductSegment(self.createProductSegment(segment, vtecRecords))
                            found = True
                    if not found:
                        # Make a new FFS productSegmentGroup
                       productSegmentGroup = self.createProductSegmentGroup(pil, self._FFS_ProductName, 'area', self._vtecEngine, 'counties', True,
                                                [self.createProductSegment(segment, vtecRecords)], etn=etn, formatPolygon=True)
                       productSegmentGroups.append(productSegmentGroup)
        for productSegmentGroup in productSegmentGroups:
            self._addProductParts(productSegmentGroup)
        return productSegmentGroups

    def _addProductParts(self, productSegmentGroup):
        productID = productSegmentGroup.productID
        productSegments = productSegmentGroup.productSegments
        if productID == 'FFW':
            productSegmentGroup.setProductParts(self._hydroProductParts._productParts_FFW(productSegments))
        elif productID == 'FFS':
            productSegmentGroup.setProductParts(self._hydroProductParts._productParts_FFS(productSegments))
