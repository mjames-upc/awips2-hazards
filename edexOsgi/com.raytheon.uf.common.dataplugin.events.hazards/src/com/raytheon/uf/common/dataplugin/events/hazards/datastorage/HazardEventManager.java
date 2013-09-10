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
package com.raytheon.uf.common.dataplugin.events.hazards.datastorage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jep.JepException;

import com.raytheon.uf.common.dataplugin.events.EventSet;
import com.raytheon.uf.common.dataplugin.events.IValidator;
import com.raytheon.uf.common.dataplugin.events.ValidationException;
import com.raytheon.uf.common.dataplugin.events.hazards.HazardConstants;
import com.raytheon.uf.common.dataplugin.events.hazards.HazardEventFactory;
import com.raytheon.uf.common.dataplugin.events.hazards.IHazardEventFactory;
import com.raytheon.uf.common.dataplugin.events.hazards.PracticeHazardEventFactory;
import com.raytheon.uf.common.dataplugin.events.hazards.event.IHazardEvent;
import com.raytheon.uf.common.dataplugin.events.hazards.event.collections.HazardHistoryList;
import com.raytheon.uf.common.dataplugin.events.hazards.requests.HazardDataStorageRequest;
import com.raytheon.uf.common.dataplugin.events.hazards.requests.HazardDataStorageRequest.RequestType;
import com.raytheon.uf.common.dataplugin.events.hazards.requests.HazardRetrieveRequest;
import com.raytheon.uf.common.dataplugin.events.hazards.requests.HazardRetrieveRequestResponse;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.python.PythonScript;
import com.raytheon.uf.common.serialization.ExceptionWrapper;
import com.raytheon.uf.common.serialization.comm.RequestRouter;
import com.raytheon.uf.common.serialization.comm.response.ServerErrorResponse;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.TimeRange;
import com.vividsolutions.jts.geom.Geometry;

/**
 * All access to the registry/database for hazards will happen through here.
 * Contains methods to get, store, update, delete, and create new hazards. This
 * class should be the only class used to access the database for hazards.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 5, 2012            mnash     Initial creation
 * 
 * </pre>
 * 
 * @author mnash
 * @version 1.0
 */

public class HazardEventManager implements IHazardEventManager {
    /** The logger */
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(HazardEventManager.class);

    private final boolean practice;

    private IHazardEventFactory factory;

    // TODO, not currently used but reserved for future use.
    private Map<String, List<String>> requiredFields = new HashMap<String, List<String>>();

    // for clarity to declare what mode we should be sending hazards out in
    public static enum Mode {
        OPERATIONAL {
            @Override
            IHazardEventFactory getFactory() {
                return new HazardEventFactory();
            }
        },
        PRACTICE {
            @Override
            IHazardEventFactory getFactory() {
                return new PracticeHazardEventFactory();
            }
        };

        abstract IHazardEventFactory getFactory();
    }

    public HazardEventManager(Mode mode) {
        practice = mode == Mode.PRACTICE ? true : false;
        factory = mode.getFactory();
        // requiredFields = getRequiredFields();
    }

    /**
     * Retrieve the required fields from the python scripts
     * 
     * @return
     */
    private Map<String, List<String>> getRequiredFields() {
        IPathManager manager = PathManagerFactory.getPathManager();
        Map<LocalizationLevel, LocalizationFile> files = manager
                .getTieredLocalizationFile(LocalizationType.CAVE_STATIC,
                        "hazards" + File.separator + "HazardRequiredFields.py");
        LocalizationFile executeFile = manager.getLocalizationFile(manager
                .getContext(LocalizationType.CAVE_STATIC,
                        LocalizationLevel.BASE), "hazards" + File.separator
                + "LoadConfig.py");
        String configPath = "";

        ArrayList<LocalizationLevel> keys = new ArrayList<LocalizationLevel>(
                files.keySet());
        Collections.sort(keys);

        Map<String, List<String>> fields = new HashMap<String, List<String>>(1);
        Map<String, Object> args = new HashMap<String, Object>(1);

        for (LocalizationLevel level : keys) {
            try {
                PythonScript py = new PythonScript(executeFile.getFile()
                        .getAbsolutePath(),
                        HazardEventManager.class.getClassLoader());
                // TODO need to tell python which one to execute, using level
                fields.putAll((Map<String, List<String>>) py.execute(
                        "loadConfig", args));
            } catch (JepException e) {
                statusHandler
                        .handle(Priority.PROBLEM,
                                "Unable to load "
                                        + level.name()
                                        + " hazard required fields, validation will not occur with them",
                                e);
            }
        }

        return fields;
    }

    /**
     * Creates an event based on the mode.
     */
    @Override
    public IHazardEvent createEvent() {
        return factory.getHazardEvent();
    }

    @Override
    public IHazardEvent createEvent(IHazardEvent event) {
        return factory.getHazardEvent(event);
    }

