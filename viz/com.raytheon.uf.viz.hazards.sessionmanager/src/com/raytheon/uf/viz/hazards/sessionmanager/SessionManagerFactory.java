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
package com.raytheon.uf.viz.hazards.sessionmanager;

import gov.noaa.gsd.common.eventbus.BoundedReceptionEventBus;

import com.raytheon.uf.common.dataplugin.events.hazards.datastorage.HazardEventManager;
import com.raytheon.uf.common.dataplugin.events.hazards.datastorage.HazardEventManager.Mode;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.viz.hazards.sessionmanager.config.impl.ObservedSettings;
import com.raytheon.uf.viz.hazards.sessionmanager.events.impl.ObservedHazardEvent;
import com.raytheon.uf.viz.hazards.sessionmanager.impl.SessionManager;
import com.raytheon.uf.viz.hazards.sessionmanager.messenger.IMessenger;
import com.raytheon.viz.core.mode.CAVEMode;

/**
 * This is the preferred method of obtaining a new ISessionManager. This
 * protects any code using an ISessionManager form the details of any specific
 * SessionManager implementation.
 * 
 * Currently there is only a single ISessionManager implementation but the
 * factory may be used in the future to implement spring and/or extension point
 * loading.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 11, 2013 1257       bsteffen    Initial creation
 * Dec 05, 2014 4124       Chris.Golden Changed to work with parameterized config manager.
 * Nov 10, 2015 12762      Chris.Golden Added code to implement and use new recommender
 *                                      manager.
 * Jun 23, 2016 19537      Chris.Golden Added use of spatial context provider.
 * Jul 27, 2016 19924      Chris.Golden Added use of display resource context provider.
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */

public class SessionManagerFactory {

    public static ISessionManager<ObservedHazardEvent, ObservedSettings> getSessionManager(
            IMessenger messenger,
            ISpatialContextProvider spatialContextProvider,
            IDisplayResourceContextProvider displayResourceContextProvider,
            IFrameContextProvider frameContextProvider,
            BoundedReceptionEventBus<Object> eventBus) {
        Mode mode = CAVEMode.getMode() == CAVEMode.PRACTICE ? Mode.PRACTICE
                : Mode.OPERATIONAL;
        return new SessionManager(PathManagerFactory.getPathManager(),
                new HazardEventManager(mode), spatialContextProvider,
                displayResourceContextProvider, frameContextProvider,
                messenger, eventBus);
    }
}
