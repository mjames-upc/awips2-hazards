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
package com.raytheon.uf.viz.hazards.sessionmanager.events;

import gov.noaa.gsd.viz.megawidgets.MegawidgetSpecifierManager;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Range;
import com.raytheon.uf.common.dataplugin.events.hazards.HazardConstants.HazardStatus;
import com.raytheon.uf.common.dataplugin.events.hazards.event.IHazardEvent;
import com.raytheon.uf.viz.hazards.sessionmanager.config.ISessionConfigurationManager;
import com.raytheon.uf.viz.hazards.sessionmanager.originator.IOriginator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Manages all events in a session.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 21, 2013 1257       bsteffen    Initial creation
 * Oct 22, 2013 1463       blawrence   Added methods for hazard conflict
 *                                     detection.
 *  
 * Nov 29, 2013 2380       daniel.s.schaffer@noaa.gov Fixing bugs in settings-based filtering
 * Nov 29, 2013 2378       blawrenc    Added methods for proposing,
 *                                     issuing and ending hazard 
 *                                     events. This keeps the 
 *                                     knowledge of what to do
 *                                     in these cases better
 *                                     encapsulated in the
 *                                     event manager.
 * Apr 09, 2014 2925       Chris.Golden Added method to set event type, and anotherto get the
 *                                      megawidget specifier manager for a given hazard event.
 * May 15, 2014 2925       Chris.Golden Added methods to set hazard category, set last modified
 *                                      event, and get set of hazards for which proposal is
 *                                      possible. Also changed getSelectedEvents() to return a
 *                                      list.
 * Aug 20, 2014 4243       Chris.Golden Added new method to receive notification of a script
 *                                      command having been invoked.
 * Sep 16, 2014 4753       Chris.Golden Changed event script to include mutable properties.
 * Dec  1, 2014 4188       Dan Schaffer Now allowing hazards to be shrunk or expanded when appropriate.
 * Dec 13, 2014 4959       Dan Schaffer Spatial Display cleanup and other bug fixes
 * Jan 08, 2015 5700       Chris.Golden Changed to generalize the meaning of a command invocation
 *                                      for a particular event, since it no longer only means
 *                                      that an event-modifying script is to be executed.
 * Jan  7, 2015 4959       Dan Schaffer Ability to right click to add/remove UGCs from hazards
 * Jan 26, 2015 5952       Dan Schaffer Fix incorrect hazard area designation.
 * Feb  1, 2015 2331       Chris.Golden Added code to track the allowable boundaries of all hazard
 *                                      events' start and end times, so that the user will not move
 *                                      them beyond the allowed ranges.
 * Feb 12, 2015 4959       Dan Schaffer Modify MB3 add/remove UGCs to match Warngen
 * Mar 13, 2015 6090       Dan Schaffer Relaxed geometry validity check.
 * Sep 15, 2015 7629       Robert.Blum  Added method that persists a list of events.
 * Mar 24, 2016 15676      Chris.Golden Changed setModifiedEventGeometry() to return true if it
 *                                      succeeds in changing the geometry, false otherwise.
 * Mar 26, 2016 15676      Chris.Golden Removed geometry validity checks (that is, checks to see
 *                                      if Geometry objects pass the isValid() test), as the
 *                                      session event manager shouldn't be policing this; it should
 *                                      assume it gets valid geometries.
 * Jun 06, 2016 19432      Chris.Golden Added method to set a flag indicating whether newly-created
 *                                      (by the user) hazard events should be added to the selected
 *                                      set or not.
 * Jul 25, 2016   19537    Chris.Golden Changed collections of events that were returned into lists,
 *                                      since the unordered nature of the collections was not
 *                                      appropriate. Added originator parameters for methods for
 *                                      setting high- and low-res geometries for hazard events.
 *                                      Removed obsolete set-geometry method.
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */

public interface ISessionEventManager<E extends IHazardEvent> {

    /**
     * The issued attribute will be available as a Boolean for all hazards in
     * the session to mark whether the event has been previously issued, it will
     * not be persisted.
     */
    public static final String ATTR_ISSUED = "issued";

