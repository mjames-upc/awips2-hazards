import CommonMetaData
from HazardConstants import *

class MetaData(CommonMetaData.MetaData):
    
    def execute(self, hazardEvent=None, metaDict=None):
        self.initialize(hazardEvent, metaDict)
	if self.hazardStatus == "ending":
	  metaData = [
	      self.getEndingSynopsis(),
	      ]

	else:
         metaData = [
                    self.getPointID(),
                    self.getImmediateCause(),
                    self.getFloodSeverity(),
                    self.getFloodRecord(),
                    self.getRiseCrestFall(),
                    self.getRainAmt(),
                    self.getCTAs(),
                    ] + self.setCAP_Fields()
        return {
                METADATA_KEY: metaData,
                INTERDEPENDENCIES_SCRIPT_KEY: self.getInterdependenciesScriptFromLocalizedFile("RiseCrestFallUntilFurtherNoticeInterdependencies.py")
                }    
                
        
    # CALLS TO ACTION
    def getCTAs(self):
        return {
                "fieldType":"CheckList",
                "label":"Calls to Action (1 or more):",
                "fieldName": "cta",
                "values": ["stayTunedCTA"],
                "choices": self.getCTA_Choices()
                }        
    def getCTA_Choices(self):
        return [
            self.ctaNoCTA(),
            self.ctaFloodWarningMeans(),
            self.ctaDoNotDrive(),
            self.ctaRiverBanks(),
            self.ctaTurnAround(),
            self.ctaStayTuned(),
            self.ctaNightTime(),
            self.ctaAutoSafety(),
            self.ctaRisingWater(),
            self.ctaForceOfWater(),
            self.ctaLastStatement(),
            self.ctaWarningInEffect(),
            self.ctaReportFlooding(),
            ]

    # CAP fields        
    def setCAP_Fields(self):
        # Set the defaults for the CAP Fields
        capFields = self.getCAP_Fields()
        for entry in capFields:
            for fieldName, values in [
                        ("urgency", "Expected"),
                        ("severity", "Severe"),
                        ("certainty", "Likely"),
                        ("responseType", "None"),
                        ]:
                if entry["fieldName"] == fieldName:
                    entry["values"] = values  
        return capFields          
        
