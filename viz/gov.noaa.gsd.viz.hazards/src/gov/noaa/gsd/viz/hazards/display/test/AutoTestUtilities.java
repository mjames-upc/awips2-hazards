/**
 * This software was developed and / or modified by the
 * National Oceanic and Atmospheric Administration (NOAA), 
 * Earth System Research Laboratory (ESRL), 
 * Global Systems Division (GSD), 
 * Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.hazards.display.test;

import static com.raytheon.uf.common.dataplugin.events.hazards.HazardConstants.HAZARD_EVENT_FULL_TYPE;
import static com.raytheon.uf.common.dataplugin.events.hazards.HazardConstants.HAZARD_EVENT_IDENTIFIER;
import gov.noaa.gsd.common.eventbus.BoundedReceptionEventBus;
import gov.noaa.gsd.viz.hazards.UIOriginator;
import gov.noaa.gsd.viz.hazards.display.HazardServicesAppBuilder;
import gov.noaa.gsd.viz.hazards.display.action.CurrentSettingsAction;
import gov.noaa.gsd.viz.hazards.display.action.HazardDetailAction;
import gov.noaa.gsd.viz.hazards.display.action.StaticSettingsAction;
import gov.noaa.gsd.viz.hazards.display.action.ToolAction;
import gov.noaa.gsd.viz.hazards.jsonutilities.Dict;
import gov.noaa.gsd.viz.hazards.utilities.HazardEventBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.raytheon.uf.common.dataplugin.events.hazards.event.HazardEventUtilities;
import com.raytheon.uf.common.dataplugin.events.hazards.event.IHazardEvent;
import com.raytheon.uf.common.hazards.productgen.GeneratedProductList;
import com.raytheon.uf.common.hazards.productgen.IGeneratedProduct;
import com.raytheon.uf.viz.hazards.sessionmanager.config.types.ISettings;
import com.raytheon.uf.viz.hazards.sessionmanager.config.types.Settings;
import com.raytheon.uf.viz.hazards.sessionmanager.config.types.ToolType;
import com.raytheon.uf.viz.hazards.sessionmanager.events.ISessionEventManager;
import com.raytheon.uf.viz.hazards.sessionmanager.events.InvalidGeometryException;
import com.raytheon.uf.viz.hazards.sessionmanager.events.impl.ObservedHazardEvent;
import com.raytheon.uf.viz.hazards.sessionmanager.originator.Originator;
import com.raytheon.uf.viz.hazards.sessionmanager.recommenders.RecommenderExecutionContext;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.TopologyException;

/**
 * Description: Constants and utilities for {@link FunctionalTest}
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 30, 2013 2166       daniel.s.schaffer@noaa.gov      Initial creation
 * Nov  04, 2013   2182     daniel.s.schaffer@noaa.gov      Started refactoring
 * Nov 15, 2013  2182       daniel.s.schaffer@noaa.gov    Refactoring JSON - ProductStagingDialog
 * Nov 16, 2013  2166       daniel.s.schaffer@noaa.gov    Added some utilities
 *  
 * Nov 29, 2013 2380    daniel.s.schaffer@noaa.gov Added code for test of settings-based filtering
 * Feb 07, 2014 2890        bkowal     Product Generation JSON refactor.
 * Apr 09, 2014    2925       Chris.Golden Fixed to work with new HID event propagation.
 * Dec 16, 2014    4124       Chris.Golden Changed to work with new ISettings interface.
 * </pre>
 * 
 * @author daniel.s.schaffer@noaa.gov
 * @version 1.0
 */
public class AutoTestUtilities {

    public static Double EVENT_BUILDER_OFFSET = 0.0025;

    static final String CAUSE = "cause";

    static final String OAX = "OAX";

    static final String AREAL_FLOOD_WATCH_PHEN = "FA";

    static final String AREAL_FLOOD_WATCH_PHEN_SIG = AREAL_FLOOD_WATCH_PHEN
            + ".A";

    static final String FLASH_FLOOD_WATCH_PHEN_SIG = "FF.A";

    static final String FLOOD_WATCH_PHEN_SIG = "FL.A";

    static final String FLOOD_WARNING_PHEN_SIG = "FL.W";

    static final String AREAL_FLOOD_WARNING_PHEN_SIG = "FA.W";

    static final String AREAL_FLOOD_WATCH = "AREAL FLOOD WATCH";

    static final String FLASH_FLOOD_WATCH = "FLASH FLOOD WATCH";

    static final String AREAL_FLOOD_WARNING = "AREAL FLOOD WARNING";

    static final String FLOOD_WARNING = "FLOOD WARNING";

    static final String FLOOD_WATCH = "FLOOD WATCH";

    static final String AREAL_FLOOD_WATCH_FULLTYPE = "FA.A ("
            + AREAL_FLOOD_WATCH + ")";

    static final String FLASH_FLOOD_WATCH_FULLTYPE = "FF.A ("
            + FLASH_FLOOD_WATCH + ")";

