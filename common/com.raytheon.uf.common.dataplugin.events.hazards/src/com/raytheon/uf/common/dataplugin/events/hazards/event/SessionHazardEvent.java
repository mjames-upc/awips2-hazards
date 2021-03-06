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
package com.raytheon.uf.common.dataplugin.events.hazards.event;

import static com.raytheon.uf.common.dataplugin.events.hazards.HazardConstants.HIGH_RESOLUTION_GEOMETRY_IS_VISIBLE;
import static com.raytheon.uf.common.dataplugin.events.hazards.HazardConstants.VISIBLE_GEOMETRY;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.common.dataplugin.events.hazards.HazardConstants;
import com.raytheon.uf.common.dataplugin.events.hazards.HazardConstants.HazardStatus;
import com.raytheon.uf.common.message.WsId;
import com.vividsolutions.jts.geom.Geometry;

import gov.noaa.gsd.common.utilities.geometry.AdvancedGeometryUtilities;
import gov.noaa.gsd.common.utilities.geometry.IAdvancedGeometry;
import gov.noaa.gsd.common.visuals.VisualFeature;
import gov.noaa.gsd.common.visuals.VisualFeaturesList;

/**
 * An {@link IHazardEvent} that is intended for client session use. It has no
 * storage annotations, and is not meant to be persisted.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 24, 2013            mnash       Initial creation
 * Nov 14, 2013 1472       bkowal      Renamed hazard subtype to subType
 * Dec    2013  2368       thansen   Added getHazardType
 * Apr 23, 2014 2925       Chris.Golden Augmented with additional methods to
 *                                      set the type components atomically, or
 *                                      the start and end time atomically.
 * Jun 30, 2014 3512       Chris.Golden Added addHazardAttributes() method.
 * Feb 22, 2015 6561       mpduff      Override getInsertTime and update toString
 * Jul 31, 2015 7458       Robert.Blum Added new userName and workstation fields.
 * Aug 03, 2015 8836       Chris.Cody   Changes for a configurable Event Id
 * Oct 29, 2015 11864      Robert.Blum  Added ExpirationTime field.
 * Mar 01, 2016 15676      Chris.Golden Added visual features to hazard event.
 * Mar 26, 2016 15676      Chris.Golden Added more methods to get and set
 *                                      individual visual features.
 * May 02, 2016 18235      Chris.Golden Added source field.
 * Jun 10, 2016 19537      Chris.Golden Combined base and selected visual feature
 *                                      lists for each hazard event into one,
 *                                      replaced by visibility constraints
 *                                      based upon selection state to individual
 *                                      visual features.
 * Jun 23, 2016 19537      Chris.Golden Changed to use new visual feature list
 *                                      method for visual feature replacement.
 * Sep 12, 2016 15934      Chris.Golden Changed hazard events to use advanced
 *                                      geometries instead of JTS geometries.
 * Sep 21, 2016 15934      Chris.Golden Changed to work with new version of
 *                                      AdvancedGeometryUtilities.
 * Oct 27, 2016 22956     Ben.Phillippe Changed getDisplayEventID to use
 *                                      FULL_ON_DIFF.
 * Dec 12, 2016 21504      Robert.Blum  Updates to correctly revert non-issued
 *                                      hazards.
 * Jan 26, 2017 21635      Roger.Ferrel Add issueSite.
 * Feb 01, 2017 15556      Chris.Golden Added visible-in-history-list flag. Also
 *                                      added insert time record.
 * Feb 16, 2017 29138      Chris.Golden Removed the visible-in-history-list flag
 *                                      since use of the history list is being
 *                                      reduced with advent of ability to save
 *                                      a "latest version" to the database that
 *                                      is not part of the history list.
 * Feb 16, 2017 28708      mduff        Changes for HazardServicesEventIdUtil.
 * Mar 13, 2017 28708      mduff        Changes to support event id refactor.
 * Mar 30, 2017 15528      Chris.Golden Added modified flag as part of basic
 *                                      hazard event, since this flag must be
 *                                      persisted as part of the hazard event.
 * Apr 28, 2017 33430      Robert.Blum  Removed HazardMode.
 * May 24, 2017 15561      Chris.Golden Added getPhensig() method.
 * Dec 17, 2017 20739      Chris.Golden Refactored away access to directly
 *                                      mutable session events.
 * Jan 26, 2018 33428      Chris.Golden Added issuance count.
 * May 08, 2018 15561      Chris.Golden Renamed to SessionHazardEvent.
 * Jun 06, 2018 15561      Chris.Golden Added practice flag for hazard event
 *                                      construction.
 * </pre>
 * 
 * @author mnash
 * @version 1.0
 */

