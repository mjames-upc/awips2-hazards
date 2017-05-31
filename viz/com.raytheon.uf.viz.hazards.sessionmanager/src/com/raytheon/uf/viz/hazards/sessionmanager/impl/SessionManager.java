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
package com.raytheon.uf.viz.hazards.sessionmanager.impl;

import gov.noaa.gsd.common.eventbus.BoundedReceptionEventBus;
import gov.noaa.gsd.common.utilities.IRunnableAsynchronousScheduler;

import java.util.Collection;
import java.util.List;

import com.raytheon.uf.common.activetable.request.ClearPracticeVTECTableRequest;
import com.raytheon.uf.common.dataplugin.events.hazards.HazardNotification;
import com.raytheon.uf.common.dataplugin.events.hazards.datastorage.IHazardEventManager;
import com.raytheon.uf.common.dataplugin.events.hazards.event.HazardServicesEventIdUtil;
import com.raytheon.uf.common.dataplugin.events.hazards.registry.HazardEventServiceException;
import com.raytheon.uf.common.dataplugin.hazards.interoperability.registry.services.client.InteroperabilityRequestServices;
import com.raytheon.uf.common.hazards.productgen.data.HazardSiteDataRequest;
import com.raytheon.uf.common.hazards.productgen.data.ProductDataUtil;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationOpFailedException;
import com.raytheon.uf.common.serialization.comm.IServerRequest;
import com.raytheon.uf.common.serialization.comm.RequestRouter;
import com.raytheon.uf.common.site.SiteMap;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.localization.LocalizationManager;
import com.raytheon.uf.viz.core.requests.ThriftClient;
import com.raytheon.uf.viz.hazards.sessionmanager.IDisplayResourceContextProvider;
import com.raytheon.uf.viz.hazards.sessionmanager.IFrameContextProvider;
import com.raytheon.uf.viz.hazards.sessionmanager.ISessionManager;
import com.raytheon.uf.viz.hazards.sessionmanager.ISpatialContextProvider;
import com.raytheon.uf.viz.hazards.sessionmanager.alerts.IHazardSessionAlertsManager;
import com.raytheon.uf.viz.hazards.sessionmanager.alerts.impl.AllHazardsFilterStrategy;
import com.raytheon.uf.viz.hazards.sessionmanager.alerts.impl.HazardEventExpirationAlertStrategy;
import com.raytheon.uf.viz.hazards.sessionmanager.alerts.impl.HazardSessionAlertsManager;
import com.raytheon.uf.viz.hazards.sessionmanager.config.ISessionConfigurationManager;
import com.raytheon.uf.viz.hazards.sessionmanager.config.impl.ObservedSettings;
import com.raytheon.uf.viz.hazards.sessionmanager.config.impl.SessionConfigurationManager;
import com.raytheon.uf.viz.hazards.sessionmanager.config.types.ToolType;
import com.raytheon.uf.viz.hazards.sessionmanager.events.ISessionEventManager;
import com.raytheon.uf.viz.hazards.sessionmanager.events.ISessionSelectionManager;
import com.raytheon.uf.viz.hazards.sessionmanager.events.SessionAutoCheckConflictsModified;
import com.raytheon.uf.viz.hazards.sessionmanager.events.SessionHatchingToggled;
import com.raytheon.uf.viz.hazards.sessionmanager.events.SessionModified;
import com.raytheon.uf.viz.hazards.sessionmanager.events.impl.ObservedHazardEvent;
import com.raytheon.uf.viz.hazards.sessionmanager.events.impl.SessionEventManager;
import com.raytheon.uf.viz.hazards.sessionmanager.events.impl.SessionSelectionManager;
import com.raytheon.uf.viz.hazards.sessionmanager.messenger.IMessenger;
import com.raytheon.uf.viz.hazards.sessionmanager.originator.Originator;
import com.raytheon.uf.viz.hazards.sessionmanager.product.ISessionProductManager;
import com.raytheon.uf.viz.hazards.sessionmanager.product.impl.SessionProductManager;
import com.raytheon.uf.viz.hazards.sessionmanager.recommenders.ISessionRecommenderManager;
import com.raytheon.uf.viz.hazards.sessionmanager.recommenders.RecommenderExecutionContext;
import com.raytheon.uf.viz.hazards.sessionmanager.recommenders.impl.SessionRecommenderManager;
import com.raytheon.uf.viz.hazards.sessionmanager.time.ISessionTimeManager;
import com.raytheon.uf.viz.hazards.sessionmanager.time.impl.SessionTimeManager;
import com.raytheon.viz.core.mode.CAVEMode;

