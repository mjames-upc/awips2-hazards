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

import com.raytheon.uf.common.colormap.Color;
import com.raytheon.uf.viz.hazards.sessionmanager.config.impl.types.HazardAlertTimerConfigCriterion;

/**
 * Description: TODO
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * MMM DD, YYYY            daniel.s.schaffer@noaa.gov      Initial creation
 * 
 * </pre>
 * 
 * @author daniel.s.schaffer@noaa.gov
 * @version 1.0
 */
public class HazardEventExpirationTimer extends HazardEventAlert implements
        IHazardEventExpirationAlert {

    protected final HazardAlertTimerConfigCriterion alertCriterion;

    private Date hazardExpiration;

    public HazardEventExpirationTimer(String eventID,
            HazardAlertTimerConfigCriterion alertCriterion) {
        super(eventID);
        this.alertCriterion = alertCriterion;
    }

    public Color getColor() {
        return alertCriterion.getColor();
    }

    public boolean isBold() {
        return alertCriterion.isBold();
    }

    public boolean isBlinking() {
        return alertCriterion.isBlinking();
    }

    public boolean isItalic() {
        return alertCriterion.isItalic();
    }

    @Override
    public Date getHazardExpiration() {
        return hazardExpiration;
    }

    @Override
    public void setHazardExpiration(Date hazardExpiration) {
        this.hazardExpiration = hazardExpiration;
    }

}