    /**
     * The hazard category attribute will be available as a String for any new
     * hazards without a phenSig. After a phenSig has been assigned hazard
     * category should be looked up from the configuration manager. This
     * attribute will not be persisted.
     */
    public static final String ATTR_HAZARD_CATEGORY = "hazardCategory";

    /**
     * Add a new event to the Session, for example the event might come from a
     * user geometry or from a recommender. The new event will automatically be
     * selected and checked.
     * 
     * @param event
     * @param originator
     * @return
     */
    public E addEvent(IHazardEvent event, IOriginator originator);

    /**
     * Get the event with the given ID or null if there is no such event in the
     * session.
     * 
     * @param eventId
     * @return
     */
    public E getEventById(String eventId);

    /**
     * Set the specified event to have the specified category. As a side effect,
     * the event is changed to have no type.
     * 
     * @param event
     *            Event to be modified.
     * @param category
     *            Category for the event.
     * @param originator
     *            Originator of this change.
     */
    public void setEventCategory(E event, String category,
            IOriginator originator);

    /**
     * Set the specified event to have the specified type. If the former cannot
     * change its type, a new event will be created as a result.
     * 
     * @param event
     *            Event to be modified.
     * @param phenomenon
     *            Phenomenon, or <code>null</code> if the event is to have no
     *            type.
     * @param significance
     *            Phenomenon, or <code>null</code> if the event is to have no
     *            type.
     * @param subType
     *            Phenomenon, or <code>null</code> if the event is to have no
     *            subtype.
     * @param originator
     *            Originator of this change.
     * @return True if the event type was set, or false if the attempt resulted
     *         in the creation of a new event with the new type, and the
     *         original event has not had its type changed.
     */
    public boolean setEventType(E event, String phenomenon,
            String significance, String subType, IOriginator originator);

    /**
     * Set the specified event's time range.
     * 
     * @param event
     *            Event to be modified.
     * @param startTime
     *            New start time.
     * @param endTime
     *            New end time.
     * @param originator
     *            Originator of this change.
     * @return True if the new time range is now in use, false if it was
     *         rejected because one or both values fell outside their allowed
     *         boundaries.
     */
    public boolean setEventTimeRange(E event, Date startTime, Date endTime,
            IOriginator originator);

    /**
     * Set the specified event's geometry. It is assumed that the specified
     * geometry is valid, that is, that {@link Geometry#isValid()} would return
     * <code>true</code>.
     * 
     * @param event
     *            Event to be modified.
     * @param geometry
     *            New geometry.
     * @param originator
     *            Originator of this change.
     * @return True if the new geometry is now in use, false if it was rejected.
     */
    public boolean setEventGeometry(E event, Geometry geometry,
            IOriginator originator);

    /**
     * Get the megawidget specifier manager for the specified event. Note that
     * this method must be implemented to return a cached manager if
     * appropriate, unlike the
     * {@link ISessionConfigurationManager#getMegawidgetSpecifiersForHazardEvent(IHazardEvent)}
     * method.
     * 
     * @param event
     *            Hazard event for which to retrieve the manager.
     * @return Megawidget specifier manager, holding specifiers for the
     *         megawidgets as well as any side effects applier to be used with
     *         the megawidgets.
     */
    public MegawidgetSpecifierManager getMegawidgetSpecifiers(E event);

    /**
     * Get the duration selector choices that are available for the specified
     * event, given the latter's status.
     * 
     * @param event
     *            Event for which to fetch the duration selector choices.
     * @return List of choices; each of these is of the form given by the
     *         description of the
     *         {@link gov.noaa.gsd.viz.megawidgets.validators.SingleTimeDeltaStringChoiceValidatorHelper}
     *         class. The list is pruned of any choices that are not currently
     *         available for the specified event if its end time cannot shrink
     *         or expand. If the specified event does not use a duration
     *         selector for its end time, an empty list is returned.
     */
    public List<String> getDurationChoices(E event);