public class SessionHazardEvent implements ISessionHazardEvent {

    private boolean modified;

    private Geometry flattenedGeometry;

    private IAdvancedGeometry geometry;

    private VisualFeaturesList visualFeatures;

    private String site;

    private String issueSite;

    private String eventId;

    private HazardStatus hazardStatus;

    private HazardStatus hazardPreExpiredStatus;

    private int issuanceCount;

    private String phenomenon;

    private String significance;

    private String subtype;

    private Date startTime;

    private Date endTime;

    private Date creationTime;

    private Date insertTime;

    private Date expirationTime;

    private Source source;

    private WsId wsId;

    private Map<String, Serializable> attributes;

    public SessionHazardEvent(boolean practice) {
        attributes = new HashMap<String, Serializable>();
        attributes.put(HazardConstants.PRACTICE, practice);
        attributes.put(VISIBLE_GEOMETRY, HIGH_RESOLUTION_GEOMETRY_IS_VISIBLE);
    }

    public SessionHazardEvent(IReadableHazardEvent event) {
        this(event.getHazardAttribute(HazardConstants.PRACTICE) != null
                ? (Boolean) event.getHazardAttribute(HazardConstants.PRACTICE)
                : false);
        setModified(event.isModified());
        setEventID(event.getEventID());
        setSiteID(event.getSiteID());
        setIssueSiteID(event.getIssueSiteID());
        setStartTime(event.getStartTime());
        setEndTime(event.getEndTime());
        setCreationTime(event.getCreationTime());
        setInsertTime(event.getInsertTime());
        setExpirationTime(event.getExpirationTime());
        setGeometry(event.getGeometry());
        setVisualFeatures(event.getVisualFeatures());
        setPhenomenon(event.getPhenomenon());
        setSignificance(event.getSignificance());
        setSubType(event.getSubType());
        setStatus(event.getStatus());
        if (event instanceof ISessionHazardEvent) {
            this.hazardPreExpiredStatus = ((ISessionHazardEvent) event)
                    .getPreExpiredStatus();
        }
        setIssuanceCount(event.getIssuanceCount());
        setSource(event.getSource());
        setWsId(event.getWsId());
        if (event.getHazardAttributes() != null) {
            getHazardAttributes().putAll(event.getHazardAttributes());
        }
        insertTime = event.getInsertTime();
    }

    @Override
    public boolean isModified() {
        return modified;
    }

    @Override
    public void setModified(boolean modified) {
        this.modified = modified;
    }

    @Override
    public Geometry getFlattenedGeometry() {
        return flattenedGeometry;
    }

    @Override
    public IAdvancedGeometry getGeometry() {
        return geometry;
    }

    @Override
    public VisualFeature getVisualFeature(String identifier) {
        return (visualFeatures == null ? null
                : visualFeatures.getByIdentifier(identifier));
    }

    @Override
    public VisualFeaturesList getVisualFeatures() {
        return visualFeatures;
    }

    @Override
    public String getSiteID() {
        return site;
    }

    @Override
    public void setSiteID(String site) {
        this.site = site;
    }

    @Override
    public String getIssueSiteID() {
        return issueSite;
    }

    @Override
    public void setIssueSiteID(String issueSite) {
        this.issueSite = issueSite;
    }

    @Override
    public String getEventID() {
        return eventId;
    }

    @Override
    public void setEventID(String uuid) {
        this.eventId = uuid;
    }

    @Override
    public String getDisplayEventID() {
        return HazardServicesEventIdUtil
                .getInstance(
                        (boolean) getHazardAttribute(HazardConstants.PRACTICE))
                .getDisplayId(getEventID(),
                        HazardEventUtilities.getSiteIdentifier());
    }

    @Override
    public HazardStatus getStatus() {
        return hazardStatus;
    }

