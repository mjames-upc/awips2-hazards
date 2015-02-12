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
package com.raytheon.uf.viz.hazards.sessionmanager.geomaps;

import static com.raytheon.uf.common.dataplugin.events.hazards.HazardConstants.HAZARD_AREA;
import static com.raytheon.uf.common.dataplugin.events.hazards.HazardConstants.HAZARD_AREA_ALL;
import static com.raytheon.uf.common.dataplugin.events.hazards.HazardConstants.HAZARD_AREA_INTERSECTION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.raytheon.uf.common.dataaccess.DataAccessLayer;
import com.raytheon.uf.common.dataaccess.IDataRequest;
import com.raytheon.uf.common.dataaccess.geom.IGeometryData;
import com.raytheon.uf.common.dataaccess.impl.DefaultGeometryData;
import com.raytheon.uf.common.dataplugin.events.hazards.HazardConstants;
import com.raytheon.uf.common.dataplugin.events.hazards.event.HazardEventUtilities;
import com.raytheon.uf.common.dataplugin.events.hazards.event.IHazardEvent;
import com.raytheon.uf.common.hazards.configuration.types.HazardTypeEntry;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.hazards.sessionmanager.config.ISessionConfigurationManager;
import com.raytheon.uf.viz.hazards.sessionmanager.config.impl.ObservedSettings;
import com.raytheon.uf.viz.hazards.sessionmanager.ugcbuilder.IugcToMapGeometryDataBuilder;
import com.raytheon.uf.viz.hazards.sessionmanager.ugcbuilder.impl.CountyUGCBuilder;
import com.raytheon.uf.viz.hazards.sessionmanager.ugcbuilder.impl.FireWXZoneUGCBuilder;
import com.raytheon.uf.viz.hazards.sessionmanager.ugcbuilder.impl.MarineZoneUGCBuilder;
import com.raytheon.uf.viz.hazards.sessionmanager.ugcbuilder.impl.NullUGCBuilder;
import com.raytheon.uf.viz.hazards.sessionmanager.ugcbuilder.impl.OffshoreZoneUGCBuilder;
import com.raytheon.uf.viz.hazards.sessionmanager.ugcbuilder.impl.ZoneUGCBuilder;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.TopologyException;

/**
 * Misc functionality for building the UGC information associated with a hazard
 * event.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 02, 2014 2536       blawrenc    Initial creation
 * Mar 20, 2014 3290       bkowal      A county will only be included in an
 *                                     interoperability hazard if 10% of the
 *                                     county is within the selected region.
 *                                     This check is necessary to handle
 *                                     discrepancies between the different
 *                                     map resolutions.
 * Apr 28, 2014 3556       bkowal      Updated to use the new hazards common 
 *                                     configuration plugin.
 * Dec 05, 2014 2124       Chris.Golden Changed to work with parameterized
 *                                      config manager.
 * Jan 22, 2015 4959       Dan Schaffer Ability to right click to add/remove UGCs from hazards
 * Jan 26, 2015 5952       Dan Schaffer Fix incorrect hazard area designation.
 * Feb  6, 2015 4375       Dan Schaffer Added error check for empty UGCs.
 * Feb 12, 2015 4959       Dan Schaffer Modify MB3 add/remove UGCs to match Warngen
 * </pre>
 * 
 * @author blawrenc
 * @version 1.0
 */

public class GeoMapUtilities {

    private static final double KM_PER_DEGREE_AT_EQUATOR = 111.03;

    private enum MapGeometryExtractionApproach {
        CONTAINING, CONTAINED, INTERSECTION
    }

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(GeoMapUtilities.class);

    /**
     * Contains the mappings between geometry tables in the geodatabase and
     * attributes that need to be retrieved from them to support UGC mapping.
     */
    private static Map<String, String[]> geoTableAttributesMap;

    /*
     * The default amount of area that must be selected within a country for
     * inclusion. Currently at 10%.
     */
    private static final double DEFAULT_INTEROPERABILITY_OVERLAP_REQUIREMENT = 0.10;

