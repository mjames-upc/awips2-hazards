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

import com.raytheon.uf.viz.hazards.sessionmanager.ISessionNotification;

/**
 * Uses eventbus to send out notifications.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 10, 2013            bsteffen     Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */

public class SessionNotificationSender implements ISessionNotificationSender {

    private final BoundedReceptionEventBus<Object> bus;

    public SessionNotificationSender(BoundedReceptionEventBus<Object> bus) {
        this.bus = bus;
    }

    @Override
    public void postNotification(ISessionNotification notification) {
        bus.publish(notification);
    }

    @Override
    public void postNotificationAsync(ISessionNotification notification) {
        bus.publishAsync(notification);
    }
}
