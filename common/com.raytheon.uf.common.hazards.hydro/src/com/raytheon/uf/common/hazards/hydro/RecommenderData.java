/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.hazards.hydro;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

/**
 * This is a data "holder" class for data items that are generated by the
 * RiverForecastManager.
 * 
 * These structures are generated specifically for the River Flood Recommender
 * components. The purpose of this class is to be a central repository for a
 * complete set of RiverForecastGroup, RiverForecastPoint,
 * Hydrograph(HydrographForecast/HydrographObserved), SHEF
 * (SHEFForecast/SHEFObserved) Impacts (FloodStmt), Crest History (Crest),
 * database and computed database data objects. It also contains generated data
 * objects such as HydroEvent objects.
 * <p>
 * The end goal is to generate and make use of this class ONLY ONCE per CAVE
 * session. This will greatly speed up CAVE Hazard Event generation and reduce
 * database traffic.
 * <p>
 * The system memory persistence of an instance of this object will be
 * maintained by the RecommenderDataCache object.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 08, 2015 6562       Chris.Cody  Initial creation: Restructure River Forecast Points/Recommender
 * </pre>
 * 
 * @author Chris.Cody
 */

public class RecommenderData {

    /**
     * Raw System Time as a Unix timestamp (cannot be frozen).
     */
    public final long absoluteSystemTime;

    /**
     * Hydrological Service Area ID.
     */
    public final String hsaId;

    /**
     * Set CAVE System Time as a Unix timestamp (can be frozen).
     */
    public final long currentSystemTime;

    /**
     * A List of RiverForcastGroup objects. This List represents a DEEP query
     * and its objects may be traversed to access its sub objects.
     */
    public List<RiverForecastGroup> riverForecastGroupList;

    /**
     * A Map of RiverForcastGroup objects. This contains the SAME INSTANCES as
     * the above list, but are organized to be directly accessed by GroupId.
     */
    public Map<String, RiverForecastGroup> riverForecastGroupMap;

    /**
     * A List of RiverForcastPoint objects. This List represents a DEEP query
     * and its objects may be traversed to access its sub objects.
     */
    public List<RiverForecastPoint> riverForecastPointList;

    /**
     * A Map of RiverForcastPoint objects. This contains the SAME INSTANCES as
     * the above list, but are organized to be directly accessed by lid
     * (PointID).
     */
    public Map<String, RiverForecastPoint> riverForcastPointMap;

    /**
     * A Constructed List of HydroEvent objects. This List represents a Fully
     * computed set of Hydro Events for the above list of River Forecast Point
     * objects.
     */
    public List<HydroEvent> hydroEventList;

    /**
     * A Map of RiverForcastPoint objects. This contains the SAME INSTANCES as
     * the above list, but are organized to be directly accessed by lid
     * (PointID).
     */
    public Map<String, HydroEvent> hydroEventMap;

    /**
     * A Constructed Map of computed PreviousEvent objects. This List represents
     * a Fully computed set of Previous Event "objects" for the above list of
     * River Forecast Point objects.
     */
    public Map<String, Object> previousEventMap;

    /**
     * Object Constructor.
     * 
     * @param hsaId
     * @param currentSystemTime
     * @param riverForecastGroupList
     * @param riverForecastPointList
     * @param hydroEventList
     * @param previousEventMap
     */
    public RecommenderData(String hsaId, long currentSystemTime,
            List<RiverForecastGroup> riverForecastGroupList,
            List<RiverForecastPoint> riverForecastPointList,
            List<HydroEvent> hydroEventList,
            Map<String, Object> previousEventMap) {

        this.absoluteSystemTime = System.currentTimeMillis();
        this.hsaId = hsaId;
        this.currentSystemTime = currentSystemTime;
        this.riverForecastGroupList = riverForecastGroupList;
        this.riverForecastPointList = riverForecastPointList;
        this.hydroEventList = hydroEventList;
        this.previousEventMap = previousEventMap;

        buildMaps();

    }

    public long getAbsoluteSystemTime() {
        return (this.absoluteSystemTime);
    }

    public String getHsaId() {
        return (this.hsaId);
    }

    public long getCurrentSystemTime() {
        return (this.currentSystemTime);
    }

    public List<RiverForecastGroup> getRiverForecastGroupList() {
        return (this.riverForecastGroupList);
    }

    public Map<String, RiverForecastGroup> getRiverForecastGroupMap() {
        return (this.riverForecastGroupMap);
    }

    public List<RiverForecastPoint> getRiverForecastPointList() {
        return (this.riverForecastPointList);
    }

    public Map<String, RiverForecastPoint> getRiverForecastPointMap() {
        return (this.riverForcastPointMap);
    }

    public List<HydroEvent> getHydroEventList() {
        return (this.hydroEventList);
    }

    public Map<String, HydroEvent> getHydroEventMap() {
        return (this.hydroEventMap);
    }

    public Map<String, Object> getPreviousEventMap() {
        return (this.previousEventMap);
    }

    /**
     * Build internal Maps from input Lists.
     */
    private void buildMaps() {
        this.riverForecastGroupMap = Maps
                .newHashMapWithExpectedSize(riverForecastGroupList.size());
        for (RiverForecastGroup riverForecastGroup : this.riverForecastGroupList) {
            this.riverForecastGroupMap.put(riverForecastGroup.getGroupId(),
                    riverForecastGroup);
        }

        this.riverForcastPointMap = Maps
                .newHashMapWithExpectedSize(this.riverForecastPointList.size());
        for (RiverForecastPoint riverForecastPoint : this.riverForecastPointList) {
            this.riverForcastPointMap.put(riverForecastPoint.getLid(),
                    riverForecastPoint);
        }

        this.hydroEventMap = Maps
                .newHashMapWithExpectedSize(this.hydroEventList.size());
        for (HydroEvent hydroEvent : this.hydroEventList) {
            this.hydroEventMap.put(hydroEvent.getForecastPoint().getLid(),
                    hydroEvent);
        }
    }

}