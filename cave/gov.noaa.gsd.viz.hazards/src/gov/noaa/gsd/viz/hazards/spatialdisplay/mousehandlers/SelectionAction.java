/**
 * This software was developed and / or modified by the National Oceanic and
 * Atmospheric Administration (NOAA), Earth System Research Laboratory (ESRL),
 * Global Systems Division (GSD), Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.hazards.spatialdisplay.mousehandlers;

import gov.noaa.gsd.viz.hazards.jsonutilities.JSONUtilities;
import gov.noaa.gsd.viz.hazards.spatialdisplay.HazardServicesMouseHandlers;
import gov.noaa.gsd.viz.hazards.spatialdisplay.SpatialView.SpatialViewCursorTypes;
import gov.noaa.gsd.viz.hazards.spatialdisplay.drawableelements.HazardServicesLine;
import gov.noaa.gsd.viz.hazards.spatialdisplay.drawableelements.HazardServicesPoint;
import gov.noaa.gsd.viz.hazards.spatialdisplay.drawableelements.HazardServicesPolygon;
import gov.noaa.gsd.viz.hazards.spatialdisplay.drawableelements.HazardServicesSymbol;
import gov.noaa.gsd.viz.hazards.spatialdisplay.drawableelements.IHazardServicesShape;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.elements.Line;
import gov.noaa.nws.ncep.ui.pgen.elements.Symbol;

import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;

import com.google.common.collect.Lists;
import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.viz.ui.VizWorkbenchManager;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * Drawing action associated with the selection and manipulation of hazard
 * polygons.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * November 2011             Bryon.Lawrence   Initial creation
 * Jul 15, 2013      585     Chris.Golden     Changed to no longer be a singleton.
 * Aug  9, 2013 1921       daniel.s.schaffer@noaa.gov  Support of replacement of JSON with POJOs
 * </pre>
 * 
 * @author Bryon.Lawrence
 */
public class SelectionAction extends NonDrawingAction {
    /**
     * Defines the mouse selection sensitivity in pixels.
     */
    public static final double SELECTION_DISTANCE_PIXELS = 15.0;

    /**
     * Defines the type of move operation. SINGLE_POINT -- the user is moving a
     * vertex ALL_POINTS -- the user is moving the entire polygon.
     */
    private enum MoveType {
        SINGLE_POINT, ALL_POINTS;
    }

    public SelectionAction() {
    }

    @Override
    protected IInputHandler createMouseHandler() {
        return new SelectionHandler();
    }

    /**
     * Retrieves the mouse handler associated with the Selection Drawing Action.
     */
    @Override
    public IInputHandler getMouseHandler() {
        IInputHandler handler = super.getMouseHandler();

        /*
         * Ensure key state variables are set to false.
         */
        ((SelectionHandler) handler).shiftKeyIsDown = false;
        ((SelectionHandler) handler).ctrlKeyIsDown = false;

        return handler;
    }

    /**
     * 
     * Description: Mouse handler associated with the Selection Drawing Action.
     * 
     * <pre>
     * 
     * SOFTWARE HISTORY
     * Date         Ticket#    Engineer             Description
     * ------------ ---------- -----------          --------------------------
     * November 2011           bryon.lawrence       Initial creation
     * 
     * </pre>
     * 
     * @author bryon.lawrence
     * @version 1.0
     */
    public class SelectionHandler extends NonDrawingAction.NonDrawingHandler {
        /**
         * Flag which keeps track of the pressed state of the shift key.
         */
        private boolean shiftKeyIsDown = false;

        /**
         * Flag which keeps track of the pressed state of the ctrl key.
         */
        private boolean ctrlKeyIsDown = false;

        /**
         * For move operations
         */
        private MoveType moveType = null;

        /**
         * The index of the polygon point being moved.
         */
        private int movePointIndex = -1;

        /**
         * Flag indicating if this a move of a polygon vertex.
         */
        private boolean isVertexMove = false;

        /**
         * Flag indicating whether or not to allow panning.
         */
        private boolean allowPanning = false;

        /**
         * Listens for the press of the SHIFT or CTRL keys. If the mouse pointer
         * is over a hazard, this signifies multiple selection via left mouse
         * click. If the mouse pointer is not over a hazard, then either a
         * selection box (SHIFT key) or selection lasso (CTRL key) can be drawn
         * to select multiple hazards.
         */
        @Override
        public boolean handleKeyDown(final int key) {

            if (key == SWT.CTRL) {
                ctrlKeyIsDown = true;

            } else if (key == SWT.SHIFT) {
                shiftKeyIsDown = true;
            }

            return true;

        }

