/**
 * This software was developed and / or modified by the National Oceanic and
 * Atmospheric Administration (NOAA), Earth System Research Laboratory (ESRL),
 * Global Systems Division (GSD), Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.hazards.spatialdisplay.mousehandlers;

import gov.noaa.gsd.viz.hazards.spatialdisplay.SpatialView.SpatialViewCursorTypes;
import gov.noaa.gsd.viz.hazards.spatialdisplay.drawableelements.HazardServicesSymbol;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.tools.InputHandlerDefaultImpl;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;

import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.viz.ui.VizWorkbenchManager;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * Base drawing action(refer to RT collaboration)
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 04, 2013            Xiangbao Jing      Initial induction into repo
 * Jun 24, 2013            Bryon Lawrence     Changed how the move element
 *                                            function works. It is now based
 *                                            on click point versus geometry
 *                                            centroid.
 * Jul 15, 2013      585   Chris.Golden       Changed to no longer be a singleton.
 * Jul 18, 2013     1264     Chris.Golden     Added support for drawing lines and
 *                                            points.
 * </pre>
 * 
 * @author Xiangbao Jing
 */
public class NonDrawingAction extends AbstractMouseHandler {

    public static final String pgenType = "TornadoWarning";

    public static final String pgenCategory = "MET";

    /**
     * Construct a standard instance.
     */
    public NonDrawingAction() {
    }

    @Override
    protected IInputHandler createMouseHandler() {
        return new NonDrawingHandler();
    }

    public class NonDrawingHandler extends InputHandlerDefaultImpl {

        AbstractDrawableComponent ghostEl = null;

        Coordinate ptSelected = null;