    /**
     * Look-up table for UGC-related columns for tables in the maps geodatabase.
     */
    static {
        Map<String, String[]> tempMap = Maps.newHashMap();

        tempMap.put(HazardConstants.MAPDATA_COUNTY, new String[] {
                HazardConstants.UGC_FIPS, HazardConstants.UGC_STATE });
        tempMap.put(HazardConstants.MAPDATA_ZONE, new String[] {
                HazardConstants.UGC_STATE, HazardConstants.UGC_ZONE });
        tempMap.put(HazardConstants.MAPDATA_FIRE_ZONES, new String[] {
                HazardConstants.UGC_STATE, HazardConstants.UGC_ZONE });
        tempMap.put(HazardConstants.MAPDATA_MARINE_ZONES,
                new String[] { HazardConstants.UGC_ID });
        tempMap.put(HazardConstants.MAPDATA_OFFSHORE,
                new String[] { HazardConstants.UGC_ID });

        geoTableAttributesMap = Collections.unmodifiableMap(tempMap);
    }

    /**
     * Contains the mappings between geodatabase table names and the UGCBuilders
     * which correspond to them.
     */
    private static Map<String, IugcToMapGeometryDataBuilder> geoTableUGCBuilderMap;

    /**
     * Look-up IUGCBuilders for tables in the maps geodatabase.
     */
    static {
        Map<String, IugcToMapGeometryDataBuilder> tempMap = new HashMap<>();

        tempMap.put(HazardConstants.MAPDATA_COUNTY, new CountyUGCBuilder());
        tempMap.put(HazardConstants.MAPDATA_ZONE, new ZoneUGCBuilder());
        tempMap.put(HazardConstants.MAPDATA_FIRE_ZONES,
                new FireWXZoneUGCBuilder());
        tempMap.put(HazardConstants.MAPDATA_MARINE_ZONES,
                new MarineZoneUGCBuilder());
        tempMap.put(HazardConstants.MAPDATA_OFFSHORE,
                new OffshoreZoneUGCBuilder());

        geoTableUGCBuilderMap = Collections.unmodifiableMap(tempMap);
    }

    /**
     * Map for caching geometries retrieved from the geo database.
     */
    private static Map<String, Map<String, Set<IGeometryData>>> mapGeometryCache = Maps
            .newHashMap();

    private final ISessionConfigurationManager<ObservedSettings> configManager;

    private final GeometryFactory geometryFactory = new GeometryFactory();

    public GeoMapUtilities(
            ISessionConfigurationManager<ObservedSettings> configManager) {
        this.configManager = configManager;
    }

    /**
     * Given a {@link IHazardEvent}, returns a list of geometries from the maps
     * database which intersects all of the geometries associated with the
     * event.
     * 
     * @param applyIntersectionThreshold
     *            True if the intersection thresholds for this hazard type
     *            should be applied when finding the intersection
     * @param hazardEvent
     *            The hazard event to determine the intersections.
     * 
     * @return A set of intersecting map db geometries. This set will be empty
     *         if there are no intersecting geometries.
     */
    public Set<IGeometryData> getIntersectingMapGeometries(
            boolean applyIntersectionThreshold, final IHazardEvent hazardEvent) {

        Set<IGeometryData> geometryData = getMapGeometries(hazardEvent);

        return getIntersectingMapGeometries(applyIntersectionThreshold,
                hazardEvent, geometryData);

    }