    /**
     * Get the events from the registry/database based on filters given. Filters
     * will be evaluated by the handler on EDEX
     */
    @Override
    public Map<String, HazardHistoryList> getEventsByFilter(
            Map<String, List<Object>> filters) {
        try {
            HazardRetrieveRequest request = new HazardRetrieveRequest();
            request.setPractice(practice);
            request.setFilters(filters);
            Object responseObject = RequestRouter.route(request);
            if (responseObject instanceof HazardRetrieveRequestResponse) {
                HazardRetrieveRequestResponse response = (HazardRetrieveRequestResponse) responseObject;
                return response.getEvents();
            } else if (responseObject instanceof ServerErrorResponse) {
                ServerErrorResponse response = (ServerErrorResponse) responseObject;
                statusHandler.handle(Priority.ERROR, response.getException()
                        .getMessage(), ExceptionWrapper
                        .unwrapThrowable(response.getException()));
            } else {
                statusHandler.handle(Priority.PROBLEM,
                        "Received an unexpected response of type "
                                + responseObject.getClass());
                return null;
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }
        // return an empty list here to prevent null pointer exceptions
        return new HashMap<String, HazardHistoryList>();
    }

    /**
     * This method takes an event and attempts to store it in the
     * registry/database. First a validation check is made to check that
     * everything is correctly set in the event.
     */
    @Override
    public boolean storeEvent(IHazardEvent... event) {
        return storeEvents(Arrays.asList(event));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.dataplugin.events.datastorage.IEventManager#
     * storeEvents(java.util.List)
     */
    @Override
    public boolean storeEvents(List<IHazardEvent> events) {
        try {
            List<IHazardEvent> evs = new ArrayList<IHazardEvent>();
            evs.addAll(events);
            for (IHazardEvent ev : events) {
                if (ev instanceof IValidator) {
                    IValidator validator = (IValidator) ev;
                    try {
                        validator.isValid();
                    } catch (ValidationException e) {
                        evs.remove(ev);
                        statusHandler.handle(
                                Priority.ERROR,
                                "Event " + ev.getSiteID() + "-"
                                        + ev.getEventID() + "-"
                                        + ev.getIssueTime()
                                        + " is not valid, not storing.", e);
                    }
                }
            }
            HazardDataStorageRequest request = new HazardDataStorageRequest();
            request.setEvents(evs.toArray(new IHazardEvent[0]));
            request.setPractice(practice);
            request.setType(RequestType.STORE);
            RequestRouter.route(request);
            return true;
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }
        return false;
    }

    /**
     * This method takes an event and attempts to update it in the
     * registry/database. First a validation check is made to check that
     * everything is correctly set in the event.
     */
    @Override
    public boolean updateEvent(IHazardEvent... event) {
        return updateEvents(Arrays.asList(event));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.dataplugin.events.datastorage.IEventManager#
     * updateEvents(T[])
     */
    @Override
    public boolean updateEvents(List<IHazardEvent> events) {
        try {
            List<IHazardEvent> evs = new ArrayList<IHazardEvent>(events);
            for (IHazardEvent ev : events) {
                if (ev instanceof IValidator) {
                    IValidator validator = (IValidator) ev;
                    try {
                        validator.isValid();
                    } catch (ValidationException e) {
                        evs.remove(ev);
                        statusHandler.handle(
                                Priority.ERROR,
                                "Event " + ev.getSiteID() + "-"
                                        + ev.getEventID() + "-"
                                        + ev.getIssueTime()
                                        + " is not valid, not updating.");
                    }
                }
            }
            HazardDataStorageRequest request = new HazardDataStorageRequest();
            request.setEvents(evs.toArray(new IHazardEvent[0]));
            request.setPractice(practice);
            request.setType(RequestType.UPDATE);
            RequestRouter.route(request);
            return true;
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.dataplugin.events.datastorage.IEventManager#
     * removeEvent(com.raytheon.uf.common.dataplugin.events.IEvent)
     */
    @Override
    public boolean removeEvent(IHazardEvent... event) {
        return removeEvents(Arrays.asList(event));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.dataplugin.events.datastorage.IEventManager#
     * removeEvents(T[])
     */
    @Override
    public boolean removeEvents(List<IHazardEvent> events) {
        try {
            HazardDataStorageRequest request = new HazardDataStorageRequest();
            request.setEvents(events.toArray(new IHazardEvent[0]));
            request.setPractice(practice);
            request.setType(RequestType.DELETE);
            RequestRouter.route(request);
            return true;
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.dataplugin.events.hazards.datastorage.
     * IHazardEventManager#getBySite(java.lang.String)
     */
    @Override
    public Map<String, HazardHistoryList> getBySiteID(String site) {
        HazardQueryBuilder builder = new HazardQueryBuilder();
        builder.addKey(HazardConstants.SITEID, site);
        return getEventsByFilter(builder.getQuery());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.dataplugin.events.hazards.datastorage.
     * IHazardEventManager#getByPhenomenon(java.lang.String)
     */
    @Override
    public Map<String, HazardHistoryList> getByPhenomenon(String phenomenon) {
        HazardQueryBuilder builder = new HazardQueryBuilder();
        builder.addKey(HazardConstants.PHENOMENON, phenomenon);
        return getEventsByFilter(builder.getQuery());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.dataplugin.events.hazards.datastorage.
     * IHazardEventManager#getBySignificance(java.lang.String)
     */
    @Override
    public Map<String, HazardHistoryList> getBySignificance(String significance) {
        HazardQueryBuilder builder = new HazardQueryBuilder();
        builder.addKey(HazardConstants.SIGNIFICANCE, significance);
        return getEventsByFilter(builder.getQuery());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.dataplugin.events.hazards.datastorage.
     * IHazardEventManager#getByPhensig(java.lang.String, java.lang.String)
     */
    @Override
    public Map<String, HazardHistoryList> getByPhensig(String phen, String sig) {
        HazardQueryBuilder builder = new HazardQueryBuilder();
        builder.addKey(HazardConstants.PHENOMENON, phen);
        builder.addKey(HazardConstants.SIGNIFICANCE, sig);
        return getEventsByFilter(builder.getQuery());
    }

    @Override
    public Map<String, HazardHistoryList> getByMultiplePhensigs(
            List<String> phensigs) {
        HazardQueryBuilder builder = new HazardQueryBuilder();
        for (String phensig : phensigs) {
            builder.addKey(HazardConstants.PHENSIG, phensig);
        }
        return getEventsByFilter(builder.getQuery());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.dataplugin.events.hazards.datastorage.
     * IHazardEventManager#getByEventId(java.lang.String)
     */
    @Override
    public HazardHistoryList getByEventID(String eventId) {
        HazardQueryBuilder builder = new HazardQueryBuilder();
        builder.addKey(HazardConstants.EVENTID, eventId);
        HazardHistoryList list = new HazardHistoryList();
        Map<String, HazardHistoryList> events = getEventsByFilter(builder
                .getQuery());
        list.addAll(events.get(eventId));
        return list;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.dataplugin.events.datastorage.IEventManager#
     * getByGeometry(com.vividsolutions.jts.geom.Geometry)
     */
    @Override
    public Map<String, HazardHistoryList> getByGeometry(Geometry geometry) {
        HazardQueryBuilder builder = new HazardQueryBuilder();
        builder.addKey(HazardConstants.GEOMETRY, geometry);
        return getEventsByFilter(builder.getQuery());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.dataplugin.events.datastorage.IEventManager#getByTime
     * (java.util.Date, java.util.Date)
     */
    @Override
    public Map<String, HazardHistoryList> getByTime(Date startTime, Date endTime) {
        HazardQueryBuilder builder = new HazardQueryBuilder();
        builder.addKey(HazardConstants.STARTTIME, startTime);
        builder.addKey(HazardConstants.ENDTIME, endTime);
        return getEventsByFilter(builder.getQuery());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.dataplugin.events.hazards.datastorage.
     * IHazardEventManager#getByTimeRange(com.raytheon.uf.common.time.TimeRange)
     */
    @Override
    public Map<String, HazardHistoryList> getByTimeRange(TimeRange range) {
        return getByTime(range.getStart(), range.getEnd());
    };

    /**
     * Takes a EventSet<IHazardEvent>, which can be unrelated hazards, and
     * stores them individually. Just a convenience method.
     */
    @Override
    public void storeEventSet(EventSet<IHazardEvent> set) {
        Iterator<IHazardEvent> eventIter = set.iterator();
        while (eventIter.hasNext()) {
            IHazardEvent event = eventIter.next();
            storeEvent(event);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.dataplugin.events.datastorage.IEventManager#getAll
     * ()
     */
    @Override
    public Map<String, HazardHistoryList> getAll() {
        HazardQueryBuilder builder = new HazardQueryBuilder();
        return getEventsByFilter(builder.getQuery());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.dataplugin.events.hazards.datastorage.
     * IHazardEventManager#removeAllEvents()
     */
    @Override
    public boolean removeAllEvents() {
        // allow for a pass through, we should be throwing an exception here as
        // removing all events is not a good idea
        Map<String, HazardHistoryList> list = getAll();
        List<IHazardEvent> events = new ArrayList<IHazardEvent>();
        for (Entry<String, HazardHistoryList> entry : list.entrySet()) {
            events.addAll(entry.getValue().getEvents());
        }
        return removeEvents(events);
        // throw new UnsupportedOperationException(
        // "Cannot remove all events from the "
        // + (practice == true ? "practice" : "operational")
        // + " storage area");
    }
}