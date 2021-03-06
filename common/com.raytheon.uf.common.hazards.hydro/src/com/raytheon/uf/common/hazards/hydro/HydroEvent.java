package com.raytheon.uf.common.hazards.hydro;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.raytheon.uf.common.dataplugin.events.hazards.HazardConstants;
import com.raytheon.uf.common.dataplugin.events.hazards.event.AbstractHazardServicesEventIdUtil;
import com.raytheon.uf.common.dataplugin.shef.tables.Vteccause;
import com.raytheon.uf.common.dataplugin.shef.tables.Vtecevent;
import com.raytheon.uf.common.dataplugin.shef.tables.Vtecrecord;
import com.raytheon.uf.common.dataplugin.shef.tables.Vtecsever;
import com.raytheon.uf.common.time.util.TimeUtil;

/**
 * Description: Represents a hydrologic hazard event. Includes information that
 * would be found in both the VTEC and HVTEC portions of a product.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * July 9, 2012            Bryon.Lawrence    Initial creation
 * May 1, 2014  3581       bkowal       Relocate to common hazards hydro
 * May 08, 2015 6562       Chris.Cody   Restructure River Forecast Points/Recommender
 * Aug 13, 2015 8836       Chris.Cody   Changes for a configurable Event Id
 * May 04, 2016 15584      Kevin.Bisanz Added toString()
 * Mar 13, 2017 28708      mduff        Changes to support event id refactor.
 * Jun 01, 2017 15561      Chris.Golden Changed to no longer use "type" attribute,
 *                                      so that the latter's existence is not relied
 *                                      upon; hazard type is already provided for each
 *                                      hazard event by its phenomenon, significance
 *                                      and subtype.
 * </pre>
 * 
 * @author Bryon.Lawrence
 */
public class HydroEvent {
    public static final int END_TIME_WITHIN = (int) (TimeUtil.MILLIS_PER_HOUR
            / 2);

    /**
     * VTEC related information.
     */
    public static final String PHENOMENA = "FL";

    public static final String SIGNIF_WARNING = "W";

    public static final String SIGNIF_WATCH = "A";

    public static final String SIGNIF_ADVISORY = "Y";

    public static final String SIGNIF_STATEMENT = "S";

    public static final String NO_ACTION = "N/A";

    /**
     * Define the product ids; their consecutive order is important
     */
    public static final int OTHER_PROD = 0;

    public static final int RVS = 1;

    public static final int FLS = 2;

    public static final int FLW = 3;

    /**
     * defines the different reasons for selecting the products; the unique
     * values themselves are arbitrary and for internal use only
     */
    public enum HydroEventReason {
        RVS_NO_DATA, RVS_NO_FLOODING, FLS_CONTINUED_FLOODING, FLS_EXPIRED_FLOODING, FLS_ENDED_FLOODING, FLS_GROUP_IN_FLS, FLW_NEW_FLOODING, FLW_INCREASED_FLOODING, FLW_GROUP_IN_FLW
    }

    /*
     * Forecast point this event applies to.
     */
    private RiverForecastPoint riverForecastPoint;

    /**
     * Recommendations. Of the VTEC fields, only the P-vtec action is determined
     * stored as a true recommendation. The actual vtec fields are initialized
     * separately to these recommended values. The other vtec fields do not have
     * recommended values determined with the recommendations primarily because
     * the ETN is dependent on how many events are included.
     */
    private String recommendedAction;

    private HydroEventReason recommendationReason;

    private int recommendationIndex;

    /**
     * P-VTEC line information. If a double_pvtec situation is requested, this
     * line of information is actually used to represent the second of the two
     * p-vtec lines. The office if is managed externally via the service backup
     * switching mechanisms.
     */
    private boolean eventFound;

    private boolean eventActive;

    /**
     * private String productID; private Date productTime; private Date
     * expireTime;
     */
    private final Vtecevent vtecInfo;

    /**
     * Previous event information for each possible event tracked, i.e. the FL.W
     * warning, the FL.A watch, and FL.Y advisory. This is the previous event,
     * whether it be active or inactive.
     */
    private HydroEvent previousFLW;

    private HydroEvent previousFLA;

    private HydroEvent previousFLY;

    /**
     * Information for the previous event, but for the most recent inactive
     * event. This information is needed for determining the time window for
     * which to retrieve stage/discharge information. Only stage/discharge data
     * after the most recent inactive product are usfed.
     */
    private HydroEvent inactiveFLW;