    /**
     * Returns a collection of geometry data from the maps geodatabase for the
     * specified cwa and maps table.
     * 
     * This method buffers the requested geometries to speed up query
     * efficiency.
     * 
     * @param mapDBtableName
     *            The name of the maps table to retrieve map geometries from
     * @param mapLabelParameter
     *            The name of the field in the maps db table to use for
     *            labeling.
     * @param cwa
     *            The county warning area identifier (e.g. OAX)
     * @return {@link IGeometryData}s for the specified geo table and cwa.
     */
    public Set<IGeometryData> getMapGeometries(String mapDBtableName,
            String mapLabelParameter, String cwa) {

        Map<String, Set<IGeometryData>> mapGeometryMap;

        if (mapGeometryCache.containsKey(cwa)) {
            mapGeometryMap = mapGeometryCache.get(cwa);

            if (mapGeometryMap.containsKey(mapDBtableName)) {
                return mapGeometryMap.get(mapDBtableName);
            }
        }

        IDataRequest mapDataRequest = DataAccessLayer.newDataRequest();
        mapDataRequest.setDatatype("maps");
        String mapdataTable = "mapdata." + mapDBtableName;
        mapDataRequest.addIdentifier(HazardConstants.TABLE_IDENTIFIER,
                mapdataTable);
        mapDataRequest.addIdentifier(HazardConstants.GEOMETRY_FIELD_IDENTIFIER,
                "the_geom");
        mapDataRequest.addIdentifier(HazardConstants.IN_LOCATION_IDENTIFIER,
                "true");
        mapDataRequest.addIdentifier(HazardConstants.LOCATION_FIELD_IDENTIFIER,
                HazardConstants.CWA_IDENTIFIER);
        mapDataRequest.setLocationNames(cwa);
        mapDataRequest.addIdentifier(HazardConstants.CWA_IDENTIFIER, cwa);

        /*
         * Label parameter may be an empty string if defined as such in the
         * HazardTypes.py config file.
         */
        List<String> parameterList = Lists.newArrayList();

        if (mapLabelParameter != null && mapLabelParameter.length() > 0) {
            parameterList.add(mapLabelParameter);
        }

        if (geoTableAttributesMap.containsKey(mapDBtableName)) {
            parameterList.addAll(Lists.newArrayList(geoTableAttributesMap
                    .get(mapDBtableName)));
        }

        if (parameterList.size() > 0) {
            mapDataRequest.setParameters(parameterList
                    .toArray(new String[parameterList.size()]));
        } else {
            /*
             * TODO: Talk to Raytheon about how to avoid supplying a parameter.
             * A Null Pointer Exception results if no parameters are supplied.
             */
            mapDataRequest.setParameters("gid");
        }

        IGeometryData[] geometryData = DataAccessLayer
                .getGeometryData(mapDataRequest);

        if (mapGeometryCache.containsKey(cwa)) {
            mapGeometryMap = mapGeometryCache.get(cwa);
        } else {
            mapGeometryMap = Maps.newHashMap();
            mapGeometryCache.put(cwa, mapGeometryMap);
        }

        Set<IGeometryData> result = new HashSet<>();
        for (IGeometryData ig : geometryData) {
            result.add(ig);
        }
        mapGeometryMap.put(mapDBtableName, result);

        return result;
    }

    /**
     * Given {@link IHazardEvent}, builds a set of geometries representing the
     * actual hazard area.
     * 
     * @param hazardEvent
     *            The hazard event to build the hazard area for.
     * 
     * @return {@link IGeometryData}s describing the actual hazard area
     *         associated with this event.
     */
    public List<IGeometryData> buildHazardAreaForEvent(IHazardEvent hazardEvent) {
        String hazardType = hazardEvent.getHazardType();
        String mapDBtableName = getMapDBtableName(hazardType);

        String mapLabelParameter = configManager.getHazardTypes()
                .get(hazardType).getUgcLabel();

        String cwa = configManager.getSiteID();
        return buildHazardAreaForEvent(mapDBtableName, mapLabelParameter, cwa,
                hazardEvent);

    }