    static final String AREAL_FLOOD_WARNING_FULLTYPE = "FA.W ("
            + AREAL_FLOOD_WARNING + ")";

    static final String FLW_FULL_TEXT = FLOOD_WARNING_PHEN_SIG + " ("
            + FLOOD_WARNING + ")";

    static final String FFW_NON_CONVECTIVE_PHEN_SIG = "FF.W.NonConvective";

    static final String FFW_NON_CONVECTIVE_FULL_TEXT = FFW_NON_CONVECTIVE_PHEN_SIG
            + " (FLASH FLOOD WARNING)";

    static final String FLOOD_WATCH_PRODUCT_ID = "FFA";

    static final String FLOOD_WARNING_PRODUCT_ID = "FLW";

    static final String FLOOD_STATEMENT_PRODUCT_ID = "FLS";

    static final String NEW_VTEC_STRING = "NEW.K" + OAX;

    static final String CON_VTEC_STRING = "CON.K" + OAX;

    static final String CAN_VTEC_STRING = "CAN.K" + OAX;

    static final String EXA_VTEC_STRING = "EXA.K" + OAX;

    static final String EXT_VTEC_STRING = "EXT.K" + OAX;

    static final String HYDROLOGY = "Hydrology";

    static final String SEV2 = "sev2";

    static final String INCLUDE = "include";

    static final String LOW_CONFIDENCE_URGENCY_LEVEL = "Low Confidence (Potential Structure Failure)";

    static final String HIGH_CONFIDENCE_URGENCY_LEVEL = "High Confidence (Structure Failure Imminent)";

    static final String DAM_HAS_FAILED_URGENCY_LEVEL = "Structure has Failed!!";

    static public enum DamBreakUrgencyLevels {

        LOW_CONFIDENCE_URGENCY_LEVEL(
                "Low Confidence (Potential Structure Failure)"), HIGH_CONFIDENCE_URGENCY_LEVEL(
                "High Confidence (Structure Failure Imminent)"), DAM_FAILED_URGENCY_LEVEL(
                "Structure has Failed!!");

        private DamBreakUrgencyLevels(final String text) {
            this.text = text;
        }

        private final String text;

        @Override
        public String toString() {
            return text;
        }

    }

    static final String URGENCY_LEVEL = "urgencyLevel";

    static final String BRANCHED_OAK_DAM = "Branched Oak Dam";

    static final String DAM_NAME = "damName";

    static final String FORECAST_TYPE = "forecastType";

    private final HazardServicesAppBuilder appBuilder;

    private final BoundedReceptionEventBus<Object> eventBus;

    private final HazardEventBuilder hazardEventBuilder;

    private final ISettings settings;

    public AutoTestUtilities(HazardServicesAppBuilder appBuilder) {
        this.appBuilder = appBuilder;
        this.eventBus = appBuilder.getEventBus();
        this.hazardEventBuilder = new HazardEventBuilder(
                appBuilder.getSessionManager());
        this.settings = appBuilder.getSessionManager()
                .getConfigurationManager().getSettings();
    }

    void createEvent(Double centerX, Double centerY) {
        Coordinate[] coordinates = buildEventArea(centerX, centerY);
        try {
            IHazardEvent hazardEvent = hazardEventBuilder
                    .buildPolygonHazardEvent(coordinates);
            hazardEventBuilder.addEvent(hazardEvent, Originator.OTHER);
        } catch (InvalidGeometryException e) {
            throw new TopologyException(e.getMessage());
        }
    }

    void assignSelectedEventType(String eventType) {
        ObservedHazardEvent selectedEvent = getSelectedEvent();

        String[] phenSigSubType = HazardEventUtilities
                .getHazardPhenSigSubType(eventType);

        getEventManager().setEventType(selectedEvent, phenSigSubType[0],
                phenSigSubType[1], phenSigSubType[2],
                UIOriginator.HAZARD_INFORMATION_DIALOG);
    }

    ISessionEventManager<ObservedHazardEvent> getEventManager() {
        return appBuilder.getSessionManager().getEventManager();
    }

    ObservedHazardEvent getSelectedEvent() {
        return getEventManager().getSelectedEvents().iterator().next();
    }

    Settings buildEventFilterCriteria(Set<String> visibleTypes,
            Set<String> visibleStates, Set<String> visibleSites) {
        Settings result = new Settings();
        result.setVisibleTypes(visibleTypes);
        result.setVisibleStatuses(visibleStates);
        result.setVisibleSites(visibleSites);
        return result;
    }