    /**
     * Receive notification that a command was invoked within the user interface
     * that requires a metadata refresh or a script to be run in response.
     * 
     * @param event
     *            Hazard event for which to run the script or refresh the
     *            metadata.
     * @param identifier
     *            Identifier of the command that was invoked.
     * @param mutableProperties
     *            Mutable properties to be passed to the script, if one is run.
     */
    public void eventCommandInvoked(E event, String identifier,
            Map<String, Map<String, Object>> mutableProperties);

    /**
     * Get all events with the given state from the session. This will never
     * return null, if no states exist an empty collection is returned.
     * 
     * @param state
     * @return
     */
    public Collection<E> getEventsByStatus(HazardStatus state);

    /**
     * Remove an event from the session.
     * 
     * @param event
     * @param originator
     */
    public void removeEvent(E event, IOriginator originator);

    /**
     * Remove events from the session.
     * 
     * @param events
     * @param originator
     */
    public void removeEvents(Collection<E> events, IOriginator originator);

    /**
     * Get all events that are currently part of this session.
     * 
     * @return
     */
    public List<E> getEvents();

    /**
     * 
     * @return the selected events
     */
    public List<E> getSelectedEvents();

    /**
     * Return the selected event that was most recently modified.
     * 
     * @return
     */
    public E getLastModifiedSelectedEvent();

    /**
     * Set the selected event that was most recently modified.
     * 
     * @param event
     *            New last-modified event.
     * @param originator
     *            Originator of the change.
     */
    public void setLastModifiedSelectedEvent(E event, IOriginator originator);

    /**
     * Set the selected events. Any currently selected events that are no in
     * selectedEvents will be deslected, all events in the selectedEvents set
     * will get ATTR_SELECTED set to True.
     * 
     * @param selectedEvents
     * @param originator
     */
    public void setSelectedEvents(Collection<E> selectedEvents,
            IOriginator originator);

    /**
     * Set the selected events corresponding to on the eventIDs.
     * 
     * @param selectedEventIDs
     * @param originator
     */
    public void setSelectedEventForIDs(Collection<String> selectedEventIDs,
            IOriginator originator);

    /**
     * 
     * @return the checked events
     */
    public List<E> getCheckedEvents();

    public List<E> getEventsForCurrentSettings();

    /**
     * Tests whether it is valid to change a hazard type(includes phen, sig, and
     * subtype).
     * 
     * @param event
     * @return
     */
    public boolean canChangeType(E event);

    /**
     * Tests if an event's area can be changed.
     * 
     * @param event
     *            The event to test
     * @return True - the event's area can be changed. False - the event's area
     *         cannot be changed.
     */
    public boolean canEventAreaBeChanged(E event);

    /**
     * Sort the events using a comparator. This can be useful with
     * SEND_SELECTED_BACK or SEND_SELECTED_TO_FRONT
     * 
     * @param comparator
     */
    public void sortEvents(Comparator<E> comparator);

    /**
     * Checks all events for conflicts.
     * 
     * @param
     * @return A map of events with a map of any conflicting events and lists of
     *         corresponding conflicting geometries.
     */
    public Map<IHazardEvent, Map<IHazardEvent, Collection<String>>> getAllConflictingEvents();

    /**
     * Tests if a specific event conflicts spatially with an existing event or
     * event(s).
     * 
     * @param event
     *            Event to test for conflicts
     * @param startTime
     *            - modified start time of hazard event
     * @param endTime
     *            - modified end time of hazard event
     * @param geometry
     *            - modified geometry of hazard event.
     * @param phenSigSubtype
     *            Contains phenomena, significance and an optional sub-type.
     * 
     * @return A map of events which conflict spatially with an existing event
     *         or events. Each event in the map will have a list of area names
     *         where the conflict is occurring. This map will be empty if there
     *         are no conflicting hazards.
     */
    public Map<IHazardEvent, Collection<String>> getConflictingEvents(
            IHazardEvent event, Date startTime, Date endTime,
            Geometry geometry, String phenSigSubtype);

