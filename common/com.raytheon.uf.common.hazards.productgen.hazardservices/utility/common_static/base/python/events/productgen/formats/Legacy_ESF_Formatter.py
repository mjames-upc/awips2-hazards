'''
    Description: Legacy formatter for hydro ESF products
    
    SOFTWARE HISTORY
    Date         Ticket#    Engineer    Description
    ------------ ---------- ----------- --------------------------
    Apr 30, 2015    7579    Robert.Blum Changes for multiple hazards per section.
'''
import FormatTemplate

import types, re, sys, collections
from KeyInfo import KeyInfo
from com.raytheon.uf.common.hazards.productgen import ProductUtils
import Legacy_Hydro_Formatter

class Format(Legacy_Hydro_Formatter.Format):

    def initialize(self) :
        super(Format, self).initialize()
        self.initProductPartMethodMapping()
        
    def initProductPartMethodMapping(self):
        self.productPartMethodMapping = {
            'wmoHeader': self._wmoHeader,
            'ugcHeader': self._ugcHeader,
            'easMessage': self._easMessage,
            'productHeader': self._productHeader,
            'wmoHeader': self._wmoHeader,
            'ugcHeader': self._ugcHeader,
            'narrativeForecastInformation': self._narrativeForecastInformation
                                }

    def execute(self, productDict):
        self.productDict = productDict
        self.initialize()
        legacyText = self._createTextProduct()
        return [ProductUtils.wrapLegacy(legacyText)], self._editableParts

    ######################################################
    #  Product Part Methods 
    ######################################################

    ################# Product Level

    ################# Segment Level

    ################# Section Level    
    def _narrativeForecastInformation(self, sectionDict):
        # Get saved value from productText table if available
        narrative = self._getSavedVal('narrativeForecastInformation', sectionDict)
        if not narrative:
            # ESF sections will only contain one hazard
            hazard = sectionDict.get('hazardEvents')[0]
            narrative = hazard.get('narrativeForecastInformation', '')
        self._setVal('narrativeForecastInformation', narrative, sectionDict, 'Narrative Forecast Information')
        return narrative
    