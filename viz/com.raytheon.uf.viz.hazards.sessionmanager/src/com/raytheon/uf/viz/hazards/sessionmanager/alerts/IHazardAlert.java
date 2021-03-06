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

import java.util.Date;

/**
 * Description: An alert for hazard services
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 19, 2013   1325    daniel.s.schaffer@noaa.gov      Initial creation
 * Dec  1, 2014 3249       Dan Schaffer Issue #3249.  Fixed problem where stale 
 *                                                    alerts would appear when 
 *                                                    you leave hazard services 
 *                                                    and come back much later.
 * 
 * </pre>
 * 
 * @author daniel.s.schaffer@noaa.gov
 * @version 1.0
 */
public interface IHazardAlert {

    /**
     * @return state as indicated in {@link HazardAlertState}
     */
    HazardAlertState getState();

    /**
     * Set the state of this
     */
    void setState(HazardAlertState state);

    /**
     * @return the time at which this is scheduled to be activated.
     */
    Date getActivationTime();

    void setActivationTime(Date activationTime);

    Date getDeactivationTime();

    void setDeactivationTime(Date deactivationTime);

}
