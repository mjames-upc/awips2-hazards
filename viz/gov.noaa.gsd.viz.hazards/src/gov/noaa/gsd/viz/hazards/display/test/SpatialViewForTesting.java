/**
 * This software was developed and / or modified by the
 * National Oceanic and Atmospheric Administration (NOAA), 
 * Earth System Research Laboratory (ESRL), 
 * Global Systems Division (GSD), 
 * Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.hazards.display.test;

import gov.noaa.gsd.viz.hazards.display.HazardServicesAppBuilder;
import gov.noaa.gsd.viz.hazards.display.HazardServicesMessageHandler;
import gov.noaa.gsd.viz.hazards.spatialdisplay.HazardServicesDrawingAction;
import gov.noaa.gsd.viz.hazards.spatialdisplay.HazardServicesMouseHandlers;
import gov.noaa.gsd.viz.hazards.spatialdisplay.ISpatialView;
import gov.noaa.gsd.viz.hazards.spatialdisplay.SpatialPresenter;
import gov.noaa.gsd.viz.hazards.spatialdisplay.SpatialView.SpatialViewCursorTypes;
import gov.noaa.gsd.viz.hazards.spatialdisplay.SpatialDisplay;
import gov.noaa.gsd.viz.hazards.spatialdisplay.mousehandlers.MouseHandlerFactory;
import gov.noaa.gsd.viz.hazards.spatialdisplay.selectbyarea.SelectByAreaDbMapResource;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.raytheon.uf.viz.hazards.sessionmanager.config.types.Settings;

/**
 * Description: Mock {@link ISpatialView} used for testing.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 22, 2013 2166       daniel.s.schaffer@noaa.gov      Initial creation
 * 
 * </pre>
 * 
 * @author daniel.s.schaffer@noaa.gov
 * @version 1.0
 */
@SuppressWarnings("rawtypes")
public class SpatialViewForTesting implements ISpatialView {

    @Override
    public void dispose() {
    }

    @Override
    public List contributeToMainUI(Enum type) {
        return null;
    }

    @Override
    public void initialize(SpatialPresenter presenter,
            MouseHandlerFactory mouseFactory) {
    }

    @Override
    public void setSettings(Settings settings) {
    }

    @Override
    public void drawEvents(boolean toogleAutoHazardChecking,
            boolean areHatchedAreasDisplayed) {

    }

    @Override
    public void redoTimeMatching() {
    }

    @Override
    public void setDisplayZoomParameters(double longitude, double latitude,
            double multiplier) {
    }

    @Override
    public void recenterRezoomDisplay() {
    }

    @Override
    public void issueRefresh() {
    }

    @Override
    public void setMouseHandler(HazardServicesMouseHandlers mouseHandler,
            String... args) {
    }

    @Override
    public void unregisterCurrentMouseHandler() {
    }

    @Override
    public void modifyShape(HazardServicesDrawingAction drawingAction,
            HazardServicesAppBuilder appBuilder,
            HazardServicesMessageHandler messageHandler) {
    }

    @Override
    public void manageViewFrames(Date selectedTime) {
    }

    @Override
    public void addGeometryDisplayResourceToPerspective() {
    }

    @Override
    public void setCursor(SpatialViewCursorTypes cursorType) {
    }

    @Override
    public void drawingActionComplete() {

    }

    @Override
    public SpatialDisplay getSpatialDisplay() {
        return null;
    }

    @Override
    public void loadGeometryOverlayForSelectedEvent() {
    }

    @Override
    public SelectByAreaDbMapResource getSelectableGeometryDisplay() {
        return null;
    }

    @Override
    public void setUndoEnabled(Boolean undoFlag) {
    }

    @Override
    public void setRedoEnabled(Boolean redoFlag) {
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
