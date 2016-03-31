/**
 * This software was developed and / or modified by the
 * National Oceanic and Atmospheric Administration (NOAA), 
 * Earth System Research Laboratory (ESRL), 
 * Global Systems Division (GSD), 
 * Evaluation & Decision Support Branch (EDS)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * Graph widget, allowing the display and manipulation of one or more plotted
 * points.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- ------------ --------------------------
 * Mar 30, 2016   15931    Chris.Golden Initial creation.
 * </pre>
 * 
 * @author Chris.Golden
 * @version 1.0
 */
public class Graph extends Canvas {

    // Private Static Constants

    /**
     * Diameter of each point marker in pixels.
     */
    private static final int POINT_DIAMETER = 13;

    /**
     * Vertical buffer between X axis labels, if any, and the X axis itself, in
     * pixels.
     */
    private static final int X_AXIS_LABEL_BUFFER = 8;

    /**
     * Horizontal buffer between Y axis labels, if any, and the Y axis itself,
     * in pixels.
     */
    private static final int Y_AXIS_LABEL_BUFFER = 8;

    // Public Enumerated Types

    /**
     * Source of a configuration change.
     */
    public enum ChangeSource {

        /**
         * Method invocation.
         */
        METHOD_INVOCATION,

        /**
         * User-GUI interaction ongoing.
         */
        USER_GUI_INTERACTION_ONGOING,

        /**
         * User-GUI interaction complete.
         */
        USER_GUI_INTERACTION_COMPLETE
    }

    // Private Variables

    /**
     * Minimum visible X value; this is derived from the smallest X coordinate
     * found in the {@link #plottedPoints} list.
     */
    private int minimumVisibleValueX;

    /**
     * Maximum visible X value; this is derived from the largest X coordinate
     * found in the {@link #plottedPoints} list.
     */
    private int maximumVisibleValueX;

    /**
     * Minimum visible Y value.
     */
    private int minimumVisibleValueY;

    /**
     * Maximum visible Y value.
     */
    private int maximumVisibleValueY;

    /**
     * X interval between vertical hatch lines; if <code>0</code>, no vertical
     * lines are drawn.
     */
    private int intervalHatchX;

    /**
     * Y interval between horizontal hatch lines; if <code>0</code>, no
     * horizontal lines are drawn.
     */
    private int intervalHatchY;

    /**
     * X interval between labels; if <code>0</code>, no labels are drawn on the
     * X axis. Ignored if {@link #intervalHatchX} is <code>0</code>; otherwise,
     * must be a multiple of <code>xHatchInterval</code>.
     */
    private int intervalLabelX;

    /**
     * Y interval between labels; if <code>0</code>, no labels are drawn on the
     * Y axis. Ignored if {@link #intervalHatchY} is <code>0</code>; otherwise,
     * must be a multiple of <code>yHatchInterval</code>.
     */
    private int intervalLabelY;

    /**
     * Suffix to be appended to any labels along the X axis; may be
     * <code>null</code>.
     */
    private String suffixLabelX;

    /**
     * Suffix to be appended to any labels along the Y axis; may be
     * <code>null</code>.
     */
    private String suffixLabelY;

    /**
     * List of plotted points, ordered by X value.
     */
    private final List<PlottedPoint> plottedPoints = new ArrayList<>();

    /**
     * List of colors to be used to color the "rows" in the graph, i.e. the
     * colors will vary going up the Y axis if any are specified. The interval
     * covered by each row will be determined by taking the difference between
     * the maximum and minimum visible Y values and dividing it by the number of
     * colors specified within this list. If the list is empty, the background
     * color will be used for the graph.
     */
    private final List<Color> rowColors = new ArrayList<Color>();

    /**
     * Set of listeners; these receive notifications of visible value range or
     * value changes when the latter occur.
     */
    private final Set<IGraphListener> listeners = new HashSet<>();

    /**
     * Desired width of the widget.
     */
    private int preferredWidth;

    /**
     * Desired height of the widget.
     */
    private int preferredHeight;

    /**
     * Last recorded width of the client area.
     */
    private int lastWidth;

    /**
     * Last recorded height of the client area.
     */
    private int lastHeight;

    /**
     * Number of pixels by which to inset the client area of the widget from the
     * left border.
     */
    private int leftInset;

    /**
     * Number of pixels by which to inset the client area of the widget from the
     * top border.
     */
    private int topInset;

    /**
     * Number of pixels by which to inset the client area of the widget from the
     * right border.
     */
    private int rightInset;

    /**
     * Number of pixels by which to inset the client area of the widget from the
     * bottom border.
     */
    private int bottomInset;

    /**
     * Specifier of the plotted point currently being dragged, or
     * <code>null</code> if none is being dragged.
     */
    private PlottedPoint draggingPoint = null;

    /**
     * Specifier of the plotted point over which the mouse cursor is presently
     * located, or <code>null</code> if the mouse cursor is not over any plotted
     * point.
     */
    private PlottedPoint activePoint = null;

    /**
     * Flag indicating whether or not a determination of which plotted point is
     * active has been scheduled to occur later.
     */
    private boolean determinationOfActivePointScheduled = false;

    /**
     * Maximum width of an X axis label in pixels, given the current range of X
     * values and font, or <code>0</code> if no X axis labels are in use.
     */
    private int xLabelWidth;

    /**
     * Maximum width of a Y axis label in pixels, given the current range of Y
     * values and font, or <code>0</code> if no Y axis labels are in use.
     */
    private int yLabelWidth;

    /**
     * Maximum height of an X label in pixels, given the current font, or
     * <code>0</code> if no X axis labels are in use.
     */
    private int xLabelHeight;

    /**
     * Maximum height of a Y label in pixels, given the current font, or
     * <code>0</code> if no Y axis labels are in use.
     */
    private int yLabelHeight;