    /**
     * Given the geometry associated with a hazard event, builds geometries
     * representing the actual hazard area.
     * 
     * @param mapDBtableName
     *            The name of the map db geo table
     * @param mapLabelParameter
     *            The name of the field in the maps db table to use for
     *            labeling.
     * @param cwa
     *            The county warning area
     * @param hazardEvent
     *            The hazard event to build the hazard area for.
     * 
     * @return {@link IGeometryData}s describing the actual hazard area
     *         associated with this event.
     */
    public List<IGeometryData> buildHazardAreaForEvent(String mapDBtableName,
            String mapLabelParameter, String cwa, IHazardEvent hazardEvent) {
        List<IGeometryData> result = new ArrayList<>();

        try {
            @SuppressWarnings("unchecked")
            Map<String, String> hazardArea = (Map<String, String>) hazardEvent
                    .getHazardAttribute(HAZARD_AREA);

            Set<IGeometryData> mapGeometryData = getMapGeometries(hazardEvent);
            Map<String, IGeometryData> mapping = getUgcsGeometryDataMapping(
                    mapDBtableName, mapGeometryData);

            for (String ugc : hazardArea.keySet()) {
                IGeometryData mappingData = mapping.get(ugc);
                if (hazardArea.get(ugc).equals(HAZARD_AREA_ALL)) {
                    result.add(mappingData);
                } else if (hazardArea.get(ugc).equals(HAZARD_AREA_INTERSECTION)) {
                    Geometry mappingGeometry = mappingData.getGeometry();
                    DefaultGeometryData intersectionGeometryData = new DefaultGeometryData();
                    GeometryCollection asCollection = (GeometryCollection) hazardEvent
                            .getGeometry();
                    for (int geometryIndex = 0; geometryIndex < asCollection
                            .getNumGeometries(); geometryIndex++) {
                        intersectionGeometryData.setGeometry(mappingGeometry
                                .intersection(asCollection
                                        .getGeometryN(geometryIndex)));
                        result.add(intersectionGeometryData);
                    }

                }

            }

        } catch (TopologyException e) {
            statusHandler.error("Unexpected geometry error.  Redraw hazard");
        }
        return result;
    }

    /**
     * Returns a set of geometries from the maps database clipped to fit in the
     * geometry of the given {@link IHazardEvent}
     * 
     * @param hazardEvent
     *            The hazard event to build the hazard area for.
     * @return {@link IGeometryData}s. The result will be empty if there are no
     *         clipped geometries.
     */
    public Set<IGeometryData> getClippedMapGeometries(
            final IHazardEvent hazardEvent) {

        String hazardType = hazardEvent.getHazardType();
        String mapDBtableName = getMapDBtableName(hazardType);

        String mapLabelParameter = configManager.getHazardTypes()
                .get(hazardType).getUgcLabel();
        String cwa = configManager.getSiteID();
        Geometry hazardGeometry = hazardEvent.getGeometry();

        Set<IGeometryData> result = Sets.newHashSet();

        Set<IGeometryData> mapGeometries = getMapGeometries(mapDBtableName,
                mapLabelParameter, cwa);

        for (int i = 0; i < hazardGeometry.getNumGeometries(); ++i) {
            for (IGeometryData mapGeometry : mapGeometries) {

                for (int k = 0; k < mapGeometry.getGeometry()
                        .getNumGeometries(); ++k) {

                    Geometry clippedGeometry = geometryFactory.createPolygon(
                            null, null);

                    if (mapGeometry.getGeometry().getGeometryN(k)
                            .intersects(hazardGeometry.getGeometryN(i))) {
                        Geometry intersectionGeometry = mapGeometry
                                .getGeometry().getGeometryN(k)
                                .intersection(hazardGeometry.getGeometryN(i));

                        clippedGeometry = clippedGeometry
                                .union(intersectionGeometry);

                        for (int j = 0; j < clippedGeometry.getNumGeometries(); ++j) {
                            DefaultGeometryData clippedGeoData = new DefaultGeometryData();
                            clippedGeoData.setGeometry(clippedGeometry
                                    .getGeometryN(j));
                            result.add(clippedGeoData);
                        }
                    }
                }
            }

        }

        return result;
    }

