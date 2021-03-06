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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Description: A hazard services alert.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 26, 2013  1325     daniel.s.schaffer@noaa.gov      Initial creation
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
public class HazardAlert implements IHazardAlert {

    protected HazardAlertState state;

    private Date activationTime;

    private Date deactivationTime;

    /**
     * 
     */
    public HazardAlert() {
        state = HazardAlertState.UNSCHEDULED;
    }

    /**
     * @return the state
     */
    @Override
    public HazardAlertState getState() {
        return state;
    }

    /**
     * @return the activationTime
     */
    @Override
    public Date getActivationTime() {
        return activationTime;
    }

    /**
     * @param activationTime
     *            the activationTime to set
     */
    @Override
    public void setActivationTime(Date activationTime) {
        this.activationTime = activationTime;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    /**
     * @param state
     *            the state to set
     */
    @Override
    public void setState(HazardAlertState state) {
        this.state = state;
    }

    /**
     * @return the deactivationTime
     */
    @Override
    public Date getDeactivationTime() {
        return deactivationTime;
    }

    /**
     * @param deactivationTime
     *            the deactivationTime to set
     */
    @Override
    public void setDeactivationTime(Date deactivationTime) {
        this.deactivationTime = deactivationTime;
    }

}