        /**
         * Listens for the release of the Shift or Ctrl key. This signals an
         * exit from multiple selection mode.
         * 
         * @param key
         *            the key number.
         * @return Whether or not the event was handled.
         */
        @Override
        public boolean handleKeyUp(int key) {
            if (key == SWT.SHIFT) {
                shiftKeyIsDown = false;
            } else if (key == SWT.CTRL) {
                ctrlKeyIsDown = false;
            }
            return false;
        }

        /**
         * @see com.raytheon.viz.ui.input.IInputHandler#handleMouseDown(int,
         *      int, int)
         */
        @Override
        public boolean handleMouseDown(int anX, int aY, int button) {
            if (button == 1) {
                AbstractEditor editor = ((AbstractEditor) VizWorkbenchManager
                        .getInstance().getActiveEditor());
                Coordinate loc = editor.translateClick(anX, aY);

                if (loc != null) {
                    findSelectedDE(loc, anX, aY);
                }
            }

            return false;
        }

        /**
         * Determines if the mouse pointer is within a polygon. If it is, then
         * this becomes the containing DE.
         * 
         * @param loc
         *            The coordinate representing the location of the mouse
         *            pointer.
         * @param x
         *            X coordinate in pixel space.
         * @param y
         *            Y coordinate in pixel space.
         * @return Whether or not a containing component was found.
         */
        private boolean findSelectedDE(Coordinate loc, int x, int y) {
            boolean selectedDEFound = true;

            if (getDrawingLayer().getSelectedDE() == null) {
                AbstractDrawableComponent nadc = null;

                /*
                 * Retrieve a list of drawables which contain this mouse click
                 * point
                 */
                List<AbstractDrawableComponent> containingComponentsList = getDrawingLayer()
                        .getContainingComponents(loc, x, y);

                /*
                 * Retrieve the currently selected hazard shape
                 */
                AbstractDrawableComponent selectedElement = getDrawingLayer()
                        .getSelectedHazardIHISLayer();

                if (selectedElement != null) {
                    String selectedElementEventID = getDrawingLayer()
                            .elementClicked(selectedElement, false, false);

                    /*
                     * If there is more than one containing component, make sure
                     * that the topmost element is equal to the selected
                     * element. If there are symbols (including points), give
                     * them precedence.
                     */
                    for (AbstractDrawableComponent comp : containingComponentsList) {
                        if (comp instanceof HazardServicesSymbol) {
                            String containingComponentEventID = getDrawingLayer()
                                    .elementClicked(comp, false, false);

                            if (containingComponentEventID
                                    .equals(selectedElementEventID)) {
                                nadc = comp;
                                break;
                            }
                        }

                    }

                    if (nadc == null && containingComponentsList.size() > 0) {
                        AbstractDrawableComponent comp = containingComponentsList
                                .get(0);
                        String containingComponentEventID = getDrawingLayer()
                                .elementClicked(comp, false, false);

                        if (containingComponentEventID
                                .equals(selectedElementEventID)) {
                            nadc = comp;
                        }
                    }
                }

                if (nadc == null) {
                    /*
                     * We need a better way of determining which containing
                     * component of multiple components to chose as the
                     * selected.
                     */
                    if (containingComponentsList.size() > 0) {
                        nadc = containingComponentsList.get(0);
                    }
                }

                if (nadc != null) {
                    getDrawingLayer().setSelectedDE(nadc);
                    allowPanning = false;
                } else {
                    // Pass this event on.
                    allowPanning = true;
                    selectedDEFound = false;
                }
            }

            return selectedDEFound;
        }