    /**
     * Number of pixels per X unit.
     */
    private double pixelsPerUnitX;

    /**
     * Number of pixels per Y unit.
     */
    private double pixelsPerUnitY;

    /**
     * Number of pixels per hatch interval along the X axis.
     */
    private int pixelsPerHatchX;

    /**
     * Number of pixels per hatch interval along the Y axis.
     */
    private int pixelsPerHatchY;

    /**
     * Graph width in pixels, not including the width of labels along the Y axis
     * if any.
     */
    private int graphWidth;

    /**
     * Graph height in pixels, not including the height of labels along the X
     * axis if any.
     */
    private int graphHeight;

    /**
     * List of upper Y boundaries for each color in {@link #rowColors}.
     */
    private final List<Integer> rowColorUpperBounds = new ArrayList<>();

    // Public Constructors

    /**
     * Construct a standard instance.
     * 
     * @param parent
     *            Parent of this widget.
     * @param minimumVisibleValueY
     *            Minimum visible Y value.
     * @param maximumVisibleValueY
     *            Maximum visible Y value.
     */
    public Graph(Composite parent, int minimumVisibleValueY,
            int maximumVisibleValueY) {
        super(parent, SWT.NONE);

        /*
         * Initialize the widget.
         */
        initializeGraph(minimumVisibleValueY, maximumVisibleValueY);
    }

    // Public Methods

    /**
     * Add the specified graph control listener; the latter will be notified of
     * value changes.
     * 
     * @param listener
     *            Listener to be added.
     * @return True if this listener was not already part of the set of
     *         listeners, false otherwise.
     */
    public final boolean addGraphListener(IGraphListener listener) {
        return listeners.add(listener);
    }

    /**
     * Remove the specified graph control listener; if the latter was registered
     * previously via {@link #addGraphListener(IGraphListener)} it will no
     * longer be notified of value changes.
     * 
     * @param listener
     *            Listener to be removed.
     * @return True if the listener was found and removed, otherwise false.
     */
    public final boolean removeGraphListener(IGraphListener listener) {
        return listeners.remove(listener);
    }

    @Override
    public final void setEnabled(boolean enable) {
        super.setEnabled(enable);

        /*
         * End any drag of a point.
         */
        if ((enable == false) && (draggingPoint != null)) {
            plottedPointDragEnded(null);
        }

        /*
         * Ensure that the correct point is active, and redraw the widget.
         */
        determineActivePoint();
        redraw();
    }

    @Override
    public final void setFont(Font font) {

        /*
         * Let the superclass do its work.
         */
        super.setFont(font);

        /*
         * Recalculate the preferred size and the active point, and redraw.
         */
        computePreferredSize(true);
        scheduleDetermineActivePointIfEnabled();
        redraw();
    }

    /**
     * Get the minimum visible Y value.
     * 
     * @return Minimum visible Y value.
     */
    public final int getMinimumVisibleValueY() {
        return minimumVisibleValueY;
    }

    /**
     * Get the maximum visible Y value.
     * 
     * @return Maximum visible Y value.
     */
    public final int getMaximumVisibleValueY() {
        return maximumVisibleValueY;
    }

    /**
     * Set the minimum and maximum visible Y values.
     * 
     * @param minimumValue
     *            New minimum visible Y value; must be less than
     *            <code>maximumValue</code>.
     * @param maximumValue
     *            New maximum visible Y value; must be greater than
     *            <code>minimumValue</code>.
     */
    public final void setVisibleValuesY(int minimumValue, int maximumValue) {

        /*
         * Ensure that the minimum value is less than the maximum value.
         */
        if (minimumValue >= maximumValue) {
            throw new IllegalArgumentException(
                    "minimum value must be less than maximum");
        }

        /*
         * If the boundaries have not changed, do nothing more.
         */
        if ((this.minimumVisibleValueY == minimumValue)
                && (this.maximumVisibleValueY == maximumValue)) {
            return;
        }

        /*
         * Set the new values, recompute the preferred size, make sure the right
         * point is active (if any), and redraw.
         */
        this.minimumVisibleValueY = minimumValue;
        this.maximumVisibleValueY = maximumValue;
        computePreferredSize(true);
        scheduleDetermineActivePointIfEnabled();
        redraw();
    }

    /**
     * Get the X interval between vertical hatch lines.
     * 
     * @return X interval between vertical hatch lines; if <code>0</code>, no
     *         vertical lines are drawn.
     */
    public final int getIntervalHatchX() {
        return intervalHatchX;
    }

    /**
     * Set the X interval between vertical hatch lines.
     * 
     * @param interval
     *            X interval between vertical hatch lines; must be a
     *            non-negative integer. If <code>0</code>, no vertical lines are
     *            drawn.
     */
    public final void setIntervalHatchX(int interval) {
        if (interval < 0) {
            throw new IllegalArgumentException(
                    "hatch interval must be non-negative");
        }

        /*
         * Set the new value, recompute the preferred size, make sure the right
         * point is active (if any), and redraw.
         */
        intervalHatchX = interval;
        computePreferredSize(true);
        scheduleDetermineActivePointIfEnabled();
        redraw();
    }

    /**
     * Get the Y interval between horizontal hatch lines.
     * 
     * @return Y interval between horizontal hatch lines; if <code>0</code>, no
     *         horizontal lines are drawn.
     */
    public final int getIntervalHatchY() {
        return intervalHatchY;
    }

    /**
     * Set the Y interval between horizontal hatch lines.
     * 
     * @param interval
     *            Y interval between horizontal hatch lines; must be a
     *            non-negative integer. If <code>0</code>, no horizontal lines
     *            are drawn.
     */
    public final void setIntervalHatchY(int interval) {
        if (interval < 0) {
            throw new IllegalArgumentException(
                    "hatch interval must be non-negative");
        }

        /*
         * Set the new value, recompute the preferred size, make sure the right
         * point is active (if any), and redraw.
         */
        intervalHatchY = interval;
        computePreferredSize(true);
        scheduleDetermineActivePointIfEnabled();
        redraw();
    }

