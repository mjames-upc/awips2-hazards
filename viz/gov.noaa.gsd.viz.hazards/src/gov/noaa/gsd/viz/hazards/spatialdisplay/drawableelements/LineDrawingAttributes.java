/**
 * This software was developed and / or modified by the National Oceanic and
 * Atmospheric Administration (NOAA), Earth System Research Laboratory (ESRL),
 * Global Systems Division (GSD), Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.hazards.spatialdisplay.drawableelements;

import static gov.noaa.gsd.viz.hazards.spatialdisplay.drawableelements.LineStyle.LINE_DASHED_2;
import static gov.noaa.gsd.viz.hazards.spatialdisplay.drawableelements.LineStyle.LINE_DASHED_3;

import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.hazards.sessionmanager.ISessionManager;
import com.raytheon.uf.viz.hazards.sessionmanager.config.impl.ObservedSettings;
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
 * Dec 05, 2014 4124       Chris.Golden        Changed to work with newly parameterized
 *                                             config manager.
 * Mar 16, 2016 15676      Chris.Golden        Moved to more appropriate location.
 * Mar 24, 2016 15676      Chris.Golden        Added ability to set size scale, and to use
 *                                             dotted line style.
 * </pre>
 * 
 * @author Bryon.Lawrence
 */
public class LineDrawingAttributes extends HazardServicesDrawingAttributes {

    public static double SIZE_SCALE = 7.5;

    private double sizeScale = SIZE_SCALE;

    public LineDrawingAttributes(
            ISessionManager<ObservedHazardEvent, ObservedSettings> sessionManager)
            throws VizException {
        super(sessionManager.getConfigurationManager());
        setClosed(false);
        setFilled(false);
    }

    @Override
    public void setDottedLineStyle() {
        this.lineStyle = LINE_DASHED_2;
    }

    @Override
    public void setDashedLineStyle() {
        this.lineStyle = LINE_DASHED_3;
    }

    @Override
    public double getSizeScale() {
        return sizeScale;
    }

    public void setSizeScale(double sizeScale) {
        this.sizeScale = sizeScale;
    }
}