        @Override
        public boolean handleMouseUp(int x, int y, int button) {
            /*
             * Button 2 functionality for adding/deleting vertices... This
             * mimics WarnGen.
             */
            if (button == 2) {
                if (moveType == MoveType.SINGLE_POINT) {
                    deleteNode();
                } else {
                    addNode();
                }
            } else if (isVertexMove) {
                // Was this part of a vertex move operation?
                getDrawingLayer().setSelectedDE(null);

                isVertexMove = false;

                AbstractDrawableComponent selectedElement = getDrawingLayer()
                        .getSelectedHazardIHISLayer();

                if ((selectedElement != null)
                        && (selectedElement instanceof IHazardServicesShape)) {
                    IHazardServicesShape eventShape = (IHazardServicesShape) selectedElement;
                    String jsonString = JSONUtilities.createModifiedHazardJSON(
                            eventShape, getShapesForEvent(eventShape.getID()),
                            selectedElement.getPoints());
                    getDrawingLayer().notifyModifiedEvent(jsonString);
                }

            }

            if (ghostEl != null) {
                DrawableElement selectedDE = getDrawingLayer().getSelectedDE();
                if (selectedDE != null) {
                    IHazardServicesShape origShape = (IHazardServicesShape) selectedDE;
                    Class<?> selectedDEclass = selectedDE.getClass();
                    List<Coordinate> coords = null;
                    if (selectedDEclass.equals(HazardServicesSymbol.class)
                            || selectedDEclass
                                    .equals(HazardServicesPoint.class)) {
                        coords = Lists.newArrayList(((Symbol) ghostEl)
                                .getLocation());
                    } else if ((selectedDEclass
                            .equals(HazardServicesPolygon.class))
                            || (selectedDEclass
                                    .equals(HazardServicesLine.class))) {
                        coords = ((Line) ghostEl).getPoints();
                    }
                    String jsonString = JSONUtilities.createModifiedHazardJSON(
                            origShape, getShapesForEvent(origShape.getID()),
                            coords);
                    getDrawingLayer().notifyModifiedEvent(jsonString);
                }
            } else if (!allowPanning) {
                /*
                 * Treat this has a hazard selection.
                 */
                getDrawingLayer().elementClicked(
                        getDrawingLayer().getSelectedDE(),
                        shiftKeyIsDown || ctrlKeyIsDown, true);
            }

            getDrawingLayer().removeGhostLine();

            getDrawingLayer().setSelectedDE(null);
            ghostEl = null;

            getDrawingLayer().issueRefresh();

            movePointIndex = -1;
            moveType = null;

            return true;

        }

        @Override
        public boolean handleMouseDownMove(int anX, int aY, int button) {
            /*
             * Retrieve a list of drawables if any which contain the mouse click
             * point
             */
            final AbstractEditor editor = ((AbstractEditor) VizWorkbenchManager
                    .getInstance().getActiveEditor());

            Coordinate loc = editor.translateClick(anX, aY);
            List<AbstractDrawableComponent> containingComponentsList = getDrawingLayer()
                    .getContainingComponents(loc, anX, aY);

            /*
             * Check if the user is holding down the Ctrl or Shift keys with the
             * mouse pointer outside of a displayed hazard geometry. If so,
             * treat this as a multiselection action.
             */
            if (containingComponentsList == null
                    || containingComponentsList.size() == 0) {

                if (shiftKeyIsDown) {
                    getSpatialPresenter()
                            .getView()
                            .setMouseHandler(
                                    HazardServicesMouseHandlers.RECTANGLE_MULTI_SELECTION,
                                    new String[] {});
                    return true;

                } else if (ctrlKeyIsDown) {
                    getSpatialPresenter()
                            .getView()
                            .setMouseHandler(
                                    HazardServicesMouseHandlers.FREE_HAND_MULTI_SELECTION,
                                    new String[] {});
                    return true;

                }
            }
            /*
             * Check to see if the user is moving a point in a hazard polygon...
             */
            if ((moveType != null) && (moveType == MoveType.SINGLE_POINT)
                    && (button == 1)) {
                AbstractDrawableComponent selectedElement = getDrawingLayer()
                        .getSelectedHazardIHISLayer();

                if ((selectedElement != null) && (movePointIndex >= 0)) {
                    isVertexMove = true;

                    /*
                     * Replace the previous coordinate with the new one. If this
                     * is a polygon, the last point must be the same as the
                     * first, so ensure that this is the case if the first point
                     * is the one being moved. (The last point is never the one
                     * being moved for poly- gons.)
                     */
                    List<Coordinate> coords = selectedElement.getPoints();
                    loc = editor.translateClick(anX, aY);
                    coords.set(movePointIndex, loc);
                    if (selectedElement.getClass().equals(
                            HazardServicesPolygon.class)
                            && (movePointIndex == 0)) {
                        coords.set(coords.size() - 1, loc);
                    }

                    /*
                     * The shape's coords are updated...
                     */
                    ghostEl = selectedElement.copy();
                    getDrawingLayer().setGhostLine(ghostEl);
                    getDrawingLayer().issueRefresh();
                }

                return true;
            }

            /*
             * Are we trying to move something that is not currently selected?
             * Then allow panning.
             */
            AbstractDrawableComponent selectedComponent = getDrawingLayer()
                    .getSelectedDE();

            if (selectedComponent != null
                    && (selectedComponent != getDrawingLayer()
                            .getSelectedHazardIHISLayer())
                    && !(selectedComponent instanceof HazardServicesSymbol)) {
                allowPanning = true;
            }

            if (!allowPanning || moveType == MoveType.ALL_POINTS) {
                return super.handleMouseDownMove(anX, aY, button);
            } else {
                return false;
            }
        }