    @Override
    public void setStatus(HazardStatus status) {
        this.hazardStatus = status;
        if ((status != HazardStatus.ELAPSING)
                && (status != HazardStatus.ELAPSED)) {
            this.hazardPreExpiredStatus = status;
        }
    }

    @Deprecated
    public void setStatus(String status) {
        setStatus(HazardStatus.valueOf(String.valueOf(status).toUpperCase()));
    }

    @Override
    public HazardStatus getPreExpiredStatus() {
        return hazardPreExpiredStatus;
    }

    @Override
    public int getIssuanceCount() {
        return issuanceCount;
    }

    @Override
    public void setIssuanceCount(int count) {
        this.issuanceCount = count;
    }

    @Override
    public String getPhenomenon() {
        return phenomenon;
    }

    @Override
    public void setPhenomenon(String phenomenon) {
        this.phenomenon = phenomenon;
    }

    @Override
    public String getSignificance() {
        return significance;
    }

    @Override
    public void setSignificance(String significance) {
        this.significance = significance;
    }

    @Override
    public String getSubType() {
        return subtype;
    }

    @Override
    public void setSubType(String subtype) {
        this.subtype = subtype;
    }

    @Override
    public String getHazardType() {
        return HazardEventUtilities.getHazardType(this);
    }

    @Override
    public void setHazardType(String phenomenon, String significance,
            String subtype) {
        setPhenomenon(phenomenon);
        setSignificance(significance);
        setSubType(subtype);
    }

    @Override
    public String getPhensig() {
        return HazardEventUtilities.getHazardPhenSig(this);
    }

    @Override
    public Date getStartTime() {
        return startTime;
    }

    @Override
    public void setStartTime(Date date) {
        this.startTime = date;
    }

    @Override
    public Date getEndTime() {
        return endTime;
    }

    @Override
    public void setEndTime(Date date) {
        this.endTime = date;
    }

    @Override
    public void setTimeRange(Date startTime, Date endTime) {
        setStartTime(startTime);
        setEndTime(endTime);
    }

    @Override
    public Date getCreationTime() {
        return creationTime;
    }

    @Override
    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    @Override
    public Date getInsertTime() {
        return insertTime;
    }

    @Override
    public void setInsertTime(Date date) {
        this.insertTime = date;
    }

    @Override
    public Date getExpirationTime() {
        return expirationTime;
    }

    @Override
    public void setExpirationTime(Date date) {
        this.expirationTime = date;
    }

    @Override
    public void setGeometry(IAdvancedGeometry geometry) {
        this.geometry = geometry;
        this.flattenedGeometry = AdvancedGeometryUtilities
                .getJtsGeometryAsCollection(geometry);
    }

    @Override
    public boolean setVisualFeature(VisualFeature visualFeature) {
        if (visualFeatures == null) {
            return false;
        }
        return visualFeatures.replace(visualFeature);
    }

    @Override
    public void setVisualFeatures(VisualFeaturesList visualFeatures) {
        this.visualFeatures = visualFeatures;
    }

    @Override
    public Source getSource() {
        return source;
    }

    @Override
    public void setSource(Source source) {
        this.source = source;
    }

    @Override
    public Map<String, Serializable> getHazardAttributes() {
        return attributes;
    }

    @Override
    public void setHazardAttributes(Map<String, Serializable> attributes) {
        this.attributes = new HashMap<>(attributes);
    }

    @Override
    public void addHazardAttribute(String key, Serializable value) {
        attributes.put(key, value);
    }

    @Override
    public void addHazardAttributes(Map<String, Serializable> attributes) {
        this.attributes.putAll(attributes);
    }

    @Override
    public void removeHazardAttribute(String key) {
        attributes.remove(key);
    }

