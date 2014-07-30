/**
 * This software was developed and / or modified by the National Oceanic and
 * Atmospheric Administration (NOAA), Earth System Research Laboratory (ESRL),
 * Global Systems Division (GSD), Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.hazards.spatialdisplay.mousehandlers;

import gov.noaa.gsd.viz.hazards.display.action.SpatialDisplayAction;
import gov.noaa.gsd.viz.hazards.spatialdisplay.SpatialView.SpatialViewCursorTypes;
import gov.noaa.gsd.viz.hazards.utilities.Utilities;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.Line;
import gov.noaa.nws.ncep.ui.pgen.elements.Symbol;

import java.io.Serializable;
import java.util.Map;

import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * Drawing action for the drag me to storm dot.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 04, 2013            Xiangbao Jing     Initial induction into repo
 * Jul 15, 2013    585     Chris.Golden      Changed to support loading from bundle
 *                                           and to no longer be a singleton.
 * Aug 21, 2013 1921       daniel.s.schaffer@noaa.gov  Call recommender framework directly
 * Sep 05, 2013 1264       blawrenc          Removed getInstance method. It is not used.
 * </pre>
 * 
 * @author Xiangbao Jing
 */
public class DragDropAction extends NonDrawingAction {

    private String toolName = null;

    @Override
    protected IInputHandler createMouseHandler() {
        return new MoveHandler();
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolName() {
        return toolName;
    }

    public class MoveHandler extends NonDrawingAction.NonDrawingHandler {
        /*
         * (non-Javadoc)
         * 
         * @see com.raytheon.viz.ui.input.IInputHandler#handleMouseUp(int, int,
         * int)
         */
        @Override
        public boolean handleMouseUp(int x, int y, int button) {

            if (ghostEl != null) {
                Coordinate coord;

                if (ghostEl instanceof Line) {
                    Line dot = (Line) ghostEl;
                    coord = dot.getCentroid();
                } else if (ghostEl instanceof Symbol) {
                    Symbol symbol = (Symbol) ghostEl;
                    coord = symbol.getLocation();
                } else {
                    return false;
                }

                long selectedTime = getSpatialPresenter().getSelectedTime()
                        .getTime();

                selectedTime /= 1000;

                Map<String, Serializable> toolParameters = Utilities
                        .buildStormStrackToolDraggedPointParameters(coord.y,
                                coord.x, selectedTime);

                SpatialDisplayAction action = new SpatialDisplayAction(
                        SpatialDisplayAction.ActionType.RUN_TOOL, toolName,
                        toolParameters);
                getSpatialPresenter().fireAction(action);
                getSpatialPresenter().getView().drawingActionComplete();

                getDrawingLayer().removeGhostLine();
                getDrawingLayer().removeEvent("DragDropDot");
                getDrawingLayer().setSelectedDE(null);
                ghostEl = null;

                // We are done dragging the storm dot. Switch back
                // to the previously used mouse handler.
                getSpatialPresenter().getView().drawingActionComplete();
                getSpatialPresenter().getView().setCursor(
                        SpatialViewCursorTypes.ARROW_CURSOR);

                // Tell the Spatial Display to fire a DMTS message

                getDrawingLayer().issueRefresh();

                return true;
            } else {
                return false;
            }

        }

        @Override
        public boolean handleMouseDown(int anX, int aY, int button) {

            boolean mouseActionHandled = super.handleMouseDown(anX, aY, button);

            AbstractDrawableComponent elSelected = getDrawingLayer()
                    .getSelectedDE();

            if (elSelected != null) {
                return mouseActionHandled;
            } else {
                return false;
            }
        }

        @Override
        public boolean handleMouseDownMove(int anX, int aY, int button) {

            AbstractDrawableComponent elSelected = getDrawingLayer()
                    .getSelectedDE();

            if (elSelected != null) {
                return super.handleMouseDownMove(anX, aY, button);
            } else {
                return false;
            }
        }

    }
}