    /**
     * Find a subset of mapGeometries containing the given {@link Geometry}
     * 
     * @param allMapGeometryData
     *            - the map geometries from which the subset will be computed
     * @return A subset of map geometries containing this geometry
     */
    public Set<IGeometryData> getContainingMapGeometries(
            Set<IGeometryData> allMapGeometryData, Geometry geometry) {
        return extractMapGeometries(allMapGeometryData, geometry,
                MapGeometryExtractionApproach.CONTAINING);
    }

    /**
     * Find a subset of mapGeometries intersecting the given {@link Geometry}
     * 
     * @param allMapGeometryData
     *            - the map geometries from which the subset will be computed
     * @return A subset of map geometries intersecting this geometry
     */
    public Set<IGeometryData> getIntersectingMapGeometries(
            IHazardEvent hazardEvent) {
        Set<IGeometryData> mapGeometryData = getMapGeometries(hazardEvent);
        return extractMapGeometries(mapGeometryData, hazardEvent.getGeometry(),
                MapGeometryExtractionApproach.INTERSECTION);
    }

    /**
     * Construct a mapping between UGC names and map geometries
     * 
     * @param mapDBtableName
     *            The map database table to retrieve geometries from
     * @param mapGeometryData
     *            the map geometries
     * @return the mapping
     */
    public Map<String, IGeometryData> getUgcsGeometryDataMapping(
            String mapDBtableName, Set<IGeometryData> mapGeometryData) {
        IugcToMapGeometryDataBuilder ugcBuilder = getUGCBuilder(mapDBtableName);
        Map<String, IGeometryData> result = ugcBuilder
                .ugcsToMapGeometryData(mapGeometryData);
        return result;
    }

    /**
     * Factory method which builds the correct IUGCBuilder based on the provided
     * geodatabase table name.
     * 
     * @param geoTableName
     *            The name of the geodatabase table
     * @return An IUGCBuilder object which knows how to construct UGCs for the
     *         specified geodatabase table.
     */
    public IugcToMapGeometryDataBuilder getUGCBuilder(String geoTableName) {

        if (geoTableUGCBuilderMap.containsKey(geoTableName)) {
            return geoTableUGCBuilderMap.get(geoTableName);
        } else {
            statusHandler.error("No UGC handler found for maps database table "
                    + geoTableName);
            return new NullUGCBuilder();
        }
    }

    /**
     * Determine if this is a polygon-based hazard
     * 
     * @param hazardEvent
     * @return true if the hazardEvent is a polygon based hazard type
     */
    public boolean isPolygonBased(IHazardEvent hazardEvent) {
        String hazardType = HazardEventUtilities.getHazardType(hazardEvent);

        HazardTypeEntry hazardTypeEntry = configManager.getHazardTypes().get(
                hazardType);

        return hazardTypeEntry.isPolygonBased();
    }

    /**
     * Find the mapDBtableName for the given {@link IHazardEvent}
     * 
     * @param hazardEvent
     * @return The name of the field in the maps db table to use for labeling.
     */
    public String getMapLabelParameter(IHazardEvent hazardEvent) {
        String mapLabelParameter = configManager.getHazardTypes()
                .get(hazardEvent.getHazardType()).getUgcLabel();
        return mapLabelParameter;
    }

    /**
     * Find the mapLabelParameter for the given {@link IHazardEvent}
     * 
     * @param hazardEvent
     * @return The name of the maps table to retrieve map geometries from.
     */
    public String getMapDBtableName(IHazardEvent hazardEvent) {
        String mapDBtableName = configManager.getHazardTypes()
                .get(hazardEvent.getHazardType()).getUgcType();
        return mapDBtableName;
    }

    /**
     * Returns a collection of geometry data from the maps geodatabase for the
     * specified hazadEvent
     * 
     * @param hazardEvent
     * @return A list of geometry data for the specified geo table and cwa.
     */
    private Set<IGeometryData> getMapGeometries(IHazardEvent hazardEvent) {
        String hazardType = hazardEvent.getHazardType();
        String mapDBtableName = getMapDBtableName(hazardType);

        String mapLabelParameter = configManager.getHazardTypes()
                .get(hazardType).getUgcLabel();

        String cwa = configManager.getSiteID();
        return getMapGeometries(mapDBtableName, mapLabelParameter, cwa);
    }