    Coordinate[] buildEventArea(Double centerX, Double centerY) {
        List<Coordinate> result = new ArrayList<>();

        result.add(buildPoint(centerX, centerY, -EVENT_BUILDER_OFFSET,
                -EVENT_BUILDER_OFFSET));
        result.add(buildPoint(centerX, centerY, EVENT_BUILDER_OFFSET,
                -EVENT_BUILDER_OFFSET));
        result.add(buildPoint(centerX, centerY, EVENT_BUILDER_OFFSET,
                EVENT_BUILDER_OFFSET));
        result.add(buildPoint(centerX, centerY, -EVENT_BUILDER_OFFSET,
                EVENT_BUILDER_OFFSET));
        result.add(buildPoint(centerX, centerY, -EVENT_BUILDER_OFFSET,
                -EVENT_BUILDER_OFFSET));
        return result.toArray(new Coordinate[result.size()]);
    }

    private Coordinate buildPoint(Double centerX, Double centerY,
            Double xOffset, Double yOffset) {
        return new Coordinate(centerX + xOffset, centerY + yOffset);
    }

    Map<String, Serializable> buildEventTypeSelection(
            IHazardEvent selectedEvent, String fullType) {
        /*
         * Build a simulated hazard type selection in the HID.
         */
        Map<String, Serializable> hazardTypeSelection = new HashMap<>();
        hazardTypeSelection.put(HAZARD_EVENT_IDENTIFIER,
                selectedEvent.getEventID());
        hazardTypeSelection.put(ISessionEventManager.ATTR_HAZARD_CATEGORY,
                AutoTestUtilities.HYDROLOGY);
        hazardTypeSelection.put(HAZARD_EVENT_FULL_TYPE, fullType);
        return hazardTypeSelection;
    }

    public void issueFromProductEditor(
            ProductEditorViewForTesting mockProductEditorView) {
        mockProductEditorView.invokeIssueButton();
    }

    /**
     * Issues one or more products associated with hazard events.
     */
    public void issueFromHID() {
        eventBus.publishAsync(new HazardDetailAction(
                HazardDetailAction.ActionType.ISSUE));
    }

    public void previewFromHID() {
        eventBus.publishAsync(new HazardDetailAction(
                HazardDetailAction.ActionType.PREVIEW));
    }

    void retrieveReviweableProducts() {

    }

    List<String> legacyProductsFromEditorView(
            ProductEditorViewForTesting editorView) {
        List<String> result = new ArrayList<>();
        List<GeneratedProductList> generatedProductsStorage = editorView
                .getGeneratedProductsList();
        for (GeneratedProductList generatedProductList : generatedProductsStorage) {
            for (IGeneratedProduct iGeneratedProduct : generatedProductList) {
                Map<String, List<Serializable>> entries = iGeneratedProduct
                        .getEntries();
                String key = entries.keySet().iterator().next();
                String text = (String) entries.get(key).get(0);
                result.add(text);
            }
        }
        return result;
    }

    void runDamBreakRecommender(DamBreakUrgencyLevels urgencyLevel) {
        Map<String, Serializable> damBreakInfo = new HashMap<>();
        damBreakInfo.put(DAM_NAME, BRANCHED_OAK_DAM);
        damBreakInfo.put(URGENCY_LEVEL, urgencyLevel.toString());
        eventBus.publishAsync(new ToolAction(
                ToolAction.RecommenderActionEnum.RUN_RECOMMENDER_WITH_PARAMETERS,
                FunctionalTest.DAM_BREAK_FLOOD_RECOMMENDER,
                ToolType.RECOMMENDER, damBreakInfo, RecommenderExecutionContext
                        .getEmptyContext()));
    }

    void changeStaticSettings(String settingsID) {
        StaticSettingsAction action = new StaticSettingsAction(
                StaticSettingsAction.ActionType.SETTINGS_CHOSEN, settingsID);
        eventBus.publishAsync(action);
    }

    void changeCurrentSettings(ISettings settings) {
        CurrentSettingsAction action = new CurrentSettingsAction(settings,
                UIOriginator.SETTINGS_MENU);
        eventBus.publishAsync(action);
    }

    /*
     * This is a helper method to convert a GeneratedProductList to a
     * List<Dict>. This method will go away as part of the JSON refacctor.
     */
    @Deprecated
    public static List<Dict> createGeneratedProductsDictList(
            GeneratedProductList generatedProducts) {
        List<Dict> generatedProductsDictList = new ArrayList<Dict>();
        if (generatedProducts != null) {

            for (IGeneratedProduct generatedProduct : generatedProducts) {
                Dict d = new Dict();
                String productID = generatedProduct.getProductID();
                d.put("productID", productID);
                Dict val = new Dict();
                for (String format : generatedProduct.getEntries().keySet()) {
                    val.put(format, generatedProduct.getEntries().get(format)
                            .get(0));
                }

                d.put("products", val);
                generatedProductsDictList.add(d);
            }
        }

        return generatedProductsDictList;
    }

    /**
     * @return the hazardEventBuilder
     */
    public HazardEventBuilder getHazardEventBuilder() {
        return hazardEventBuilder;
    }

    public Integer numInstancesContainingText(List<String> strings, String text) {
        int result = 0;
        for (String string : strings) {
            if (string.contains(text)) {
                result += 1;
            }
        }
        return result;
    }

}