        /*
         * (non-Javadoc)
         * 
         * @see com.raytheon.viz.ui.input.IInputHandler#handleMouseDown(int,
         * int, int)
         */
        @Override
        public boolean handleMouseDown(int anX, int aY, int button) {

            // Check if mouse is in geographic extent
            AbstractEditor editor = ((AbstractEditor) VizWorkbenchManager
                    .getInstance().getActiveEditor());
            Coordinate loc = editor.translateClick(anX, aY);
            if (loc == null) {
                return false;
            }

            // If mouse button = 1, select the event, but do not
            // at this time send a message to the IHIS Layer.
            if (button == 1) {

                if (getDrawingLayer().getSelectedDE() == null) {
                    /*
                     * Get the nearest element and set it as the selected
                     * element. Note: for Contours, we should select the nearest
                     * ContourLine, ContourMinmax or ContourCircle.
                     */
                    AbstractDrawableComponent nadc = getDrawingLayer()
                            .getContainingComponent(loc, anX, aY);
                    getDrawingLayer().setSelectedDE(nadc);

                    // Remove the label associated with the element.
                    getDrawingLayer().removeElementLabel(nadc);

                    getDrawingLayer().issueRefresh();
                }

                return true;

            } else {
                return true;
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.raytheon.viz.ui.input.IInputHandler#handleMouseDownMove(int,
         * int, int)
         */
        @Override
        public boolean handleMouseDownMove(int anX, int aY, int button) {

            // Check if mouse is in geographic extent
            AbstractEditor editor = ((AbstractEditor) VizWorkbenchManager
                    .getInstance().getActiveEditor());
            Coordinate loc = editor.translateClick(anX, aY);
            if (loc == null) {
                return false;
            }

            AbstractDrawableComponent elSelected = getDrawingLayer()
                    .getSelectedDE();

            Color ghostColor = new java.awt.Color(255, 255, 255);
            double distanceToSelect = 20;

            if (elSelected != null) {
                // start to copy if the click is near the selected hazard.
                if (ghostEl == null) {
                    GeometryFactory gf = new GeometryFactory();
                    Point clickPoint = gf.createPoint(loc);
                    double distance;
                    if (elSelected instanceof HazardServicesSymbol) {
                        Point centroid = gf.createPoint(elSelected.getPoints()
                                .get(0));
                        distance = centroid.distance(clickPoint);
                    } else {
                        List<Coordinate> drawnPoints = elSelected.getPoints();

                        /*
                         * Must make a copy of the points. Do not want to modify
                         * the List of Coordinates in the geometry itself.
                         */
                        Coordinate[] copyOfCoords = new Coordinate[drawnPoints
                                .size() + 1];
                        copyOfCoords = drawnPoints.toArray(copyOfCoords);
                        copyOfCoords[drawnPoints.size()] = drawnPoints.get(0);

                        LineString ls = gf.createLineString(copyOfCoords);

                        distance = ls.distance(clickPoint);
                    }

                    if (distance < distanceToSelect) {
                        ptSelected = new Coordinate(clickPoint.getX(),
                                clickPoint.getY());
                        ghostEl = getDrawingLayer().getSelectedDE().copy();

                    }
                }

                if (ghostEl != null) {
                    // use screen coordinate to copy/move
                    // double[] locScreen =
                    // mapEditor.translateInverseClick(loc);
                    double[] ptScreen = editor
                            .translateInverseClick(ptSelected);

                    double deltaX = anX - ptScreen[0];
                    double deltaY = aY - ptScreen[1];

                    // calculate locations of the ghost el
                    for (int idx = 0; idx < elSelected.getPoints().size(); idx++) {
                        double[] scnPt = editor
                                .translateInverseClick(elSelected.getPoints()
                                        .get(idx));
                        scnPt[0] += deltaX;
                        scnPt[1] += deltaY;

                        Coordinate cord = editor.translateClick(scnPt[0],
                                scnPt[1]);
                        if (cord == null) {
                            continue;
                        }
                        ghostEl.getPoints().get(idx).x = cord.x;
                        ghostEl.getPoints().get(idx).y = cord.y;

                    }
                    // set ghost color
                    ghostEl.setColors(new Color[] { ghostColor,
                            new java.awt.Color(255, 255, 255) });

                    getDrawingLayer().setGhostLine(ghostEl);
                    getDrawingLayer().issueRefresh();
                }

            }

            return true;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.raytheon.viz.ui.input.IInputHandler#handleMouseUp(int, int,
         * int)
         */
        @Override
        public boolean handleMouseUp(int x, int y, int button) {

            if (ghostEl != null) {

                // reset color for the el and add it to PGEN resource

                Iterator<DrawableElement> iterator1 = ghostEl
                        .createDEIterator();
                Iterator<DrawableElement> iterator2 = getDrawingLayer()
                        .getSelectedDE().createDEIterator();

                while (iterator1.hasNext() && iterator2.hasNext()) {
                    iterator1.next().setColors(iterator2.next().getColors());
                }

                getDrawingLayer().addElement(ghostEl);

                getDrawingLayer().removeGhostLine();
                ghostEl = null;
                getDrawingLayer().setSelectedDE(null);
                getDrawingLayer().issueRefresh();

            }

            return true;

        }

        @Override
        public boolean handleMouseMove(int x, int y) {
            AbstractEditor editor = ((AbstractEditor) VizWorkbenchManager
                    .getInstance().getActiveEditor());
            Coordinate loc = editor.translateClick(x, y);

            if (loc != null) {
                AbstractDrawableComponent nadc = getDrawingLayer()
                        .getContainingComponent(loc, x, y);

                // There is a selected element. Is the
                // mouse over it?
                if (nadc != null) {
                    // Set the mouse cursor to a move symbol
                    getSpatialPresenter().getView().setCursor(
                            SpatialViewCursorTypes.MOVE_SHAPE_CURSOR);

                } else {
                    // Set the mouse cursor to an arrow
                    getSpatialPresenter().getView().setCursor(
                            SpatialViewCursorTypes.ARROW_CURSOR);
                }

            }

            return false;
        }

    }

}