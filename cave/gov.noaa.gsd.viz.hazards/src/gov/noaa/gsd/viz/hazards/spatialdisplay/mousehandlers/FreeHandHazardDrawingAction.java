/**
 * This software was developed and / or modified by the National Oceanic and
 * Atmospheric Administration (NOAA), Earth System Research Laboratory (ESRL),
 * Global Systems Division (GSD), Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.hazards.spatialdisplay.mousehandlers;

import gov.noaa.gsd.viz.hazards.display.action.SpatialDisplayAction;
import gov.noaa.gsd.viz.hazards.jsonutilities.JSONUtilities;
import gov.noaa.gsd.viz.hazards.spatialdisplay.PolygonDrawingAttributes;
import gov.noaa.gsd.viz.hazards.spatialdisplay.SpatialPresenter;
import gov.noaa.gsd.viz.hazards.utilities.Utilities;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.AttrDlg;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElementFactory;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableType;
import gov.noaa.nws.ncep.ui.pgen.elements.Line;
import gov.noaa.nws.ncep.ui.pgen.tools.InputHandlerDefaultImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.ui.PlatformUI;

import com.google.common.collect.Lists;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.viz.ui.VizWorkbenchManager;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

/**
 * Rect drawing action(refer to RT collaboration)
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 04, 2013            Xiangbao Jing      Initial induction into repo
 * 
 * </pre>
 * 
 * @author Xiangbao Jing
 */
public class FreeHandHazardDrawingAction extends AbstractMouseHandler {

    /**
     * Logging mechanism.
     */
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(FreeHandHazardDrawingAction.class);

    private static FreeHandHazardDrawingAction eventBoxDrawingAction = null;

    /** The mouse handler */
    protected IInputHandler theHandler;

    protected AttrDlg attrDlg = null;

    private SpatialPresenter spatialPresenter;

    public static final String pgenType = "TornadoWarning";

    public static final String pgenCategory = "MET";

    /**
     * Call this function to retrieve an instance of the EventBoxDrawingAction.
     * 
     * @param ihisDrawingLayer
     * @param spatialPresenter
     * @return FreeHandHazardDrawingAction
     */
    public static FreeHandHazardDrawingAction getInstance(
            SpatialPresenter spatialPresenter) {
        if (eventBoxDrawingAction == null) {
            eventBoxDrawingAction = new FreeHandHazardDrawingAction(
                    spatialPresenter);
        } else {
            eventBoxDrawingAction.setSpatialPresenter(spatialPresenter);
            eventBoxDrawingAction.setDrawingLayer(spatialPresenter.getView()
                    .getSpatialDisplay());
        }

        return eventBoxDrawingAction;

    }

    private FreeHandHazardDrawingAction(SpatialPresenter spatialPresenter)