    private String getMapDBtableName(String hazardType) {
        String mapDBtableName = configManager.getHazardTypes().get(hazardType)
                .getUgcType();
        return mapDBtableName;
    }

    private Set<IGeometryData> getIntersectingMapGeometries(
            boolean applyIntersectionThreshold, final IHazardEvent hazardEvent,
            Set<IGeometryData> geometryData) {
        HazardTypeEntry hazardTypeEntry = configManager.getHazardTypes().get(
                hazardEvent.getHazardType());
        boolean inclusionFractionTest = hazardTypeEntry
                .isInclusionFractionTest();
        double inclusionFraction = hazardTypeEntry.getInclusionFraction();
        boolean inclusionAreaTest = hazardTypeEntry.isInclusionAreaTest();
        double inclusionAreaInSqKm = hazardTypeEntry.getInclusionAreaInSqKm();
        Set<IGeometryData> result = getIntersectingMapGeometries(
                applyIntersectionThreshold, hazardEvent, geometryData,
                inclusionFractionTest, inclusionFraction, inclusionAreaTest,
                inclusionAreaInSqKm);

        /*
         * By policy, you recalculate the warned areas without thresholding if
         * you get none that meet the thresholds
         */
        if (result.isEmpty()) {
            result = getIntersectingMapGeometries(false, hazardEvent,
                    geometryData, inclusionFractionTest, inclusionFraction,
                    inclusionAreaTest, inclusionAreaInSqKm);
        }
        return result;
    }

    private Set<IGeometryData> getIntersectingMapGeometries(
            boolean applyIntersectionThreshold, final IHazardEvent hazardEvent,
            Set<IGeometryData> geometryData, boolean inclusionFractionTest,
            double inclusionFraction, boolean inclusionAreaTest,
            double inclusionAreaInSqKm) {
        Set<IGeometryData> result = Sets.newHashSet();

        Geometry geometry = hazardEvent.getGeometry();
        for (IGeometryData geoData : geometryData) {

            Geometry mapGeometry = geoData.getGeometry();

            outer: for (int j = 0; j < mapGeometry.getNumGeometries(); ++j) {

                for (int i = 0; i < geometry.getNumGeometries(); ++i) {

                    Geometry hazardGeometry = geometry.getGeometryN(i);
                    Geometry mapdbGeometry = mapGeometry.getGeometryN(j);

                    if (hazardGeometry.intersects(mapdbGeometry)) {
                        /*
                         * The default rule is to include the geometry.
                         */
                        boolean defaultIncludeGeometry = true;

                        /*
                         * We currently run a default inclusion test on
                         * interoperability hazards due to differences in grid
                         * resolution.
                         */
                        if (hazardEvent.getHazardAttributes().containsKey(
                                HazardConstants.GFE_INTEROPERABILITY)) {
                            defaultIncludeGeometry = defaultInclusionTest(
                                    mapGeometry, hazardGeometry);
                        }

                        if (applyIntersectionThreshold) {

                            boolean includeGeometry = testInclusion(
                                    mapGeometry, hazardGeometry,
                                    inclusionFractionTest, inclusionFraction,
                                    inclusionAreaTest, inclusionAreaInSqKm);

                            if ((includeGeometry && defaultIncludeGeometry)
                                    || mapdbGeometry.contains(hazardGeometry)
                                    || hazardGeometry.contains(mapdbGeometry)) {
                                result.add(geoData);
                                continue outer;
                            }
                        } else if (defaultIncludeGeometry) {
                            result.add(geoData);
                            continue outer;
                        }
                    }
                }
            }
        }
        return result;
    }

