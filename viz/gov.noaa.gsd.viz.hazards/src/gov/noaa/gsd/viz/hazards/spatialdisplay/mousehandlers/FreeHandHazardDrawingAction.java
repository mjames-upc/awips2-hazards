/**
 * This software was developed and / or modified by the National Oceanic and
 * Atmospheric Administration (NOAA), Earth System Research Laboratory (ESRL),
 * Global Systems Division (GSD), Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.hazards.spatialdisplay.mousehandlers;

import gov.noaa.gsd.viz.hazards.spatialdisplay.PolygonDrawingAttributes;
import gov.noaa.gsd.viz.hazards.spatialdisplay.SpatialView.SpatialViewCursorTypes;
import gov.noaa.nws.ncep.ui.pgen.display.ILine;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElementFactory;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableType;
import gov.noaa.nws.ncep.ui.pgen.elements.Line;
import gov.noaa.nws.ncep.ui.pgen.tools.InputHandlerDefaultImpl;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Event;

import com.google.common.collect.Lists;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.uf.viz.hazards.sessionmanager.ISessionManager;
import com.raytheon.uf.viz.hazards.sessionmanager.config.impl.ObservedSettings;
import com.raytheon.uf.viz.hazards.sessionmanager.events.impl.ObservedHazardEvent;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.VizWorkbenchManager;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * Rect drawing action(refer to RT collaboration)
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 04, 2013            Xiangbao Jing      Initial induction into repo
 * Jul 15, 2013      585   Chris.Golden       Changed to no longer be a singleton.
 * Aug  9, 2013 1921       daniel.s.schaffer@noaa.gov  Support of replacement of JSON with POJOs
 * Nov  04, 2013 2182     daniel.s.schaffer@noaa.gov      Started refactoring
 * Nov 23, 2013     1462   bryon.lawrence     Changed polygons to be drawn without fill by default.
 * Sep 09, 2014 3994       Robert.Blum        Added handleMouseEnter to reset the cursor type.
 * Dec 05, 2014 4124       Chris.Golden       Changed to work with newly parameterized
 *                                            config manager.
 * Dec 13, 2014 4959       Dan Schaffer Spatial Display cleanup and other bug fixes
 * </pre>
 * 
 * @author Xiangbao Jing
 */
public class FreeHandHazardDrawingAction extends AbstractMouseHandler {

    /**
     * Logging mechanism.
     */
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(FreeHandHazardDrawingAction.class);

    protected ILine freeLine = null;

    public FreeHandHazardDrawingAction(
            ISessionManager<ObservedHazardEvent, ObservedSettings> sessionManager) {

        /*
         * Create the attribute container.
         */
        try {
            freeLine = new PolygonDrawingAttributes(false, sessionManager);
        } catch (VizException e) {
            statusHandler.error("FreeHandHazardDrawingAction.<init>: Creation "
                    + "of polygon drawing attributes failed.", e);
        }
    }

    @Override
    protected IInputHandler createMouseHandler() {
        return new FreeHandHazardDrawingHandler();
    }

    public class FreeHandHazardDrawingHandler extends InputHandlerDefaultImpl {
        private final ArrayList<Coordinate> points = new ArrayList<>();

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

        // Needed to override this to prevent
        // it from being passed on to CAVE panning routines.
        @Override
        public boolean handleMouseUp(int x, int y, int mouseButton) {
            // Finishes the editing action, if one has
            // been initiated.
            AbstractEditor editor = EditorUtil
                    .getActiveEditorAs(AbstractEditor.class);
            Coordinate loc = editor.translateClick(x, y);

            if (mouseButton != 1 || loc == null) {
                return false;
            }

            getSpatialDisplay().removeGhostLine();

            points.add(loc);

            List<Coordinate> pointsCopy = new ArrayList<Coordinate>(points);
            points.clear();

            // Indicate that this drawing action is done.
            getSpatialPresenter().drawingActionComplete(
                    new ArrayList<Coordinate>(pointsCopy));

            return true;
        }

        // Needed to override this to prevent odd panning
        // behavior when drawing to CAVE
        @Override
        public boolean handleMouseDownMove(int x, int y, int mouseButton) {
            AbstractEditor editor = EditorUtil
                    .getActiveEditorAs(AbstractEditor.class);
            Coordinate loc = editor.translateClick(x, y);

            if ((mouseButton != 1) || (loc == null)) {
                return false;
            }

            if (points != null && points.size() >= 1) {
                points.add(loc);

                // create the ghost element and put it in the drawing layer
                AbstractDrawableComponent ghost = def.create(DrawableType.LINE,
                        freeLine, "Line", "LINE_SOLID", points,
                        getSpatialDisplay().getActiveLayer());

                List<Coordinate> ghostPts = Lists.newArrayList(points);
                ((Line) ghost).setLinePoints(ghostPts);

                getSpatialDisplay().setGhostLine(ghost);
                getSpatialDisplay().issueRefresh();
            }

            return true;
        }

        @Override
        public boolean handleMouseEnter(Event event) {
            getSpatialPresenter().getView().setCursor(
                    SpatialViewCursorTypes.DRAW_CURSOR);
            return handleMouseMove(event.x, event.y);
        }

    }
}