    private HydroEvent inactiveFLA;

    private HydroEvent inactiveFLY;

    /**
     * Default constructor.
     */
    public HydroEvent() {
        this.vtecInfo = new Vtecevent();
    }

    /**
     * Builds a hydro event
     * 
     * @param eventDict
     *            event dict containing information to load into this hydro
     *            event. [ < Event Id, Map [property , value>> ]
     */
    public HydroEvent(Map<String, Object> eventDict) {
        this();
        loadEvent(eventDict);
    }

    /**
     * Builds a hydro event
     * 
     * @param forecastPoint
     *            River forecast point
     * @param hazardSettings
     *            Settings controlling behavior of river flood recommender.
     * @param eventDict
     *            Map of event identifiers to dictionaries representing previous
     *            events holding flood information [ < Event Id, Map [property ,
     *            value>> ] with the child maps including "phen" and "sig"
     *            entries for phenomenon and significance.
     */
    public HydroEvent(RiverForecastPoint riverForecastPoint, String hsaId,
            Map<String, Object> eventDict, long systemTime) {
        this();
        this.riverForecastPoint = riverForecastPoint;

        // Retrieve previous FL.W if any...
        Map<String, Object> previousFLWevent = getPreviousEvent(hsaId,
                eventDict, HydroEvent.SIGNIF_WARNING);

        // If available, load the previous observed and forecast data in
        // the forecast point.
        riverForecastPoint
                .setFromPreviousObservedForecastValues(previousFLWevent);

        // Retrieve previous FL.A if any...
        Map<String, Object> previousFLYevent = getPreviousEvent(hsaId,
                eventDict, HydroEvent.SIGNIF_WATCH);

        // Retrieve previous FL.Y if any...
        Map<String, Object> previousFLAevent = getPreviousEvent(hsaId,
                eventDict, HydroEvent.SIGNIF_ADVISORY);

        this.previousFLW = new HydroEvent(previousFLWevent);

        this.previousFLA = new HydroEvent(previousFLAevent);
        this.previousFLY = new HydroEvent(previousFLYevent);

        // Retrieve previous inactive FL.W if any...
        Map<String, Object> previousInactiveFLWevent = getPreviousInactiveEvent(
                hsaId, eventDict, HydroEvent.SIGNIF_WARNING, systemTime);

        // Retrieve previous inactive FL.Y if any...
        Map<String, Object> previousInactiveFLYevent = getPreviousInactiveEvent(
                hsaId, eventDict, HydroEvent.SIGNIF_WARNING, systemTime);

        // Retrieve previous inactive FL.A if any...
        Map<String, Object> previousInactiveFLAevent = getPreviousInactiveEvent(
                hsaId, eventDict, HydroEvent.SIGNIF_WARNING, systemTime);

        this.inactiveFLW = new HydroEvent(previousInactiveFLWevent);
        this.inactiveFLY = new HydroEvent(previousInactiveFLYevent);
        this.inactiveFLA = new HydroEvent(previousInactiveFLAevent);
    }

    /**
     * 
     * @return the river forecast point associated with this hydro event.
     */
    public RiverForecastPoint getForecastPoint() {
        return riverForecastPoint;
    }

    /**
     * 
     * @param geoId
     * @param eventDict
     *            Map of event identifiers to dictionaries representing previous
     *            events [ < Event Id, Map [property , value>> ] with the child
     *            maps including "phen" and "sig" entries for phenomenon and
     *            significance.
     * @param significance
     * @return The previously issued event for this forecast point if any.
     */
    private Map<String, Object> getPreviousEvent(String geoId,
            Map<String, Object> eventDict, String significance) {
        Map<String, Object> previousEvent = null;
        long mostRecentCreationTime = Long.MIN_VALUE;

        if ((eventDict != null) && (!eventDict.isEmpty())) {
            Set<String> eventIDs = eventDict.keySet();

            for (String eventID : eventIDs) {

                @SuppressWarnings("unchecked")
                Map<String, Object> dict = (Map<String, Object>) eventDict
                        .get(eventID);
                String siteID = (String) dict.get(HazardConstants.SITE_ID);
                String phenomena = (String) dict
                        .get(HazardConstants.HAZARD_EVENT_PHEN);
                String eventSignificance = (String) dict
                        .get(HazardConstants.HAZARD_EVENT_SIG);

                /*
                 * Retrieve the most recent previous event that matches the geo
                 * id and the requested significance code.
                 */
                if (siteID.equals(geoId)
                        && significance.equals(eventSignificance)
                        && phenomena.equals(HydroEvent.PHENOMENA)) {
                    Number eventCreationTimeNumber = (Number) dict
                            .get(HazardConstants.CREATION_TIME);
                    long eventCreationTime = eventCreationTimeNumber
                            .longValue();

                    if (eventCreationTime >= mostRecentCreationTime) {
                        mostRecentCreationTime = eventCreationTime;
                        previousEvent = dict;
                    }
                }
            }
        }

        return previousEvent;
    }