    private Set<IGeometryData> extractMapGeometries(
            Set<IGeometryData> allMapGeometryData, Geometry geometry,
            MapGeometryExtractionApproach extractionApproach) {
        Set<IGeometryData> result = Sets.newHashSet();

        for (IGeometryData mapGeometryData : allMapGeometryData) {
            Geometry mapGeometry = mapGeometryData.getGeometry();

            outer: for (int mapGeometryIndex = 0; mapGeometryIndex < mapGeometry
                    .getNumGeometries(); ++mapGeometryIndex) {
                for (int geometryIndex = 0; geometryIndex < geometry
                        .getNumGeometries(); ++geometryIndex) {

                    Geometry mg = mapGeometry.getGeometryN(mapGeometryIndex);
                    Geometry gg = geometry.getGeometryN(geometryIndex);
                    if (extractionApproach == MapGeometryExtractionApproach.CONTAINING) {
                        if (mg.contains(gg)) {
                            result.add(mapGeometryData);
                            continue outer;
                        }
                    } else if (extractionApproach == MapGeometryExtractionApproach.CONTAINED) {
                        if (gg.contains(mg)) {
                            result.add(mapGeometryData);
                            continue outer;
                        }
                    } else {
                        /*
                         * Some trickiness here. We can't just intersect because
                         * we only want a match if the geometries intersect in
                         * at least some interior points. (i.e. there should be
                         * no match if only boundary points intersect).
                         * Geometry#crosses does not work because it returns
                         * false if all of the points are crossed. So take this
                         * approach instead.
                         */
                        if (gg.intersects(mg)) {
                            Geometry difference = gg.difference(mg);

                            if (!gg.equalsTopo(difference)) {
                                result.add(mapGeometryData);
                                continue outer;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /*
     * Default inclusion rules test. Currently used for interoperability
     * hazards.
     */
    private boolean defaultInclusionTest(Geometry mapGeometry,
            Geometry hazardGeometry) {

        return testInclusion(mapGeometry, hazardGeometry, true,
                DEFAULT_INTEROPERABILITY_OVERLAP_REQUIREMENT, false, 0.0);
    }

    /**
     * Test if the given map geometry should be included. The test depends on
     * the given hazardGeometry and user configuration of thresholds.
     * 
     * Note that even if the configuration says not to do the
     * inclusionFractionTest, we have to test for the inclusion being greater
     * than a small number to mitigate the round-off errors in the JTS
     * intersection calculations If we don't do this then, due to round-off
     * error, if you create a hazard by selecting by area, and specify a polygon
     * based hazard type (such as FA.W), the hatching will show neighboring
     * counties erroneously included. And the ultimate product will be wrong as
     * well.
     * 
     * @param mapGeometry
     * @param hazardGeometry
     * @param inclusionFractionTest
     * @param inclusionFraction
     * @param inclusionAreaTest
     * @param inclusionAreaInSqKm
     * @return true if the mapGeometry should be included.
     */
    private boolean testInclusion(Geometry mapGeometry,
            Geometry hazardGeometry, boolean inclusionFractionTest,
            double inclusionFraction, boolean inclusionAreaTest,
            double inclusionAreaInSqKm) {
        try {
            if (inclusionFractionTest == false) {
                inclusionFraction = 0.0001;
            }

            Geometry intersection = hazardGeometry.intersection(mapGeometry);
            double intersectionAreaInSquareDegrees = intersection.getArea();
            double intersectionFraction = intersectionAreaInSquareDegrees
                    / mapGeometry.getArea();

            boolean included = intersectionFraction > inclusionFraction;
            if (included && inclusionAreaTest) {
                Point centroid = intersection.getCentroid();
                double cosLat = Math.cos(Math.PI * centroid.getCoordinate().y);
                double inclusionAreaInSquareDegrees = inclusionAreaInSqKm
                        / (KM_PER_DEGREE_AT_EQUATOR * KM_PER_DEGREE_AT_EQUATOR * cosLat);
                included = intersectionAreaInSquareDegrees > inclusionAreaInSquareDegrees;
            }
            return included;
        } catch (TopologyException e) {
            /*
             * TODO Use {@link GeometryPrecisionReducer}?
             */
            statusHandler.error("Encountered topology exception", e);
            return false;
        }
    }
}