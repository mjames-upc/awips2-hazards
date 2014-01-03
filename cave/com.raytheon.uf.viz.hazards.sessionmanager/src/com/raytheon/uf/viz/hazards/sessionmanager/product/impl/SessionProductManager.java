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
package com.raytheon.uf.viz.hazards.sessionmanager.product.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.google.common.collect.Lists;
import com.raytheon.uf.common.dataplugin.events.EventSet;
import com.raytheon.uf.common.dataplugin.events.IEvent;
import com.raytheon.uf.common.dataplugin.events.hazards.HazardConstants;
import com.raytheon.uf.common.dataplugin.events.hazards.HazardConstants.HazardState;
import com.raytheon.uf.common.dataplugin.events.hazards.datastorage.HazardEventManager;
import com.raytheon.uf.common.dataplugin.events.hazards.event.BaseHazardEvent;
import com.raytheon.uf.common.dataplugin.events.hazards.event.HazardEventUtilities;
import com.raytheon.uf.common.dataplugin.events.hazards.event.IHazardEvent;
import com.raytheon.uf.common.dataplugin.events.hazards.requests.HasConflictsRequest;
import com.raytheon.uf.common.hazards.productgen.IGeneratedProduct;
import com.raytheon.uf.common.hazards.productgen.ProductGeneration;
import com.raytheon.uf.common.hazards.productgen.ProductUtils;
import com.raytheon.uf.common.python.concurrent.IPythonJobListener;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.SimulatedTime;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.localization.LocalizationManager;
import com.raytheon.uf.viz.core.requests.ThriftClient;
import com.raytheon.uf.viz.hazards.sessionmanager.config.ISessionConfigurationManager;
import com.raytheon.uf.viz.hazards.sessionmanager.config.impl.types.HazardTypeEntry;
import com.raytheon.uf.viz.hazards.sessionmanager.config.impl.types.HazardTypes;
import com.raytheon.uf.viz.hazards.sessionmanager.config.impl.types.ProductGeneratorEntry;
import com.raytheon.uf.viz.hazards.sessionmanager.config.impl.types.ProductGeneratorTable;
import com.raytheon.uf.viz.hazards.sessionmanager.events.ISessionEventManager;
import com.raytheon.uf.viz.hazards.sessionmanager.events.impl.ObservedHazardEvent;
import com.raytheon.uf.viz.hazards.sessionmanager.events.impl.SessionEventManager;
import com.raytheon.uf.viz.hazards.sessionmanager.events.impl.SessionEventUtilities;
import com.raytheon.uf.viz.hazards.sessionmanager.impl.ISessionNotificationSender;
import com.raytheon.uf.viz.hazards.sessionmanager.messenger.IMessenger;
import com.raytheon.uf.viz.hazards.sessionmanager.product.ISessionProductManager;
import com.raytheon.uf.viz.hazards.sessionmanager.product.ProductFailed;
import com.raytheon.uf.viz.hazards.sessionmanager.product.ProductGenerated;
import com.raytheon.uf.viz.hazards.sessionmanager.product.ProductInformation;
import com.raytheon.uf.viz.hazards.sessionmanager.time.ISessionTimeManager;
import com.raytheon.uf.viz.hazards.sessionmanager.undoable.IUndoRedoable;
import com.raytheon.viz.core.mode.CAVEMode;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * Implementation of ISessionProductManager
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 20, 2013 1257       bsteffen    Initial creation
 * Aug 10, 2013 1265       blawrenc    Added logic to clear 
 *                                     undo/redo information
 *                                     when an event is issued. Also
 *                                     replaced key strings with constants
 *                                     from HazardConstants.py.
 * Aug 12, 2013 1360       hansen      Added logic to handle expiration time
 *                                     other product information from product
 *                                     generators
 * Aug 16, 2013 1325       daniel.s.schaffer@noaa.gov    Alerts integration
 * Aug 20, 2013 1360       blawrenc    Fixed problem with incorrect states showing in console.
 * Aug 26, 2013 1921       blawrenc    Added call to VizApp.runAsync to jobFinished and
 *                                     jobFailed methods. This seems to remedy the 
 *                                     currentModification exception that we were occasionally
 *                                     seeing.
 * Aug 29, 2013 1921       blawrenc    Added logic to issue that the "replaces" information is
 *                                     removed from an event upon issuance.
 * Sep 12, 2013 717        jsanchez    Disseminated the legacy text product.
 * Sep 19, 2013 2046       mnash       Update for product generation.
 * 
 * Sept 16, 2013 1298      thansen     Added popup dialog trying to preview or issue non-supported 
 *                                     hazards
 * Oct 23, 2013 2277       jsanchez    Use thrift request to check for grid conflicts.
 * Nov 04, 2013 2182     daniel.s.schaffer@noaa.gov      Started refactoring
 * Nov 15, 2013  2182       daniel.s.schaffer@noaa.gov    Refactoring JSON - ProductStagingDialog
 * Nov 21, 2013  2446       daniel.s.schaffer@noaa.gov Bug fixes in product staging dialog
 * Nov 29, 2013  2378       blarenc    Simplified state changes when products are issued.
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */

public class SessionProductManager implements ISessionProductManager {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SessionProductManager.class);

    private final ISessionTimeManager timeManager;

    /*
     * A full configuration manager is needed to get access to the product
     * generation table, which is not exposed in ISessionConfigurationManager
     */
    private final ISessionConfigurationManager configManager;

    private final ISessionEventManager eventManager;

    private final ISessionNotificationSender notificationSender;

    private final ProductGeneration productGen;

    /*
     * The messenger for displaying questions and warnings to the user and
     * retrieving answers. This allows the viz side (App Builder) to be
     * responsible for these dialogs, but gives the event manager and other
     * managers access to them without creating a dependency on the
     * gov.noaa.gsd.viz.hazards plugin. Since all parts of Hazard Services can
     * use the same code for creating these dialogs, it makes it easier for them
     * to be stubbed for testing.
     */
    private final IMessenger messenger;

    public SessionProductManager(ISessionTimeManager timeManager,
            ISessionConfigurationManager configManager,
            ISessionEventManager eventManager,
            ISessionNotificationSender notificationSender, IMessenger messenger) {
        this.timeManager = timeManager;
        this.configManager = configManager;
        this.eventManager = eventManager;
        this.notificationSender = notificationSender;
        this.productGen = new ProductGeneration();
        this.messenger = messenger;
    }

    @Override
    public Collection<ProductInformation> getSelectedProducts() {
        List<ProductInformation> result = new ArrayList<ProductInformation>();
        ProductGeneratorTable pgt = configManager.getProductGeneratorTable();
        List<String> supportedHazards = new ArrayList<String>();
        Set<String> unsupportedHazards = new HashSet<String>();
        for (Entry<String, ProductGeneratorEntry> entry : pgt.entrySet()) {
            if (entry.getValue().isReservedNameNotYetImplemented()) {
                continue;
            }
            Set<IHazardEvent> productEvents = new HashSet<IHazardEvent>();
            Set<IHazardEvent> possibleProductEvents = new HashSet<IHazardEvent>();

            for (IHazardEvent e : eventManager.getEvents()) {
                if (e.getPhenomenon() == null || e.getSignificance() == null) {
                    continue;
                }
                String key = HazardEventUtilities.getHazardType(e);
                for (String[] pair : entry.getValue().getAllowedHazards()) {
                    if (pair[0].equals(key)) {
                        supportedHazards.add(key);
                        if (e.getHazardAttribute(
                                ISessionEventManager.ATTR_SELECTED)
                                .equals(true)) {
                            productEvents.add(e);
                        } else if (e.getState() != HazardState.POTENTIAL
                                && e.getState() != HazardState.ENDED
                                && isCombinable(e)) {
                            possibleProductEvents.add(e);
                        }
                    }
                }
            }
            if (!productEvents.isEmpty()) {
                ProductInformation info = new ProductInformation();
                info.setProductName(entry.getKey());
                info.setProductEvents(productEvents);
                info.setPossibleProductEvents(possibleProductEvents);
                // TODO actually get dialog info. Currently getting the dialog
                // info breaks the Replace Watch with Warning Story.
                // info.setDialogInfo(productGen.getDialogInfo(entry.getKey()));
                info.setDialogInfo(Collections.<String, String> emptyMap());
                info.setFormats(new String[] { "XML", "Legacy", "CAP" });
                result.add(info);
            }
        }
        /*
         * Put up dialog to warn user that products are not supported for some
         * requested hazard types
         */
        for (IHazardEvent e : eventManager.getEvents()) {
            String key = HazardEventUtilities.getHazardType(e);
            boolean found = false;
            for (String supported : supportedHazards) {
                if (supported.equals(key)) {
                    found = true;
                    break;
                }
            }
            if (!found
                    && e.getHazardAttribute(ISessionEventManager.ATTR_SELECTED)
                            .equals(true)) {
                unsupportedHazards.add(key);
            }
        }
        if (!unsupportedHazards.isEmpty()) {
            String message = "Products for the following hazard types are not yet supported: ";
            for (String type : unsupportedHazards) {
                message += type + " ";
            }
            String[] buttons;
            if (!result.isEmpty()) {
                message += "\nPress Continue to generate products for the supported hazard types.";
                buttons = new String[] { HazardConstants.CANCEL_BUTTON,
                        HazardConstants.CONTINUE_BUTTON };
            } else {
                buttons = new String[] { "OK" };
            }
            Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                    .getShell();
            MessageDialog dialog = new MessageDialog(shell,
                    "Unsupported HazardTypes", null, message,
                    MessageDialog.ERROR, buttons, 0);
            int response = dialog.open();
            if (response == 0) {
                result = new ArrayList<ProductInformation>();
            }
        }
        // TODO remove the reverse. Currently removing the reverse breaks
        // the Replace Watch with Warning Story.
        Collections.reverse(result);
        return result;
    }

    private boolean isCombinable(IHazardEvent e) {
        String type = HazardEventUtilities.getHazardType(e);
        HazardTypes hazardTypes = configManager.getHazardTypes();
        HazardTypeEntry hazardTypeEntry = hazardTypes.get(type);
        boolean result = hazardTypeEntry.isCombinableSegments();
        return result;
    }

    @Override
    public void generate(ProductInformation information, boolean issue,
            boolean confirm) {

        if (validateSelectedHazardsForProductGeneration()
                && eventManager.clipSelectedHazardGeometries()) {

            eventManager.reduceSelectedHazardGeometries();

            if (issue && confirm) {
                boolean answer = messenger
                        .getQuestionAnswerer()
                        .getUserAnswerToQuestion(
                                "Are you sure "
                                        + "you want to issue the hazard event(s)?");
                if (!answer) {
                    return;
                }
            }
            EventSet<IEvent> events = new EventSet<IEvent>();
            events.addAttribute(HazardConstants.CURRENT_TIME, timeManager
                    .getCurrentTime().getTime());
            events.addAttribute(HazardConstants.SITEID,
                    configManager.getSiteID());
            events.addAttribute(HazardConstants.BACKUP_SITEID,
                    LocalizationManager.getInstance().getCurrentSite());
            String mode = CAVEMode.getMode() == CAVEMode.PRACTICE ? HazardEventManager.Mode.PRACTICE
                    .toString() : HazardEventManager.Mode.OPERATIONAL
                    .toString();
            events.addAttribute("hazardMode", mode);
            String runMode = CAVEMode.getMode().toString();
            events.addAttribute("runMode", runMode);
            events.addAttribute("vtecMode", "O");

            if (issue) {
                events.addAttribute(HazardConstants.ISSUE_FLAG, "True");
            } else {
                events.addAttribute(HazardConstants.ISSUE_FLAG, "False");
            }

            HashMap<String, String> sessionDict = new HashMap<String, String>();
            // TODO
            // There is no operational database currently.
            // When this is fixed, then the correct CAVEMode needs to
            // be entered into the sessionDict.
            // sessionDict.put(HazardConstants.TEST_MODE, CAVEMode.getMode()
            // .toString());
            sessionDict.put(HazardConstants.TEST_MODE, "PRACTICE");
            events.addAttribute(HazardConstants.SESSION_DICT, sessionDict);

            if (information.getDialogSelections() != null) {
                for (Entry<String, String> entry : information
                        .getDialogSelections().entrySet()) {
                    events.addAttribute(entry.getKey(), entry.getValue());
                }
            }
            for (IHazardEvent event : information.getProductEvents()) {
                event = new BaseHazardEvent(event);
                for (Entry<String, Serializable> entry : event
                        .getHazardAttributes().entrySet()) {
                    if (entry.getValue() instanceof Date) {
                        entry.setValue(((Date) entry.getValue()).getTime());
                    }
                }
                String headline = configManager.getHeadline(event);
                event.addHazardAttribute(HazardConstants.HEADLINE, headline);
                if (event.getHazardAttribute(HazardConstants.FORECAST_POINT) != null) {
                    event.addHazardAttribute(HazardConstants.GEO_TYPE,
                            HazardConstants.POINT_TYPE);
                } else {
                    Class<?> geometryClass = event.getGeometry().getClass();
                    if (geometryClass.equals(Point.class)) {
                        event.addHazardAttribute(HazardConstants.GEO_TYPE,
                                HazardConstants.POINT_TYPE);
                    } else if (geometryClass.equals(LineString.class)) {
                        event.addHazardAttribute(HazardConstants.GEO_TYPE,
                                HazardConstants.LINE_TYPE);
                    } else {
                        event.addHazardAttribute(HazardConstants.GEO_TYPE,
                                HazardConstants.AREA_TYPE);
                    }
                }
                event.removeHazardAttribute(HazardConstants.HAZARD_EVENT_TYPE);

                /*
                 * Need to re-initialize product information when issuing
                 */
                if (issue) {
                    event.removeHazardAttribute(HazardConstants.EXPIRATIONTIME);
                    event.removeHazardAttribute(HazardConstants.ISSUETIME);
                    event.removeHazardAttribute(HazardConstants.VTEC_CODES);
                    event.removeHazardAttribute(HazardConstants.ETNS);
                    event.removeHazardAttribute(HazardConstants.PILS);
                }
                event.removeHazardAttribute(ISessionEventManager.ATTR_ISSUED);
                event.removeHazardAttribute(ISessionEventManager.ATTR_CHECKED);
                event.removeHazardAttribute(ISessionEventManager.ATTR_SELECTED);
                event.removeHazardAttribute(ISessionEventManager.ATTR_HAZARD_CATEGORY);

                events.add(event);
            }

            String product = information.getProductName();
            String[] formats = information.getFormats();
            IPythonJobListener<List<IGeneratedProduct>> listener = new JobListener(
                    issue, notificationSender, information);
            productGen.generate(product, events, formats, listener);
        }
    }

    @Override
    public void issue(ProductInformation information) {

        /*
         * Need to look at all events in the SessionManager because some events
         * for which products were generated may not have been selected. For
         * example, two FA.A's, one selected, one not, and the user adds the
         * second one via the product staging dialog.
         */
        for (IHazardEvent sessionEvent : eventManager.getEvents()) {
            /*
             * Update Hazard Events with product information returned from the
             * Product Generators
             */
            for (IEvent ev : information.getProducts().get(0).getEventSet()) {
                IHazardEvent updatedEvent = (IHazardEvent) ev;
                if (checkForConflicts(updatedEvent)) {
                    statusHandler
                            .info("There is a grid conflict with the hazard event.");
                    // TODO It needs to be decided if we should prevent the user
                    // from issuing a hazard if there is a grid conflict.
                }
                if (sessionEvent.getEventID().equals(updatedEvent.getEventID())) {

                    ObservedHazardEvent newEvent = new ObservedHazardEvent(
                            updatedEvent, (SessionEventManager) eventManager);

                    SessionEventUtilities.mergeHazardEvents(newEvent,
                            sessionEvent);
                    /*
                     * This ensures that the "replaces" string is removed for
                     * the next generation of a product.
                     */
                    sessionEvent
                            .removeHazardAttribute(HazardConstants.REPLACES);

                    if (updatedEvent.getState().equals(HazardState.ENDED)) {
                        eventManager.endEvent(sessionEvent);
                    } else {
                        eventManager.issueEvent(sessionEvent);
                    }

                    break;
                }

            }
        }
        /*
         * Disseminate the products
         */
        for (IGeneratedProduct product : information.getProducts()) {
            /*
             * This is temporary: issueFormats should be user configurable and
             * will be addressed by Issue #691 -- Clean up Configuration Files
             */
            String[] issueFormats = { "Legacy", "CAP", "XML" };
            for (String format : issueFormats) {
                List<Object> objs = product.getEntry(format);
                if (objs != null) {
                    for (Object obj : objs) {
                        ProductUtils.disseminate(String.valueOf(obj));
                    }
                }
            }
        }

    }

    private boolean checkForConflicts(IHazardEvent hazardEvent) {
        boolean hasConflicts = true;
        try {
            // checks if selected events conflicting with existing grids
            // based on time and phensigs
            HasConflictsRequest request = new HasConflictsRequest();
            request.setPhenSig(hazardEvent.getPhenomenon() + "."
                    + hazardEvent.getSignificance());
            request.setSiteID(hazardEvent.getSiteID());
            request.setStartTime(hazardEvent.getStartTime());
            request.setEndTime(hazardEvent.getEndTime());
            hasConflicts = (Boolean) ThriftClient.sendRequest(request);
        } catch (VizException e) {
            statusHandler
                    .error("Unable to check if selected event has any grid conflicts.",
                            e);
        }

        return hasConflicts;
    }

    // @Override
    public void issue_old(ProductInformation information) {

        for (IHazardEvent selectedEvent : information.getProductEvents()) {
            if (selectedEvent.getState() != HazardState.ENDED) {
                Serializable previewState = selectedEvent
                        .getHazardAttribute(HazardConstants.PREVIEW_STATE);
                if (previewState != null
                        && previewState.toString().equalsIgnoreCase(
                                HazardState.ENDED.toString())) {
                    selectedEvent.setState(HazardState.ENDED);
                } else {
                    for (IEvent ev : information.getProducts().get(0)
                            .getEventSet()) {
                        IHazardEvent event = (IHazardEvent) ev;
                        if (selectedEvent.getEventID().equals(
                                event.getEventID())) {
                            ObservedHazardEvent newEvent = new ObservedHazardEvent(
                                    event, (SessionEventManager) eventManager);
                            SessionEventUtilities.mergeHazardEvents(newEvent,
                                    selectedEvent);
                            break;
                        }
                    }

                    // disseminates the legacy product
                    for (IGeneratedProduct product : information.getProducts()) {
                        List<Object> objs = product.getEntry("Legacy");
                        if (objs != null) {
                            for (Object obj : objs) {
                                ProductUtils.disseminate(String.valueOf(obj));
                            }
                        }
                    }

                    /*
                     * This ensures that the "replaces" string is removed for
                     * the next generation of a product.
                     */
                    selectedEvent
                            .removeHazardAttribute(HazardConstants.REPLACES);
                    selectedEvent.setState(HazardState.ISSUED);
                }
                /*
                 * Clear the undo/redo events.
                 */
                ((IUndoRedoable) selectedEvent).clearUndoRedo();
            }
        }
    }

    /**
     * Listens for the completion of product generation and notifies the event
     * bus.
     */
    private class JobListener implements
            IPythonJobListener<List<IGeneratedProduct>> {

        private final boolean issue;

        private final ISessionNotificationSender notificationSender;

        private final ProductInformation info;

        public JobListener(boolean issue,
                ISessionNotificationSender notificationSender,
                ProductInformation info) {
            this.issue = issue;
            this.notificationSender = notificationSender;
            this.info = info;
        }

        @Override
        public void jobFinished(final List<IGeneratedProduct> result) {

            /*
             * Need to place the result on the thread the Session Manager is
             * running. At the moment this is the UI thread.
             */
            VizApp.runAsync(new Runnable() {

                @Override
                public void run() {
                    info.setProducts(result);
                    if (issue) {
                        issue(info);
                    }
                    notificationSender.postNotification(new ProductGenerated(
                            info));
                }
            });
        }

        @Override
        public void jobFailed(final Throwable e) {

            VizApp.runAsync(new Runnable() {

                @Override
                public void run() {
                    info.setError(e);
                    notificationSender
                            .postNotification(new ProductFailed(info));
                }
            });
        }

    }

    @Override
    public void shutdown() {
        /**
         * Nothing to do right now.
         */
    }

    @Override
    public boolean validateSelectedHazardsForProductGeneration() {

        Collection<IHazardEvent> selectedEvents = eventManager
                .getSelectedEvents();
        Date simulatedTime = SimulatedTime.getSystemTime().getTime();
        List<String> eventIds = Lists.newArrayList();

        for (IHazardEvent selectedEvent : selectedEvents) {

            /*
             * Test if the end time of the selected event is in the past.
             * Products will not be generated for events with end times in the
             * past.
             */
            if (selectedEvent.getEndTime().before(simulatedTime)) {
                eventIds.add(selectedEvent.getEventID());
            }
        }

        if (!eventIds.isEmpty()) {
            StringBuffer warningMessage = new StringBuffer();
            warningMessage.append(eventIds.size() > 1 ? "Events " : "Event ");

            for (String eventId : eventIds) {
                warningMessage.append(eventId);
                warningMessage.append(", ");
            }

            warningMessage.deleteCharAt(warningMessage.lastIndexOf(","));
            warningMessage.append(eventIds.size() > 1 ? "have end times "
                    : "has an end time ");
            warningMessage.append("before the CAVE time.\n");
            warningMessage.append("Product generation halted.");
            messenger.getWarner().warnUser(warningMessage.toString());
        }

        return eventIds.isEmpty() ? true : false;
    }

}