        /**
         * This checks the mouse pointer to see if it is over the selected
         * drawable element. If it is, then the mouse cursor sets itself to a
         * move icon. If the mouse is over a vertex in the polygon, then the
         * cursor turns into a hand.
         */
        @Override
        public boolean handleMouseMove(int x, int y) {
            AbstractEditor editor = ((AbstractEditor) VizWorkbenchManager
                    .getInstance().getActiveEditor());

            editor.getActiveDisplayPane().setFocus();

            if (moveType == MoveType.ALL_POINTS) {
                handleMouseDownMove(x, y, 1);
            } else {

                // Need to check here if the user selected a move mode from the
                // right-click context menu. If so, then call
                // handleMouseDownMove().
                // Treat this as if the user is holding down the mouse button

                // AbstractEditor editor = ((AbstractEditor) VizWorkbenchManager
                // .getInstance().getActiveEditor());
                Coordinate loc = editor.translateClick(x, y);
                moveType = null;

                if (loc != null) {
                    AbstractDrawableComponent selectedElement = getDrawingLayer()
                            .getSelectedHazardIHISLayer();

                    if (selectedElement != null) {
                        String selectedElementEventID = getDrawingLayer()
                                .elementClicked(selectedElement, false, false);

                        AbstractDrawableComponent nadc = null;
                        // First try to find a component that completely
                        // contains
                        // the click point. There could be several of these.
                        List<AbstractDrawableComponent> containingComponentList = getDrawingLayer()
                                .getContainingComponents(loc, x, y);

                        for (AbstractDrawableComponent comp : containingComponentList) {
                            String containingComponentEventID = getDrawingLayer()
                                    .elementClicked(comp, false, false);

                            if (containingComponentEventID
                                    .equals(selectedElementEventID)) {
                                /*
                                 * Since there may be multiple geometries
                                 * associated with an event ID, make sure that
                                 * the selectedElement in the tool layer
                                 * reflects the correct geometry.
                                 */
                                if (!comp.equals(selectedElement)) {
                                    getDrawingLayer()
                                            .setSelectedHazardIHISLayer(comp);
                                }
                                /*
                                 * Need a bit more logic here. Just because two
                                 * components have the same event id doesn't
                                 * make them equal.
                                 */
                                if (comp.equals(selectedElement)) {
                                    nadc = comp;
                                    break;
                                }
                            }
                        }

                        if (nadc == null) {
                            // There is no containing element.
                            // Try to find the closest element...
                            AbstractDrawableComponent comp = getDrawingLayer()
                                    .getNearestComponent(loc);

                            if (comp != null) {

                                String containingComponentEventID = getDrawingLayer()
                                        .elementClicked(comp, false, false);

                                if (containingComponentEventID
                                        .equals(selectedElementEventID)) {
                                    nadc = comp;
                                }
                            }
                        }

                        if (nadc != null) {
                            // Set the mouse cursor to a move symbol
                            getSpatialPresenter().getView().setCursor(
                                    SpatialViewCursorTypes.MOVE_SHAPE_CURSOR);

                            // Determine if the vertices on this shape
                            // may be edited.
                            LineString editableVertices = ((IHazardServicesShape) nadc)
                                    .getEditableVertices();

                            if (editableVertices != null) {
                                // Set the flag indicating that the handle
                                // bars on the selected polygon may be
                                // displayed.
                                getDrawingLayer().setDrawSelectedHandleBars(
                                        true);

                                // Test to determine if the mouse is close to
                                // the border of the geometry.
                                Coordinate mouseScreenCoord = new Coordinate(x,
                                        y);

                                GeometryFactory gf = editableVertices
                                        .getFactory();

                                Point clickPointScreen = gf
                                        .createPoint(mouseScreenCoord);

                                // Create a line string with screen coordinates.
                                Coordinate[] shapeCoords = editableVertices
                                        .getCoordinates();

                                Coordinate[] shapeScreenCoords = new Coordinate[shapeCoords.length];

                                for (int i = 0; i < shapeCoords.length; ++i) {
                                    double[] coords = editor
                                            .translateInverseClick(shapeCoords[i]);
                                    shapeScreenCoords[i] = new Coordinate(
                                            coords[0], coords[1]);
                                }

                                LineString ls2 = gf
                                        .createLineString(shapeScreenCoords);

                                double dist = clickPointScreen.distance(ls2);

                                if (dist <= SELECTION_DISTANCE_PIXELS) {
                                    getSpatialPresenter().getView().setCursor(
                                            SpatialViewCursorTypes.DRAW_CURSOR);
                                }

                                // Test to determine if the mouse is close to
                                // one of the hazard's vertices...
                                Coordinate coords[] = selectedElement
                                        .getPoints().toArray(new Coordinate[4]);

                                double minDistance = Double.MAX_VALUE;

                                // Convert to screen coords (pixels)
                                for (int i = 0; i < coords.length
                                        && coords[i] != null; ++i) {
                                    double[] screen = editor
                                            .translateInverseClick(coords[i]);
                                    Coordinate vertexScreenCoord = new Coordinate(
                                            screen[0], screen[1]);

                                    dist = mouseScreenCoord
                                            .distance(vertexScreenCoord);

                                    if (dist <= SELECTION_DISTANCE_PIXELS) {
                                        if (dist < minDistance) {
                                            getSpatialPresenter()
                                                    .getView()
                                                    .setCursor(
                                                            SpatialViewCursorTypes.MOVE_NODE_CURSOR);
                                            moveType = MoveType.SINGLE_POINT;
                                            movePointIndex = i;
                                            minDistance = dist;
                                            break;
                                        }
                                    }
                                }
                            }
                        } else {
                            getDrawingLayer().setDrawSelectedHandleBars(false);

                            getSpatialPresenter().getView().setCursor(
                                    SpatialViewCursorTypes.ARROW_CURSOR);
                        }

                    } else {
                        getDrawingLayer().setDrawSelectedHandleBars(false);
                        getSpatialPresenter().getView().setCursor(
                                SpatialViewCursorTypes.ARROW_CURSOR);
                    }
                }
            }

            return false;
        }

