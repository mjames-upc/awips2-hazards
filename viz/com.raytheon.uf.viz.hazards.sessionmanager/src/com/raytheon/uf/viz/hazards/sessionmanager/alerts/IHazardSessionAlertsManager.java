/**
 * This software was developed and / or modified by the
 * National Oceanic and Atmospheric Administration (NOAA), 
 * Earth System Research Laboratory (ESRL), 
 * Global Systems Division (GSD), 
 * Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package com.raytheon.uf.viz.hazards.sessionmanager.alerts;

import com.google.common.collect.ImmutableList;
import com.raytheon.uf.common.dataplugin.events.hazards.HazardNotification;
import com.raytheon.uf.viz.hazards.sessionmanager.alerts.impl.IHazardAlertJob;
import com.raytheon.uf.viz.hazards.sessionmanager.alerts.impl.IHazardAlertStrategy;

/**
 * Description: Manages alerts in hazard services. It partitions alerts based on
 * classes of notifications of external events such as
 * {@link HazardNotification}. So when such a notification arrives, it will
 * check for any updates to the current alerts corresponding to that
 * notification. The business of creating new alerts or identifying alerts to be
 * canceled is handled by the {@link IHazardAlertStrategy} corresponding to the
 * notification.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * July 08, 2013   1325     daniel.s.schaffer@noaa.gov      Initial creation
 * Nov 20, 2013   2159     daniel.s.schaffer@noaa.gov Now interoperable with DRT
 * 
 * </pre>
 * 
 * @author daniel.s.schaffer@noaa.gov
 * @version 1.0
 */
public interface IHazardSessionAlertsManager {

    /**
     * @param notificationClass
     *            When this notification is received, the alert generating
     *            strategy will be called
     * @param strategy
     */
    void addAlertGenerationStrategy(Class<?> notificationClass,
            IHazardAlertStrategy strategy);

    /**
     * Schedule the given {@link IHazardAlert}
     */
    void scheduleAlert(IHazardAlert hazardAlert);

    /**
     * Activate the {@link IHazardAlert} contained in this
     * {@link IHazardAlertJob}
     */
    void activateAlert(IHazardAlertJob hazardAlertJob);

    /**
     * Cancel the given {@link IHazardAlert}
     */
    void cancelAlert(IHazardAlert hazardAlert);

    /**
     * @return all scheduled plus active alerts
     */
    ImmutableList<IHazardAlert> getAlerts();

    /**
     * @return the currently active {@link IHazardAlert}s
     */
    ImmutableList<IHazardAlert> getActiveAlerts();

    /**
     * Execute any startup required.
     */
    void start();

    /**
     * Execute any shutdown required.
     */
    void shutdown();

}