    /**
     * Get a map of selected event identifiers to any events with which they
     * conflict. The returned object will be kept current by the instance of
     * this class, so that it will continue to be valid as long as the session
     * event manager exists. At any given instant after it is fetched via this
     * method, it may be queried to determine whether or not a specific selected
     * hazard event conflicts with others.
     * <p>
     * Note that the map is unmodifiable; attempts to modify it will result in
     * an {@link UnsupportedOperationException}.
     * 
     * @return Map of selected event identifiers to any events with which they
     *         conflict. The latter is an empty collection if there are no
     *         conflicting hazards.
     */
    public Map<String, Collection<IHazardEvent>> getConflictingEventsForSelectedEvents();

    /**
     * Get a set indicating which hazard event identifiers are allowed to have
     * their end time "until further notice" mode toggled. The returned object
     * will be kept current by the instance of this class, so that it will
     * continue to be valid as long as the session event manager exists. At any
     * given instant after it is fetched via this method, it may be queried to
     * determine whether or not a specific hazard event within this session may
     * use "until further notice".
     * <p>
     * Note that the set is unmodifiable; attempts to modify it will result in
     * an {@link UnsupportedOperationException}.
     * 
     * @return Set of hazard event identifiers indicating which events may have
     *         their end time "until further notice" mode toggled.
     */
    public Set<String> getEventIdsAllowingUntilFurtherNotice();

    /**
     * Get a map of hazard event identifiers to their corresponding start time
     * editability limitations. Each hazard event being managed must have an
     * entry in this map. The returned object will be kept current by the
     * instance of this class, so that it will continue to be valid as long as
     * the session event manager exists. At any given instant after it is
     * fetched via this method, it may be queried to determine the start time
     * boundaries for a specific hazard event within this session.
     * <p>
     * Note that the map is unmodifiable; attempts to modify it will result in
     * an {@link UnsupportedOperationException}.
     * 
     * @return Map of hazard event identifiers to their corresponding start time
     *         editability limitations.
     */
    public Map<String, Range<Long>> getStartTimeBoundariesForEventIds();

    /**
     * Get a map of hazard event identifiers to their corresponding end time
     * editability limitations. Each hazard event being managed must have an
     * entry in this map. The returned object will be kept current by the
     * instance of this class, so that it will continue to be valid as long as
     * the session event manager exists. At any given instant after it is
     * fetched via this method, it may be queried to determine the end time
     * boundaries for a specific hazard event within this session.
     * <p>
     * Note that the map is unmodifiable; attempts to modify it will result in
     * an {@link UnsupportedOperationException}.
     * 
     * @return Map of hazard event identifiers to their corresponding end time
     *         editability limitations.
     */
    public Map<String, Range<Long>> getEndTimeBoundariesForEventIds();

    /**
     * Sets the state of the event to ENDED, persists it to the database and
     * notifies all listeners of this state change.
     * 
     * @param event
     * @param originator
     */
    public void endEvent(E event, IOriginator originator);

    /**
     * Sets the state of the event to ISSUED, persists it to the database and
     * notifies all listeners of this.
     * 
     * @param event
     * @param originator
     */
    public void issueEvent(E event, IOriginator originator);

    /**
     * Get a set indicating which hazard event identifiers are allowed to have
     * their status changed to "proposed".
     * 
     * TODO: For now, the set is not kept current, so it is valid only at the
     * time it is retrieved via this method and should not be cached for future
     * checks. It would be far less wasteful to have it behave like the "until
     * further notice" set, and have it be kept current by the instance of this
     * class, so that it will continue to be valid as long as the session event
     * manager exists. At any given instant after it is fetched via this method,
     * it could be queried to determine whether or not a specific hazard event
     * within this session may have its status changed to "proposed".
     * <p>
     * Note that the set is unmodifiable; attempts to modify it will result in
     * an {@link UnsupportedOperationException}.
     * 
     * @return Set of hazard event identifiers indicating which events may have
     *         their status changed to "proposed".
     */
    public Set<String> getEventIdsAllowingProposal();

    /**
     * Sets the state of the event to PROPOSED, persists it to the database and
     * notifies all listeners of this.
     * 
     * @param event
     * @param originator
     */
    public void proposeEvent(E event, IOriginator originator);

