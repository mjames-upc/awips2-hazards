/**
 * This software was developed and / or modified by the National Oceanic and
 * Atmospheric Administration (NOAA), Earth System Research Laboratory (ESRL),
 * Global Systems Division (GSD), Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.hazards.spatialdisplay;

import static gov.noaa.gsd.viz.hazards.spatialdisplay.LineStyle.LINE_DASHED_2;

import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.hazards.sessionmanager.ISessionManager;
import com.raytheon.uf.viz.hazards.sessionmanager.events.impl.ObservedHazardEvent;

/**
 * <Description> The PGEN drawing attributes associated with a polygon drawn on
 * the Spatial Display in Hazard Services. All drawables in Hazard Services are
 * rendered using PGEN drawing classes.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 04, 2013            Bryon.Lawrence      Initial induction into repo
 * Aug  9, 2013 1921       daniel.s.schaffer@noaa.gov  Support of replacement of JSON with POJOs
 * 
 * </pre>
 * 
 * @author Bryon.Lawrence
 */
public class SelectionRectangleDrawingAttributes extends
        HazardServicesDrawingAttributes {

    public static double SIZE_SCALE = 7.5;

    public SelectionRectangleDrawingAttributes(
            ISessionManager<ObservedHazardEvent> sessionManager)
            throws VizException {
        super(sessionManager.getConfigurationManager());
        filled = false;
    }

    /**
     * Sets flag indicating whether or not this event drawable shoulf be closed.
     * 
     * @return Boolean
     */
    @Override
    public Boolean isClosedLine() {
        return true;
    }

    @Override
    public void setDashedLineStyle() {
        this.lineStyle = LINE_DASHED_2;
    }

    @Override
    public double getSizeScale() {
        return SIZE_SCALE;
    }
}