    @Override
    public Serializable getHazardAttribute(String key) {
        return attributes.get(key);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((eventId == null) ? 0 : eventId.hashCode());
        result = prime * result
                + ((geometry == null) ? 0 : geometry.hashCode());
        result = prime * result
                + ((visualFeatures == null) ? 0 : visualFeatures.hashCode());
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        result = prime * result
                + ((hazardStatus == null) ? 0 : hazardStatus.hashCode());
        result = prime * result + ((hazardPreExpiredStatus == null) ? 0
                : hazardPreExpiredStatus.hashCode());
        result = prime * result + issuanceCount;
        result = prime * result
                + ((phenomenon == null) ? 0 : phenomenon.hashCode());
        result = prime * result
                + ((significance == null) ? 0 : significance.hashCode());
        result = prime * result + ((subtype == null) ? 0 : subtype.hashCode());
        result = prime * result + ((site == null) ? 0 : site.hashCode());
        result = prime * result
                + ((issueSite == null) ? 0 : issueSite.hashCode());
        result = prime * result
                + ((startTime == null) ? 0 : startTime.hashCode());
        result = prime * result + ((endTime == null) ? 0 : endTime.hashCode());
        result = prime * result
                + ((creationTime == null) ? 0 : creationTime.hashCode());
        result = prime * result
                + ((insertTime == null) ? 0 : insertTime.hashCode());
        result = prime * result
                + ((expirationTime == null) ? 0 : expirationTime.hashCode());
        result = prime * result + (modified ? 1 : 0);
        result = prime * result + ((wsId == null) ? 0 : wsId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SessionHazardEvent other = (SessionHazardEvent) obj;
        if (attributes == null) {
            if (other.attributes != null) {
                return false;
            }
        } else if (!attributes.equals(other.attributes)) {
            return false;
        }
        if (startTime == null) {
            if (other.startTime != null) {
                return false;
            }
        } else if (!startTime.equals(other.startTime)) {
            return false;
        }
        if (endTime == null) {
            if (other.endTime != null) {
                return false;
            }
        } else if (!endTime.equals(other.endTime)) {
            return false;
        }
        if (creationTime == null) {
            if (other.creationTime != null) {
                return false;
            }
        } else if (!creationTime.equals(other.creationTime)) {
            return false;
        }
        if (insertTime == null) {
            if (other.insertTime != null) {
                return false;
            }
        } else if (!insertTime.equals(other.insertTime)) {
            return false;
        }
        if (expirationTime == null) {
            if (other.expirationTime != null) {
                return false;
            }
        } else if (!expirationTime.equals(other.expirationTime)) {
            return false;
        }
        if (eventId == null) {
            if (other.eventId != null) {
                return false;
            }
        } else if (!eventId.equals(other.eventId)) {
            return false;
        }
        if (geometry == null) {
            if (other.geometry != null) {
                return false;
            }
        } else if (!geometry.equals(other.geometry)) {
            return false;
        }
        if (visualFeatures == null) {
            if (other.visualFeatures != null) {
                return false;
            }
        } else if (!visualFeatures.equals(other.visualFeatures)) {
            return false;
        }
        if (source != other.source) {
            return false;
        }
        if (hazardStatus != other.hazardStatus) {
            return false;
        }
        if (hazardPreExpiredStatus != other.hazardPreExpiredStatus) {
            return false;
        }
        if (issuanceCount != other.issuanceCount) {
            return false;
        }
        if (phenomenon == null) {
            if (other.phenomenon != null) {
                return false;
            }
        } else if (!phenomenon.equals(other.phenomenon)) {
            return false;
        }
        if (significance == null) {
            if (other.significance != null) {
                return false;
            }
        } else if (!significance.equals(other.significance)) {
            return false;
        }
        if (subtype == null) {
            if (other.subtype != null) {
                return false;
            }
        } else if (!subtype.equals(other.subtype)) {
            return false;
        }
        if (site == null) {
            if (other.site != null) {
                return false;
            }
        } else if (!site.equals(other.site)) {
            return false;
        }
        if (issueSite == null) {
            if (other.issueSite != null) {
                return false;
            }
        } else if (!issueSite.equals(other.issueSite)) {
            return false;
        }
        if (wsId == null) {
            if (other.wsId != null) {
                return false;
            }
        } else if (!wsId.equals(other.wsId)) {
            return false;
        }
        return (modified == other.modified);
    }

    @Override
    public String toString() {
        return eventId + " " + this.phenomenon + " " + this.significance;
    }

    @Override
    public Geometry getProductGeometry() {
        return HazardEventUtilities.getProductGeometry(this);
    }

    @Override
    public void setProductGeometry(Geometry geom) {
        HazardEventUtilities.setProductGeometry(this, geom);
    }

    @Override
    public WsId getWsId() {
        return wsId;
    }

    @Override
    public void setWsId(WsId wsId) {
        this.wsId = wsId;
    }
}