    /**
     * Sets the state of the events to PROPOSED, persists them to the database
     * and notifies all listeners of this.
     * 
     * @param events
     * @param originator
     */
    public void proposeEvents(Collection<E> events, IOriginator originator);

    /**
     * Makes visible the hazard (high resolution) representation of the selected
     * hazard geometries.
     * 
     * @param originator
     */
    public void setHighResolutionGeometriesVisibleForSelectedEvents(
            IOriginator originator);

    /**
     * Builds, stores and makes visible the product (low resolution)
     * representation of the selected hazard geometries. This includes clipping
     * to the CWA, modifying the polygons to conform to the hazard areas and
     * simplifying the polygons to conform to the max 20 point rule.
     * 
     * @param originator
     * @return true - this function successfully clipped the hazard geometries
     *         false - this function failed, probably because a geometry was
     *         outside of the forecast area (cwa or hsa).
     */
    public boolean setLowResolutionGeometriesVisibleForSelectedEvents(
            IOriginator originator);

    /**
     * Makes visible the hazard (high resolution) representation of the selected
     * hazard geometries.
     * 
     * @param originator
     */
    public void setHighResolutionGeometryVisibleForCurrentEvent(
            IOriginator originator);

    /**
     * Builds, stores and makes visible the product (low resolution)
     * representation of the current hazard geometry.
     * 
     * @param originator
     * @return true - this function successfully clipped the hazard geometru
     *         false - this function failed, probably because the geometry was
     *         outside of the forecast area.
     */
    public boolean setLowResolutionGeometryVisibleForCurrentEvent(
            IOriginator originator);

    /**
     * Updates the UGC information associated with the selected hazard events.
     * 
     * @param
     * @return
     */
    public void updateSelectedHazardUGCs();

    /**
     * Execute any shutdown needed.
     */
    public void shutdown();

    /**
     * @param eventId
     *            of the event the user is currently pointing to.
     */
    public void setCurrentEvent(String eventId);

    /**
     * @param the
     *            event the user is currently pointing to.
     */
    public void setCurrentEvent(E event);

    /**
     * 
     * @return event the user is currently pointing to
     */
    public E getCurrentEvent();

    /**
     * @return true if the user is currently pointing to an event
     */
    public boolean isCurrentEvent();

    /**
     * @return true if the event is selected
     */
    public boolean isSelected(E event);

    /**
     * Determine whether the specified hazard event may accept the specified
     * geometry as its new geometry. It is assumed that the geometry is valid,
     * i.e. {@link Geometry#isValid()} returns <code>true</code>, if this method
     * is told not to check geometry validity.
     * 
     * @param geometry
     *            Geometry to be used.
     * @param hazardEvent
     *            Hazard event to have its geometry changed.
     * @param checkGeometryValidity
     *            Flag indicating whether or not to check the geometry's
     *            validity itself.
     * @return True if the geometry of the given hazard event can be modified to
     *         the given geometry, false otherwise,.
     */
    public boolean isValidGeometryChange(Geometry geometry, E hazardEvent,
            boolean checkGeometryValidity);

    /**
     * Find the UGC enclosing the given location. If that UGC is included in the
     * currently selected event then remove it; if it is not included, add it.
     * If more or less than one event is selected, then do not make any change.
     * 
     * @param location
     *            Coordinate enclosed by a UGC
     * @param originator
     *            Originator of the change.
     */
    public void addOrRemoveEnclosingUGCs(Coordinate location,
            IOriginator originator);

    /**
     * @param hazardEvent
     * @return the initial hazardAreas for the given hazardEvent
     */
    public Map<String, String> buildInitialHazardAreas(IHazardEvent hazardEvent);

    /**
     * Update the hazard areas.
     * 
     * @param hazardEvent
     */
    public void updateHazardAreas(IHazardEvent hazardEvent);

    public void saveEvents(List<IHazardEvent> events);

    /**
     * Set the flag indicating whether or not newly user-created events should
     * be added to the current selection set.
     * 
     * @param New
     *            value.
     */
    public void setAddCreatedEventsToSelected(boolean addCreatedEventsToSelected);
}