        /**
         * Add a new node to a selected geometry.
         */
        public void addNode() {
            AbstractEditor editor = ((AbstractEditor) VizWorkbenchManager
                    .getInstance().getActiveEditor());
            AbstractDrawableComponent selectedElement = getDrawingLayer()
                    .getSelectedHazardIHISLayer();

            if (selectedElement != null) {
                int lastMouseX = editor.getActiveDisplayPane().getLastMouseX();
                int lastMouseY = editor.getActiveDisplayPane().getLastMouseY();

                Coordinate clickScreenCoord = new Coordinate(lastMouseX,
                        lastMouseY);

                /*
                 * Try using the geometry associated with the drawable.
                 */
                LineString ls = ((IHazardServicesShape) selectedElement)
                        .getEditableVertices();
                Coordinate[] coords = ls.getCoordinates();

                int numCoordPoints = coords.length;

                /*
                 * The last coordinate may sometimes be equal to the first
                 * coordinate. In this case, do not consider the last
                 * coordinate.
                 */
                if (coords[0].equals(coords[numCoordPoints - 1])) {
                    --numCoordPoints;
                }

                double minDistance = Double.MAX_VALUE;
                int minPosition = Integer.MIN_VALUE;

                Coordinate coLinearCoord = null;

                for (int i = 1; i <= numCoordPoints; i++) {
                    double screenCoords[] = editor
                            .translateInverseClick(coords[i - 1]);
                    Coordinate screenCoordA = new Coordinate(screenCoords[0],
                            screenCoords[1]);

                    if (i < numCoordPoints) {
                        screenCoords = editor.translateInverseClick(coords[i]);
                    } else {
                        screenCoords = editor.translateInverseClick(coords[0]);
                    }

                    Coordinate screenCoordB = new Coordinate(screenCoords[0],
                            screenCoords[1]);
                    LineSegment lineSegment = new LineSegment(screenCoordA,
                            screenCoordB);

                    double dist = lineSegment.distance(clickScreenCoord);

                    if (dist <= SELECTION_DISTANCE_PIXELS) {
                        if (dist < minDistance) {
                            minDistance = dist;
                            minPosition = i;

                            coLinearCoord = lineSegment
                                    .closestPoint(clickScreenCoord);
                        }
                    }
                }

                if (coLinearCoord != null) {
                    Coordinate[] coords2 = new Coordinate[numCoordPoints + 1];

                    int k = 0;

                    for (k = 0; k < minPosition; k++) {
                        coords2[k] = coords[k];
                    }

                    // The vertex being added.
                    coords2[k] = editor.translateClick(coLinearCoord.x,
                            coLinearCoord.y);

                    for (k += 1; k < coords2.length; k++) {
                        coords2[k] = coords[k - 1];
                    }

                    // Build a json message with the new points.
                    IHazardServicesShape eventShape = (IHazardServicesShape) selectedElement;
                    String jsonString = JSONUtilities.createModifiedHazardJSON(
                            eventShape, getShapesForEvent(eventShape.getID()),
                            Arrays.asList(coords2));

                    getDrawingLayer().notifyModifiedEvent(jsonString);

                    movePointIndex = -1;
                    moveType = null;

                }
            }
        }

