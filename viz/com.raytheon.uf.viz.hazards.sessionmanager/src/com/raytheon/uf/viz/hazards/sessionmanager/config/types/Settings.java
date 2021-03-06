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
package com.raytheon.uf.viz.hazards.sessionmanager.config.types;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import gov.noaa.gsd.common.utilities.DragAndDropGeometryEditSource;
import gov.noaa.gsd.common.utilities.DragAndDropGeometryEditSourceAdapter;
import gov.noaa.gsd.common.utilities.TimeResolution;
import gov.noaa.gsd.common.utilities.TimeResolutionAdapter;

/**
 * Basic settings implementation.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- ------------ --------------------------
 * May 20, 2013 1257       bsteffen     Initial creation
 * Aug 22, 2013  787       blawrenc     Added capability to associate the setting
 *                                      with one or more perspectives.
 * Dec 05, 2014 4124       Chris.Golden Made implementation of new ISettings
 *                                      interface, needed to allow for proper
 *                                      use of ObservedSettings.
 * Jan 29, 2015 4375       Dan Schaffer Console initiation of RVS product generation
 * Feb 15, 2015 2271       Dan Schaffer Incur recommender/product generator init costs immediately
 * Feb 23, 2015 3618       Chris.Golden Added possible sites to settings.
 * May 18, 2015 8227       Chris.Cody   Remove NullRecommender
 * Aug 03, 2015 8836       Chris.Cody   Changes for a configurable Event Id
 * May 10, 2016 18515      Chris.Golden Added "deselect after issuing" flag.
 * Oct 19, 2016 21873      Chris.Golden Added time resolution.
 * Oct 23, 2017 21730      Chris.Golden Added defaultType.
 * Jan 17, 2018 33428      Chris.Golden Removed no-longer-needed flag indicating
 *                                      whether a new geometry should be added to
 *                                      a selected event's geometry.
 * Jan 22, 2018 25765      Chris.Golden Added "priority for drag-and-drop geometry
 *                                      edit" flag to make geometry editing from
 *                                      the spatial display more flexible.
 * May 04, 2018 50032      Chris.Golden Added "additionalFilters" and
 *                                      "visibleAdditionalFilters" properties.
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
@XmlRootElement(name = "HazardServicesSettings")
@XmlAccessorType(XmlAccessType.FIELD)
public class Settings implements ISettings {

    /**
     * The ID of this settings object.
     */
    private String settingsID;

    /**
     * Which types of events should be loaded/displayed
     */
    private Set<String> visibleTypes;

    /**
     * Which statuses of events should be loaded/displayed
     */
    private Set<String> visibleStatuses;

    /**
     * Additional filters, if any.
     */
    private List<Object> additionalFilters;

    /**
     * Visible values for additional filters; meaningless unless
     * {@link #additionalFilters} is provided.
     */
    private Map<String, Object> visibleAdditionalFilters;

    /**
     * Which tools can be run
     */
    private List<Tool> toolbarTools;

    /**
     * How long the time window should be
     */
    private Long defaultTimeDisplayDuration;

    /**
     * Time resolution.
     */
    @XmlJavaTypeAdapter(TimeResolutionAdapter.class)
    private TimeResolution timeResolution;

    /**
     * Prior ity for drag-and-drop geometry edits.
     */
    @XmlJavaTypeAdapter(DragAndDropGeometryEditSourceAdapter.class)
    private DragAndDropGeometryEditSource priorityForDragAndDropGeometryEdits;

    /**
     * Where to center the map when this settings is loaded.
     */
    private MapCenter mapCenter;

    /**
     * Which hazard category new events should go into.
     */
    private String defaultCategory;

    /**
     * Which hazard type should be assigned to new events.
     */
    private String defaultType;

    /**
     * Which sites are possible for being loaded/displayed
     */
    private Set<String> possibleSites;

    /**
     * Which sites events should be loaded/displayed; must be a subset of
     * {@link #possibleSites}.
     */
    private Set<String> visibleSites;

    /**
     * A Pretty name for this configuration to show users.
     */
    private String displayName;

    /**
     * A reasonable duration for new events.
     */
    private Long defaultDuration;

    /**
     * Which columns to display in the temporal view.
     */
    private List<String> visibleColumns;

    /**
     * All columns that can be displayed in the temporal view.
     */
    private Map<String, Column> columns;

    /**
     * ID of the settings on which this one is based, used for incremental
     * override.
     */
    private String staticSettingsID;

    /**
     * Whether new events should be added to the selection or replace the
     * selection.
     */
    private Boolean addToSelected;

    /**
     * Identifiers of perspectives (if any) associated with this Setting. When
     * Hazard Services is started, these are searched to determine the
     * appropriate setting to load.
     */
    private Set<String> perspectiveIDs;

    /**
     * <pre>
     * Mode for displaying a Hazard Event ID:. ALWAYS_FULL Always returns the
     * full ID (default) FULL_ON_DIFF Always return the full id if there is any
     * difference between the current settings for: appId, siteId or year.
     * PROG_ON_DIFF Build a progressively larger Display Id based on the largest
     * difference between the Current Settings and the given Event Id. Different
     * App = Full ID "ZZ-SSS-YYYY-999999" Different Site = Site Level ID:
     * "SSS-YYYY-999999" (Same App) Different Year = Year Level ID:
     * "YYYY-999999" (Same App and Site) Same App, Site and Year: Serial ID:
     * "999999" ALWAYS_SITE Always display the Site Id and Serial Id
     * "SSS-999999" ONLY_SERIAL Only displays the Serial ID despite other
     * differences.
     * 
     */
    private String eventIdDisplayType;

    private Boolean deselectAfterIssuing;

    public Settings() {

    }

    public Settings(ISettings other) {
        apply(other);
    }

    /**
     * Copy all settings from another Settings object into this one.
     * 
     * @param other
     */
    @Override
    public void apply(ISettings other) {
        setSettingsID(other.getSettingsID());
        setVisibleTypes(other.getVisibleTypes());
        setVisibleStatuses(other.getVisibleStatuses());
        setAdditionalFilters(other.getAdditionalFilters());
        setVisibleAdditionalFilters(other.getVisibleAdditionalFilters());
        setToolbarTools(other.getToolbarTools());
        setDefaultTimeDisplayDuration(other.getDefaultTimeDisplayDuration());
        setTimeResolution(other.getTimeResolution());
        setPriorityForDragAndDropGeometryEdits(
                other.getPriorityForDragAndDropGeometryEdits());
        setMapCenter(other.getMapCenter());
        setDefaultCategory(other.getDefaultCategory());
        setDefaultType(other.getDefaultType());
        setPossibleSites(other.getPossibleSites());
        setVisibleSites(other.getVisibleSites());
        setDisplayName(other.getDisplayName());
        setDefaultDuration(other.getDefaultDuration());
        setVisibleColumns(other.getVisibleColumns());
        setColumns(other.getColumns());
        setStaticSettingsID(other.getStaticSettingsID());
        setAddToSelected(other.getAddToSelected());
        setPerspectiveIDs(other.getPerspectiveIDs());
        setEventIdDisplayType(other.getEventIdDisplayType());
        setDeselectAfterIssuing(other.getDeselectAfterIssuing());
    }

    @Override
    public String getSettingsID() {
        return settingsID;
    }

    @Override
    public void setSettingsID(String settingsID) {
        this.settingsID = settingsID;
    }

    @Override
    public Set<String> getVisibleTypes() {
        return visibleTypes;
    }

    @Override
    public void setVisibleTypes(Set<String> visibleTypes) {
        this.visibleTypes = visibleTypes;
    }

    @Override
    public Set<String> getVisibleStatuses() {
        return visibleStatuses;
    }

    @Override
    public void setVisibleStatuses(Set<String> visibleStatuses) {
        this.visibleStatuses = visibleStatuses;
    }

    @Override
    public List<Object> getAdditionalFilters() {
        return additionalFilters;
    }

    @Override
    public void setAdditionalFilters(List<Object> additionalFilters) {
        this.additionalFilters = additionalFilters;
    }

    @Override
    public Map<String, Object> getVisibleAdditionalFilters() {
        return visibleAdditionalFilters;
    }

    @Override
    public void setVisibleAdditionalFilters(
            Map<String, Object> visibleAdditionalFilters) {
        this.visibleAdditionalFilters = visibleAdditionalFilters;
    }

    @Override
    public List<Tool> getToolbarTools() {
        return toolbarTools;
    }

    @Override
    public void setToolbarTools(List<Tool> toolbarTools) {
        this.toolbarTools = toolbarTools;
    }

    @Override
    public Long getDefaultTimeDisplayDuration() {
        return defaultTimeDisplayDuration;
    }

    @Override
    public void setDefaultTimeDisplayDuration(Long defaultTimeDisplayDuration) {
        this.defaultTimeDisplayDuration = defaultTimeDisplayDuration;
    }

    @Override
    public TimeResolution getTimeResolution() {
        return timeResolution;
    }

    @Override
    public void setTimeResolution(TimeResolution timeResolution) {
        this.timeResolution = timeResolution;
    }

    @Override
    public DragAndDropGeometryEditSource getPriorityForDragAndDropGeometryEdits() {
        return priorityForDragAndDropGeometryEdits;
    }

    @Override
    public void setPriorityForDragAndDropGeometryEdits(
            DragAndDropGeometryEditSource priorityForDragAndDropGeometryEdits) {
        this.priorityForDragAndDropGeometryEdits = priorityForDragAndDropGeometryEdits;
    }

    @Override
    public MapCenter getMapCenter() {
        return mapCenter;
    }

    @Override
    public void setMapCenter(MapCenter mapCenter) {
        this.mapCenter = mapCenter;
    }

    @Override
    public String getDefaultCategory() {
        return defaultCategory;
    }

    @Override
    public void setDefaultCategory(String defaultCategory) {
        this.defaultCategory = defaultCategory;
    }

    @Override
    public String getDefaultType() {
        return defaultType;
    }

    @Override
    public void setDefaultType(String defaultType) {
        this.defaultType = defaultType;
    }

    @Override
    public Set<String> getPossibleSites() {
        return possibleSites;
    }

    @Override
    public void setPossibleSites(Set<String> possibleSites) {
        this.possibleSites = possibleSites;
    }

    @Override
    public Set<String> getVisibleSites() {
        return visibleSites;
    }

    @Override
    public void setVisibleSites(Set<String> visibleSites) {
        this.visibleSites = visibleSites;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public Long getDefaultDuration() {
        return defaultDuration;
    }

    @Override
    public void setDefaultDuration(Long defaultDuration) {
        this.defaultDuration = defaultDuration;
    }

    @Override
    public List<String> getVisibleColumns() {
        return visibleColumns;
    }

    @Override
    public void setVisibleColumns(List<String> visibleColumns) {
        this.visibleColumns = visibleColumns;
    }

    @Override
    public Map<String, Column> getColumns() {
        return columns;
    }

    @Override
    public void setColumns(Map<String, Column> columns) {
        this.columns = columns;
    }

    @Override
    public String getStaticSettingsID() {
        return staticSettingsID;
    }

    @Override
    public void setStaticSettingsID(String staticSettingsID) {
        this.staticSettingsID = staticSettingsID;
    }

    @Override
    public Boolean getAddToSelected() {
        return addToSelected;
    }

    @Override
    public void setAddToSelected(Boolean addToSelected) {
        this.addToSelected = addToSelected;
    }

    @Override
    public Set<String> getPerspectiveIDs() {
        return perspectiveIDs;
    }

    @Override
    public void setPerspectiveIDs(Set<String> perspectiveIDs) {
        this.perspectiveIDs = perspectiveIDs;
    }

    @Override
    public String getEventIdDisplayType() {
        return (this.eventIdDisplayType);
    }

    @Override
    public void setEventIdDisplayType(String eventIdDisplayType) {
        this.eventIdDisplayType = eventIdDisplayType;
    }

    @Override
    public Boolean getDeselectAfterIssuing() {
        return deselectAfterIssuing;
    }

    @Override
    public void setDeselectAfterIssuing(Boolean deselectAfterIssuing) {
        this.deselectAfterIssuing = deselectAfterIssuing;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.hazards.sessionmanager.config.types.ISettings#getTool
     * (java.lang.String)
     */
    @Override
    public Tool getTool(String toolName) {
        for (Tool tool : toolbarTools) {
            if (tool.getToolName().equals(toolName)) {
                return tool;
            }
        }
        return null;

    }

}