    {
        super();

        this.spatialPresenter = spatialPresenter;
        this.drawingLayer = spatialPresenter.getView().getSpatialDisplay();

        /*
         * Create the attribute container.
         */
        try {
            attrDlg = new PolygonDrawingAttributes(PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getShell());
        } catch (VizException e) {
            statusHandler.error("FreeHandHazardDrawingAction.<init>: Creation "
                    + "of polygon drawing attributes failed.", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.gsd.viz.drawing.AbstractDrawingTool#getMouseHandler()
     */
    @Override
    public IInputHandler getMouseHandler() {
        if (theHandler == null) {
            theHandler = new FreeHandHazardDrawingHandler();
        }
        return theHandler;
    }

    public void setSpatialPresenter(SpatialPresenter spatialPresenter) {
        this.spatialPresenter = spatialPresenter;
    }

    public SpatialPresenter getSpatialPresenter() {
        return spatialPresenter;
    }

    public class FreeHandHazardDrawingHandler extends InputHandlerDefaultImpl {
        private final ArrayList<Coordinate> points = new ArrayList<Coordinate>();

        /*
         * An instance of DrawableElementFactory, which is used to create a new
         * watch box.
         */
        private final DrawableElementFactory def = new DrawableElementFactory();

        @Override
        public boolean handleMouseDown(int x, int y, int mouseButton) {
            AbstractEditor editor = ((AbstractEditor) VizWorkbenchManager
                    .getInstance().getActiveEditor());
            Coordinate loc = editor.translateClick(x, y);

            if ((mouseButton != 1) || (loc == null)) {
                return false;
            }

            points.add(loc);

            return true;
        }

        @Override
        public boolean handleMouseMove(int x, int y) {
            return true;
        }

        // Needed to override this to prevent
        // it from being passed on to CAVE panning routines.
        @Override
        public boolean handleMouseUp(int x, int y, int mouseButton) {
            // Finishes the editing action, if one has
            // been initiated.
            AbstractEditor editor = ((AbstractEditor) VizWorkbenchManager
                    .getInstance().getActiveEditor());
            Coordinate loc = editor.translateClick(x, y);

            if (mouseButton != 1 || loc == null) {
                return false;
            }

            if (points.size() < 2) {
                getDrawingLayer().removeGhostLine();
                points.clear();

                getDrawingLayer().issueRefresh();

                // Indicate that this drawing action is done.
                spatialPresenter.getView().drawingActionComplete();
            } else {
                points.add(loc);

                // Close the polygon...This is required to create a LinearRing
                // Geometry
                points.add(points.get(0));

                // Add logic to simplify the number of
                // points in the polygon. This will need
                // to eventually be user-configurable.
                LinearRing linearRing = new GeometryFactory()
                        .createLinearRing(points.toArray(new Coordinate[0]));

                Geometry polygon = new GeometryFactory().createPolygon(
                        linearRing, null);
                Geometry reducedGeometry = TopologyPreservingSimplifier
                        .simplify(polygon, 0.0001);
                List<Coordinate> reducedPoints = Arrays.asList(reducedGeometry
                        .getCoordinates());
                ArrayList<Coordinate> reducedPointsList = Lists
                        .newArrayList(reducedPoints);

                getDrawingLayer().removeGhostLine();

                // Could be LINE_SOLID or LINE_DASHED_4
                @SuppressWarnings("unused")
                AbstractDrawableComponent warningBox = def.create(
                        DrawableType.LINE, attrDlg, "Line", "LINE_DASHED_4",
                        reducedPointsList, getDrawingLayer().getActiveLayer());

                // Convert the object to JSON.
                String jsonString = JSONUtilities.createNewHazardJSON("",
                        Utilities.HAZARD_EVENT_SHAPE_TYPE_POLYGON,
                        reducedPointsList);
                points.clear();

                SpatialDisplayAction action = new SpatialDisplayAction(
                        "newEventArea");
                action.setJSON(jsonString);
                spatialPresenter.fireAction(action);

                // Indicate that this drawing action is done.
                spatialPresenter.getView().drawingActionComplete();
            }

            return false;
        }

        // Needed to override this to prevent odd panning
        // behavior when drawing to CAVE
        @Override
        public boolean handleMouseDownMove(int x, int y, int mouseButton) {
            AbstractEditor editor = ((AbstractEditor) VizWorkbenchManager
                    .getInstance().getActiveEditor());
            Coordinate loc = editor.translateClick(x, y);

            if ((mouseButton != 1) || (loc == null)) {
                return false;
            }

            if (points != null && points.size() >= 1) {
                points.add(loc);

                // create the ghost element and put it in the drawing layer
                AbstractDrawableComponent ghost = def.create(DrawableType.LINE,
                        attrDlg, "Line", "LINE_SOLID", points,
                        getDrawingLayer().getActiveLayer());

                ArrayList<Coordinate> ghostPts = new ArrayList<Coordinate>(
                        points);
                ((Line) ghost).setLinePoints(ghostPts);

                getDrawingLayer().setGhostLine(ghost);
                getDrawingLayer().issueRefresh();
            }

            return true;
        }

    }
}