        /**
         * Delete a node from a selected geometry.
         */
        public void deleteNode() {
            AbstractDrawableComponent selectedElement = getDrawingLayer()
                    .getSelectedHazardIHISLayer();

            if ((selectedElement != null)
                    && (moveType != null)
                    && (moveType == MoveType.SINGLE_POINT)
                    && (movePointIndex >= 0)
                    && (((IHazardServicesShape) selectedElement)
                            .getEditableVertices() != null)) {
                List<Coordinate> coords = selectedElement.getPoints();

                // For now, make sure there are at least three points left for
                // paths, or four points for polygons (since the latter need
                // to have the last point be identical to the first point).
                boolean isPolygon = selectedElement.getClass().equals(
                        HazardServicesPolygon.class);
                if (coords.size() > (isPolygon ? 4 : 3)) {
                    coords.remove(movePointIndex);

                    // For polygons, the last point has to be the same as the
                    // first point, so ensure that this is the case.
                    if (isPolygon && (movePointIndex == 0)) {
                        coords.set(coords.size() - 1, coords.get(0));
                    }

                    IHazardServicesShape eventShape = (IHazardServicesShape) selectedElement;
                    String jsonString = JSONUtilities.createModifiedHazardJSON(
                            eventShape, getShapesForEvent(eventShape.getID()),
                            selectedElement.getPoints());
                    getDrawingLayer().notifyModifiedEvent(jsonString);

                    movePointIndex = -1;
                    moveType = null;
                }
            }
        }

        /**
         * Moves the entire hazard element.
         * 
         * @param
         * @return
         */
        public void setMoveEntireElement() {
            moveType = MoveType.ALL_POINTS;
            AbstractDrawableComponent selectedElement = getDrawingLayer()
                    .getSelectedHazardIHISLayer();
            getDrawingLayer().setSelectedDE(selectedElement);
        }

        /**
         * Returns a list of hazard shapes for a given eventID.
         * 
         * @param eventID
         *            The event identifier. On event can have multiple shapes.
         * @return A list of shapes belonging to the hazard with the provided
         *         event identifier that may be modified.
         */
        private List<IHazardServicesShape> getShapesForEvent(String eventID) {
            List<IHazardServicesShape> shapes = Lists.newArrayList();

            List<AbstractDrawableComponent> hazards = getDrawingLayer()
                    .getDataManager().getActiveLayer().getDrawables();

            if (hazards != null) {
                for (AbstractDrawableComponent hazard : hazards) {
                    Class<?> hazardClass = hazard.getClass();

                    if (((hazardClass.equals(HazardServicesLine.class))
                            || (hazardClass.equals(HazardServicesPolygon.class))
                            || (hazardClass.equals(HazardServicesPoint.class) && ((HazardServicesPoint) hazard)
                                    .isOuter() == false) || (hazardClass
                                .equals(HazardServicesSymbol.class)))
                            && ((IHazardServicesShape) hazard).getID().equals(
                                    eventID)) {
                        shapes.add((IHazardServicesShape) hazard);
                    }
                }
            }

            return shapes;
        }

    }
}