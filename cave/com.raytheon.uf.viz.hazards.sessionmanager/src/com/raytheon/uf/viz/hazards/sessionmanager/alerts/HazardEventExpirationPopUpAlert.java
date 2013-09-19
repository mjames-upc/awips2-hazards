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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.viz.hazards.sessionmanager.config.impl.types.HazardEventExpirationAlertConfigCriterion;

/**
 * Description: A {@link IHazardEventExpirationAlert} that is a pop-up alert.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 09, 2013   1325     daniel.s.schaffer@noaa.gov      Initial creation
 * 
 * </pre>
 * 
 * @author daniel.s.schaffer@noaa.gov
 * @version 1.0
 */
public class HazardEventExpirationPopUpAlert extends HazardEventExpirationAlert
        implements IHazardEventExpirationAlert {

    public HazardEventExpirationPopUpAlert(String eventID,
            HazardEventExpirationAlertConfigCriterion alertCriterion) {
        super(eventID, alertCriterion);
    }

    public String getText() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Event %s will expire in ", getEventID()));
        Long days = alertCriterion.getMillisBeforeExpiration()
                / TimeUtil.MILLIS_PER_DAY;
        if (days >= 1) {
            sb.append(String.format("%d to %d days", days, days + 1));
        } else {
            Long hours = alertCriterion.getMillisBeforeExpiration()
                    / TimeUtil.MILLIS_PER_HOUR;
            if (hours >= 1) {
                ;
                Long diff = alertCriterion.getMillisBeforeExpiration() - hours
                        * TimeUtil.MILLIS_PER_HOUR;
                Long minutes = diff / TimeUtil.MILLIS_PER_MINUTE;
                sb.append(String.format("%d hours and %d minutes", hours,
                        minutes));
            } else {
                Long minutes = alertCriterion.getMillisBeforeExpiration()
                        / TimeUtil.MILLIS_PER_MINUTE;
                if (minutes >= 1) {
                    sb.append(String.format("%d minutes", minutes));
                } else {
                    Long seconds = alertCriterion.getMillisBeforeExpiration()
                            / TimeUtil.MILLIS_PER_SECOND;
                    sb.append(String.format("%d seconds", seconds));
                }
            }
        }
        return sb.toString();
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

}