/**
 * This software was developed and / or modified by the National Oceanic and
 * Atmospheric Administration (NOAA), Earth System Research Laboratory (ESRL),
 * Global Systems Division (GSD), Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.hazards.spatialdisplay.mousehandlers;

import gov.noaa.gsd.viz.hazards.display.action.SpatialDisplayAction;
import gov.noaa.gsd.viz.hazards.jsonutilities.Dict;
import gov.noaa.gsd.viz.hazards.jsonutilities.EventDict;
import gov.noaa.gsd.viz.hazards.jsonutilities.Polygon;
import gov.noaa.gsd.viz.hazards.spatialdisplay.SpatialPresenter;
import gov.noaa.gsd.viz.hazards.spatialdisplay.selectbyarea.SelectByAreaDbMapResource;
import gov.noaa.gsd.viz.hazards.utilities.Utilities;
import gov.noaa.nws.ncep.ui.pgen.tools.InputHandlerDefaultImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.viz.ui.VizWorkbenchManager;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
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
public class SelectByAreaDrawingActionGeometryResource {
    /*
     * This map will keep track of the geometries for each hazard that is
     * created by the select-by-area drawing tool.
     */
    private static Map<String, List<Geometry>> hazardGeometryList;

    private enum Mode {
        CREATE, ADD_TO_ZONE, REMOVE, NONE
    };

    private static SelectByAreaDrawingActionGeometryResource selectByAreaDrawingAction = null;

    private static String eventID = null;

    private static boolean modifyingEvent = false;

    /** The mouse handler */
    protected IInputHandler theHandler;

    /** The displayed resource */
    protected SelectByAreaDbMapResource zoneDisplay;

    /**
     * The message handler
     */
    private SpatialPresenter spatialPresenter;

    /**
     * Call this function to retrieve an instance of the EventBoxDrawingAction.
     * 
     * @param zoneDisplay
     * @return SelectByAreaDrawingActionGeometryResource
     */
    public static SelectByAreaDrawingActionGeometryResource getInstance(
            SelectByAreaDbMapResource zoneDisplay,
            SpatialPresenter spatialPresenter) {
        modifyingEvent = false;

        if (selectByAreaDrawingAction == null) {
            selectByAreaDrawingAction = new SelectByAreaDrawingActionGeometryResource(
                    zoneDisplay, spatialPresenter);
        } else {
            selectByAreaDrawingAction.setDrawingLayer(zoneDisplay);
            selectByAreaDrawingAction.setMessageHandler(spatialPresenter);
        }

        return selectByAreaDrawingAction;

    }

    public static SelectByAreaDrawingActionGeometryResource getInstance(
            SelectByAreaDbMapResource zoneDisplay,
            SpatialPresenter spatialPresenter, String eventID) {

        SelectByAreaDrawingActionGeometryResource.getInstance(zoneDisplay,
                spatialPresenter);
        modifyingEvent = true;
        SelectByAreaDrawingActionGeometryResource.eventID = eventID;

        return selectByAreaDrawingAction;
    }

    private void setDrawingLayer(SelectByAreaDbMapResource zoneDisplay) {
        this.zoneDisplay = zoneDisplay;
    }

    private SelectByAreaDrawingActionGeometryResource(
            SelectByAreaDbMapResource zoneDisplay,
            SpatialPresenter spatialPresenter) {
        super();
        this.zoneDisplay = zoneDisplay;
        this.spatialPresenter = spatialPresenter;
        hazardGeometryList = new HashMap<String, List<Geometry>>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.gsd.viz.drawing.AbstractDrawingTool#getMouseHandler()
     */
    public IInputHandler getMouseHandler() {
        if (theHandler == null) {
            theHandler = new SelectionHandler();
        }

        /*
         * We need to initialize the handler if an existing select-by-area
         * object is being edited.
         */
        if (modifyingEvent) {
            ((SelectionHandler) theHandler).setSelectedGeoms(eventID);
        } else {
            /*
             * Clear any cached geometries
             */
            ((SelectionHandler) theHandler).selectedGeoms.clear();
        }

        return theHandler;
    }

    /**
     * @param spatialPresenter
     *            the messageHandler to set
     */
    public void setMessageHandler(SpatialPresenter spatialPresenter) {
        this.spatialPresenter = spatialPresenter;
    }

    public class SelectionHandler extends InputHandlerDefaultImpl {

        private final Geometry mouseDownGeometry = null;

        private Geometry selectedGeometry = null;

        private List<Geometry> selectedGeoms = new ArrayList<Geometry>();

        private Mode mode = Mode.CREATE;

        /*
         * (non-Javadoc)
         * 
         * @see com.raytheon.viz.ui.input.IInputHandler#handleMouseDown(int,
         * int, int)
         */
        @Override
        public boolean handleMouseDown(int x, int y, int mouseButton) {
            if (mouseButton == 1) {
                AbstractEditor editor = (AbstractEditor) VizWorkbenchManager
                        .getInstance().getActiveEditor();

                /*
                 * if ( !modifyingEvent ) { selectedGeoms.clear(); // clear the
                 * list.. }
                 */

                Coordinate c = editor.translateClick(x, y);

                selectedGeometry = zoneDisplay.clickOnExistingGeometry(c);

                if (selectedGeometry != null) {
                    /*
                     * Having trouble with the ArrayList contains method,
                     * although according to the java doc it should work. This
                     * is because the Geometry class does not properly override
                     * the Object equals method.
                     */

                    if (!isContainedInSelectedGeometries(selectedGeometry)) {
                        mode = Mode.CREATE;
                        selectedGeoms.add(selectedGeometry);
                    } else {
                        selectedGeoms.remove(selectedGeometry);
                    }

                    zoneDisplay.setSelectedGeometries(selectedGeoms);
                    editor.refresh();
                } else {
                    mode = Mode.NONE;
                }

                return false;
            }
            return true;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.raytheon.viz.ui.input.IInputHandler#handleMouseDownMove(int,
         * int, int)
         */
        @Override
        public boolean handleMouseDownMove(int x, int y, int mouseButton) {
            AbstractEditor editor = (AbstractEditor) VizWorkbenchManager
                    .getInstance().getActiveEditor();
            Coordinate c = editor.translateClick(x, y);
            Geometry g = null;

            g = zoneDisplay.clickOnExistingGeometry(c);

            if (g != null && !isContainedInSelectedGeometries(g)) {
                selectedGeoms.add(g);
            }

            // Tell the resource to update its display of
            // the selected geometries.
            zoneDisplay.setSelectedGeometries(selectedGeoms);
            editor.refresh();

            return true;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.raytheon.viz.ui.input.IInputHandler#handleMouseUp(int, int,
         * int)
         */
        @Override
        public boolean handleMouseUp(int x, int y, int mouseButton) {

            if (mouseButton == 1) {
                AbstractEditor editor = (AbstractEditor) VizWorkbenchManager
                        .getInstance().getActiveEditor();
                Coordinate c = editor.translateClick(x, y);
                Geometry g = null;

                g = zoneDisplay.clickOnExistingGeometry(c);

                if (g != null && mouseDownGeometry != null) {
                    if (g.equals(mouseDownGeometry)) {
                        if (mode.equals(Mode.CREATE)) {
                        } else {
                            zoneDisplay.setSelectedGeometries(selectedGeoms);
                        }
                    }
                }

                editor.refresh();
                return false;
            } else {
                // Unload the ZoneDbResource??

                // Send off the selected geometries.
                if (selectedGeoms != null && selectedGeoms.size() > 0) {
                    // Try this polygon merging technique instead...
                    GeometryFactory geoFactory = selectedGeoms.get(0)
                            .getFactory();
                    Geometry geomColl = geoFactory
                            .createGeometryCollection(selectedGeoms
                                    .toArray(new Geometry[1]));
                    Geometry mergedPolygons = geomColl.buffer(0.001);

                    // Geometry mergedPolygons = geomColl.convexHull();
                    mergedPolygons = TopologyPreservingSimplifier.simplify(
                            mergedPolygons, 0.0001);

                    Polygon polygon = new Polygon("", "true", "true", "true",
                            "White", 2, "SOLID", "White",
                            mergedPolygons.getCoordinates());

                    if (!modifyingEvent) {
                        // Request an event ID for this newly drawn polygon.

                        // Send off JSON Message.
                        EventDict eventAreaObject = new EventDict();
                        eventAreaObject.put("eventId", "");
                        eventAreaObject.addShape(polygon);

                        /*
                         * Clone the list of selected geometries.
                         */
                        List<Geometry> copyGeometriesList = new ArrayList<Geometry>();

                        for (Geometry geometry : selectedGeoms) {
                            copyGeometriesList.add((Geometry) geometry.clone());
                        }

                        selectedGeoms.clear();

                        // Tell the resource to update its display of
                        // the selected geometries.
                        zoneDisplay.setSelectedGeometries(selectedGeoms);
                        String eventAreaJSON = eventAreaObject.toJSONString();
                        eventID = spatialPresenter
                                .getNewEventAreaId(eventAreaJSON);

                        SpatialDisplayAction action = new SpatialDisplayAction(
                                "newEventArea");
                        action.setJSON(eventAreaJSON);
                        action.setEventID(eventID);
                        spatialPresenter.fireAction(action);

                        hazardGeometryList.put(eventID, copyGeometriesList);

                        /*
                         * Store the geometry table that this hazard was
                         * originally based on in the eventDict as well.
                         */
                        String geometryTable = zoneDisplay.getResourceData()
                                .getTable();
                        String geometryLegend = zoneDisplay.getResourceData()
                                .getMapName();
                        Dict geoReferenceDict = new Dict();
                        geoReferenceDict.put(Utilities.HAZARD_EVENT_IDENTIFIER,
                                eventID);
                        geoReferenceDict
                                .put("geometryReference", geometryTable);
                        geoReferenceDict.put("geometryMapName", geometryLegend);
                        Dict contextMenuDict = new Dict();
                        contextMenuDict.put("Add/Remove Shapes", "");
                        geoReferenceDict.put("contextMenu", contextMenuDict);
                        action = new SpatialDisplayAction("updateEventData");
                        action.setJSON(geoReferenceDict.toJSONString());
                        spatialPresenter.fireAction(action);
                    } else {
                        /*
                         * Construct a modified event message and pass it on to
                         * the mediator.
                         */
                        // Send JSON Message
                        // Convert the object to JSON.
                        EventDict modifiedEventAreaObject = new EventDict();
                        modifiedEventAreaObject.put(
                                Utilities.HAZARD_EVENT_IDENTIFIER, eventID);
                        modifiedEventAreaObject.put(
                                Utilities.HAZARD_EVENT_SHAPE_TYPE,
                                Utilities.HAZARD_EVENT_SHAPE_TYPE_POLYGON);
                        modifiedEventAreaObject.addShape(polygon);

                        SpatialDisplayAction action = new SpatialDisplayAction(
                                "ModifyEventArea");
                        action.setModifyEventJSON(modifiedEventAreaObject
                                .toJSONString());
                        spatialPresenter.fireAction(action);

                    }

                    // Let the IHIS layer know that this drawing
                    // action is complete.
                    spatialPresenter.getView().drawingActionComplete();

                }

                return false;
            }
        }

        /**
         * Sets the initial geometry to start with. This would be used in the
         * case that an existing geometry is being edited.
         * 
         * @param polygon
         */
        public void setSelectedGeoms(String eventID) {
            /*
             * Ask the selectable geometry display for a list of its polygons
             * contained within this polygon.
             */
            List<Geometry> geometries = hazardGeometryList.get(eventID);
            zoneDisplay.setSelectedGeometries(geometries);
            selectedGeoms = geometries;
        }

        private boolean isContainedInSelectedGeometries(
                Geometry selectedGeometry) {

            for (int i = 0; i < selectedGeoms.size(); ++i) {
                if (selectedGeoms.get(i).equals(selectedGeometry)) {
                    selectedGeoms.set(i, selectedGeometry);
                    return true;
                }
            }

            return false;

        }

    }
}