    /**
     * Get the interval between X axis labels.
     * 
     * @return Interval between X axis labels; if <code>0</code>, no labels are
     *         drawn for the X axis.
     */
    public final int getIntervalLabelX() {
        return intervalLabelX;
    }

    /**
     * Set the interval between X axis labels.
     * 
     * @param interval
     *            Interval between X axis labels. Must be <code>0</code> if
     *            {@link #getHatchIntervalX()} yields <code>0</code>; if the
     *            latter yields a positive integer, then this interval must be a
     *            multiple of that integer. If <code>0</code>, no labels are
     *            drawn on the X axis.
     */
    public final void setIntervalLabelX(int interval) {
        if (((intervalHatchX == 0) && (interval != 0))
                || ((intervalHatchX > 0) && (interval % intervalHatchX != 0))) {
            throw new IllegalArgumentException(
                    "label interval must be 0 if hatch interval on same axis is 0, or multiple of hatch interval otherwise");
        }

        /*
         * Set the new value, recompute the preferred size, make sure the right
         * point is active (if any), and redraw.
         */
        intervalLabelX = interval;
        computePreferredSize(true);
        scheduleDetermineActivePointIfEnabled();
        redraw();
    }

    /**
     * Get the interval between Y axis labels.
     * 
     * @return Interval between Y axis labels; if <code>0</code>, no labels are
     *         drawn for the Y axis.
     */
    public final int getIntervalLabelY() {
        return intervalLabelY;
    }

    /**
     * Set the interval between Y axis labels.
     * 
     * @param interval
     *            Interval between Y axis labels. Must be <code>0</code> if
     *            {@link #getHatchIntervalY()} yields <code>0</code>; if the
     *            latter yields a positive integer, then this interval must be a
     *            multiple of that integer. If <code>0</code>, no labels are
     *            drawn on the Y axis.
     */
    public final void setIntervalLabelY(int interval) {
        if (((intervalHatchY == 0) && (interval != 0))
                || ((intervalHatchY > 0) && (interval % intervalHatchY != 0))) {
            throw new IllegalArgumentException(
                    "label interval must be 0 if hatch interval on same axis is 0, or multiple of hatch interval otherwise");
        }

        /*
         * Set the new value, recompute the preferred size, make sure the right
         * point is active (if any), and redraw.
         */
        intervalLabelY = interval;
        computePreferredSize(true);
        scheduleDetermineActivePointIfEnabled();
        redraw();
    }

    /**
     * Set the hatching and label intervals for both axes.
     * 
     * @param intervalHatchX
     *            X interval between vertical hatch lines; must be a
     *            non-negative integer. If <code>0</code>, no vertical lines are
     *            drawn.
     * @param intervalHatchY
     *            Y interval between horizontal hatch lines; must be a
     *            non-negative integer. If <code>0</code>, no horizontal lines
     *            are drawn.
     * @param intervalLabelX
     *            Interval between X axis labels. Must be <code>0</code> if
     *            <code>intervalHatchX</code> yields <code>0</code>; if the
     *            latter yields a positive integer, then this interval must be a
     *            multiple of that integer. If <code>0</code>, no labels are
     *            drawn on the X axis.
     * @param intervalLabelY
     *            Interval between Y axis labels. Must be <code>0</code> if
     *            <code>intervalHatchY</code> yields <code>0</code>; if the
     *            latter yields a positive integer, then this interval must be a
     *            multiple of that integer. If <code>0</code>, no labels are
     *            drawn on the Y axis.
     */
    public final void setHatchAndLabelIntervals(int intervalHatchX,
            int intervalHatchY, int intervalLabelX, int intervalLabelY) {
        if ((intervalHatchX < 0) || (intervalHatchY < 0)) {
            throw new IllegalArgumentException(
                    "hatch interval must be non-negative");
        } else if ((((intervalHatchX == 0) && (intervalLabelX != 0)) || ((intervalHatchX > 0) && (intervalLabelX
                % intervalHatchX != 0)))
                || (((intervalHatchY == 0) && (intervalLabelY != 0)) || ((intervalHatchY > 0) && (intervalLabelY
                        % intervalHatchY != 0)))) {
            throw new IllegalArgumentException(
                    "label interval must be 0 if hatch interval on same axis is 0, or multiple of hatch interval otherwise");
        }

        /*
         * Set the new values, recompute the preferred size, make sure the right
         * point is active (if any), and redraw.
         */
        this.intervalHatchX = intervalHatchX;
        this.intervalHatchY = intervalHatchY;
        this.intervalLabelX = intervalLabelX;
        this.intervalLabelY = intervalLabelY;
        computePreferredSize(true);
        scheduleDetermineActivePointIfEnabled();
        redraw();
    }

    /**
     * Get the suffix to be appended to any labels along the X axis.
     * 
     * @return Suffix to be appended to any labels along the X axis; may be
     *         <code>null</code>.
     */
    public final String getLabelSuffixX() {
        return suffixLabelX;
    }

    /**
     * Get the suffix to be appended to any labels along the Y axis.
     * 
     * @return Suffix to be appended to any labels along the Y axis; may be
     *         <code>null</code>.
     */
    public final String getLabelSuffixY() {
        return suffixLabelY;
    }

