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
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * August 2013  1360       hansen      Added fields for product information
 *
 **/
package com.raytheon.uf.viz.hazards.sessionmanager.deprecated;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.google.common.collect.Lists;
import com.raytheon.uf.common.dataplugin.events.hazards.HazardConstants.HazardState;
import com.raytheon.uf.common.dataplugin.events.hazards.event.BaseHazardEvent;
import com.raytheon.uf.common.dataplugin.events.hazards.event.IHazardEvent;
import com.raytheon.uf.viz.hazards.sessionmanager.events.ISessionEventManager;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Implements many of the fields that are used on JSON events.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 21, 2013 1257       bsteffen    Initial creation
 * Aug  9, 2013 1921       daniel.s.schaffer@noaa.gov    Enhance {@link #getGeometry()} to support multi-polygons
 * Aug     2013 1360       hansen      Added fields for product information
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
@Deprecated
public class Event {

    // TODO int
    private String eventID;

    private Shape[] shapes;

    private String backupSiteID;

    private double[][] draggedPoints;

    private Long startTime;

    private String hazardCategory;

    private Long creationTime;

    private String modifyCallbackToolName;

    private String state;

    private Long endTime;

    private String siteID;

    private Boolean checked;

    private String headline;

    private Boolean selected;

    private String color;

    private String type;

    private String fullType;

    private String cause;

    private String phen;

    private String subType;

    private String sig;

    private String damName;

    private String geoType;

    private Boolean polyModified;

    private Long expirationTime;

    private Long issueTime;

    private String etns;

    private String pils;

    private String vtecCodes;

    private static GeometryFactory geometryFactory = new GeometryFactory();

    public Event() {

    }

    public Event(IHazardEvent event) {
        Map<String, Serializable> attr = event.getHazardAttributes();

        eventID = event.getEventID();
        startTime = event.getStartTime().getTime();
        endTime = event.getEndTime().getTime();
        if (event.getIssueTime() != null) {
            creationTime = event.getIssueTime().getTime();
        } else {
            Object cTimeAttr = attr.get("creationTime");
            if (cTimeAttr instanceof Date) {
                creationTime = ((Date) cTimeAttr).getTime();
            }
        }
        Object hCatAttr = attr.get(ISessionEventManager.ATTR_HAZARD_CATEGORY);
        if (hCatAttr instanceof String) {
            hazardCategory = (String) hCatAttr;
        }
        siteID = event.getSiteID();
        backupSiteID = event.getSiteID();
        phen = event.getPhenomenon();
        sig = event.getSignificance();

        subType = event.getSubtype();
        if (subType == null) {
            subType = "";
        }

        if (phen != null && sig != null) {
            type = phen + "." + sig;
            if (subType != null && !subType.isEmpty()) {
                type += "." + subType;
            }
        }

        if (event.getState() != null) {
            state = event.getState().toString().toLowerCase();
        }

        if (attr.containsKey("cause")) {
            cause = attr.get("cause").toString();
        }
        if (attr.containsKey("damName")) {
            damName = attr.get("damName").toString();
        }

        if (type != null) {
            fullType = type + " (" + headline + ")";
        }

        checked = (Boolean) attr.get(ISessionEventManager.ATTR_CHECKED);
        color = "255 255 255";
        selected = (Boolean) attr.get(ISessionEventManager.ATTR_SELECTED);

        if (event.getState() != HazardState.ENDED
                && Boolean.TRUE.equals(attr
                        .get(ISessionEventManager.ATTR_ISSUED))) {
            state = HazardState.ISSUED.toString().toLowerCase();
        }

        geoType = "area";
        polyModified = true;

        draggedPoints = new double[0][];

        Geometry geom = event.getGeometry();
        if (geom instanceof MultiPolygon) {
            this.shapes = new Shape[geom.getNumGeometries()];
            for (int i = 0; i < shapes.length; i += 1) {
                shapes[i] = convertGeometry(geom.getGeometryN(i));
            }
        } else {
            shapes = new Shape[] { convertGeometry(geom) };
        }

        if (attr.containsKey("expirationTime")) {
            expirationTime = (Long) attr.get("expirationTime");
        }
        if (attr.containsKey("issueTime")) {
            issueTime = (Long) attr.get("issueTime");
        }
        if (attr.containsKey("vtecCodes")) {
            Serializable eventVtecCodes = attr.get("vtecCodes");
            if (eventVtecCodes != null) {
                vtecCodes = attr.get("vtecCodes").toString();
            } else {
                vtecCodes = "[]";
            }
        }
        if (attr.containsKey("etns")) {
            Serializable eventVtecCodes = attr.get("etns");
            if (eventVtecCodes != null) {
                etns = attr.get("etns").toString();
            } else {
                etns = "[]";
            }
        }
        if (attr.containsKey("pils")) {
            Serializable eventVtecCodes = attr.get("pils");
            if (eventVtecCodes != null) {
                pils = attr.get("pils").toString();
            } else {
                pils = "[]";
            }
        }

    }

    private Shape convertGeometry(Geometry geom) {
        List<double[]> points = new ArrayList<double[]>();
        for (Coordinate c : geom.getCoordinates()) {
            points.add(new double[] { c.x, c.y });
        }
        points.remove(points.size() - 1);
        Shape shape = new Shape();
        shape.setPoints(points.toArray(new double[0][]));
        shape.setShapeType("polygon");
        shape.setLabel(eventID + " ");
        if (type != null) {
            shape.setLabel(eventID + " " + type);
        }
        shape.setIsSelected(selected);
        shape.setIsVisible("true");
        shape.setInclude("true");
        return shape;
    }

    public String getEventID() {
        return eventID;
    }

    public void setEventID(String eventID) {
        this.eventID = eventID;
    }

    public Shape[] getShapes() {
        return shapes;
    }

    public void setShapes(Shape[] shapes) {
        this.shapes = shapes;
    }

    public String getBackupSiteID() {
        return backupSiteID;
    }

    public void setBackupSiteID(String backupSiteID) {
        this.backupSiteID = backupSiteID;
    }

    public double[][] getDraggedPoints() {
        return draggedPoints;
    }

    public void setDraggedPoints(double[][] draggedPoints) {
        this.draggedPoints = draggedPoints;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public String getHazardCategory() {
        return hazardCategory;
    }

    public void setHazardCategory(String hazardCategory) {
        this.hazardCategory = hazardCategory;
    }

    public Long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }

    public String getModifyCallbackToolName() {
        return modifyCallbackToolName;
    }

    public void setModifyCallbackToolName(String modifyCallbackToolName) {
        this.modifyCallbackToolName = modifyCallbackToolName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public String getSiteID() {
        return siteID;
    }

    public void setSiteID(String siteID) {
        this.siteID = siteID;
    }

    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
        this.checked = checked;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFullType() {
        return fullType;
    }

    public void setFullType(String fullType) {
        this.fullType = fullType;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public String getPhen() {
        return phen;
    }

    public void setPhen(String phen) {
        this.phen = phen;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public String getSig() {
        return sig;
    }

    public void setSig(String sig) {
        this.sig = sig;
    }

    public String getDamName() {
        return damName;
    }

    public void setDamName(String damName) {
        this.damName = damName;
    }

    public void setGeoType(String geoType) {
        this.geoType = geoType;
    }

    public String getGeoType() {
        return geoType;
    }

    public void setIssueTime(Long issueTime) {
        this.issueTime = issueTime;
    }

    public Long getIssueTime() {
        return issueTime;
    }

    public void setExpirationTime(Long expirationTime) {
        this.expirationTime = expirationTime;
    }

    public Long getExpirationTime() {
        return expirationTime;
    }

    public void setEtns(String etns) {
        this.etns = etns;
    }

    public String getEtns() {
        return etns;
    }

    public void setVtecCodes(String vtecCodes) {
        this.etns = vtecCodes;
    }

    public String getVtecCodes() {
        return vtecCodes;
    }

    public void setPils(String pils) {
        this.pils = pils;
    }

    public String getPils() {
        return pils;
    }

    public Boolean getPolyModified() {
        return polyModified;
    }

    public void setPolyModified(Boolean polyModified) {
        this.polyModified = polyModified;
    }

    public IHazardEvent toHazardEvent() {
        IHazardEvent event = new BaseHazardEvent();
        if (startTime != null) {
            event.setStartTime(new Date(startTime));
        }
        if (endTime != null) {
            event.setEndTime(new Date(endTime));
        }
        if (creationTime != null) {
            event.addHazardAttribute("creationTime", new Date(creationTime));
        }
        if (hazardCategory != null) {
            event.addHazardAttribute(ISessionEventManager.ATTR_HAZARD_CATEGORY,
                    hazardCategory);
        }
        event.setSiteID(siteID);
        event.setPhenomenon(phen);
        event.setSignificance(sig);

        event.setSubtype(subType);

        if (state != null) {
            event.setState(HazardState.valueOf(state.toUpperCase()));
        }

        if (cause != null) {
            event.addHazardAttribute("cause", cause);
        }
        if (type != null) {
            event.addHazardAttribute("type", type);
        }
        if (damName != null) {
            event.addHazardAttribute("damName", damName);
        }

        event.setGeometry(getGeometry());

        return event;
    }

    @JsonIgnore
    public Geometry getGeometry() {
        assert (shapes != null && shapes.length != 0);
        List<Polygon> polygons = Lists.newArrayList();
        for (Shape shape : shapes) {

            Polygon p = buildPolygon(shape);
            polygons.add(p);
        }
        Geometry result;
        if (polygons.size() == 1) {
            result = polygons.get(0);
        } else {
            result = new MultiPolygon(polygons.toArray(new Polygon[polygons
                    .size()]), geometryFactory);
        }

        return result;
    }

    private Polygon buildPolygon(Shape shape) {
        List<Coordinate> coords = new ArrayList<Coordinate>();
        for (double[] point : shape.points) {
            coords.add(new Coordinate(point[0], point[1]));
        }
        coords.add(coords.get(0));
        Polygon p = geometryFactory.createPolygon(geometryFactory
                .createLinearRing(coords.toArray(new Coordinate[0])), null);
        return p;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}