/**
 * Implementation of ISessionManager.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 20, 2013 1257       bsteffen    Initial creation
 * Aug 01, 2013  1325      daniel.s.schaffer@noaa.gov     Added support for alerting
 * Nov 19, 2013 1463       blawrenc    Added state of automatic hazard conflict
 *                                     testing.
 * 
 * Nov 20, 2013   2159     daniel.s.schaffer@noaa.gov Now alerts interoperable with DRT
 * 
 * Nov 23, 2013 1462       blawrenc    Added state of hatch area drawing.
 * Apr 08, 2014 2826       dgilling    Fixed filesToDeleteOnReset to reflect proper paths.
 * May 12, 2014 2925       C. Golden   Added supplying of time manager to configuration
 *                                     manager, and the firing of auto-check-conflicts-
 *                                     changed messages when the toggle changes state.
 * Oct 08, 2014 4042       C. Golden   Added generate method (moved from message handler).
 * Dec 05, 2014 2124       C. Golden   Changed to work with parameterized config manager.
 * Dec 08, 2014 2826       dgilling    Clear interoperability tables on reset events.
 * Jan 22, 2015 4959       Dan Schaffer Ability to right click to add/remove polygons from hazards
 * Feb 12, 2015 4959       Dan Schaffer Modify MB3 add/remove UGCs to match Warngen
 * Jul 06, 2015 6930       Chris.Cody   Send notification for handleRecommenderResult
 * Jul 31, 2015 7458       Robert.Blum  Setting userName and workstation fields on events that are
 *                                      created by a recommender.
 * Aug 03, 2015 8836       Chris.Cody   Changes for a configurable Event Id
 * Aug 04, 2015 6895       Ben.Phillippe Finished HS data access refactor
 * Aug 13, 2015 8836       Chris.Cody   Additional Changes for Hazard Event Id and Registry changes
 * Aug 18, 2015 9650       Chris.Golden Added checking for "deleteEventIdentifiers" attribute in
 *                                      recommender result event sets and deletion of events
 *                                      identified therein if possible.
 * Aug 20, 2015  6895      Ben.Phillippe Routing registry requests through request server
 * Nov 10, 2015 12762      Chris.Golden Added code to implement and use new recommender manager.
 * Nov 17, 2015  3473      Chris.Golden Moved all python files under HazardServices localization dir.
 * Nov 23, 2015  3473      Robert.Blum  Removed importApplicationBackupSiteData.
 * Mar 03, 2016 14004      Chris.Golden Changed to pass recommender identifier to the method
 *                                      handling recommender results, and removed bogus creation of
 *                                      notification within that method, as well as passing the
 *                                      recommender identifier as an originator to the call to add
 *                                      the event(s) created or modified by the recommender.
 * Mar 04, 2016 15933      Chris.Golden Added ability to run multiple recommenders in sequence in
 *                                      response to a time interval trigger, instead of just one
 *                                      recommender.
 * Apr 27, 2016 18266      Chris.Golden Added the setting of the selected time to that specified
 *                                      by the result of a recommender's execution, if the result
 *                                      includes a new selected time.
 * Jun 23, 2016 19537      Chris.Golden Added use of spatial context provider.
 * Jul 26, 2016 20755      Chris.Golden Added ability to save recommender-created/modified hazard
 *                                      events to the database if the recommender requests it.
 * Jul 27, 2016 19924      Chris.Golden Added use of display resource context provider.
 * Aug 15, 2016 18376      Chris.Golden Added code to unsubscribe session sub-managers from the
 *                                      event bus to aide in garbage collection.
 * Feb 01, 2017 15556      Chris.Golden Changed construction of time manager to take this object.
 *                                      Also added use of new session selection manager.
 * Feb 16, 2017 29138      Chris.Golden Changed to allow recommenders to specify that they want
 *                                      resulting events to be saved to either the "latest
 *                                      version" set in the database, or the history list in the
 *                                      database.
 * Feb 21, 2017 29138      Chris.Golden Added method to get runnable asynchronous scheduler. Also
 *                                      fixed null exception that occurred if a recommender passed
 *                                      back a null value as one of the elements in the list of
 *                                      event identifiers to be saved to the database or to the
 *                                      history list.
 * Mar 08, 2017 29138      Chris.Golden Added startup config option to allow persistence behavior
 *                                      to be tweaked via configuration. Also added code to ensure
 *                                      that any specification of null for a hazard event identifier
 *                                      in a recommender result's "saveToDatabase" or "saveToHistory"
 *                                      is treated as meaning "save to database/history all events
 *                                      with no assigned identifiers", meaning any brand new events
 *                                      created by the recommender.
 * Mar 16, 2017 29138      Chris.Golden Removed code related to startup config option to allow
 *                                      persistence behavior to be tweaked via configuration from
 *                                      this class; use of this temporary option has been moved
 *                                      into the session event manager.
 * Mar 30, 2017 15528      Chris.Golden Changed to reset modified flag when asked to do so by the
 *                                      recommender when handling the result of said recommender.
 * Apr 04, 2017 32732      Chris.Golden Added constant for indicating whether or not the origin
 *                                      (user name and workstation identifier) should be updated
 *                                      when a recommender returns modified event(s).
 * Apr 13, 2017 33142      Chris.Golden Added tracking of events that have been removed since last
 *                                      recommender execution commencement, so that when events
 *                                      are returned by recommenders, any that have been removed
 *                                      while the recommender was executing can be ignored.
 * May 31, 2017 34684      Chris.Golden Moved recommender-specific methods to the session
 *                                      recommender manager where they belong.
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
public class SessionManager implements
        ISessionManager<ObservedHazardEvent, ObservedSettings> {

    /**
     * Scheduler to be used to ensure that result notifications are published on
     * the main thread. For now, the main thread is the UI thread; when this is
     * changed, this will be rendered obsolete, as at that point there will need
     * to be a blocking queue of {@link Runnable} instances available to allow
     * the new worker thread to be fed jobs. At that point, this should be
     * replaced with an object that enqueues the <code>Runnable</code>s,
     * probably a singleton that may be accessed by the various components in
     * gov.noaa.gsd.viz.hazards and elsewhere (presumably passed to the session
     * manager when the latter is created).
     */
    @Deprecated
    private static final IRunnableAsynchronousScheduler RUNNABLE_ASYNC_SCHEDULER = new IRunnableAsynchronousScheduler() {

        @Override
        public void schedule(Runnable runnable) {

            /*
             * Since the UI thread is currently the thread being used for nearly
             * everything, just run any asynchronous tasks there.
             */
            VizApp.runAsync(runnable);
        }
    };

    /**
     * Files in localization to be removed when the events are reset from the
     * Console. These are VTEC-related. If VTEC information is allowed to
     * persist after events are deleted, VTEC processing could be compromised
     * for future events. These are assumed to be CAVE_STATIC files.
     * 
     * TODO This need to be eliminated when we go to a database solution.
     */
    private static final String[] filesToDeleteOnReset = {
            "HazardServices/testVtecRecords.json",
            "HazardServices/testVtecRecords.lock",
            "HazardServices/vtecRecords.json",
            "HazardServices/vtecRecords.lock" };

    /**
     * Logging mechanism.
     */
    private final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(this.getClass());

    private final BoundedReceptionEventBus<Object> eventBus;

    private final SessionNotificationSender sender;

    private final ISessionEventManager<ObservedHazardEvent> eventManager;

    private final ISessionSelectionManager<ObservedHazardEvent> selectionManager;

    private final ISessionTimeManager timeManager;

    private final ISessionConfigurationManager<ObservedSettings> configManager;

    private final ISessionProductManager productManager;

    private final ISessionRecommenderManager recommenderManager;

    private final IHazardSessionAlertsManager alertsManager;

    private final IHazardEventManager hazardManager;

    private final ISpatialContextProvider spatialContextProvider;

    private final IDisplayResourceContextProvider displayResourceContextProvider;

    private final IFrameContextProvider frameContextProvider;

    /*
     * Flag indicating whether or not automatic hazard checking is running.
     */
    private boolean autoHazardChecking = false;

    /*
     * Flag indicating whether or not hazard hatch areas are displayed.
     */
    private boolean hatchAreaDisplay = true;

    private volatile boolean previewOngoing = false;

    private volatile boolean issueOngoing = false;

    public SessionManager(IPathManager pathManager,
            IHazardEventManager hazardEventManager,
            ISpatialContextProvider spatialContextProvider,
            IDisplayResourceContextProvider displayResourceContextProvider,
            IFrameContextProvider frameContextProvider, IMessenger messenger,
            BoundedReceptionEventBus<Object> eventBus) {
        this.eventBus = eventBus;
        sender = new SessionNotificationSender(eventBus);
        timeManager = new SessionTimeManager(this, sender);
        configManager = new SessionConfigurationManager(this, pathManager,
                timeManager, sender);
        SessionEventManager eventManager = new SessionEventManager(this,
                timeManager, configManager, hazardEventManager, sender,
                messenger);
        this.eventManager = eventManager;
        selectionManager = new SessionSelectionManager(eventManager, sender);
        productManager = new SessionProductManager(this, timeManager,
                configManager, eventManager, selectionManager, sender,
                messenger);
        recommenderManager = new SessionRecommenderManager(this, messenger,
                eventBus);
        alertsManager = new HazardSessionAlertsManager(sender,
                getRunnableAsynchronousScheduler(), timeManager);
        alertsManager.addAlertGenerationStrategy(HazardNotification.class,
                new HazardEventExpirationAlertStrategy(alertsManager,
                        timeManager, configManager, hazardEventManager,
                        new AllHazardsFilterStrategy()));
        hazardManager = hazardEventManager;
        this.spatialContextProvider = spatialContextProvider;
        this.displayResourceContextProvider = displayResourceContextProvider;
        this.frameContextProvider = frameContextProvider;

        try {
            setupEventIdDisplay();
        } catch (HazardEventServiceException e) {
            statusHandler.error(e.getMessage(), e);
        }

        /**
         * TODO Where should a call be made to remove the NotificationJob
         * observer (done in the stop method)?
         */
        alertsManager.start();
        registerForNotification(timeManager);
        registerForNotification(configManager);
        registerForNotification(eventManager);
        registerForNotification(productManager);
        registerForNotification(recommenderManager);
        registerForNotification(alertsManager);

    }

    @Override
    public IRunnableAsynchronousScheduler getRunnableAsynchronousScheduler() {
        return RUNNABLE_ASYNC_SCHEDULER;
    }

    @Override
    public ISessionEventManager<ObservedHazardEvent> getEventManager() {
        return eventManager;
    }

    @Override
    public ISessionSelectionManager<ObservedHazardEvent> getSelectionManager() {
        return selectionManager;
    }

    @Override
    public ISessionTimeManager getTimeManager() {
        return timeManager;
    }

    @Override
    public ISessionConfigurationManager<ObservedSettings> getConfigurationManager() {
        return configManager;
    }

    @Override
    public ISessionProductManager getProductManager() {
        return productManager;
    }

    @Override
    public ISessionRecommenderManager getRecommenderManager() {
        return recommenderManager;
    }

    @Override
    public IHazardSessionAlertsManager getAlertsManager() {
        return alertsManager;
    }

    @Override
    public ISpatialContextProvider getSpatialContextProvider() {
        return spatialContextProvider;
    }

    @Override
    public IDisplayResourceContextProvider getDisplayResourceContextProvider() {
        return displayResourceContextProvider;
    }

    @Override
    public IFrameContextProvider getFrameContextProvider() {
        return frameContextProvider;
    }

    @Override
    public void registerForNotification(Object object) {
        eventBus.subscribe(object);
    }

    @Override
    public void unregisterForNotification(Object object) {
        eventBus.unsubscribe(object);
    }

    @Override
    public void shutdown() {

        unregisterForNotification(timeManager);
        unregisterForNotification(configManager);
        unregisterForNotification(eventManager);
        unregisterForNotification(productManager);
        unregisterForNotification(recommenderManager);
        unregisterForNotification(alertsManager);

        eventManager.shutdown();

        timeManager.shutdown();

        configManager.shutdown();

        productManager.shutdown();

        recommenderManager.shutdown();

        alertsManager.shutdown();
    }

    @Override
    public void toggleAutoHazardChecking() {
        autoHazardChecking = !autoHazardChecking;
        sender.postNotificationAsync(new SessionAutoCheckConflictsModified(
                Originator.OTHER));
    }

    @Override
    public boolean isAutoHazardCheckingOn() {
        return autoHazardChecking;
    }

    @Override
    public void toggleHatchedAreaDisplay() {
        hatchAreaDisplay = !hatchAreaDisplay;
        eventBus.publish(new SessionHatchingToggled(Originator.OTHER));
    }

    @Override
    public boolean areHatchedAreasDisplayed() {
        return hatchAreaDisplay;
    }

    @Override
    public void reset() {

        for (ObservedHazardEvent event : eventManager.getEvents()) {
            eventManager.removeEvent(event, Originator.OTHER);
        }

        hazardManager.removeAllEvents();

        try {
            IServerRequest clearTableReq = new ClearPracticeVTECTableRequest(
                    SiteMap.getInstance().getSite4LetterId(
                            configManager.getSiteID()), VizApp.getWsId());
            ThriftClient.sendRequest(clearTableReq);
        } catch (VizException e) {
            statusHandler
                    .error("Error clearing practice VTEC active table.", e);
        }

        try {
            InteroperabilityRequestServices.getServices(true)
                    .purgePracticeWarnings();
        } catch (Exception ex) {
            /*
             * 8836 Chris.Cody THIS IS A STOP GAP MEASURE (Interoperability).
             * There is a purge issue (which may become moot), but at present
             * the purge operations need to continue as far as possible.
             */
            String msg = "Error caught from InteroperabilityRequestServices.purgePracticeWarnings() "
                    + "\nContinuing processing to finish purge tasks."
                    + "\nAn Exception will not be thrown at this time";
            statusHandler.error(msg, ex);
        }

        try {
            InteroperabilityRequestServices.getServices(true)
                    .purgeInteropRecords();
        } catch (Exception ex) {
            /*
             * 8836 Chris.Cody THIS IS A STOP GAP MEASURE (Interoperability).
             * There is a purge issue (which may become moot), but at present
             * the purge operations need to continue as far as possible.
             */
            String msg = "Error caught from InteroperabilityRequestServices.purgeInteropRecords() "
                    + "\nContinuing processing to finish purge tasks."
                    + "\nAn Exception will not be thrown at this time";
            statusHandler.error(msg, ex);
        }

        /*
         * Reset the VTEC information in the VTEC files. This needs to be done.
         * Otherwise, it is difficult to test against the job sheets and
         * functional tests when the forecaster selects Reset->Events but old
         * VTEC information remains in the VTEC files. This solution will change
         * once VTEC is stored in the database.
         */
        IPathManager pathManager = PathManagerFactory.getPathManager();
        LocalizationContext localizationContext = pathManager.getContext(
                LocalizationContext.LocalizationType.CAVE_STATIC,
                LocalizationContext.LocalizationLevel.USER);

        for (String fileToDelete : filesToDeleteOnReset) {
            LocalizationFile localizationFile = pathManager
                    .getLocalizationFile(localizationContext, fileToDelete);

            if (localizationFile.exists()) {
                try {
                    localizationFile.delete();
                } catch (LocalizationOpFailedException e) {
                    statusHandler.error("Error while reseting.", e);
                }
            }
        }

        String mode = CAVEMode.getMode().toString();
        ProductDataUtil.deleteProductData(mode, null, null);
    }

    @Override
    public void undo() {
        Collection<ObservedHazardEvent> events = selectionManager
                .getSelectedEvents();
        if (events.size() == 1) {
            events.iterator().next().undo();
        }
    }

    @Override
    public void redo() {
        Collection<ObservedHazardEvent> events = selectionManager
                .getSelectedEvents();
        if (events.size() == 1) {
            events.iterator().next().redo();
        }
    }

    @Override
    public Boolean isUndoable() {
        Collection<ObservedHazardEvent> hazardEvents = selectionManager
                .getSelectedEvents();

        /*
         * Limited to single selected hazard events.
         */
        if (hazardEvents.size() == 1) {
            return hazardEvents.iterator().next().isUndoable();
        }

        return false;
    }

    @Override
    public Boolean isRedoable() {

        Collection<ObservedHazardEvent> hazardEvents = selectionManager
                .getSelectedEvents();

        /*
         * Limit to single selection.
         */
        if (hazardEvents.size() == 1) {
            return hazardEvents.iterator().next().isRedoable();
        }

        return false;
    }

    @Override
    public void clearUndoRedo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPreviewOngoing() {
        return previewOngoing;
    }

    @Override
    public void setPreviewOngoing(boolean previewOngoing) {
        this.previewOngoing = previewOngoing;
        notifySessionModified();
    }

    @Override
    public boolean isIssueOngoing() {
        return issueOngoing;
    }

    @Override
    public void setIssueOngoing(boolean issueOngoing) {
        this.issueOngoing = issueOngoing;
        notifySessionModified();

    }

    private void notifySessionModified() {
        eventBus.publish(new SessionModified(Originator.OTHER));
    }

    /**
     * @return the eventBus
     */
    public BoundedReceptionEventBus<Object> getEventBus() {
        return eventBus;
    }

    @Override
    public void runTools(ToolType type, List<String> identifiers,
            RecommenderExecutionContext context) {
        switch (type) {
        case RECOMMENDER:
            recommenderManager.runRecommenders(identifiers, context);
            break;
        case HAZARD_PRODUCT_GENERATOR:
            if (identifiers.size() > 1) {
                throw new UnsupportedOperationException(
                        "cannot run multiple product generators in sequence");
            }
            productManager.generateProducts(identifiers.get(0));
            break;

        case NON_HAZARD_PRODUCT_GENERATOR:
            if (identifiers.size() > 1) {
                throw new UnsupportedOperationException(
                        "cannot run multiple non-hazard product generators in sequence");
            }
            productManager.generateNonHazardProducts(identifiers.get(0));
            break;
        }
    }

    @Override
    public void generate(boolean issue) {
        if (issue) {
            setIssueOngoing(true);
        } else {
            setPreviewOngoing(true);
        }
        try {
            productManager.generateProducts(issue);
        } catch (Exception e) {
            setPreviewOngoing(false);
            setIssueOngoing(false);
            statusHandler.error("Error during product generation", e);
        }
    }

    private void setupEventIdDisplay() throws HazardEventServiceException {

        boolean isPracticeMode = (CAVEMode.getMode() == CAVEMode.PRACTICE);

        LocalizationManager lm = LocalizationManager.getInstance();
        String siteId = null;
        if (lm != null) {
            siteId = lm.getCurrentSite();
        }

        HazardServicesEventIdUtil.setupHazardEventId(isPracticeMode, siteId);
    }

    /**
     * Export Hazard Services Site Configuration Files
     * 
     * <pre>
     * Make Hazard Services Site Localization file backup call to Request Server. 
     * Request Server will execute hs_export_configuration script to tar and gzip
     * Local SITE Localization data files. 
     * Script on Request Server will then call: msg_send to move 
     * Site data to Central Registry Server X.400 directory
     * </pre>
     * 
     * @param siteId
     *            Hazard Services Site to archive (Local Site)
     */
    @Override
    public void exportApplicationSiteData(String siteId) {

        boolean isPractice = (CAVEMode.getMode() == CAVEMode.PRACTICE);

        try {
            HazardSiteDataRequest hazardSiteDataReq = new HazardSiteDataRequest(
                    siteId, isPractice);

            RequestRouter.route(hazardSiteDataReq);
        } catch (Exception e) {
            statusHandler.error(
                    "Error Exporting Hazard Services Site Data files.", e);
        }
    }
}
