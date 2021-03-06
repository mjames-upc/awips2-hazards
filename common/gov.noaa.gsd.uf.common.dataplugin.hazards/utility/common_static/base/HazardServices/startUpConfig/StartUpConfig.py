



StartUpConfig = {                                                   
    #########################
    "isNational": True, #False, 
    
    #########################
    #  MUST OVERRIDE!!
    #  Site Configuration - The following MUST BE overridden at the site level 
    
    # Map Center -- The Spatial Display will center on this lat / lon by default with the given zoom level
    "mapCenter": {
        "lat": 41.06,
        "lon":-95.91,
        "zoom": 7
    },

    # Possible Sites -- Hazards from these sites can be selected to be visible in the Hazard Services display.
    #    They will appear in the Settings dialog as a check list from which to choose
    # Example:  "possibleSites": ["BOU","PUB","GJT","CYS","OAX","FSD","DMX","GID","EAX","TOP","RAH"],
    "possibleSites": ["National"],
    
    # Visible Sites -- Hazards from these sites will be, by default, visible in the Hazard Services display
    # Example:  "visibleSites":  ["BOU", "OAX"]
    "visibleSites": ["National"],
    
    # Backup Sites 
    # Example:  "backupSites":  ["PUB", "GJT"]
    "backupSites": ["National"],
    
    # Time resolution for the time line; either "minutes" or "seconds".
    "timeResolution": "minutes",

    # Directory of mounted X.400 directory where exported Site Config data is stored.
    "siteBackupBaseDir" : "CHANGEME",

    # Indicator of what has priority for drag-and-drop-style event modifications; must be "vertex" or "boundingBox".
    "priorityForDragAndDropGeometryEdits": "boundingBox",
    
    # NOTE: The following can be added to a Settings file to trump the values in StartUpConfig
    #     "mapCenter", "possibleSites", "visibleSites", "eventIdDisplayType"
    
    #########################
    
    #########################
    #  General Display
    # eventIdDisplayType is one of:  "ALWAYS_FULL", "FULL_ON_DIFF", "PROG_ON_DIFF", "ALWAYS_SITE", "ONLY_SERIAL"  
    "eventIdDisplayType" : "FULL_ON_DIFF",
       
    #################################################################################
    #  Default Display Maps - List of map names that are loaded when Hazard Services is 
    #  started.
    #      Map names can be found in the localization perspective under 
    #      CAVE->Bundles->Maps.  Use the filename without the extension.
    #      Counties_site.xml -> Counties_site
    #  Note that only Maps that use the following tables will enable the Select By Area
    #  toolbar option:
    #      mapdata.cwa
    #      mapdata.ffmp_basins
    #      mapdata.firewxzones
    #      mapdata.zone
    #      mapdata.basins
    #      mapdata.county
    #      mapdata.isc
    #
    "displayMaps" : [ "Counties_site" ],

    #########################
    #  Hazard Event Creation
    # defaultType may be an empty string, meaning no hazard type is assigned to manually created
    # hazard events, or else one of the types defined in HazardTypes.py.
    "defaultType" : "",

    #########################
    # Hazard Information Dialog
    #    
    # Title text that is to be shown in the tabs of the Hazard Information Dialog for each event.
    # Must be a list of strings, with each string being one of the following:
    #
    #    eventID        Event identifier.
    #    siteID         Site identifier.
    #    status         Event status (e.g. issued)
    #    phenomenon     Hazard phenomenon (e.g. FF)
    #    significance   Hazard significance (e.g. W)
    #    subType        Hazard sub-type, if any (e.g. NonConvective) 
    #    hazardType     Hazard type (e.g. FF.W.NonConvective)
    #    <attrName>     See below.
    #
    # Any string that does not match any of the literals above is treated as
    # a hazard attribute name, is then used to pull out the corresponding
    # attribute value, if any. An example of an attribute name is pointID.
    #
    # Note that any element of this list that yields an empty string is
    # skipped, so if the list is [ "eventID", "pointID" ] and there is no
    # point ID for a particular hazard event, then only the event ID is
    # shown in that event's title text in the Hazard Information Dialog tab.
    "hazardDetailTabText" : [ "eventID", "hazardType", "pointID" ],
    
    # Flag indicating whether or not the scale bar with two sliders on it
    # should be shown below the start-end time UI element in the Hazard Information Dialog.
    "showHazardDetailStartEndTimeScale": False,

    # Flag indicating whether or not the Hazard Information Dialog's layout should be optimized
    # for a wider window.
    "hazardDetailWide": False,  
    
    # Flag indicating whether to include the Issue button on the Hazard Information Dialog
    "includeIssueButton": True,
    
    #########################
    # Console
    "Console": {
                "TimeLineNavigation": "onToolBar", # "onToolBar" or "belowTimeLine",
                },

    #########################
    # Recommenders
    "gagePointFirstRecommender" : "RiverFloodRecommender",
    
    #########################
    # Product Generation

    # PIL order in which Product Generation should take place.
    "disseminationOrder" : [ 'FFW', 'FLW', 'FFS', 'FLS', 'FFA'],
    
    "Alerts": [],
    
    #########################
    # Temporary switch for persistence behavior for recommenders
    # "normal": "saveToDatabase" and "saveToHistory" behave as intended
    # "history": both "saveToDatabase" and "saveToHistory" result in saves to
    #            the history list.
    # "none":    neither "saveToDatabase" nor "saveToHistory" do anything; no persisting
    #            is done in response to either of them
    "persistenceBehavior": "normal",

    #########################
    # Width in pixels to either side of all hazard event base geometries
    # that is to be filled with the color specified by "geometryBufferColor".
    # Default is 0, which means no buffer. This buffer is intended to visually
    # differentiate hazard event base geometries from the background behind
    # them. 
    "geometryBufferThickness": 2.0,
    
    # Hazard event base geometry buffer color, specified as a dictionary
    # holding a number between 0.0 and 1.0 for "red", "green", and "blue"
    # entries, and optionally, an entry between 0.0 and 1.0 for "alpha".
    # The latter, if given, provides the opacity of the color. If no "alpha"
    # is specified, the color is considered opaque. If no color is specified
    # at all, no buffer will be drawn.
    "geometryBufferColor": { "red": 0.0, "green": 0.0, "blue": 0.0, "alpha": 0.8 }

    }