    /**
     * Set the suffixes to be appended to any labels along the axes.
     * 
     * @param suffixLabelX
     *            Suffix to be appended to any labels along the X axis; may be
     *            <code>null</code>.
     * @param suffixLabelY
     *            Suffix to be appended to any labels along the Y axis; may be
     *            <code>null</code>.
     */
    public final void setLabelSuffixes(String suffixLabelX, String suffixLabelY) {
        this.suffixLabelX = suffixLabelX;
        this.suffixLabelY = suffixLabelY;

        /*
         * Recalculate the preferred size and the active point, and redraw.
         */
        computePreferredSize(true);
        scheduleDetermineActivePointIfEnabled();
        redraw();
    }

    /**
     * Get the number of plotted points.
     * 
     * @return Number of plotted points.
     */
    public final int getPlottedPointCount() {
        return plottedPoints.size();
    }

    /**
     * Get the plotted points.
     * 
     * @return Copy of the list of plotted points (with each point copied as
     *         well, since {@link PlottedPoint} instances are mutable).
     */
    public final List<PlottedPoint> getPlottedPoints() {
        List<PlottedPoint> copy = new ArrayList<>(plottedPoints.size());
        for (PlottedPoint point : plottedPoints) {
            copy.add(new PlottedPoint(point));
        }
        return copy;
    }

    /**
     * Set the plotted points.
     * 
     * @param points
     *            New list of plotted points.
     * @return True if the points are set, false otherwise. They will not be set
     *         if any of them have duplicate X values.
     */
    public final boolean setPlottedPoints(List<PlottedPoint> points) {

        /*
         * Ensure the points are sorted, and do not have duplicate X values.
         */
        if (points != null) {
            points = sortPlottedPoints(points);
        }
        if (points == null) {
            return false;
        }

        /*
         * Remember the new plotted points.
         */
        plottedPoints.clear();
        for (PlottedPoint point : points) {
            plottedPoints.add(new PlottedPoint(point));
        }

        /*
         * Get the new minimum and maximum visible X values; if they have
         * changed, recalculate display measurements.
         */
        int minimumX = (plottedPoints.isEmpty() ? 0 : plottedPoints.get(0)
                .getX());
        int maximumX = (plottedPoints.isEmpty() ? 0 : plottedPoints.get(
                plottedPoints.size() - 1).getX());
        if ((minimumX != minimumVisibleValueX)
                || (maximumX != maximumVisibleValueX)) {
            minimumVisibleValueX = minimumX;
            maximumVisibleValueX = maximumX;
            computePreferredSize(true);
        }

        /*
         * Notify listeners of the change and redraw.
         */
        notifyListeners(ChangeSource.METHOD_INVOCATION);
        redraw();
        return true;
    }

    /**
     * Get the row colors.
     * 
     * @return Row colors.
     */
    public final List<Color> getRowColors() {
        return new ArrayList<>(rowColors);
    }

    /**
     * Set the row colors.
     * 
     * @param rowColors
     *            New row colors.
     */
    public final void SetRowColors(List<Color> rowColors) {
        this.rowColors.clear();
        if (rowColors != null) {
            this.rowColors.addAll(rowColors);
        }
        computeColorRowHeights();
        redraw();
    }

    /**
     * Get the inset in pixels from the left border.
     * 
     * @return Inset in pixels.
     */
    public final int getLeftInset() {
        return leftInset;
    }

    /**
     * Get the inset in pixels from the top border.
     * 
     * @return Inset in pixels.
     */
    public final int getTopInset() {
        return topInset;
    }

    /**
     * Get the inset in pixels from the right border.
     * 
     * @return Inset in pixels.
     */
    public final int getRightInset() {
        return rightInset;
    }

    /**
     * Get the inset in pixels from the bottom border.
     * 
     * @return Inset in pixels.
     */
    public final int getBottomInset() {
        return bottomInset;
    }

    /**
     * Set the insets around the edges of the widget, indicating the number of
     * pixels to inset each side of the client area from that side's border.
     * 
     * @param left
     *            Number of pixels to inset from the left side.
     * @param top
     *            Number of pixels to inset from the top side.
     * @param right
     *            Number of pixels to inset from the right side.
     * @param bottom
     *            Number of pixels to inset from the bottom side.
     */
    public final void setInsets(int left, int top, int right, int bottom) {

        /*
         * Remember the insets.
         */
        leftInset = left;
        topInset = top;
        rightInset = right;
        bottomInset = bottom;

        /*
         * Recalculate the preferred size.
         */
        computePreferredSize(true);

        /*
         * Determine the active plotted point.
         */
        scheduleDetermineActivePointIfEnabled();
    }

    @Override
    public final Point computeSize(int wHint, int hHint, boolean changed) {

        /*
         * Calculate the preferred size if needed.
         */
        computePreferredSize(false);

        /*
         * Return the size based upon the preferred size and the hints.
         */
        return new Point((wHint == SWT.DEFAULT ? preferredWidth : wHint),
                (hHint == SWT.DEFAULT ? preferredHeight : hHint));
    }

    @Override
    public final Rectangle computeTrim(int x, int y, int width, int height) {
        return new Rectangle(x - leftInset, y - topInset, width + leftInset
                + rightInset, height + topInset + bottomInset);
    }

    @Override
    public final Rectangle getClientArea() {
        Rectangle bounds = getBounds();
        return new Rectangle(leftInset, topInset, bounds.width
                - (leftInset + rightInset), bounds.height
                - (topInset + bottomInset));
    }

    // Private Methods

