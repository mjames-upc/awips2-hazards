import collections
import HydroGenerator

class Product(HydroGenerator.Product):

    def __init__(self) :
        ''' Hazard Types covered
             ('FL.W', "Flood"),
             ('FL.Y', "Flood"),
             ('FL.A', "Flood"),
             ('HY.S', "Flood"),
        '''
        super(Product, self).__init__()

        # Used by the VTECEngineWrapper to access the productGeneratorTable
        self._productGeneratorName = 'RVS_ProductGenerator'

    def _initialize(self) :
        super(Product, self)._initialize()
        self._productID = 'RVS'
        self._productCategory = 'RVS'
        self._RVS_ProductName = "RVS Product"
        self._productName = 'RVS Product'
        self._purgeHours = 8
        self._includeAreaNames = False
        self._includeCityNames = False
        
        self._vtecProduct = False

    def defineScriptMetadata(self):
        metadata = collections.OrderedDict()
        metadata['author'] = 'Raytheon'
        metadata['description'] = 'Content generator for RVS.'
        metadata['version'] = '1.0'
        return metadata
       
    def defineDialog(self, eventSet):
        """
        @return: dialog definition to solicit user input before running tool
        """  
        dialogDict = {"title": "RVS Product"}

        headlineStatement = {
             "fieldType": "Text",
             "fieldName": "headlineStatement",
             "expandHorizontally": True,
             "visibleChars": 25,
             "lines": 1,
             "values": "|* Enter Headline Statement *|",
            } 

        narrativeInformation = {
             "fieldType": "Text",
             "fieldName": "narrativeInformation",
             "expandHorizontally": True,
             "visibleChars": 25,
             "lines": 25,
             "values": "|* Enter Narrative Information *|",
            } 
                        
        fieldDicts = [headlineStatement, narrativeInformation]
        dialogDict["metadata"] = fieldDicts        
        return dialogDict


    def executeFrom(self, dataList, keyInfo=None):
        if keyInfo is not None:
            dataList = self.correctProduct(dataList, keyInfo, False)
        return dataList

    def execute(self, eventSet, dialogInputMap):
        '''
        Inputs:
        @param eventSet: a list of hazard events (hazardEvents) plus
                               a map of additional variables
        @return productDicts, hazardEvents: 
             Each execution of a generator can produce 1 or more 
             products from the set of hazard events
             For each product, a productID and one dictionary is returned as input for 
             the desired formatters.
             Also, returned is a set of hazard events, updated with product information.

        '''
        self._initialize() 
        self.logger.info("Start ProductGeneratorTemplate:execute RVS")
        
        # Extract information for execution
        self._getVariables(eventSet, dialogInputMap)
        eventSetAttributes = eventSet.getAttributes()

        rvsHazardEvents = []
        for hazardEvent in self._inputHazardEvents:
            if hazardEvent.getHazardType not in ['FL.W', 'FL.Y', 'FL.A', 'HY.S']:
                rvsHazardEvents.append(hazardEvent)  
        self._dialogInputMap = dialogInputMap          
        productDicts, hazardEvents = self._makeProducts_FromHazardEvents(rvsHazardEvents, eventSetAttributes) 

        return productDicts, hazardEvents

        
    def _getSegments(self, hazardEvents):
        '''
        @param hazardEvents: list of Hazard Events
        @return a list of segments for the hazard events
        '''
        return self._getSegments_ForPointsAndAreas(hazardEvents)
    
    def _groupSegments(self, segments):
        '''
        RVS products do not have segments, so create a productSegmentGroup with no segments. 
        '''        
        productSegmentGroups = []
        productSegmentGroups.append(self.createProductSegmentGroup('RVS', self._RVS_ProductName, 'area', None, 'counties', False, [])) 
        for productSegmentGroup in productSegmentGroups:
            self._addProductParts(productSegmentGroup)
        return productSegmentGroups
    
    def _addProductParts(self, productSegmentGroup):
        productSegments = productSegmentGroup.productSegments
        productSegmentGroup.setProductParts(self._hydroProductParts._productParts_RVS(productSegments))

    def _createProductLevelProductDictionaryData(self, productDict):
        hazardEventsList = self._generatedHazardEvents
        if hazardEventsList is not None:
            hazardEventDicts = []
            for hazardEvent in hazardEventsList:
                metaData = self.getHazardMetaData(hazardEvent)
                hazardEventDict = self._createHazardEventDictionary(hazardEvent, {}, metaData)
                hazardEventDicts.append(hazardEventDict)
            productDict['hazardEvents'] = hazardEventDicts

        ugcs = []
        eventIDs = []
        for hazardEvent in self._generatedHazardEvents:
            ugcs.extend(hazardEvent.get('ugcs'))
            eventIDs.append(hazardEvent.getEventID())
        productDict['ugcs'] = ugcs
        productDict['eventIDs'] = eventIDs
        timezones = self._tpc.hazardTimeZones(ugcs)
        productDict['timezones'] = timezones

        expireTime = self._tpc.getExpireTime(self._issueTime, self._purgeHours, [], fixedExpire=True)
        productDict['expireTime'] = expireTime

    def _createAndAddSegmentsToDictionary(self, productDict, productSegmentGroup):
        pass
    
    def _headlineStatement(self, productDict, productSegmentGroup, arguments=None):        
        productDict['headlineStatement'] =  self._dialogInputMap.get('headlineStatement')

    def _narrativeInformation(self, productDict, productSegmentGroup, arguments=None):        
        productDict['narrativeInformation'] =  self._dialogInputMap.get('narrativeInformation')

