'''
    Description: Hazard Information Dialog Metadata for hazard type FA.A
'''
import CommonMetaData
from HazardConstants import *

class MetaData(CommonMetaData.MetaData):
    
    def execute(self, hazardEvent=None, metaDict=None):
        self.initialize(hazardEvent, metaDict)
        if self.hazardStatus in ["elapsed", "ending", "ended"]:
            metaData = [
                    self.getPreviousEditedText(),
                    self.getEndingSynopsis(), 
                    ]
        elif self.hazardStatus == 'pending':
            metaData = [
                    self.getPreviousEditedText(),
                    self.getForceSegment(),
                    self.getImmediateCause(),
                    self.getHiddenFloodSeverity(),
                    self.basisStatement(),
                    self.impactsStatement(),
                    self.getCTAs(), 
                    ]
        else:
            metaData = [
                    self.getPreviousEditedText(),
                    self.getForceSegment(),
                    self.getImmediateCause(),
                    self.getHiddenFloodSeverity(),
                    self.basisStatement(),
                    self.impactsStatement(),
                    self.getCTAs(), 
                    # Preserving CAP defaults for future reference.
#                     self.getCAP_Fields([
#                                         ("urgency", "Future"),
#                                         ("severity", "Severe"),
#                                         ("certainty", "Possible"),
#                                         ("responseType", "Prepare"),
#                                         ])
                    ]
            if hazardEvent is not None and self.hazardStatus != "ending":
                damOrLeveeName = hazardEvent.get('damOrLeveeName')
                immediateCause = hazardEvent.get("immediateCause")
                if immediateCause == self.immediateCauseDM()['identifier']:
                    metaData.insert(1, self.getDamOrLevee(damOrLeveeName))

        return {
                METADATA_KEY: metaData
                }    
            
    # CALLS TO ACTION
    def getCTA_Choices(self):
        return [
            self.ctaSafety(),
            self.ctaStayAway(),
            ]

    def basisStatement(self):
        return {
             "fieldType": "Text",
             "fieldName": "basisStatement",
             "label" : "Basis Text:",
             "expandHorizontally": True,
             "visibleChars": 60,
             "lines": 6,
             "promptText": "Enter basis text",
            } 

    def impactsStatement(self):
        return {
             "fieldType": "Text",
             "fieldName": "impactsStatement",
             "label" : "Impacts Text:",
             "expandHorizontally": True,
             "visibleChars": 60,
             "lines": 6,
             "promptText": "Enter impacts text",
            }

def applyInterdependencies(triggerIdentifiers, mutableProperties):
    propertyChanges = CommonMetaData.applyInterdependencies(triggerIdentifiers, mutableProperties)
    return propertyChanges