    /**
     * Initialize the widget.
     * 
     * @param minimumVisibleValueY
     *            Minimum visible Y value.
     * @param maximumVisibleValueY
     *            Maximum visible Y value.
     */
    private void initializeGraph(int minimumVisibleValueY,
            int maximumVisibleValueY) {

        /*
         * Remember the minimum and maximum values.
         */
        this.minimumVisibleValueY = minimumVisibleValueY;
        this.maximumVisibleValueY = maximumVisibleValueY;

        /*
         * Add a listener for paint request events.
         */
        addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                Graph.this.paintControl(e);
            }
        });

        /*
         * Add a listener for resize events.
         */
        addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Graph.this.controlResized(e);
            }
        });

        /*
         * Add a listener for dispose events.
         */
        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                draggingPoint = null;
                activePoint = null;
            }
        });

        /*
         * Add mouse listeners to handle drags, clicks, and mouse-over events.
         */
        addMouseListener(new MouseListener() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {

                /*
                 * No action.
                 */
            }

            @Override
            public void mouseDown(MouseEvent e) {
                if ((e.button == 1) && isVisible() && isEnabled()) {
                    mousePressOverWidget(e);
                }
            }

            @Override
            public void mouseUp(MouseEvent e) {
                if (draggingPoint != null) {
                    plottedPointDragEnded(e);
                } else if ((e.button == 1) && isVisible() && isEnabled()) {
                    mouseOverWidget(e.x, e.y);
                }
            }
        });
        addMouseMoveListener(new MouseMoveListener() {
            @Override
            public void mouseMove(MouseEvent e) {
                if (draggingPoint != null) {
                    plottedPointDragged(e);
                } else if (isVisible() && isEnabled()) {
                    mouseOverWidget(e.x, e.y);
                }
            }
        });
        addMouseTrackListener(new MouseTrackListener() {
            @Override
            public void mouseEnter(MouseEvent e) {
                if (isVisible() && isEnabled()) {
                    mouseOverWidget(e.x, e.y);
                }
            }

            @Override
            public void mouseExit(MouseEvent e) {
                if (activePoint != null) {
                    activePoint = null;
                    redraw();
                }
            }

            @Override
            public void mouseHover(MouseEvent e) {

                /*
                 * No action.
                 */
            }
        });
    }

    /**
     * Respond to the resizing of the widget.
     * 
     * @param e
     *            Control event that triggered this invocation.
     */
    private void controlResized(ControlEvent e) {

        /*
         * If the widget is visible, the width has previously been meaningfully
         * recorded, and the resize behavior requires that the viewport change
         * with size, calculate the new value range for the new size.
         */
        Rectangle clientArea = getClientArea();

        /*
         * Remember the now-current width.
         */
        lastWidth = clientArea.width;
        lastHeight = clientArea.height;

        /*
         * Recompute display measurements and determine the active point.
         */
        computeDisplayMeasurements();
        scheduleDetermineActivePointIfEnabled();
    }

    /**
     * Compute the preferred size of the widget.
     * 
     * @param force
     *            Force a recompute if the size has already been computed.
     */
    private void computePreferredSize(boolean force) {

        /*
         * Compute the preferred size only if it has not yet been computed, or
         * if being forced to do it regardless of previous computations.
         */
        if (force || (preferredWidth == 0)) {

            /*
             * Calculate the preferred width and height based on the font
             * currently in use. The preferred width allows for all the labels
             * that should be shown along the X axis to be displayed, and the
             * preferred height allows for the same along the Y axis.
             */
            GC sampleGC = new GC(this);
            FontMetrics fontMetrics = sampleGC.getFontMetrics();

            /*
             * Calculate the maximum X and Y label widths and heights. No labels
             * will be drawn for an axis if that axis's hatch interval is 0, or
             * if the label interval is 0, or if the label interval is not a
             * multiple of the hatch interval.
             */
            xLabelWidth = ((intervalLabelX == 0) || (intervalHatchX == 0)
                    || (intervalLabelX % intervalHatchX != 0) ? 0 : Math.max(
                    sampleGC.stringExtent(Integer
                            .toString(minimumVisibleValueX)
                            + (suffixLabelX == null ? "" : suffixLabelX)).x,
                    sampleGC.stringExtent(Integer
                            .toString(maximumVisibleValueX)
                            + (suffixLabelX == null ? "" : suffixLabelX)).x));
            yLabelWidth = ((intervalLabelY == 0) || (intervalHatchY == 0)
                    || (intervalLabelY % intervalHatchY != 0) ? 0 : Math.max(
                    sampleGC.stringExtent(Integer
                            .toString(minimumVisibleValueY)
                            + (suffixLabelY == null ? "" : suffixLabelY)).x,
                    sampleGC.stringExtent(Integer
                            .toString(maximumVisibleValueY)
                            + (suffixLabelY == null ? "" : suffixLabelY)).x));
            xLabelHeight = (xLabelWidth == 0 ? 0 : fontMetrics.getHeight());
            yLabelHeight = (yLabelWidth == 0 ? 0 : fontMetrics.getHeight());

            /*
             * When calculating the preferred width, take into account the
             * labels along the Y axis and the horizontal space they take up.
             */
            preferredWidth = 0;
            if (minimumVisibleValueX != maximumVisibleValueX) {
                int numLabels = (maximumVisibleValueX - minimumVisibleValueX)
                        / (intervalLabelX == 0 ? 10 : intervalLabelX);
                preferredWidth = (xLabelWidth * numLabels) + yLabelWidth;
            }
            if (preferredWidth < 400) {
                preferredWidth = 400;
            }
            preferredWidth += getLeftInset() + getRightInset();

            /*
             * When calculating the preferred height, take into account the
             * labels along the X axis and the vertical space they take up.
             */
            if (minimumVisibleValueY != maximumVisibleValueY) {
                int numLabels = (maximumVisibleValueY - minimumVisibleValueY)
                        / (intervalLabelY == 0 ? 10 : intervalLabelY);
                preferredHeight = yLabelHeight
                        * (numLabels + (intervalLabelX == 0 ? 0 : 1));
            }
            if (preferredHeight < 300) {
                preferredHeight = 300;
            }
            preferredHeight += getTopInset() + getBottomInset();

            sampleGC.dispose();

            /*
             * Recompute the display measurements.
             */
            computeDisplayMeasurements();
        }
    }

    /**
     * Compute the measurements needed for displaying the widget.
     */
    private void computeDisplayMeasurements() {

        /*
         * If there is no width or height, do not try to compute measurements.
         */
        if (isReadyForPainting() == false) {
            return;
        }

        /*
         * If there there is no X or Y range, then very little needs to be done.
         * Computation of pixels-per-unit, etc. is required if both X and Y axes
         * have ranges of values.
         */
        int xRange = maximumVisibleValueX - minimumVisibleValueX;
        int yRange = maximumVisibleValueY - minimumVisibleValueY;
        int widthWithoutLabels = lastWidth
                - (yRange > 0 ? yLabelWidth + Y_AXIS_LABEL_BUFFER : 0);
        int heightWithoutLabels = lastHeight
                - (xRange > 0 ? xLabelHeight + X_AXIS_LABEL_BUFFER : 0);

        /*
         * Calculate the number of pixels per unit in each direction.
         */
        if (xRange > 0) {
            pixelsPerUnitX = ((double) widthWithoutLabels) / (double) xRange;
        } else {
            pixelsPerUnitX = 0;
        }
        if (yRange > 0) {
            pixelsPerUnitY = ((double) heightWithoutLabels) / (double) yRange;
        } else {
            pixelsPerUnitY = 0;
        }

        /*
         * Calculate the number of pixels per hatch mark, if any, in each
         * direction. If a ridiculously small amount (or 0, or negative), which
         * will occur if there are too many hatch marks for the pixel range
         * available, or if there are not meant to be any hatch marks, then
         * assume no hatch marks, and calculate the pixels per unit exactly,
         * since no hatch marks means no concern over uneven-looking intervals.
         * Otherwise, calculate the pixels per unit by dividing the pixels per
         * hatch by the hatch interval.
         */
        if (xRange > 0) {
            pixelsPerHatchX = (int) (((double) (widthWithoutLabels * intervalHatchX)) / (double) xRange);
            if (pixelsPerHatchX < 4) {
                pixelsPerHatchX = 0;
                pixelsPerUnitX = ((double) widthWithoutLabels)
                        / (double) xRange;
                graphWidth = widthWithoutLabels;
                // System.err.println("Setting graph width the cheesy way to "
                // + graphWidth);
            } else {
                pixelsPerUnitX = ((double) pixelsPerHatchX)
                        / (double) intervalHatchX;
                graphWidth = pixelsPerHatchX * (xRange / intervalHatchX);
                // System.err.println("Setting graph width the excellent way to "
                // + graphWidth);
            }
        } else {
            pixelsPerHatchX = 0;
            graphWidth = widthWithoutLabels;
        }
        if (yRange > 0) {
            pixelsPerHatchY = (int) (((double) (heightWithoutLabels * intervalHatchY)) / (double) yRange);
            if (pixelsPerHatchY < 4) {
                pixelsPerHatchY = 0;
                pixelsPerUnitY = ((double) heightWithoutLabels)
                        / (double) yRange;
                graphHeight = heightWithoutLabels;
                // System.err.println("Setting graph height the cheesy way to "
                // + graphHeight);
            } else {
                pixelsPerUnitY = ((double) pixelsPerHatchY)
                        / (double) intervalHatchY;
                graphHeight = pixelsPerHatchY * (yRange / intervalHatchY);
                // System.err.println("Setting graph height the excellent way to "
                // + graphHeight);
            }
        } else {
            pixelsPerHatchY = 0;
            graphHeight = heightWithoutLabels;
        }

        /*
         * Calculate the upper Y boundaries of the color rows.
         */
        computeColorRowHeights();
    }

    /**
     * Compute the color row heights.
     */
    private void computeColorRowHeights() {
        rowColorUpperBounds.clear();
        if (rowColors.isEmpty()) {
            return;
        }
        double pixelsPerColorRow = ((double) graphHeight)
                / (double) rowColors.size();
        double totalSoFar = 0.0;
        for (int j = 0; j < rowColors.size() - 1; j++) {
            rowColorUpperBounds
                    .add((int) (totalSoFar + pixelsPerColorRow + 0.5));
            totalSoFar += pixelsPerColorRow;
        }
        rowColorUpperBounds.add(graphHeight);
    }

    /**
     * Paint the widget.
     * 
     * @param e
     *            Event that triggered this invocation.
     */
    private void paintControl(PaintEvent e) {

        /*
         * If not ready for painting, do nothing.
         */
        if (isReadyForPainting() == false) {
            return;
        }

        /*
         * Calculate the preferred size if needed, and determine the width and
         * height to be used when painting this time around.
         */
        computePreferredSize(false);
        Rectangle clientArea = getClientArea();
        int xOffset = (yLabelWidth == 0 ? 0 : yLabelWidth + Y_AXIS_LABEL_BUFFER)
                + clientArea.x;
        int yOffset = clientArea.y;

        /*
         * Get the default colors, as they are needed later.
         */
        Color background = e.gc.getBackground();
        Color foreground = e.gc.getForeground();

        /*
         * Draw the background.
         */
        if (e.gc.getBackground() != null) {
            e.gc.fillRectangle(clientArea);
        }

        /*
         * Fill in the colors for the rows, if any.
         */
        if (rowColors.isEmpty() == false) {
            int lastUpperBound = 0;
            for (int j = 0; j < rowColors.size(); j++) {
                int upperBound = rowColorUpperBounds.get(j);
                Rectangle colorArea = new Rectangle(xOffset, yOffset
                        + graphHeight - upperBound, graphWidth, upperBound
                        - lastUpperBound);
                e.gc.setBackground(rowColors.get(j));
                e.gc.fillRectangle(colorArea);
                lastUpperBound = upperBound;
            }
        }

        /*
         * Draw any vertical hatch marks needed.
         */
        if (pixelsPerHatchX != 0) {
            e.gc.setForeground(Display.getDefault().getSystemColor(
                    SWT.COLOR_DARK_GRAY));
            for (int x = 0; x <= graphWidth; x += pixelsPerHatchX) {
                e.gc.drawLine(x + xOffset, yOffset + graphHeight, x + xOffset,
                        yOffset);
            }
        }

        /*
         * Draw any horizontal hatch marks needed.
         */
        if (pixelsPerHatchY != 0) {
            e.gc.setForeground(Display.getDefault().getSystemColor(
                    SWT.COLOR_DARK_GRAY));
            for (int y = 0; y <= graphHeight; y += pixelsPerHatchY) {
                e.gc.drawLine(xOffset, yOffset + graphHeight - y, xOffset
                        + graphWidth, yOffset + graphHeight - y);
            }
        }

        /*
         * Draw any X axis labels needed. Any that might be too close to the
         * previous one is skipped.
         */
        if (xLabelWidth != 0) {
            e.gc.setForeground(Display.getDefault().getSystemColor(
                    SWT.COLOR_BLACK));
            int lastEndpoint = -10000;
            for (int x = 0, value = minimumVisibleValueX; x <= graphWidth; x += (intervalLabelX / intervalHatchX)
                    * pixelsPerHatchX, value += intervalLabelX) {
                if (lastEndpoint > x + xOffset - xLabelWidth) {
                    continue;
                }
                String label = Integer.toString(value)
                        + (suffixLabelX == null ? "" : suffixLabelX);
                Point extent = e.gc.stringExtent(label);
                e.gc.drawString(label, x + xOffset - (extent.x / 2), yOffset
                        + graphHeight + X_AXIS_LABEL_BUFFER, true);
                lastEndpoint = x + xOffset + xLabelWidth;
            }
        }

        /*
         * Draw any Y axis labels needed. Any that might be too close to the
         * previous one is skipped.
         */
        if (yLabelWidth != 0) {
            e.gc.setForeground(Display.getDefault().getSystemColor(
                    SWT.COLOR_BLACK));
            int lastEndpoint = -10000;
            for (int y = 0, value = minimumVisibleValueY; y <= graphHeight; y += (intervalLabelY / intervalHatchY)
                    * pixelsPerHatchY, value += intervalLabelY) {
                if (lastEndpoint > y + yOffset - yLabelHeight) {
                    continue;
                }
                String label = Integer.toString(value)
                        + (suffixLabelY == null ? "" : suffixLabelY);
                Point extent = e.gc.stringExtent(label);
                // System.err.println("Extent width for " + label + " is "
                // + extent.x + ", label width for Y axis is "
                // + yLabelWidth);
                e.gc.drawString(label, xOffset
                        - (extent.x + Y_AXIS_LABEL_BUFFER), yOffset
                        + graphHeight - (y + (yLabelHeight / 2)), true);
                lastEndpoint = y + yOffset + yLabelHeight;
            }
        }

        /*
         * Draw the border around the widget.
         */
        e.gc.setForeground(Display.getDefault().getSystemColor(
                SWT.COLOR_DARK_GRAY));
        Rectangle borderRect = new Rectangle(xOffset, yOffset, graphWidth,
                graphHeight);
        e.gc.drawRectangle(borderRect);

        /*
         * Draw the points, if any, and the line connecting them together.
         */
        if (plottedPoints.isEmpty() == false) {
            e.gc.setBackground(Display.getDefault().getSystemColor(
                    SWT.COLOR_BLACK));
            int[] polyLineCoords = new int[plottedPoints.size() * 2];
            for (int j = 0; j < plottedPoints.size(); j++) {
                PlottedPoint point = plottedPoints.get(j);
                int x, y;
                if ((point.getX() - minimumVisibleValueX) % intervalHatchX == 0) {
                    x = ((point.getX() - minimumVisibleValueX) / intervalHatchX)
                            * pixelsPerHatchX;
                } else {
                    x = (int) (((point.getX() - minimumVisibleValueX) * pixelsPerUnitX) + 0.5);
                }
                if ((point.getY() - minimumVisibleValueY) % intervalHatchY == 0) {
                    y = ((point.getY() - minimumVisibleValueY) / intervalHatchY)
                            * pixelsPerHatchY;
                } else {
                    y = (int) (((point.getY() - minimumVisibleValueY) * pixelsPerUnitY) + 0.5);
                }
                polyLineCoords[j * 2] = xOffset + x;
                polyLineCoords[(j * 2) + 1] = yOffset + graphHeight - y;
                e.gc.fillOval(xOffset + x - (POINT_DIAMETER / 2), yOffset
                        + graphHeight - (y + (POINT_DIAMETER / 2)),
                        POINT_DIAMETER, POINT_DIAMETER);
            }
            e.gc.setForeground(Display.getDefault().getSystemColor(
                    SWT.COLOR_BLACK));
            e.gc.drawPolyline(polyLineCoords);
        }

        /*
         * Reset the colors.
         */
        e.gc.setBackground(background);
        e.gc.setForeground(foreground);
    }

    /**
     * Determine which editable plotted point the specified coordinates are
     * within the bounds of, if any.
     * 
     * @param x
     *            X coordinate, relative to the widget.
     * @param y
     *            Y coordinate, relative to the widget.
     * @return Specifier of the editable plotted point within which the
     *         coordinate exists, or <code>null</code> if the coordinate is not
     *         within an editable plotted point.
     */
    private PlottedPoint getEditablePointForCoordinates(int x, int y) {

        /*
         * TODO: Fill in.
         */
        return null;
    }

    /**
     * Schedule a determination of the currently active plotted point to occur
     * after current events have been processed if the mouse is positioned over
     * the widget and if the widget is active. If the determination is being
     * requested by a process that should occur quickly, this method is
     * preferable to {@link #determineActivePointIfEnabled()}.
     */
    private void scheduleDetermineActivePointIfEnabled() {

        /*
         * If the requested determination is already scheduled or the widget is
         * disabled or invisible, do nothing.
         */
        if (determinationOfActivePointScheduled || (isVisible() == false)
                || (isEnabled() == false)) {
            return;
        }

        /*
         * Schedule a determination to be made after other events are processed.
         */
        determinationOfActivePointScheduled = true;
        getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                determineActivePointIfEnabled();
            }
        });
    }

    /**
     * Determine the currently active plotted point if the mouse is positioned
     * over the widget and if the widget is active. This method should not be
     * used if the caller requires a quick execution; in that case, use of
     * {@link #scheduleDetermineActivePointIfEnabled()} is preferable.
     */
    private void determineActivePointIfEnabled() {
        determinationOfActivePointScheduled = false;
        if ((isDisposed() == false) && isVisible() && isEnabled()) {
            determineActivePoint();
        }
    }

    /**
     * Respond to the mouse being pressed over the widget.
     * 
     * @param e
     *            Mouse event that occurred.
     */
    private void mousePressOverWidget(MouseEvent e) {

        /*
         * Get the editable point over which the mouse event occurred, if any.
         */
        PlottedPoint newDraggingPoint = getEditablePointForCoordinates(e.x, e.y);

        /*
         * If a point has become active or inactive, redraw.
         */
        if ((draggingPoint != newDraggingPoint)
                && ((draggingPoint == null) || (draggingPoint
                        .equals(newDraggingPoint) == false))) {
            draggingPoint = newDraggingPoint;
            redraw();
        }
    }

    /**
     * Respond to a point being dragged.
     * 
     * @param e
     *            Mouse event that occurred.
     */
    private void plottedPointDragged(MouseEvent e) {
        dragPlottedPointToPoint(draggingPoint, e, false);
        if (isDisposed() == false) {
            redraw();
        }
    }

    /**
     * Respond to a plotted point drag ending.
     * 
     * @param e
     *            Mouse event that occurred, if a mouse event triggered this
     *            invocation, or <code>null</code> if the invocation is the
     *            result of a programmatic change.
     */
    private void plottedPointDragEnded(MouseEvent e) {
        PlottedPoint point = draggingPoint;
        draggingPoint = null;
        if (e != null) {
            dragPlottedPointToPoint(point, e, true);
            if (isDisposed() == false) {
                activePoint = getEditablePointForCoordinates(e.x, e.y);
            }
        }
        if (isDisposed() == false) {
            redraw();
        }
    }

    /**
     * Determine the currently active plotted point if the mouse is positioned
     * over the widget.
     */
    private void determineActivePoint() {

        /*
         * Calculate the mouse location relative to this widget.
         */
        Point mouseLocation = getDisplay().getCursorLocation();
        Point offset = toDisplay(0, 0);
        mouseLocation.x -= offset.x;
        mouseLocation.y -= offset.y;

        /*
         * If the mouse is over the widget, process its position to determine
         * which plotted point, if any, should now be active.
         */
        if ((mouseLocation.x >= 0) && (mouseLocation.x < lastWidth)
                && (mouseLocation.y >= 0) && (mouseLocation.y < lastHeight)) {
            mouseOverWidget(mouseLocation.x, mouseLocation.y);
        }
    }

    /**
     * Respond to the mouse cursor moving over the widget when a drag is not
     * occurring, or to an editability change for at least one plotted point.
     * 
     * @param x
     *            X coordinate of the mouse, relative to the widget.
     * @param y
     *            Y coordinate of the mouse, relative to the widget.
     */
    private void mouseOverWidget(int x, int y) {

        /*
         * If the widget is disposed, do nothing.
         */
        if (isDisposed() || (isVisible() == false)) {
            return;
        }

        /*
         * Get the editable plotted point over which the mouse event occurred,
         * if any.
         */
        PlottedPoint newPoint = getEditablePointForCoordinates(x, y);

        /*
         * If a point has become active or inactive, redraw.
         */
        if ((activePoint != newPoint)
                && ((activePoint == null) || (activePoint.equals(newPoint) == false))) {
            activePoint = newPoint;
            redraw();
        }
    }

    /**
     * Drag the specified plotted point to the specified point, or as close to
     * it as possible.
     * 
     * @param point
     *            Plotted point to be dragged.
     * @param e
     *            Mouse event that prompted this drag.
     * @param dragEnded
     *            Flag indicating whether or not the drag has ended.
     */
    private void dragPlottedPointToPoint(PlottedPoint point, MouseEvent e,
            boolean dragEnded) {

        /*
         * TODO: Fill in.
         */
    }

    /**
     * Sort the specified plotted points, returning a sorted version of the
     * list, or <code>null</code> if any of the points have duplicate X values.
     * 
     * @param points
     *            Plotted points to be sorted.
     * @return Sorted copy of the list, or <code>null</code> if the points
     *         contain duplicate X values.
     */
    private List<PlottedPoint> sortPlottedPoints(List<PlottedPoint> points) {
        List<PlottedPoint> copy = new ArrayList<>(points);
        Collections.sort(copy);
        for (int j = 1; j < copy.size(); j++) {
            if (copy.get(j - 1).getX() == copy.get(j).getX()) {
                return null;
            }
        }
        return copy;
    }

    /**
     * Determine whether or not the widget can be displayed, based upon its
     * client size.
     * 
     * @return True if it can be displayed, false otherwise.
     */
    private boolean isReadyForPainting() {
        return ((lastWidth != 0) && (lastHeight != 0));
    }

    /**
     * Notify listeners of value changes.
     */
    private void notifyListeners(ChangeSource source) {
        for (IGraphListener listener : listeners) {
            listener.plottedPointsChanged(this, source);
        }
    }
}