    /**
     * Retrieves the previous inactive event.
     * 
     * @param geoId
     *            The identifier of the forecast point.
     * @param eventDict
     *            Map of event identifiers to dictionaries representing previous
     *            events. [ < Event Id, Map [property , value>> ] with the child
     *            maps including "phen" and "sig" entries for phenomenon and
     *            significance.
     * @param significance
     *            The significance
     * @param systemTime
     *            Current System Time in milliseconds
     * @return The previous event (now inactive) for this forecast point.
     */
    private Map<String, Object> getPreviousInactiveEvent(String geoId,
            Map<String, Object> eventDict, String significance,
            long systemTime) {
        String activeETN = null;

        Map<String, Object> previousInactiveEvent = null;
        long mostRecentCreationTime = Long.MIN_VALUE;

        if ((eventDict != null) && (!eventDict.isEmpty())) {
            Set<String> eventIDs = eventDict.keySet();

            /*
             * The first loop is for finding an active event.
             */
            for (String eventID : eventIDs) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dict = (Map<String, Object>) eventDict
                        .get(eventID);
                String siteID = (String) dict.get(HazardConstants.SITE_ID);
                String phenomena = (String) dict
                        .get(HazardConstants.HAZARD_EVENT_PHEN);
                String eventSignificance = (String) dict
                        .get(HazardConstants.HAZARD_EVENT_SIG);

                /*
                 * Retrieve the most recent previous event that matches the geo
                 * id and the requested significance code.
                 */
                if (siteID.equals(geoId)
                        && significance.equals(eventSignificance)
                        && phenomena.equals(HydroEvent.PHENOMENA)) {

                    Number number = (Number) dict
                            .get(HazardConstants.HAZARD_EVENT_END_TIME);
                    long previousEventEndtime = number.longValue();

                    /*
                     * Check whether or not the event is active...
                     */
                    boolean active = checkIfEventActive(systemTime,
                            previousEventEndtime);

                    if (active && (activeETN == null)) {
                        // This is not a true ETN
                        activeETN = AbstractHazardServicesEventIdUtil
                                .getSerialIdFromFullId(eventID);
                        break;
                    }
                }
            }

            /*
             * The second loop is for finding the latest inactive event which
             * does not have the same etn as the current active event (if any).
             * Only consider previous inactive events that are not an earlier
             * issuance for the current active event, as noted by the ETN. This
             * is needed to avoid using earlier issuances of the current event,
             * which may have their end time in the past and therefore be
             * considered expired, but the event was extended and is still an
             * ongoing event and therefore must not be considered as the
             * inactive event.
             */
            for (String eventID : eventIDs) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dict = (Map<String, Object>) eventDict
                        .get(eventID);
                String siteID = (String) dict.get(HazardConstants.SITE_ID);
                String phenomena = (String) dict
                        .get(HazardConstants.HAZARD_EVENT_PHEN);
                String eventSignificance = (String) dict
                        .get(HazardConstants.HAZARD_EVENT_SIG);

                /*
                 * Retrieve the most recent previous event that matches the geo
                 * id and the requested significance code.
                 */
                if (siteID.equals(geoId)
                        && significance.equals(eventSignificance)
                        && phenomena.equals(HydroEvent.PHENOMENA)) {
                    Number number = (Number) dict
                            .get(HazardConstants.CREATION_TIME);
                    long eventCreationTime = number.longValue();
                    number = (Number) dict
                            .get(HazardConstants.HAZARD_EVENT_END_TIME);
                    long previousEventEndtime = number.longValue();

                    boolean active = checkIfEventActive(systemTime,
                            previousEventEndtime);

                    // This is not a true ETN
                    String eventNumericSuffix = AbstractHazardServicesEventIdUtil
                            .getSerialIdFromFullId(eventID);

                    if ((eventCreationTime >= mostRecentCreationTime)
                            && (!active) && (eventNumericSuffix
                                    .equals(activeETN) == false)) {
                        mostRecentCreationTime = eventCreationTime;
                        previousInactiveEvent = dict;
                    }

                }
            }

        }

        return previousInactiveEvent;
    }

    /**
     * Loads a new HydroEvent with event information
     * 
     * @param eventDict
     *            contains the event information to load into this hydro event.
     *            [ < Event Id, Map [property , value>> ]
     */
    private void loadEvent(Map<String, Object> eventDict) {
        if (eventDict != null) {
            this.eventFound = true;

            /*
             * There is no action ... trying to have the recommender not have to
             * deal with this...
             */
            this.vtecInfo.setVtecaction(null);
            this.vtecInfo.setVtecphenom(null);
            this.vtecInfo.setVtecsignif(null);

            // This is not a true ETN
            String eventID = (String) eventDict.get(HazardConstants.EVENT_ID);
            String eventIdNumericSuffix = AbstractHazardServicesEventIdUtil
                    .getSerialIdFromFullId(eventID);
            this.vtecInfo.setEtn(Short.parseShort(eventIdNumericSuffix));

            /*
             * There is no expiration time... yet.
             */
            this.vtecInfo.setExpiretime(null);

            Long startTime = (Long) eventDict
                    .get(HazardConstants.HAZARD_EVENT_START_TIME);
            this.vtecInfo.setBegintime(new Date(startTime));
            Long endTime = (Long) eventDict
                    .get(HazardConstants.HAZARD_EVENT_END_TIME);
            this.vtecInfo.setEndtime(new Date(endTime));

            String severity = (String) eventDict
                    .get(HazardConstants.FLOOD_SEVERITY_CATEGORY);
            this.vtecInfo.setVtecsever(new Vtecsever(severity));

            String cause = (String) eventDict
                    .get(HazardConstants.IMMEDIATE_CAUSE);
            this.vtecInfo.setVteccause(new Vteccause(cause));

            String record = (String) eventDict
                    .get(HazardConstants.FLOOD_RECORD);
            this.vtecInfo.setVtecrecord(new Vtecrecord(record));

            Long riseTime = (Long) eventDict.get(HazardConstants.RISE_ABOVE);
            this.vtecInfo.setRisetime(new Date(riseTime));

            Long crestTime = (Long) eventDict.get(HazardConstants.CREST);
            this.vtecInfo.setCresttime(new Date(crestTime));

            Long fallTime = (Long) eventDict.get(HazardConstants.FALL_BELOW);
            this.vtecInfo.setFalltime(new Date(fallTime));

            // Typesources not implemented at the moment.
            this.vtecInfo.setRisets(null);
            this.vtecInfo.setCrests(null);
            this.vtecInfo.setFallts(null);

            // There is no crestValue, will add to this dict via
            // the recommender.
            this.vtecInfo.setCrestValue(null);
        } else {
            this.eventFound = false;

            //
            // There is no action ... trying to have the
            // recommender not have to deal with this...
            //
            this.vtecInfo.setVtecaction(null);
            this.vtecInfo.setVtecphenom(null);
            this.vtecInfo.setVtecsignif(null);

            this.vtecInfo.setEtn(null);

            this.vtecInfo.setExpiretime(null);
            this.vtecInfo.setBegintime(null);
            this.vtecInfo.setEndtime(null);

            this.vtecInfo.setVtecsever(null);
            this.vtecInfo.setVteccause(null);
            this.vtecInfo.setVtecrecord(null);

            this.vtecInfo.setRisetime(null);
            this.vtecInfo.setCresttime(null);
            this.vtecInfo.setFalltime(null);
            this.vtecInfo.setRisets(null);
            this.vtecInfo.setCrests(null);
            this.vtecInfo.setFallts(null);
        }
    }

    /**
     * @param currentSystemTime
     *            Current Time in Milliseconds for comparison.
     * @param previousEndtimeInMilliseconds
     *            The end time of the event to be tested.
     * @return true - the event is active, false - the event is not active.
     */
    public boolean checkIfEventActive(long systemTime,
            long previousEndtimeInMilliseconds) {

        if (previousEndtimeInMilliseconds == 0) {
            return true;
        }

        return previousEndtimeInMilliseconds > systemTime;
    }

    /**
     * Tests whether or not an event is active based on the CAVE system current
     * time.
     * 
     * @param event
     *            The hydro event to test for active status
     * @return true - the event is active, false - the event is not active
     */
    public boolean checkIfEventActive(HydroEvent event,
            long currentSystemTime) {
        boolean active = false;

        Vtecevent vtecInfo = event.getPreviousFLW().getVtecInfo();
        String action = vtecInfo.getVtecaction().getAction();
        Date endtime = vtecInfo.getEndtime();

        if (action != null && action.length() > 0 && !action.equals("CAN")
                && !action.equals("EXP") && !action.equals("ROU")
                && action.length() > 0) {
            /*
             * If the endtime is not specified, assume the event is still
             * active. Unspecified can occur if the endtime has a missing value
             * indicator or is set to 0. A 0 value van occur because the time
             * may be NULL in the database, which is converted to a 0 value by
             * the time conversion functions.
             */
            if (endtime == null) {
                active = true;
            } else {
                /*
                 * If the end time is past the current time, then the event is
                 * still active.
                 */
                long prevTime = endtime.getTime();
                active = checkIfEventActive(currentSystemTime, prevTime);
            }
        }

        return active;
    }

    public boolean checkIfEventActive(long currentSystemTime) {
        return (checkIfEventActive(this, currentSystemTime));
    }

    /**
     * 
     * @param
     * @return The previous FLW for this forecast point.
     */
    public HydroEvent getPreviousFLW() {
        return previousFLW;
    }

    /**
     * 
     * @param
     * @return true - An event was found for this river forecast point, false -
     *         no event was found.
     */
    public boolean isEventFound() {
        return eventFound;
    }

    /**
     * @return the VTEC information associated with this event.
     */
    public Vtecevent getVtecInfo() {
        return vtecInfo;
    }

    /**
     * Toggles the active state of this event.
     * 
     * @param eventActive
     */
    public void setEventActive(boolean eventActive) {
        this.eventActive = eventActive;
    }

    /**
     * @return the active state of this event.
     */
    public boolean isEventActive() {
        return eventActive;
    }

    /**
     * @param recommended
     *            Action the recommendedAction to set
     */
    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    /**
     * @return the recommended Action
     */
    public String getRecommendedAction() {
        return recommendedAction;
    }

    /**
     * @param recommendation
     *            Reason the recommendation reason to set
     */
    public void setRecommendationReason(HydroEventReason recommendationReason) {
        this.recommendationReason = recommendationReason;
    }

    /**
     * @return the recommendation reason
     */
    public HydroEventReason getRecommendationReason() {
        return recommendationReason;
    }

    /**
     * @param recommendationIndex
     *            the recommendationIndex to set
     */
    public void setRecommendationIndex(int recommendationIndex) {
        this.recommendationIndex = recommendationIndex;
    }

    /**
     * @return the recommendationIndex
     */
    public int getRecommendationIndex() {
        return recommendationIndex;
    }

    /**
     * @param previousFLA
     *            the previous FLA to set
     */
    public void setPreviousFLA(HydroEvent previousFLA) {
        this.previousFLA = previousFLA;
    }

    /**
     * @return the previous FLA
     */
    public HydroEvent getPreviousFLA() {
        return previousFLA;
    }

    /**
     * @param previousFLY
     *            the previous FLY to set
     */
    public void setPreviousFLY(HydroEvent previousFLY) {
        this.previousFLY = previousFLY;
    }

    /**
     * @return the previous FLY
     */
    public HydroEvent getPreviousFLY() {
        return previousFLY;
    }

    /**
     * @param inactiveFLW
     *            the inactive FLW to set
     */
    public void setInactiveFLW(HydroEvent inactiveFLW) {
        this.inactiveFLW = inactiveFLW;
    }

    /**
     * @return the inactive FLW
     */
    public HydroEvent getInactiveFLW() {
        return inactiveFLW;
    }

    /**
     * @param inactiveFLA
     *            the inactive FLA to set
     */
    public void setInactiveFLA(HydroEvent inactiveFLA) {
        this.inactiveFLA = inactiveFLA;
    }

    /**
     * @return the inactive FLA
     */
    public HydroEvent getInactiveFLA() {
        return inactiveFLA;
    }

    /**
     * @param inactiveFLY
     *            the inactive FLY to set
     */
    public void setInactiveFLY(HydroEvent inactiveFLY) {
        this.inactiveFLY = inactiveFLY;
    }

    /**
     * @return the inactive FLY
     */
    public HydroEvent getInactiveFLY() {
        return inactiveFLY;
    }

    @Override
    public String toString() {
        return "RiverForecastPoint:" + riverForecastPoint;
    }

}
