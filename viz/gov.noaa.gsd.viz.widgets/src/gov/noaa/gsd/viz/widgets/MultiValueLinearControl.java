/**
 * This software was developed and / or modified by the
 * National Oceanic and Atmospheric Administration (NOAA), 
 * Earth System Research Laboratory (ESRL), 
 * Global Systems Division (GSD), 
 * Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.PopupDialog;
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * Multi-value linear control widget, an abstract class from which linear
 * widgets allowing the display and manipulation of one or more values may be
 * derived. Subclasses provide the visual elements of the widget so that it may
 * be drawn. Each such control has minimum and maximum possible values, these
 * being the absolute value boundaries; and lower and upper viewport values,
 * meaning the range of values currently visible, as opposed to offscreen to the
 * left or right of the widget.
 * <p>
 * Each linear control widget may have zero or more marked values, meaning
 * values that are visually marked off along its length using colors as
 * specified, and that may have the ranges between them visually distinguished
 * using other colors that may be specified. Such values can only be changed
 * programmatically; they cannot be moved via mouse events. Marked values may be
 * either constrained, meaning that they are constrained by the values of
 * adjacent constrained marked values, or free, meaning that they are not
 * constrained by other marked values in any way.
 * </p>
 * <p>
 * Additionally, widget each may have zero or more values represented by
 * "thumbs" that may be moved in order to manipulate said values. Marked values
 * are thus static unless changed via method calls, whereas thumb values may be
 * changed either programmatically or via user interaction. Each thumb value is
 * either termed constrained, meaning that it is constrained by the values of
 * adjacent constrained thumbs, or free, meaning that it is not constrained by
 * other thumbs in any way. Each thumb of either type may be editable or
 * read-only at any given time; if in the latter state, that thumb may not be
 * moved via user interaction.
 * </p>
 * <p>
 * Constrained thumb values may be linked together so that when one is dragged
 * they are all dragged, maintaining the distances they had from one another
 * before the drag occurred, via the
 * {@link #setConstrainedThumbIntervalLocked(boolean)}.
 * </p>
 * <p>
 * Colors may be assigned to the marked values, to the ranges between the marked
 * values, and to the ranges in between the thumb values. Subclasses should
 * honor these color choices by using them in some capacity when rendering said
 * marked values and ranges.
 * </p>
 * <p>
 * By default, the control allows thumb values to be anything between the
 * current allowable minimum and maximum values. If a coarser value set is
 * desired (for example, values are from 0 to 100, but only multiples of 10
 * should be used), then a snap value calculator may be assigned that generates
 * the correct "snap" values for values of too fine a granularity. A snap value
 * calculator is only used for values generated via GUI manipulation of thumbs;
 * it is not used for programmatic manipulation of thumb or marked values.
 * </p>
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 04, 2013            Chris.Golden      Initial induction into repo
 * Jun 10, 2013            Chris.Golden      Added overrideable methods that
 *                                           allow subclasses to react to
 *                                           color changes by clearing or
 *                                           recreating subclass-specific
 *                                           resources, etc. Also fixed bug
 *                                           in mapValueToPixel() that
 *                                           caused mis-mappings when
 *                                           converted values fell outside
 *                                           the range representable by
 *                                           integers.
 * Nov 05, 2013    2336    Chris.Golden      Added new ChangeSource that
 *                                           indicates ongoing GUI changes
 *                                           (caused by ongoing drags) as
 *                                           opposed to the end of a GUI
 *                                           change (end of a drag).
 * Jan 28, 2014    2161    Chris.Golden      Added ability to render each
 *                                           thumb editable or read-only
 *                                           individuallly, instead of
 *                                           controlling editability at a
 *                                           coarser widget level.
 * Mar 06, 2014    2155    Chris.Golden      Fixed bug that caused thumbs
 *                                           that are not being dragged to
 *                                           be needlessly adjusted in an
 *                                           overabundance of caution when
 *                                           another thumb is being dragged.
 *                                           This would not have been a
 *                                           problem except that thumbs set
 *                                           to Until Further Notice values
 *                                           were being changed by dragging
 *                                           of other thumbs in the same
 *                                           widget.
 * Jun 27, 2014    3512    Chris.Golden      Added option to lock intervals
 *                                           between constrained values,
 *                                           and changed commenting style.
 * Jul 03, 2014    3512    Chris.Golden      Improved code to have it avoid
 *                                           making a thumb look active
 *                                           when the widget is invisible.
 * Sep 11, 2014    1283    Robert.Blum       Changed out the ToolTip with a 
 *                                           custom one that displays on the 
 *                                           correct monitor.
 * Jan 26, 2014    2331    Chris.Golden      Added ability to specify lower
 *                                           and upper bounds for allowable
 *                                           values for constrained thumbs,
 *                                           with such boundaries being
 *                                           individual to each thumb.
 * Feb 03, 2015    2331    Chris.Golden      Fixed bug that disallowed
 *                                           zero-length value boundary
 *                                           ranges.
 * Jul 23, 2015    4245    Chris.Golden      Fixed bug that caused an
 *                                           exception if a control was
 *                                           created without giving it a
 *                                           tooltip text provider.
 * Aug 12, 2015    4123    Chris.Golden      Fixed bug with translation
 *                                           between distance along visual
 *                                           representation and value, which
 *                                           was undetected until now due to
 *                                           the fact that the widget had
 *                                           never been used with a small
 *                                           range of possible values before.
 * Oct 19, 2016   21873    Chris.Golden      Changed so that mouse-wheel
 *                                           zooming occurs with the center
 *                                           of the zoom being at the point
 *                                           where the mouse cursor lies.
 * Nov 18, 2016   26363    Robert.Blum       Fix widget disposed error.
 * </pre>
 * 
 * @author Chris.Golden
 * @version 1.0
 */
public abstract class MultiValueLinearControl extends Canvas {

    // Private Static Constants

    /**
     * Default snap value calculator; this merely returns the value after
     * checking that it is within the specified bounds. It does not limit
     * granularity.
     */
    private static final ISnapValueCalculator DEFAULT_SNAP_VALUE_CALCULATOR = new ISnapValueCalculator() {
        @Override
        public long getSnapThumbValue(long value, long minimum, long maximum) {
            return (value < minimum ? minimum
                    : (value > maximum ? maximum : value));
        }
    };

    /**
     * Default value range used when lower and upper bounding values must be
     * made different, and the amount of difference required is unknown.
     */
    private static final long DEFAULT_VISIBLE_OFFSET = 100L;

    // Public Enumerated Types

    /**
     * Source of a configuration change.
     */
    public enum ChangeSource {

        /**
         * Widget resize.
         */
        RESIZE,

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

    /**
     * Resize behavior, indicating the possible responses the widget may have to
     * being resized.
     */
    public enum ResizeBehavior {

        /**
         * Keep the maximum visible value constant, varying instead the ratio of
         * pixels to value units. Thus, when resized, the value range shown in
         * the widget will remain the same, with the number of pixels per value
         * increasing or decreasing instead.
         */
        CHANGE_PIXELS_PER_VALUE_UNIT,

        /**
         * Alter the minimum and/or maximum visible values, keeping constant the
         * ratio of pixels to value units. Thus, when resized, the value range
         * shown in the widget will grow or shrink, keeping the number of pixels
         * per value constant.
         */
        CHANGE_VALUE_RANGE
    };

    // Protected Enumerated Types

    /**
     * Value types.
     */
    protected enum ValueType {

        // Values
        CONSTRAINED("constrained"), FREE("free");

        // Private Variables

        /**
         * Description of this type.
         */
        private String description;

        // Private Constructors

        /**
         * Construct a standard instance.
         * 
         * @param description
         *            Text description of this type.
         */
        private ValueType(String description) {
            this.description = description;
        }

        // Public Methods

        @Override
        public String toString() {
            return description;
        }
    }

    // Protected Classes

    /**
     * Thumb specifier, encapsulating all information necessary to identify a
     * thumb.
     */
    protected class ThumbSpecifier {

        // Public Variables

        /**
         * Index of the thumb.
         */
        public int index;

        /**
         * Type of the thumb.
         */
        public ValueType type;

        // Public Constructors

        /**
         * Construct a standard instance.
         * 
         * @param type
         *            Type of the thumb.
         * @param index
         *            Index of the thumb.
         */
        public ThumbSpecifier(ValueType type, int index) {
            this.type = type;
            this.index = index;
        }

        // Public Methods

        @Override
        public boolean equals(Object other) {
            return ((other != null) && (other instanceof ThumbSpecifier)
                    && (index == ((ThumbSpecifier) other).index)
                    && (type == ((ThumbSpecifier) other).type));
        }
    }

    // Private Variables

    /**
     * Minimum possible value.
     */
    private long minimumValue = 0L;

    /**
     * Maximum possible value.
     */
    private long maximumValue = 0L;

    /**
     * Minimum value that is visible within the current viewport.
     */
    private long lowerVisibleValue = 0L;

    /**
     * Maximum value that is visible within the current viewport.
     */
    private long upperVisibleValue = 0L;

    /**
     * List of constrained thumb values.
     */
    private final List<Long> constrainedThumbValues = new ArrayList<Long>();

    /**
     * List of flags indicating whether or not the constrained thumbs are
     * editable. Each index holds the flag for the thumb value at the
     * corresponding index from {@link #constrainedThumbValues}.
     */
    private final List<Boolean> constrainedThumbValuesEditable = new ArrayList<Boolean>();

    /**
     * Map of constrained thumb indices to their minimum values, as specified by
     * {@link #setAllowableConstrainedValueRange(int, long, long)}. Any
     * constrained thumb with an index that has no such mapping is assumed to
     * not have a particular minimum constraint.
     */
    private final Map<Integer, Long> allowableMinimumConstrainedThumbValues = new HashMap<>();

    /**
     * Map of constrained thumb indices to their maximum values, as specified by
     * {@link #setAllowableConstrainedValueRange(int, long, long)}. Any
     * constrained thumb with an index that has no such mapping is assumed to
     * not have a particular maximum constraint.
     */
    private final Map<Integer, Long> allowableMaximumConstrainedThumbValues = new HashMap<>();

    /**
     * List of minimum values that constrained thumbs can have, based upon the
     * snap value calculator (if any), the minimum thumb gap, and the minimum
     * allowable value overall as well as any individual constrained thumbs'
     * minimum values (the latter as specified by
     * {@link #allowableMinimumConstrainedThumbValues}). Each index holds the
     * minimum value that the thumb value at the corresponding index from
     * {@link #constrainedThumbValues} can have. Note that these minimum values
     * are only used to determine values when constrained thumb values are
     * changed via user-GUI interaction.
     */
    private final List<Long> minConstrainedThumbValues = new ArrayList<Long>();

    /**
     * List of maximum values that constrained thumbs can have, based upon the
     * snap value calculator (if any), the minimum thumb gap, and the maximum
     * allowable value overall as well as any individual constrained thumbs'
     * maximum values (the latter as specified by
     * {@link #allowableMinimumConstrainedThumbValues}). Each index holds the
     * maximum value that the thumb value at the corresponding index from
     * {@link #constrainedThumbValues} can have. Note that these maximum values
     * are only used to determine values when constrained thumb values are
     * changed via user-GUI interaction.
     */
    private final List<Long> maxConstrainedThumbValues = new ArrayList<Long>();

    /**
     * List of free thumb values.
     */
    private final List<Long> freeThumbValues = new ArrayList<Long>();

    /**
     * List of flags indicating whether or not the free thumbs are editable.
     * Each index holds the flag for the thumb value at the corresponding index
     * from {@link #freeThumbValues}.
     */
    private final List<Boolean> freeThumbValuesEditable = new ArrayList<Boolean>();

    /**
     * List of constrained marked values.
     */
    private final List<Long> constrainedMarkedValues = new ArrayList<Long>();

    /**
     * List of free marked values.
     */
    private final List<Long> freeMarkedValues = new ArrayList<Long>();

    /**
     * Minimum value between adjacent constrained thumbs.
     */
    private long minimumConstrainedThumbGap = 0L;

    /**
     * List of colors to be used in some capacity to display constrained marked
     * values; the color at each index corresponds to the marked value at the
     * same index in {@link #constrainedMarkedValues}. If a color is
     * <code>null</code>, a default color is used for that marked value.
     */
    private final List<Color> constrainedMarkedValueColors = new ArrayList<Color>();

    /**
     * List of colors to be used in some capacity to display free marked values;
     * the color at each index corresponds to the marked value at the same index
     * in {@link #freeMarkedValues}. If a color is <code>null</code>, a default
     * color is used for that marked value.
     */
    private final List<Color> freeMarkedValueColors = new ArrayList<Color>();

    /**
     * List of colors to be used in some capacity as a fill for the display of a
     * range between two adjacent constrained marked values or between such a
     * marked value and the start or end of the widget. The first color is used
     * for the range between the left end of the widget and the first
     * constrained marked value; the second for the range between the first and
     * second constrained marked values (or the first constrained marked value
     * and the end, if only one constrained marked value is in use), etc. A
     * value of <code>null</code> at an index means that there is no color to be
     * drawn for that constrained marked value range.
     */
    private final List<Color> constrainedMarkedRangeColors = new ArrayList<Color>();

    /**
     * List of colors to be used in some capacity as a fill for the display of a
     * range between two adjacent constrained thumbs or between such a thumb and
     * the start or end of the widget. The first color is used for the range
     * between the left end of the widget and the first constrained thumb; the
     * second for the range between the first and second constrained thumbs (or
     * the first constrained thumb and the end, if only one constrained thumb is
     * in use), etc. A value of <code>null</code> at an index means that there
     * is no color to be drawn for that range.
     */
    private final List<Color> constrainedThumbRangeColors = new ArrayList<Color>();

    /**
     * Set of listeners; these receive notifications of visible value range or
     * value changes when the latter occur.
     */
    private final Set<IMultiValueLinearControlListener> listeners = new HashSet<IMultiValueLinearControlListener>();

    /**
     * Resize behavior.
     */
    private ResizeBehavior resizeBehavior = ResizeBehavior.CHANGE_PIXELS_PER_VALUE_UNIT;

    /**
     * Snap value calculator, used to generate values that are of the
     * appropriate granularity for this instance.
     */
    private ISnapValueCalculator snapValueCalculator = DEFAULT_SNAP_VALUE_CALCULATOR;

    /**
     * Tooltip text provider, used to generate text for tooltips to be displayed
     * over the widget; if <code>null</code>, no tooltips are shown.
     */
    private IMultiValueTooltipTextProvider tooltipTextProvider = null;

    /**
     * Tooltip to be used if the {@link #tooltipTextProvider} yields a text
     * string (not <code>null</code>) when invoked. If the provider is <code>
     * null</code>, this will be as well.
     */
    private CustomToolTip tooltip = new CustomToolTip(getShell(),
            PopupDialog.HOVER_SHELLSTYLE);

    /**
     * Flag indicating whether the constrained thumbs are to be drawn above or
     * below the free thumbs.
     */
    private boolean constrainedThumbsDrawnAboveFree = true;

    /**
     * Flag indicating whether the constrained thumbs' intervals are locked.
     */
    private boolean constrainedThumbIntervalLocked = false;

    /**
     * Flag indicating whether the constrained marked values are to be drawn
     * above or below the free marked values.
     */
    private boolean constrainedMarksDrawnAboveFree = true;

    /**
     * Desired width of the widget.
     */
    private int preferredWidth = 0;

    /**
     * Desired height of the widget.
     */
    private int preferredHeight = 0;

    /**
     * Last recorded width of the client area.
     */
    private int lastWidth = 0;

    /**
     * Last recorded height of the client area.
     */
    private int lastHeight = 0;

    /**
     * Number of pixels by which to inset the client area of the widget from the
     * left border.
     */
    private int leftInset = 0;

    /**
     * Number of pixels by which to inset the client area of the widget from the
     * top border.
     */
    private int topInset = 0;

    /**
     * Number of pixels by which to inset the client area of the widget from the
     * right border.
     */
    private int rightInset = 0;

    /**
     * Number of pixels by which to inset the client area of the widget from the
     * bottom border.
     */
    private int bottomInset = 0;

    /**
     * Specifier of the thumb currently being dragged, or <code>null</code> if
     * none is being dragged.
     */
    private ThumbSpecifier draggingThumb = null;

    /**
     * Specifier of the thumb over which the mouse cursor is presently located,
     * or <code>null</code> if the mouse cursor is not over any thumb.
     */
    private ThumbSpecifier activeThumb = null;

    /**
     * Flag indicating whether or not the viewport may get dragged during the
     * mouse events that follow until the mouse-up occurs.
     */
    private boolean mayBeDraggingViewport = false;

    /**
     * Flag indicating whether or not the viewport was actually dragged;
     * meaningless unless {@link #mayBeDraggingViewport} is true.
     */
    private boolean viewportWasActuallyDragged = false;

    /**
     * Last X coordinate to which a drag of the viewport occurred.
     */
    private int lastViewportDragX = 0;

    /**
     * Thumb type drawing order as an array of value types.
     */
    private final ValueType[] thumbTypeDrawingOrder = new ValueType[2];

    /**
     * Thumb type hit test order as an array of value types.
     */
    private final ValueType[] thumbTypeHitTestOrder = new ValueType[2];

    /**
     * Marked value type drawing order as an array of value types.
     */
    private final ValueType[] markTypeDrawingOrder = new ValueType[2];

    /**
     * Flag indicating whether or not a determination of which thumb is active
     * has been scheduled to occur later.
     */
    private boolean determinationOfActiveThumbScheduled = false;

    // Public Constructors

    /**
     * Construct a standard instance with a resize behavior of
     * {@link ResizeBehavior#CHANGE_PIXELS_PER_VALUE_UNIT}.
     * 
     * @param parent
     *            Parent of this widget.
     * @param minimumValue
     *            Absolute minimum possible value.
     * @param maximumValue
     *            Absolute maximum possible value.
     */
    public MultiValueLinearControl(Composite parent, long minimumValue,
            long maximumValue) {
        super(parent, SWT.NONE);

        /*
         * Initialize the widget.
         */
        initializeMultiValueLinearControl(minimumValue, maximumValue);
    }

    /**
     * Construct a standard instance.
     * 
     * @param parent
     *            Parent of this widget.
     * @param minimumValue
     *            Absolute minimum possible value.
     * @param maximumValue
     *            Absolute maximum possible value.
     * @param resizeBehavior
     *            Resize behavior.
     */
    public MultiValueLinearControl(Composite parent, long minimumValue,
            long maximumValue, ResizeBehavior resizeBehavior) {
        super(parent, SWT.NONE);
        this.resizeBehavior = resizeBehavior;

        /*
         * Initialize the widget.
         */
        initializeMultiValueLinearControl(minimumValue, maximumValue);
    }

    // Public Methods

    /**
     * Determine whether or not the viewport is draggable via a mouse click and
     * drag.
     * 
     * @return True if the viewport is draggable, false otherwise.
     */
    public abstract boolean isViewportDraggable();

    /**
     * Add the specified multi-value linear control listener; the latter will be
     * notified of value and visible value range changes.
     * 
     * @param listener
     *            Listener to be added.
     * @return True if this listener was not already part of the set of
     *         listeners, false otherwise.
     */
    public final boolean addMultiValueLinearControlListener(
            IMultiValueLinearControlListener listener) {
        return listeners.add(listener);
    }

    /**
     * Remove the specified multi-value linear control listener; if the latter
     * was registered previously via
     * {@link #addMultiValueLinearControlListener(IMultiValueLinearControlListener)}
     * it will no longer be notified of value and visible value range changes.
     * 
     * @param listener
     *            Listener to be removed.
     * @return True if the listener was found and removed, otherwise false.
     */
    public final boolean removeMultiValueLinearControlListener(
            IMultiValueLinearControlListener listener) {
        return listeners.remove(listener);
    }

    @Override
    public final void setEnabled(boolean enable) {
        super.setEnabled(enable);

        /*
         * End any drag of a thumb.
         */
        if ((enable == false) && (draggingThumb != null)) {
            thumbDragEnded(null);
        }

        /*
         * Ensure that the correct thumb is active.
         */
        determineActiveThumb();

        /*
         * Perform any superclass-specific tasks.
         */
        widgetEnabledStateChanged();
    }

    /**
     * Get the resize behavior.
     * 
     * @return Resize behavior.
     */
    public final ResizeBehavior getResizeBehavior() {
        return resizeBehavior;
    }

    /**
     * Set the resize behavior.
     * 
     * @param behavior
     *            Resize behavior.
     */
    public final void setResizeBehavior(ResizeBehavior behavior) {
        resizeBehavior = behavior;
    }

    /**
     * Get the snap value calculator, used to generate values that are of the
     * appropriate granularity for this instance.
     * 
     * @return Snap value calculator, or <code>null</code> if values may be any
     *         value between the current allowable minimum and maximum values.
     */
    public final ISnapValueCalculator getSnapValueCalculator() {
        return (snapValueCalculator == DEFAULT_SNAP_VALUE_CALCULATOR ? null
                : snapValueCalculator);
    }

    /**
     * Set the snap value calculator, used to generate values that are of the
     * appropriate granularity for this instance.
     * 
     * @param calculator
     *            Snap value calculator, or <code>null</code> if values may be
     *            any value between the current allowable minimum and maximum
     *            values.
     * @return True if the new snap value calculator can be used, false if it
     *         cannot because the interplay of it with the various boundaries
     *         results in an illegal state.
     */
    public final boolean setSnapValueCalculator(
            ISnapValueCalculator calculator) {
        ISnapValueCalculator oldSnapValueCalculator = snapValueCalculator;
        snapValueCalculator = (calculator != null ? calculator
                : DEFAULT_SNAP_VALUE_CALCULATOR);
        if (calculateConstrainedThumbValueBounds() == false) {
            snapValueCalculator = oldSnapValueCalculator;
            return false;
        }
        return true;
    }

    /**
     * Get the tooltip text provider, used to generate text for tooltips shown
     * over this widget.
     * 
     * @return Tooltip text provider, or <code>null</code> if no tooltip text
     *         provider is being used.
     */
    public final IMultiValueTooltipTextProvider getTooltipTextProvider() {
        return tooltipTextProvider;
    }

    /**
     * Set the tooltip text provider, used to generate text for tooltips shown
     * over this widget.
     * 
     * @param provider
     *            Tooltip text provider to be used, or <code>null</code> if none
     *            is to be used.
     */
    public final void setTooltipTextProvider(
            IMultiValueTooltipTextProvider provider) {
        tooltipTextProvider = provider;
    }

    /**
     * Determine whether constrained thumbs are to be drawn above or below free
     * thumbs, if thumbs of different types overlap.
     * 
     * @return Flag indicating whether constrained thumbs are to be drawn above
     *         or below free thumbs.
     */
    public final boolean isConstrainedThumbDrawnAboveFree() {
        return constrainedThumbsDrawnAboveFree;
    }

    /**
     * Set the flag indicating whether constrained thumbs are to be drawn above
     * or below free thumbs, if thumbs of different types overlap.
     * 
     * @param value
     *            Flag indicating whether constrained thumbs are to be drawn
     *            above or below free thumbs.
     */
    public final void setConstrainedThumbDrawnAboveFree(boolean value) {
        constrainedThumbsDrawnAboveFree = value;
        determineThumbTypeOrders();
        redraw();
    }

    /**
     * Determine whether constrained thumbs' intervals are locked. If they are
     * locked, dragging one thumb causes other thumbs to move along with it,
     * maintaining their distances from one another.
     * 
     * @return Flag indicating whether constrained thumbs' intervals are locked.
     */
    public final boolean isConstrainedThumbIntervalLocked() {
        return constrainedThumbIntervalLocked;
    }

    /**
     * Set the flag indicating whether constrained thumbs' intervals are locked.
     * When locked, dragging one thumb causes other thumbs to move along with
     * it, maintaining their distances from one another.
     * 
     * @param value
     *            Flag indicating whether constrained thumbs' intervals are
     *            locked.
     * @return True if the new flag value can be used, false if it cannot
     *         because the interplay of it with the various boundaries results
     *         in an illegal state.
     */
    public final boolean setConstrainedThumbIntervalLocked(boolean value) {
        boolean oldValue = constrainedThumbIntervalLocked;
        constrainedThumbIntervalLocked = value;
        if (calculateConstrainedThumbValueBounds() == false) {
            constrainedThumbIntervalLocked = oldValue;
            return false;
        }
        return true;
    }

    /**
     * Determine whether constrained marked values are to be drawn above or
     * below free ones, if marked values of different types overlap.
     * 
     * @return Flag indicating whether constrained marked values are to be drawn
     *         above or below free ones.
     */
    public final boolean isConstrainedMarkedValueDrawnAboveFree() {
        return constrainedMarksDrawnAboveFree;
    }

    /**
     * Set the flag indicating whether constrained marked values are to be drawn
     * above or below free ones, if marked values of different types overlap.
     * 
     * @param value
     *            Flag indicating whether constrained marked values are to be
     *            drawn above or below free ones.
     */
    public final void setConstrainedMarkedValueDrawnAboveFree(boolean value) {
        constrainedMarksDrawnAboveFree = value;
        determineMarkTypeOrder();
        scheduleDetermineActiveThumbIfEnabled();
        redraw();
    }

    /**
     * Get the absolute minimum possible value.
     * 
     * @return Absolute minimum possible value.
     */
    public final long getMinimumAllowableValue() {
        return minimumValue;
    }

    /**
     * Get the absolute maximum possible value.
     * 
     * @return Absolute maximum possible value.
     */
    public final long getMaximumAllowableValue() {
        return maximumValue;
    }

    /**
     * Set the absolute minimum and maximum possible values. Note that this
     * clears any individual minimum and maximum values for constrained thumbs
     * as set by {@link #setAllowableConstrainedValueRange(int, long, long)},
     * and sets the minimum constrained thumb gap to something compatible with
     * the new boundaries if the previous value for the gap is incompatible.
     * 
     * @param minimumValue
     *            New absolute minimum possible value; must be less than
     *            <code>maximumValue</code>.
     * @param maximumValue
     *            New absolute maximum possible value; must be greater than
     *            <code>minimumValue</code>.
     */
    public final void setAllowableValueRange(long minimumValue,
            long maximumValue) {

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
        if ((this.minimumValue == minimumValue)
                && (this.maximumValue == maximumValue)) {
            return;
        }

        /*
         * Set the new values.
         */
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;

        /*
         * Clear the constrained thumbs' individual minimum and maximum values,
         * if any.
         */
        allowableMinimumConstrainedThumbValues.clear();
        allowableMaximumConstrainedThumbValues.clear();

        /*
         * Ensure that the visible range is properly bounded by the new
         * allowable range.
         */
        long lowerVisValue = lowerVisibleValue;
        long upperVisValue = upperVisibleValue;
        if ((minimumValue > lowerVisValue) || (maximumValue < lowerVisValue)) {
            lowerVisValue = minimumValue;
            upperVisValue = minimumValue + DEFAULT_VISIBLE_OFFSET;
            if (upperVisValue > maximumValue) {
                upperVisValue = maximumValue;
            }
        }
        if ((maximumValue < upperVisValue) || (minimumValue > upperVisValue)) {
            upperVisValue = maximumValue;
            lowerVisValue = maximumValue - DEFAULT_VISIBLE_OFFSET;
            if (lowerVisValue < minimumValue) {
                lowerVisValue = minimumValue;
            }
        }

        /*
         * If the visible range has to be changed to fit within the new
         * allowable range, change it.
         */
        if ((lowerVisValue != lowerVisibleValue)
                || (upperVisValue != upperVisibleValue)) {
            visibleValueRangeChanged(lowerVisValue, upperVisValue,
                    ChangeSource.METHOD_INVOCATION);
        }

        /*
         * Adjust the minimum thumb gap if it is now too large to allow all the
         * thumbs to fit within the allowable value range.
         */
        if ((constrainedThumbValues.size() > 1)
                && (minimumConstrainedThumbGap * (constrainedThumbValues.size()
                        - 1) > getMaximumAllowableValue() + 1
                                - getMinimumAllowableValue())) {
            minimumConstrainedThumbGap = (getMaximumAllowableValue() + 1
                    - getMinimumAllowableValue())
                    / (constrainedThumbValues.size() - 1);
        }

        /*
         * Recalculate the thumb value minimum and maximum boundaries.
         */
        calculateConstrainedThumbValueBounds();

        /*
         * Ensure that the thumb values and marked values fall within the new
         * allowable range.
         */
        correctConstrainedThumbValues();
        correctFreeThumbValues();
        correctConstrainedMarkedValues();
        correctFreeMarkedValues();

        /*
         * Make sure that the right thumb, if any, is active.
         */
        scheduleDetermineActiveThumbIfEnabled();
    }

    /**
     * Get the minimum possible value for the specified constrained thumb.
     * 
     * @param index
     *            Index of the constrained thumb for which the minimum possible
     *            value is to be fetched.
     * @return Minimum possible value for the thumb, if one was specified
     *         previously via use of the
     *         {@link #setAllowableConstrainedValueRange(int, long, long)};
     *         otherwise the absolute minimum allowable value for all thumbs.
     */
    public final long getMinimumAllowableConstrainedValue(int index) {
        Long value = allowableMinimumConstrainedThumbValues.get(index);
        return (value == null ? minimumValue : value);
    }

    /**
     * Get the maximum possible value for the specified constrained thumb.
     * 
     * @param index
     *            Index of the constrained thumb for which the maximum possible
     *            value is to be fetched.
     * @return Maximum possible value for the thumb, if one was specified
     *         previously via use of the
     *         {@link #setAllowableConstrainedValueRange(int, long, long)};
     *         otherwise the absolute maximum allowable value for all thumbs.
     */
    public final long getMaximumAllowableConstrainedValue(int index) {
        Long value = allowableMaximumConstrainedThumbValues.get(index);
        return (value == null ? maximumValue : value);
    }

    /**
     * Set the minimum and maximum possible values for the specified constrained
     * thumb. Note that if the thumb's current value lies outside of this range,
     * it is repositioned to fall within the range.
     * 
     * @param index
     *            Index of the constrained thumb for which the minimum and
     *            maximum possible values are to be set.
     * @param minimumValue
     *            New absolute minimum possible value; must be less than or
     *            equal to <code>maximumValue</code>.
     * @param maximumValue
     *            New absolute maximum possible value; must be greater than or
     *            equal to <code>minimumValue</code>.
     * @return True if the minimum and maximum possible values for the specified
     *         thumb were set successfully, false otherwise. The latter occurs
     *         if said values would result in the thumb being too close to its
     *         neighboring thumbs, or would move past either of its neighbors as
     *         a result of the requested change.
     */
    public final boolean setAllowableConstrainedValueRange(int index,
            long minimumValue, long maximumValue) {

        /*
         * Ensure that the minimum value is less than or equal to the maximum
         * value.
         */
        if (minimumValue > maximumValue) {
            throw new IllegalArgumentException(
                    "minimum value must be less than or equal to maximum");
        }

        /*
         * If the boundaries have not changed, do nothing more.
         */
        Long oldMinimumValue = allowableMinimumConstrainedThumbValues
                .get(index);
        Long oldMaximumValue = allowableMaximumConstrainedThumbValues
                .get(index);
        if ((oldMinimumValue != null) && (oldMinimumValue == minimumValue)
                && (oldMaximumValue != null)
                && (oldMaximumValue == maximumValue)) {
            return true;
        }

        /*
         * Remember the new boundaries.
         */
        allowableMinimumConstrainedThumbValues.put(index, minimumValue);
        allowableMaximumConstrainedThumbValues.put(index, maximumValue);

        /*
         * If the thumb lies outside this new range, reposition it within the
         * range.
         */
        Long oldValue = constrainedThumbValues.get(index);
        if (oldValue != null) {
            if (oldValue < minimumValue) {
                constrainedThumbValues.set(index, minimumValue);
            } else if (oldValue > maximumValue) {
                constrainedThumbValues.set(index, maximumValue);
            }
        }

        /*
         * If the boundaries are incompatible with the current limitations,
         * reset the boundaries and the value to what they were before this
         * method was invoked and do nothing more.
         */
        if (calculateConstrainedThumbValueBounds() == false) {
            if (index < constrainedThumbValues.size()) {
                constrainedThumbValues.set(index, oldValue);
            }
            allowableMinimumConstrainedThumbValues.put(index, oldMinimumValue);
            allowableMaximumConstrainedThumbValues.put(index, oldMaximumValue);
            return false;
        }

        /*
         * Make sure that the right thumb, if any, is active.
         */
        scheduleDetermineActiveThumbIfEnabled();
        return true;
    }

    /**
     * Clear the minimum and maximum possible values for the specified
     * constrained thumb.
     * 
     * @param index
     *            Index of the constrained thumb for which the minimum and
     *            maximum possible values are to be cleared.
     */
    public final void clearAllowableConstrainedValueRange(int index) {

        /*
         * If the boundaries have not changed, do nothing more.
         */
        if ((allowableMinimumConstrainedThumbValues.get(index) == null)
                && (allowableMaximumConstrainedThumbValues
                        .get(index) == null)) {
            return;
        }
    }

    /**
     * Get the minimum value that is visible in the current viewport.
     * 
     * @return Minimum value that is visible in the current viewport.
     */
    public final long getLowerVisibleValue() {
        return lowerVisibleValue;
    }

    /**
     * Get the maximum value that is visible in the current viewport.
     * 
     * @return Maximum value that is visible in the current viewport.
     */
    public final long getUpperVisibleValue() {
        return upperVisibleValue;
    }

    /**
     * Set the range of values visible, changing the current viewport.
     * 
     * @param lowerVisibleValue
     *            New lower visible value; must be less than <code>
     *            upperVisibleValue</code>.
     * @param upperVisibleValue
     *            New upper visible value; must be greater than <code>
     *            lowerVisibleValue</code>.
     */
    public final void setVisibleValueRange(long lowerVisibleValue,
            long upperVisibleValue) {

        /*
         * Ensure that the lower visible value is less than the upper visible
         * value.
         */
        if (lowerVisibleValue >= upperVisibleValue) {
            throw new IllegalArgumentException(
                    "lower visible value must be less than upper");
        }

        /*
         * Change the visible value range.
         */
        visibleValueRangeChanged(lowerVisibleValue, upperVisibleValue,
                ChangeSource.METHOD_INVOCATION);
    }

    /**
     * Zoom to the specified visible value range. The lower and upper bounds of
     * the viewport will be adjusted to show the specified range, centered
     * around the current viewport's center value.
     * 
     * @param newVisibleValueRange
     *            New visible value range.
     * @return True if the zoom resulted in a change to the viewport, false
     *         otherwise.
     */
    public final boolean zoomVisibleValueRange(long newVisibleValueRange) {
        return zoomVisibleValueRange(newVisibleValueRange, 0.5,
                ChangeSource.METHOD_INVOCATION);
    }

    /**
     * Get the number of constrained thumb values.
     * 
     * @return Number of constrained thumb values.
     */
    public final int getConstrainedThumbValueCount() {
        return constrainedThumbValues.size();
    }

    /**
     * Get the value of the specified constrained thumb.
     * 
     * @param index
     *            Index of the constrained thumb for which the value is to be
     *            fetched.
     * @return Value.
     */
    public final long getConstrainedThumbValue(int index) {
        return constrainedThumbValues.get(index);
    }

    /**
     * Set the value of the specified constrained thumb, leaving its editability
     * unchanged.
     * 
     * @param index
     *            Index of the constrained thumb for which the value is to be
     *            set; must specify an index of an existing thumb.
     * @param value
     *            New value.
     * @return True if the value was set successfully, false otherwise. The
     *         latter occurs if the new value is too close to its neighboring
     *         thumbs, or would move past either of its neighbors as a result of
     *         the requested change.
     */
    public final boolean setConstrainedThumbValue(int index, long value) {

        /*
         * Sanity check the value provided, and use it only if it passes these
         * checks.
         */
        Long minimumForThumb = allowableMinimumConstrainedThumbValues
                .get(index);
        Long maximumForThumb = allowableMaximumConstrainedThumbValues
                .get(index);
        if ((value < minimumValue) || (value > maximumValue)) {
            throw new IllegalArgumentException("value " + value
                    + " for constrained thumb " + index + " out of range ("
                    + minimumValue + " to " + maximumValue + ")");
        } else if (((minimumForThumb != null) && (value < minimumForThumb))
                || ((maximumForThumb != null) && (value > maximumForThumb))) {
            throw new IllegalArgumentException(
                    "value " + value + " for constrained thumb " + index
                            + " out of range " + "designated for it ("
                            + minimumForThumb + " to " + maximumForThumb + ")");
        } else if ((value < minimumValue) || (value > maximumValue)) {
            throw new IllegalArgumentException("value " + value
                    + " for constrained thumb " + index + " out of range ("
                    + minimumValue + " to " + maximumValue + ")");
        } else if ((index > 0) && (value < constrainedThumbValues.get(index - 1)
                + minimumConstrainedThumbGap)) {
            return false;
        } else if ((index < constrainedThumbValues.size() - 1)
                && (value > constrainedThumbValues.get(index + 1)
                        - minimumConstrainedThumbGap)) {
            return false;
        } else {
            constrainedThumbValueChanged(index, value,
                    ChangeSource.METHOD_INVOCATION);
            if (areAllConstrainedValuesEditable() == false) {
                calculateConstrainedThumbValueBounds();
            }
            return true;
        }
    }

    /**
     * Get the list of values of the constrained thumbs.
     * 
     * @return Copy of the list of the values.
     */
    public final List<Long> getConstrainedThumbValues() {
        return new ArrayList<>(constrainedThumbValues);
    }

    /**
     * Set the values for all constrained thumbs. Any new thumbs created as a
     * result are assumed to be editable.
     * 
     * @param values
     *            Array of values, one per constrained thumb. The number of
     *            constrained thumbs may be changed via this method, unlike the
     *            single-value-setting method
     *            {@link #setConstrainedThumbValue(int, long)}. The values must
     *            be in increasing order.
     * @return True if the values were set successfully, otherwise false. The
     *         latter occurs if the new values would be too close to one
     *         another.
     */
    public final boolean setConstrainedThumbValues(long... values) {

        /*
         * Ensure that the values obey the usual rules.
         */
        for (int j = 0; j < values.length; j++) {
            if (((j > 0) && (values[j] < values[j - 1]))
                    || ((j < values.length - 1)
                            && (values[j] > values[j + 1]))) {
                throw new IllegalArgumentException(
                        "values for constrained thumbs must be "
                                + "specified in increasing order");
            } else if (((j > 0)
                    && (values[j] < values[j - 1] + minimumConstrainedThumbGap))
                    || ((j < values.length - 1) && (values[j] > values[j + 1]
                            - minimumConstrainedThumbGap))) {
                return false;
            }
            Long minimumForThumb = allowableMinimumConstrainedThumbValues
                    .get(j);
            Long maximumForThumb = allowableMaximumConstrainedThumbValues
                    .get(j);
            if (((minimumForThumb != null) && (values[j] < minimumForThumb))
                    || ((maximumForThumb != null)
                            && (values[j] > maximumForThumb))) {
                return false;
            }
        }
        if ((values.length > 0) && ((values[0] < minimumValue)
                || (values[values.length - 1] > maximumValue))) {
            throw new IllegalArgumentException(
                    "values for constrained thumbs out of range");
        }

        /*
         * Determine whether the value minimum and maximum boundaries need
         * recalculating once these new values are set.
         */
        boolean recalculateBoundaries = ((values.length != constrainedThumbValues
                .size()) || (areAllConstrainedValuesEditable() == false));

        /*
         * Change the values.
         */
        constrainedThumbValuesChanged(values, ChangeSource.METHOD_INVOCATION);

        /*
         * Recalculate the thumb value minimum and maximum boundaries if
         * necessary.
         */
        if (recalculateBoundaries) {
            calculateConstrainedThumbValueBounds();
        }

        /*
         * Determine the active thumb, and return success.
         */
        scheduleDetermineActiveThumbIfEnabled();
        return true;
    }

    /**
     * Determine whether the specified constrained thumb is editable.
     * 
     * @param index
     *            Index of the constrained thumb for which to check editability.
     * @return True if the specified thumb is editable, false otherwise.
     */
    public final boolean isConstrainedThumbEditable(int index) {
        return constrainedThumbValuesEditable.get(index);
    }

    /**
     * Set the editability of the specified constrained thumb.
     * 
     * @param index
     *            Index of the constrained thumb for which the editability is to
     *            be set; must specify an index of an existing thumb.
     * @param editable
     *            New editability.
     * @return True if the new editability flag value can be used, false if it
     *         cannot because the interplay of it with the various boundaries
     *         results in an illegal state.
     */
    public final boolean setConstrainedThumbEditable(int index,
            boolean editable) {

        /*
         * Remember the new flag value, or do nothing if the new value is the
         * same as the current one.
         */
        if (editable == constrainedThumbValuesEditable.get(index)) {
            return true;
        }
        Boolean oldEditable = constrainedThumbValuesEditable.set(index,
                editable);

        /*
         * If a thumb being dragged is now uneditable, end the drag.
         */
        if ((editable == false) && (draggingThumb != null)
                && (draggingThumb.type == ValueType.CONSTRAINED)
                && (draggingThumb.index == index)) {
            thumbDragEnded(null);
        }

        /*
         * Recalculate the thumb value minimum and maximum boundaries.
         */
        if (calculateConstrainedThumbValueBounds() == false) {
            constrainedThumbValuesEditable.set(index, oldEditable);
            return false;
        }

        /*
         * Ensure that the correct thumb is active.
         */
        scheduleDetermineActiveThumbIfEnabled();
        return true;
    }

    /**
     * Get the number of free thumb values.
     * 
     * @return Number of free thumb values.
     */
    public final int getFreeThumbValueCount() {
        return freeThumbValues.size();
    }

    /**
     * Get the value of the specified free thumb.
     * 
     * @param index
     *            Index of the free thumb for which the value is to be fetched.
     * @return Value.
     */
    public final long getFreeThumbValue(int index) {
        return freeThumbValues.get(index);
    }

    /**
     * Set the value of the specified free thumb, leaving its editability
     * unchanged.
     * 
     * @param index
     *            Index of the free thumb for which the value is to be set; must
     *            specify an index of an existing thumb.
     * @param value
     *            New value.
     */
    public final void setFreeThumbValue(int index, long value) {

        /*
         * Sanity check the value provided, and use it only if it passes these
         * checks.
         */
        if ((value < minimumValue) || (value > maximumValue)) {
            throw new IllegalArgumentException("value " + value
                    + " for free thumb " + index + " out of range");
        }
        freeThumbValueChanged(index, value, ChangeSource.METHOD_INVOCATION);
    }

    /**
     * Get the list of values of the free thumbs.
     * 
     * @return Copy of the list of the values.
     */
    public final List<Long> getFreeThumbValues() {
        return new ArrayList<>(freeThumbValues);
    }

    /**
     * Set the values for all free thumbs. Any new thumbs created as a result
     * are assumed to be editable.
     * 
     * @param values
     *            Array of values, one per free thumb. The number of free thumbs
     *            may be changed via this method, unlike the
     *            single-value-setting method
     *            {@link #setFreeThumbValue(int, long)}.
     */
    public final void setFreeThumbValues(long... values) {

        /*
         * Ensure that the values obey the usual rules.
         */
        for (long value : values) {
            if ((value < minimumValue) || (value > maximumValue)) {
                throw new IllegalArgumentException(
                        "values for free " + "thumbs out of range");
            }
        }

        /*
         * Change the values.
         */
        freeThumbValuesChanged(values, ChangeSource.METHOD_INVOCATION);

        /*
         * Determine the active thumb.
         */
        scheduleDetermineActiveThumbIfEnabled();
    }

    /**
     * Determine whether the specified free thumb is editable.
     * 
     * @param index
     *            Index of the free thumb for which to check editability.
     * @return True if the specified thumb is editable, false otherwise.
     */
    public final boolean isFreeThumbEditable(int index) {
        return freeThumbValuesEditable.get(index);
    }

    /**
     * Set the editability of the specified free thumb.
     * 
     * @param index
     *            Index of the free thumb for which the editability is to be
     *            set; must specify an index of an existing thumb.
     * @param editable
     *            New editability.
     */
    public final void setFreeThumbEditable(int index, boolean editable) {

        /*
         * Remember the new flag value, or do nothing if the new value is the
         * same as the current one.
         */
        if (editable == freeThumbValuesEditable.get(index)) {
            return;
        }
        freeThumbValuesEditable.set(index, editable);

        /*
         * If a thumb being dragged is now uneditable, end the drag.
         */
        if ((editable == false) && (draggingThumb != null)
                && (draggingThumb.type == ValueType.FREE)
                && (draggingThumb.index == index)) {
            thumbDragEnded(null);
        }

        /*
         * Ensure that the correct thumb is active.
         */
        scheduleDetermineActiveThumbIfEnabled();
    }

    /**
     * Get the number of constrained marked values.
     * 
     * @return Number of constrained marked values.
     */
    public final int getConstrainedMarkedValueCount() {
        return constrainedMarkedValues.size();
    }

    /**
     * Get the specified constrained marked value.
     * 
     * @param index
     *            Index of the constrained marked value to be fetched.
     * @return Value.
     */
    public final long getConstrainedMarkedValue(int index) {
        return constrainedMarkedValues.get(index);
    }

    /**
     * Set the constrained marked value at the specified index.
     * 
     * @param index
     *            Index of the constrained marked value to be set.
     * @param value
     *            Value to use as a constrained marked value.
     * @return True if the value was set successfully, false otherwise. The
     *         latter occurs if the new value would move past either of its
     *         neighbors as a result of the requested change.
     */
    public final boolean setConstrainedMarkedValue(int index, long value) {
        if ((value < minimumValue) || (value > maximumValue)) {
            throw new IllegalArgumentException("value " + value
                    + " for constrained mark " + index + " out of range");
        } else if ((index > 0)
                && (value < constrainedMarkedValues.get(index - 1))) {
            return false;
        } else if ((index < constrainedMarkedValues.size() - 1)
                && (value > constrainedMarkedValues.get(index + 1))) {
            return false;
        }
        constrainedMarkedValues.set(index, value);
        redraw();
        return true;
    }

    /**
     * Get the list of values of the constrained marked values.
     * 
     * @return Copy of the list of the values.
     */
    public final List<Long> getConstrainedMarkedValues() {
        return new ArrayList<>(constrainedMarkedValues);
    }

    /**
     * Set all the constrained marked values.
     * 
     * @param values
     *            Array of constrained marked values. The number of constrained
     *            marked values may be changed via this method, which is not
     *            true for {@link #setConstrainedMarkedValue(int, long)}. The
     *            values must be in increasing order.
     * @return True if the constrained marked values were set successfully,
     *         otherwise false.
     */
    public final boolean setConstrainedMarkedValues(long... values) {
        for (int j = 0; j < values.length; j++) {
            if (((j > 0) && (values[j] < values[j - 1]))
                    || ((j < values.length - 1)
                            && (values[j] > values[j + 1]))) {
                throw new IllegalArgumentException(
                        "values for constrained marks must be "
                                + "specified in increasing order");
            }
        }
        if ((values.length > 0) && ((values[0] < minimumValue)
                || (values[values.length - 1] > maximumValue))) {
            throw new IllegalArgumentException(
                    "values for constrained marks out of range");
        }
        constrainedMarkedValues.clear();
        for (int j = 0; j < values.length; j++) {
            constrainedMarkedValues.add(values[j]);
        }
        redraw();
        return true;
    }

    /**
     * Get the number of free marked values.
     * 
     * @return Number of free marked values.
     */
    public final int getFreeMarkedValueCount() {
        return freeMarkedValues.size();
    }

    /**
     * Get the specified free marked value.
     * 
     * @param index
     *            Index of the free marked value to be fetched.
     * @return Value.
     */
    public final long getFreeMarkedValue(int index) {
        return freeMarkedValues.get(index);
    }

    /**
     * Set the free marked value at the specified index.
     * 
     * @param index
     *            Index of the free marked value to be set.
     * @param value
     *            Value to use as a free marked value.
     */
    public final void setFreeMarkedValue(int index, long value) {
        if ((value < minimumValue) || (value > maximumValue)) {
            throw new IllegalArgumentException("value " + value
                    + " for free mark " + index + " out of range");
        }
        freeMarkedValues.set(index, value);
        redraw();
    }

    /**
     * Get the list of values of the free marked values.
     * 
     * @return Copy of the list of the values.
     */
    public final List<Long> getFreeMarkedValues() {
        return new ArrayList<>(freeMarkedValues);
    }

    /**
     * Set all the free marked values.
     * 
     * @param values
     *            Array of free marked values. The number of free marked values
     *            may be changed via this method, which is not the case with
     *            {@link #setFreeMarkedValue(int, long)}.
     */
    public final void setFreeMarkedValues(long... values) {
        for (long value : values) {
            if ((value < minimumValue) || (value > maximumValue)) {
                throw new IllegalArgumentException(
                        "values for free " + "free marks out of range");
            }
        }
        freeMarkedValues.clear();
        for (int j = 0; j < values.length; j++) {
            freeMarkedValues.add(values[j]);
        }
        redraw();
    }

    /**
     * Get the minimum value delta allowed between adjacent constrained thumbs.
     * 
     * @return Minimum delta between constrained thumbs.
     */
    public final long getMinimumDeltaBetweenConstrainedThumbs() {
        return minimumConstrainedThumbGap;
    }

    /**
     * Set the minimum value delta allowed between adjacent constrained thumbs.
     * 
     * @param minimumDelta
     *            New minimum delta between constrained thumbs; must be 0 or
     *            larger.
     * @return True if the minimum delta was successfully set, otherwise false.
     *         The latter occurs if the specified minimum delta is too large.
     */
    public final boolean setMinimumDeltaBetweenConstrainedThumbs(
            long minimumDelta) {

        /*
         * Sanity check the provided delta, and if it is okay, use it.
         */
        if (minimumDelta < 0L) {
            throw new IllegalArgumentException("minimum delta between "
                    + "constrained thumbs must be 0 or greater");
        } else if (minimumDelta
                * (constrainedThumbValues.size() - 1) > maximumValue + 1
                        - minimumValue) {
            return false;
        } else {

            /*
             * Remember the new delta.
             */
            long oldMinimumDelta = minimumConstrainedThumbGap;
            minimumConstrainedThumbGap = minimumDelta;

            /*
             * Recalculate the thumb value minimum and maximum boundaries.
             */
            if (calculateConstrainedThumbValueBounds() == false) {
                minimumConstrainedThumbGap = oldMinimumDelta;
                return false;
            }

            /*
             * Ensure that the thumb values are far enough apart given the new
             * delta.
             */
            correctConstrainedThumbValues();

            /*
             * Determine the active thumb, and return success.
             */
            scheduleDetermineActiveThumbIfEnabled();
            return true;
        }
    }

    /**
     * Get the color used to render the constrained marked value at the
     * specified index.
     * 
     * @param index
     *            Index of the constrained marked value for which the color is
     *            to be fetched.
     * @return Color, or <code>null</code> if no color is assigned to the
     *         specified constrained marked value.
     */
    public final Color getConstrainedMarkedValueColor(int index) {

        /*
         * If the index is too high for the colors list, return null if it is
         * within bounds for the marked value list. Otherwise, an index out of
         * bounds exception will occur when the former is accessed below.
         */
        if (index >= constrainedMarkedValueColors.size()) {
            if (index < getConstrainedMarkedValueCount()) {
                return null;
            }
        }
        return constrainedMarkedValueColors.get(index);
    }

    /**
     * Set the color used to render the constrained marked value at the
     * specified index.
     * 
     * @param index
     *            Index of the constrained marked value for which the color is
     *            to be set.
     * @param color
     *            Color to use, or <code>null</code> if the default color is to
     *            be used.
     */
    public final void setConstrainedMarkedValueColor(int index, Color color) {
        ensureIndexIsWithinBounds(index, getConstrainedMarkedValueCount());
        updateConstrainedMarkedValueColor(index, color);
        constrainedMarkedValueColorChanged(index, color);
        redraw();
    }

    /**
     * Get the color used to render the free marked value at the specified
     * index.
     * 
     * @param index
     *            Index of the free marked value for which the color is to be
     *            fetched.
     * @return Color, or <code>null</code> if no color is assigned to the
     *         specified free marked value.
     */
    public final Color getFreeMarkedValueColor(int index) {

        /*
         * If the index is too high for the colors list, return null if it is
         * within bounds for the marked value list. Otherwise, an index out of
         * bounds exception will occur when the former is accessed below.
         */
        if (index >= freeMarkedValueColors.size()) {
            if (index < getFreeMarkedValueCount()) {
                return null;
            }
        }
        return freeMarkedValueColors.get(index);
    }

    /**
     * Set the color used to render the free marked value at the specified
     * index.
     * 
     * @param index
     *            Index of the free marked value for which the color is to be
     *            set.
     * @param color
     *            Color to use, or <code>null</code> if the default color is to
     *            be used.
     */
    public final void setFreeMarkedValueColor(int index, Color color) {
        ensureIndexIsWithinBounds(index, getFreeMarkedValueCount());
        updateFreeMarkedValueColor(index, color);
        freeMarkedValueColorChanged(index, color);
        redraw();
    }

    /**
     * Get the color used to render the range between the specified constrained
     * marked value and the beginning of the widget if the specified index is 0,
     * or between the last constrained marked value and the end of the widget if
     * the specified constrained marked range is the last one, or the specified
     * constrained marked value and its predecessor otherwise.
     * 
     * @param index
     *            Index marking the end of the range for which the color is to
     *            be fetched.
     * @return Color, or <code>null</code> if no color is assigned to the
     *         specified range.
     */
    public final Color getConstrainedMarkedRangeColor(int index) {

        /*
         * If the index is too high for the colors list, return null if it is
         * within bounds for the marked range list. Otherwise, an index out of
         * bounds exception will occur when the former is accessed below.
         */
        if (index >= constrainedMarkedRangeColors.size()) {
            if (index < getConstrainedMarkedValueCount() + 1) {
                return null;
            }
        }
        return constrainedMarkedRangeColors.get(index);
    }

    /**
     * Set the color used to render the range between the specified constrained
     * marked value and the beginning of the widget if the specified index is 0,
     * or between the last constrained marked value and the end of the widget if
     * the specified constrained marked range is the last one, or the specified
     * constrained marked value and its predecessor otherwise.
     * 
     * @param index
     *            Index marking the end of the range for which the color is to
     *            be set.
     * @param color
     *            Color to use, or <code>null</code> if the default color is to
     *            be used.
     */
    public final void setConstrainedMarkedRangeColor(int index, Color color) {
        ensureIndexIsWithinBounds(index, getConstrainedMarkedValueCount() + 1);
        updateRangeColor(constrainedMarkedRangeColors, index, color);
        constrainedMarkedRangeColorChanged(index, color);
        redraw();
    }

    /**
     * Get the color used to render the range between the specified constrained
     * thumb and the beginning of the widget if the specified index is 0, or
     * between the last constrained thumb and the end of the widget if the
     * specified range is the last one, or the specified constrained thumb and
     * its predecessor otherwise.
     * 
     * @param index
     *            Index marking the end of the range for which the color is to
     *            be fetched.
     * @return Color, or <code>null</code> if no color is assigned to the
     *         specified range.
     */
    public final Color getConstrainedThumbRangeColor(int index) {

        /*
         * If the index is too high for the colors list, return null if it is
         * within bounds for the thumb range list. Otherwise, an index out of
         * bounds exception will occur when the former is accessed below.
         */
        if (index >= constrainedThumbRangeColors.size()) {
            if (index < getConstrainedThumbValueCount() + 1) {
                return null;
            }
        }
        return constrainedThumbRangeColors.get(index);
    }

    /**
     * Set the color used to render the range between the specified constrained
     * thumb and the beginning of the widget if the specified index is 0, or
     * between the last constrained thumb and the end of the widget if the
     * specified range is the last one, or the specified constrained thumb and
     * its predecessor otherwise.
     * 
     * @param index
     *            Index marking the end of the range for which the color is to
     *            be set.
     * @param color
     *            Color to use, or <code>null</code> if the default color is to
     *            be used.
     */
    public final void setConstrainedThumbRangeColor(int index, Color color) {
        ensureIndexIsWithinBounds(index, getConstrainedThumbValueCount() + 1);
        updateRangeColor(constrainedThumbRangeColors, index, color);
        constrainedThumbRangeColorChanged(index, color);
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
         * Determine the active thumb.
         */
        scheduleDetermineActiveThumbIfEnabled();
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
        return new Rectangle(x - leftInset, y - topInset,
                width + leftInset + rightInset,
                height + topInset + bottomInset);
    }

    @Override
    public final Rectangle getClientArea() {
        Rectangle bounds = getBounds();
        return new Rectangle(leftInset, topInset,
                bounds.width - (leftInset + rightInset),
                bounds.height - (topInset + bottomInset));
    }

    // Protected Methods

    /**
     * Respond to a change in the enabled state of the widget. Subclasses should
     * implement this method to change the visual cues to indicate whether or
     * not the widget is enabled.
     */
    protected abstract void widgetEnabledStateChanged();

    /**
     * Calculate the preferred size of the widget. This method, if it
     * (re)calculates the preferred width and height, must save the calculated
     * values using {@link #setPreferredSize(int, int)}.
     * <p>
     * This method must be called by any other methods that may result in the
     * changing of the preferred size. It may also be called with a <code>
     * force</code> parameter of false in order to ensure that the preferred
     * width and height have been calculated, for example, from within the
     * {@link #paintControl(PaintEvent)} method.
     * 
     * @param force
     *            Flag indicating whether or not the preferred size should be
     *            recomputed if already found to be computed.
     */
    protected abstract void computePreferredSize(boolean force);

    /**
     * Paint the widget.
     * 
     * @param e
     *            Event that triggered this invocation.
     */
    protected abstract void paintControl(PaintEvent e);

    /**
     * Determine which editable thumb the specified coordinates are within the
     * bounds of, if any.
     * <p>
     * Implementations must use {@link #isConstrainedThumbEditable(int)} and
     * {@link #isFreeThumbEditable(int)} to ensure that any thumb specifier
     * returned is indeed editable.
     * 
     * @param x
     *            X coordinate, relative to the widget.
     * @param y
     *            Y coordinate, relative to the widget.
     * @return Specifier of the editable thumb within which the coordinate
     *         exists, or <code>null</code> if the coordinate is not within a
     *         thumb.
     */
    protected abstract ThumbSpecifier getEditableThumbForCoordinates(int x,
            int y);

    /**
     * Handle the specified unused mouse release event. This particular
     * implementation does nothing. If a subclass wishes to utilize such unused
     * mouse release events, it may implement the necessary functionality by
     * overriding this method.
     * 
     * @param e
     *            Mouse event.
     */
    protected void handleUnusedMouseRelease(MouseEvent e) {

        /*
         * No action.
         */
    }

    /**
     * Get the vertical offset from the top of the widget that is where the top
     * of a tooltip being displayed for a thumb is located.
     * 
     * @return Vertical offset in pixels.
     */
    protected abstract int getThumbTooltipVerticalOffsetFromTop(
            ThumbSpecifier thumb);

    /**
     * Respond to notification that a change may have occurred in the boundary
     * of the area in which a mouse hover may generate a tooltip showing the
     * value under the mouse. This is called whenever the widget is resized, and
     * subclasses should also invoke it whenever any other property of their
     * widgets change that would affect this boundary.
     * <p>
     * Implementations should call {@link #setTooltipBounds(Rectangle)} when new
     * tooltip bounds have been calculated by this method.
     */
    protected abstract void tooltipBoundsChanged();

    /**
     * Respond to the disposal of the widget. The default implementation does
     * nothing, but may be overridden to dispose of resource that were allocated
     * by this widget.
     * 
     * @param e
     *            Disposal event that triggered this invocation.
     */
    protected void widgetDisposed(DisposeEvent e) {

        /*
         * No action.
         */
    }

    /**
     * Respond to the free marked value color at the specified index changing.
     * The default implementation does nothing, but may be overridden to
     * recreate any resources associated with this value.
     * 
     * @param index
     *            Index of the value that changed color.
     * @param color
     *            New color.
     */
    protected void freeMarkedValueColorChanged(int index, Color color) {

        /*
         * No action.
         */
    }

    /**
     * Respond to the constrained marked value color at the specified index
     * changing. The default implementation does nothing, but may be overridden
     * to recreate any resources associated with this value.
     * 
     * @param index
     *            Index of the value that changed color.
     * @param color
     *            New color.
     */
    protected void constrainedMarkedValueColorChanged(int index, Color color) {

        /*
         * No action.
         */
    }

    /**
     * Respond to the marked range color at the specified index changing. The
     * default implementation does nothing, but may be overridden to recreate
     * any resources associated with this range.
     * 
     * @param index
     *            Index of the range that changed color.
     * @param color
     *            New color.
     */
    protected void constrainedMarkedRangeColorChanged(int index, Color color) {

        /*
         * No action.
         */
    }

    /**
     * Respond to the thumb range color at the specified index changing. The
     * default implementation does nothing, but may be overridden to recreate
     * any resources associated with this range.
     * 
     * @param index
     *            Index of the range that changed color.
     * @param color
     *            New color.
     */
    protected void constrainedThumbRangeColorChanged(int index, Color color) {

        /*
         * No action.
         */
    }

    /**
     * Ensure that the specified argument is not <code>null</code>.
     * 
     * @param argName
     *            Name of the argument to be checked.
     * @param argRef
     *            Argument reference to be checked for nullness.
     * @throws IllegalArgumentException
     *             If the argument is <code>null</code>.
     */
    protected final void ensureArgumentIsNotNull(String argName,
            Object argRef) {
        if (argRef == null) {
            throw new IllegalArgumentException(argName + " must be non-null");
        }
    }

    /**
     * Ensure that the specified index falls between 0 (inclusive) and and the
     * specified size (exclusive).
     * 
     * @param index
     *            Index to be checked.
     * @param size
     *            Upper bound (exclusive).
     * @throws IndexOutOfBoundsException
     *             If the index does not fall within the boundaries.
     */
    protected final void ensureIndexIsWithinBounds(int index, int size) {
        if ((index < 0) || (index >= size)) {
            throw new IndexOutOfBoundsException(
                    "Index: " + index + ", Size: " + size);
        }
    }

    /**
     * Get the preferred width as last calculated.
     * 
     * @return Preferred width as last calculated.
     */
    protected final int getPreferredWidth() {
        return preferredWidth;
    }

    /**
     * Get the preferred height as last calculated.
     * 
     * @return Preferred height as last calculated.
     */
    protected final int getPreferredHeight() {
        return preferredHeight;
    }

    /**
     * Set the bounds within which a mouse hover over a value may generate a
     * tooltip. This should be called by subclasses' implementations of
     * {@link #tooltipBoundsChanged()} to set the new bounds.
     * 
     * @param tooltipBounds
     *            New bounds for tooltips.
     */
    protected final void setTooltipBounds(Rectangle tooltipBounds) {
        this.tooltip.setToolTipBounds(tooltipBounds);
    }

    /**
     * Get the bounds within which a mouse hover over a value may generate a
     * tooltip.
     * 
     * @return Bounds within which a mouse hover over a value may generate a
     *         tooltip.
     */
    protected final Rectangle getTooltipBounds() {
        return tooltip.getToolTipBounds();
    }

    /**
     * Get the specifier of the currently active thumb, if any.
     * 
     * @return Specifier of the currently active thumb, or <code>null
     *         </code> if there is no thumb currently active.
     */
    protected final ThumbSpecifier getActiveThumb() {
        return activeThumb;
    }

    /**
     * Get the specifier of the currently dragging thumb, if any.
     * 
     * @return Specifier of the currently dragging thumb, or <code>
     *         null </code> if there is no thumb currently being dragged.
     */
    protected final ThumbSpecifier getDraggingThumb() {
        return draggingThumb;
    }

    /**
     * Get the marked value type drawing order as an array of value types.
     * 
     * @return Marked value type drawing order as an array of value types.
     */
    protected final ValueType[] getMarkTypeDrawingOrder() {
        return markTypeDrawingOrder;
    }

    /**
     * Get the thumb type drawing order as an array of value types.
     * 
     * @return Thumb type drawing order as an array of value types.
     */
    protected final ValueType[] getThumbTypeDrawingOrder() {
        return thumbTypeDrawingOrder;
    }

    /**
     * Get the thumb type hit test order as an array of value types.
     * 
     * @return Thumb type hit test order as an array of value types.
     */
    protected final ValueType[] getThumbTypeHitTestOrder() {
        return thumbTypeHitTestOrder;
    }

    /**
     * Set the specified constrained thumb to the specified value.
     * 
     * @param index
     *            Index of the constrained thumb to be changed.
     * @param newValue
     *            New value.
     * @param source
     *            Source of the change.
     */
    protected void setConstrainedThumbValue(int index, long newValue,
            ChangeSource source) {
        constrainedThumbValueChanged(index, newValue, source);
    }

    /**
     * Set the specified free thumb to the specified value.
     * 
     * @param index
     *            Index of the free thumb to be changed.
     * @param newValue
     *            New value.
     * @param source
     *            Source of the change.
     */
    protected void setFreeThumbValue(int index, long newValue,
            ChangeSource source) {
        freeThumbValueChanged(index, newValue, source);
    }

    /**
     * Set the preferred width and height; this should be called by the
     * subclass's implementation of {@link #computePreferredSize(boolean)} to
     * store the dimensions preferred given no constraints.
     * 
     * @param preferredWidth
     *            Preferred width.
     * @param preferredHeight
     *            Preferred height.
     */
    protected final void setPreferredSize(int preferredWidth,
            int preferredHeight) {
        this.preferredWidth = preferredWidth;
        this.preferredHeight = preferredHeight;
    }

    /**
     * Zoom to the specified visible value range. The lower and upper bounds of
     * the viewport will be adjusted to show the specified range, centered
     * around the current viewport's center value.
     * 
     * @param newVisibleValueRange
     *            New visible value range.
     * @param centerLocation
     *            Number between <code>0.0</code> and <code>1.0</code> inclusive
     *            that indicates where along the range the center of the zoom is
     *            located, with the former value indicating the left hand side
     *            should be used as the zoom center location, and the latter
     *            indicating the right hand side. A value of <code>0.5</code>
     *            indicates that halfway along the range is the center.
     * @param source
     *            Source of the zoom.
     * @return True if the zoom resulted in a change to the viewport, false
     *         otherwise.
     */
    protected final boolean zoomVisibleValueRange(long newVisibleValueRange,
            double centerLocation, ChangeSource source) {

        /*
         * Determine the value currently visible at the specified zoom center
         * point within the viewport.
         */
        long center = ((long) (((getUpperVisibleValue() + 1L
                - getLowerVisibleValue()) * centerLocation) + 0.5))
                + getLowerVisibleValue();

        /*
         * Get the new visible value range boundaries.
         */
        long lower = center
                - (long) (((newVisibleValueRange) * centerLocation) + 0.5);
        long upper = lower + newVisibleValueRange - 1L;

        /*
         * Sanity check the bounds.
         */
        if (lower < getMinimumAllowableValue()) {
            upper += getMinimumAllowableValue() - lower;
            lower = getMinimumAllowableValue();
        }
        if (upper > getMaximumAllowableValue()) {
            lower -= upper - getMaximumAllowableValue();
            upper = getMaximumAllowableValue();
        }

        /*
         * If the value range has changed from what the viewport already had,
         * commit to the change.
         */
        if ((lower != getLowerVisibleValue())
                || (upper != getUpperVisibleValue())) {
            visibleValueRangeChanged(lower, upper, source);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Map the specified widget-client-area-relative X coordinate in pixel space
     * to a value along the control.
     * 
     * @param x
     *            X coordinate to be translated.
     * @param mustBeVisible
     *            Flag indicating whether or not the value to which the pixel is
     *            mapped must be currently visible.
     * @return Value.
     */
    protected final long mapPixelToValue(int x, boolean mustBeVisible) {
        if (lastWidth == 0) {
            return 0L;
        }
        if (mustBeVisible && (x < leftInset)) {
            x = leftInset;
        }
        if (mustBeVisible && (x >= leftInset + lastWidth)) {
            x = leftInset + lastWidth - 1;
        }
        return Math.round(((double) (x - leftInset))
                * ((double) (upperVisibleValue - lowerVisibleValue))
                / (lastWidth - 1.0)) + lowerVisibleValue;
    }

    /**
     * Map the specified value along the linear control to a
     * widget-client-area-relative X coordinate in pixel space. The resulting
     * coordinate may be calculated to be beyond the border of the widget.
     * 
     * @param value
     *            Value.
     * @return X coordinate.
     */
    protected final int mapValueToPixel(long value) {
        if (lastWidth == 0) {
            return 0;
        }
        long result = Math
                .round(((value - lowerVisibleValue) * (lastWidth - 1.0))
                        / (upperVisibleValue - lowerVisibleValue))
                + leftInset;
        if (result < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        } else if (result > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else {
            return (int) result;
        }
    }

    /**
     * Map the specified widget-client-area-relative width in pixel space to a
     * value delta along the control.
     * 
     * @param width
     *            Width.
     * @return Value delta.
     */
    protected final long mapPixelWidthToValueDelta(int width) {
        if (lastWidth == 0) {
            return 0L;
        }
        return Math.round(((double) width)
                * ((double) (upperVisibleValue - lowerVisibleValue))
                / lastWidth);
    }

    /**
     * Map the specified value delta along the control to a
     * widget-client-area-relative width in pixel space.
     * 
     * @param delta
     *            Value delta.
     * @return Width.
     */
    protected final int mapValueDeltaToPixelWidth(long delta) {
        long result = Math.round(((double) delta) * ((double) lastWidth)
                / (upperVisibleValue - lowerVisibleValue));
        if (result < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        } else if (result > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else {
            return (int) result;
        }
    }

    /**
     * Get the width of the client area.
     * 
     * @return Width of the client area.
     */
    protected final int getClientAreaWidth() {
        return lastWidth;
    }

    /**
     * Get the height of the client area.
     * 
     * @return Height of the client area.
     */
    protected final int getClientAreaHeight() {
        return lastHeight;
    }

    /**
     * Schedule a determination of the currently active thumb to occur after
     * current events have been processed if the mouse is positioned over the
     * widget and if the widget is active. If the determination is being
     * requested by a process that should occur quickly, this method is
     * preferable to {@link #determineActiveThumbIfEnabled()}.
     */
    protected final void scheduleDetermineActiveThumbIfEnabled() {

        /*
         * If the requested determination is already scheduled or the widget is
         * disposed, disabled, or invisible, do nothing.
         */
        if (isDisposed() || determinationOfActiveThumbScheduled
                || (isVisible() == false) || (isEnabled() == false)) {
            return;
        }

        /*
         * Schedule a determination to be made after other events are processed.
         */
        determinationOfActiveThumbScheduled = true;
        getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                determineActiveThumbIfEnabled();
            }
        });
    }

    /**
     * Determine the currently active thumb if the mouse is positioned over the
     * widget and if the widget is active. This method should not be used if the
     * caller requires a quick execution; in that case, use of
     * {@link #scheduleDetermineActiveThumbIfEnabled()} is preferable.
     */
    protected final void determineActiveThumbIfEnabled() {
        determinationOfActiveThumbScheduled = false;
        if ((isDisposed() == false) && isVisible() && isEnabled()) {
            determineActiveThumb();
        }
    }

    // Private Methods

    /**
     * Initialize the widget.
     * 
     * @param minimumValue
     *            Absolute minimum possible value.
     * @param maximumValue
     *            Absolute maximum possible value.
     */
    private void initializeMultiValueLinearControl(long minimumValue,
            long maximumValue) {

        /*
         * Remember the minimum and maximum values.
         */
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;

        /*
         * Determine the mark and thumb type drawing and hit test orders.
         */
        determineMarkTypeOrder();
        determineThumbTypeOrders();

        /*
         * Add a listener for paint request events.
         */
        addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                MultiValueLinearControl.this.paintControl(e);
            }
        });

        /*
         * Add a listener for resize events.
         */
        addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                MultiValueLinearControl.this.controlResized(e);
            }
        });

        /*
         * Add a listener for dispose events.
         */
        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                if (tooltip != null) {
                    tooltip.dispose();
                    tooltip = null;
                }
                draggingThumb = null;
                activeThumb = null;
                viewportWasActuallyDragged = false;
                mayBeDraggingViewport = false;
                MultiValueLinearControl.this.widgetDisposed(e);
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
                } else if ((tooltip != null) && tooltip.isVisible()) {
                    tooltip.setVisible(false);
                }
            }

            @Override
            public void mouseUp(MouseEvent e) {
                if (draggingThumb != null) {
                    thumbDragEnded(e);
                } else if (viewportWasActuallyDragged) {
                    viewportDragEnded(e);
                } else if ((e.button == 1) && isVisible() && isEnabled()) {
                    mayBeDraggingViewport = false;
                    handleUnusedMouseRelease(e);
                    mouseOverWidget(e.x, e.y);
                }
            }
        });
        addMouseMoveListener(new MouseMoveListener() {
            @Override
            public void mouseMove(MouseEvent e) {
                if (draggingThumb != null) {
                    thumbDragged(e);
                } else if (mayBeDraggingViewport) {
                    viewportDragged(e.x, false);
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
                if (activeThumb != null) {
                    activeThumb = null;
                    redraw();
                }
                showTooltipForThumb(null);
            }

            @Override
            public void mouseHover(MouseEvent e) {
                showTooltipForPoint(e);
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
        long lowerValue = -1L, upperValue = -1L;
        if (isVisible() && (lastWidth != 0)
                && (resizeBehavior == ResizeBehavior.CHANGE_VALUE_RANGE)) {

            /*
             * Get the value delta per pixel.
             */
            double valueDeltaPerPixel = ((double) (upperVisibleValue + 1L
                    - lowerVisibleValue)) / (double) lastWidth;

            /*
             * Determine how large the new viewport should be as a value delta.
             */
            long valueRange = Math.round(valueDeltaPerPixel * clientArea.width);

            /*
             * Get the lower and upper bounds of the viewport, adjusting them if
             * they go beyond the allowable values.
             */
            lowerValue = lowerVisibleValue;
            upperValue = lowerVisibleValue + valueRange;
            if (upperValue > maximumValue) {
                lowerValue -= upperValue - maximumValue;
                upperValue = maximumValue;
                if (lowerValue < minimumValue) {
                    lowerValue = minimumValue;
                }
            }
        }

        /*
         * Remember the now-current width.
         */
        lastWidth = clientArea.width;
        lastHeight = clientArea.height;

        /*
         * Recalculate the tooltip bounds.
         */
        if (tooltip != null) {
            tooltipBoundsChanged();
        }

        /*
         * If the value range was calculated above, commit the change;
         * otherwise, determine the active thumb.
         */
        if (lowerValue != -1L) {
            visibleValueRangeChanged(lowerValue, upperValue,
                    ChangeSource.RESIZE);
        } else {
            scheduleDetermineActiveThumbIfEnabled();
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
         * Reset the flag indicating that the viewport was actually dragged.
         */
        viewportWasActuallyDragged = false;

        /*
         * Get the editable thumb over which the mouse event occurred, if any.
         */
        ThumbSpecifier newDraggingThumb = getEditableThumbForCoordinates(e.x,
                e.y);

        /*
         * If a thumb has become active or inactive, redraw; otherwise, if
         * viewport dragging is allowed, start such a drag.
         */
        if ((draggingThumb != newDraggingThumb) && ((draggingThumb == null)
                || (draggingThumb.equals(newDraggingThumb) == false))) {
            draggingThumb = newDraggingThumb;
            redraw();
        } else if (isViewportDraggable() && (newDraggingThumb == null)) {
            mayBeDraggingViewport = true;
            lastViewportDragX = e.x;
        }
    }

    /**
     * Respond to a thumb being dragged.
     * 
     * @param e
     *            Mouse event that occurred.
     */
    private void thumbDragged(MouseEvent e) {
        dragThumbToPoint(draggingThumb, e, false);
        if (isDisposed() == false) {
            redraw();
            showTooltipForThumb(draggingThumb);
        }
    }

    /**
     * Respond to a thumb drag ending.
     * 
     * @param e
     *            Mouse event that occurred, if a mouse event triggered this
     *            invocation, or <code>null</code> if the invocation is the
     *            result of a programmatic change.
     */
    private void thumbDragEnded(MouseEvent e) {
        ThumbSpecifier thumb = draggingThumb;
        draggingThumb = null;
        if (e != null) {
            dragThumbToPoint(thumb, e, true);
            if (isDisposed() == false) {
                activeThumb = getEditableThumbForCoordinates(e.x, e.y);
            }
        }
        if (isDisposed() == false) {
            redraw();
        }
    }

    /**
     * Respond to the viewport being dragged.
     * 
     * @param x
     *            New X coordinate to which mouse cursor was dragged.
     * @param dragEnded
     *            Flag indicating whether or not the drag has ended.
     */
    private void viewportDragged(int x, boolean dragEnded) {
        if ((tooltip != null) && tooltip.isVisible()) {
            tooltip.setVisible(false);
        }
        long delta = mapPixelWidthToValueDelta(lastViewportDragX - x);
        if (lowerVisibleValue + delta < minimumValue) {
            delta = minimumValue - lowerVisibleValue;
        }
        if (upperVisibleValue + delta > maximumValue) {
            delta = maximumValue - upperVisibleValue;
        }
        lastViewportDragX = x;
        if ((delta == 0L) && (dragEnded == false)) {
            return;
        }
        viewportWasActuallyDragged = true;
        visibleValueRangeChanged(lowerVisibleValue + delta,
                upperVisibleValue + delta,
                (dragEnded ? ChangeSource.USER_GUI_INTERACTION_COMPLETE
                        : ChangeSource.USER_GUI_INTERACTION_ONGOING));
    }

    /**
     * Respond to a viewport drag ending.
     * 
     * @param e
     *            Mouse event that occurred.
     */
    private void viewportDragEnded(MouseEvent e) {
        mayBeDraggingViewport = false;
        viewportDragged(e.x, true);
    }

    /**
     * Determine the currently active thumb if the mouse is positioned over the
     * widget.
     */
    private void determineActiveThumb() {

        /*
         * Calculate the mouse location relative to this widget.
         */
        Point mouseLocation = getDisplay().getCursorLocation();
        Point offset = toDisplay(0, 0);
        mouseLocation.x -= offset.x;
        mouseLocation.y -= offset.y;

        /*
         * If the mouse is over the widget, process its position to determine
         * which thumb, if any, should now be active.
         */
        if ((mouseLocation.x >= 0) && (mouseLocation.x < lastWidth)
                && (mouseLocation.y >= 0) && (mouseLocation.y < lastHeight)) {
            mouseOverWidget(mouseLocation.x, mouseLocation.y);
        }
    }

    /**
     * Respond to the mouse cursor moving over the widget when a drag is not
     * occurring, or to an editability change for at least one thumb.
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
         * Get the editable thumb over which the mouse event occurred, if any.
         */
        ThumbSpecifier newThumb = getEditableThumbForCoordinates(x, y);

        /*
         * If a thumb has become active or inactive, redraw.
         */
        if ((activeThumb != newThumb) && ((activeThumb == null)
                || (activeThumb.equals(newThumb) == false))) {
            activeThumb = newThumb;
            redraw();
        }

        /*
         * Show or hide the thumb tooltip as appropriate.
         */
        showTooltipForThumb(newThumb);
    }

    /**
     * Show a tooltip for the specified thumb, or hide it if no thumb is
     * specified.
     * 
     * @param thumb
     *            Thumb for which the tooltip is to be shown, or
     *            <code>null</code> if it is to be hidden.
     */
    private void showTooltipForThumb(ThumbSpecifier thumb) {

        /*
         * If the tooltip does not exist, do nothing.
         */
        if (tooltip == null) {
            return;
        }

        /*
         * If the mouse is over a thumb, show the tooltip if there is text
         * available for it. If the tooltip is already visible and either there
         * is no text available or if no thumb is being hovered over, hide the
         * tooltip.
         */
        if (tooltipTextProvider != null) {
            if (thumb != null) {
                String[] text;
                long value;
                if (thumb.type == ValueType.CONSTRAINED) {
                    value = constrainedThumbValues.get(thumb.index);
                    text = tooltipTextProvider
                            .getTooltipTextForConstrainedThumb(this,
                                    thumb.index, value);
                } else {
                    value = freeThumbValues.get(thumb.index);
                    text = tooltipTextProvider.getTooltipTextForFreeThumb(this,
                            thumb.index, value);
                }
                if (text != null) {
                    setTooltipText(text);
                    Point widgetLocation = toDisplay(0, 0);
                    int thumbX = mapValueToPixel(value);
                    tooltip.setLocation(widgetLocation.x + thumbX,
                            widgetLocation.y
                                    + getThumbTooltipVerticalOffsetFromTop(
                                            thumb));
                    if (tooltip.isVisible() == false) {
                        tooltip.setVisible(true);
                    }
                } else if (tooltip.isVisible()) {
                    tooltip.setVisible(false);
                }
            } else if (tooltip.isVisible()) {
                tooltip.setVisible(false);
            }
        }
    }

    /**
     * Show a tooltip for the specified value.
     * 
     * @param e
     *            Mouse event that triggered this invocation.
     */
    private void showTooltipForPoint(MouseEvent e) {

        /*
         * If the tooltip does not exist, do nothing.
         */
        if (tooltip == null) {
            return;
        }

        /*
         * If the tooltip area does not exist or does not contain the point, do
         * nothing.
         */
        if ((getTooltipBounds() == null)
                || !getTooltipBounds().contains(e.x, e.y)) {
            return;
        }

        /*
         * If the tooltip is already visible, do nothing.
         */
        if (tooltip.isVisible()) {
            return;
        }

        /*
         * Get the value at the specified point.
         */
        long value = mapPixelToValue(e.x, true);

        /*
         * Show the tooltip if there is text available for it.
         */
        if (tooltipTextProvider != null) {
            String[] text;
            text = tooltipTextProvider.getTooltipTextForValue(this, value);
            if (text != null) {
                setTooltipText(text);
                Point widgetLocation = toDisplay(e.x, e.y);
                tooltip.setLocation(widgetLocation);
                if (tooltip.isVisible() == false) {
                    tooltip.setVisible(true);
                }
            } else if (tooltip.isVisible()) {
                tooltip.setVisible(false);
            }
        }
    }

    /**
     * Set the tooltip text to that specified.
     * 
     * @param text
     *            One- or two-element array of strings. If one element, then
     *            that is the message for the tooltip, and there is no title. If
     *            two elements, the first element is the title string, and the
     *            second the message.
     */
    private void setTooltipText(String[] text) {
        tooltip.setMessage(text[text.length == 1 ? 0 : 1]);
        tooltip.setText(text.length == 2 ? text[0] : "");
    }

    /**
     * Determine the thumb type drawing and hit test orders.
     */
    private void determineThumbTypeOrders() {
        thumbTypeDrawingOrder[0] = (isConstrainedThumbDrawnAboveFree()
                ? ValueType.FREE : ValueType.CONSTRAINED);
        thumbTypeDrawingOrder[1] = (isConstrainedThumbDrawnAboveFree()
                ? ValueType.CONSTRAINED : ValueType.FREE);
        thumbTypeHitTestOrder[0] = thumbTypeDrawingOrder[1];
        thumbTypeHitTestOrder[1] = thumbTypeDrawingOrder[0];
    }

    /**
     * Determine the marked value type drawing order.
     */
    private void determineMarkTypeOrder() {
        markTypeDrawingOrder[0] = (isConstrainedMarkedValueDrawnAboveFree()
                ? ValueType.FREE : ValueType.CONSTRAINED);
        markTypeDrawingOrder[1] = (isConstrainedMarkedValueDrawnAboveFree()
                ? ValueType.CONSTRAINED : ValueType.FREE);
    }

    /**
     * Get a copy of the specified list of values as an array.
     * 
     * @param values
     *            List of values to be copied.
     * @return Copy of the list as an array.
     */
    private long[] getCopyOfValues(List<Long> values) {
        long[] valuesCopy = new long[values.size()];
        for (int j = 0; j < valuesCopy.length; j++) {
            valuesCopy[j] = values.get(j);
        }
        return valuesCopy;
    }

    /**
     * Calculate the minimum and maximum allowable values for any thumb values.
     * If successful, the {@link #minConstrainedThumbValues} and
     * {@link #maxConstrainedThumbValues} are set to hold the newly calculated
     * boundaries.
     * 
     * @return True if the calculation results in a valid set of boundaries,
     *         false if the interplay of the various factors result in illegal
     *         boundaries (that is, boundaries for different constrained values
     *         that are mutually exclusive).
     */
    private boolean calculateConstrainedThumbValueBounds() {

        /*
         * Create the new boundary lists.
         */
        List<Long> minConstrainedThumbValues = new ArrayList<>(
                this.minConstrainedThumbValues.size());
        List<Long> maxConstrainedThumbValues = new ArrayList<>(
                this.maxConstrainedThumbValues.size());

        /*
         * Calculate the minimum allowable values for the thumb values.
         */
        for (int j = 0; j < constrainedThumbValues.size(); j++) {

            /*
             * If the thumb is editable, calculate the minimum as the lowest
             * value, if it is the first thumb, or as the previous value's
             * minimum plus the minimum interval, if not the first thumb.
             */
            long value;
            if (constrainedThumbValuesEditable.get(j)) {
                value = minimumValue;
                if (j > 0) {
                    long interval = (constrainedThumbIntervalLocked
                            ? constrainedThumbValues.get(j)
                                    - constrainedThumbValues.get(j - 1)
                            : minimumConstrainedThumbGap);
                    value = minConstrainedThumbValues.get(j - 1) + interval;
                }
                value = snapValueCalculator.getSnapThumbValue(value, value,
                        maximumValue);
            } else {
                value = constrainedThumbValues.get(j);
            }

            /*
             * If there are specific boundaries for this thumb, ensure the
             * minimum being calculated is at least the specific minimum.
             */
            Long minAllowable = allowableMinimumConstrainedThumbValues.get(j);
            if ((minAllowable != null) && (value < minAllowable)) {
                value = minAllowable;
            }

            minConstrainedThumbValues.add(value);
        }

        /*
         * Calculate the maximum allowable values for the thumb values. The list
         * of maximum values is generated backwards, so it has to be reversed
         * after the values are calculated.
         */
        for (int j = constrainedThumbValues.size() - 1; j >= 0; j--) {

            /*
             * If the thumb is editable, calculate the maximum as the highest
             * value, if it is the last thumb, or as the next value's maximum
             * minus the minimum interval, if not the last thumb.
             */
            long value;
            if (constrainedThumbValuesEditable.get(j)) {
                value = maximumValue;
                if (j < constrainedThumbValues.size() - 1) {
                    long interval = (constrainedThumbIntervalLocked
                            ? constrainedThumbValues.get(j + 1)
                                    - constrainedThumbValues.get(j)
                            : minimumConstrainedThumbGap);
                    value = maxConstrainedThumbValues.get(
                            constrainedThumbValues.size() - (j + 2)) - interval;
                }
                value = snapValueCalculator.getSnapThumbValue(value,
                        minimumValue, value);
            } else {
                value = constrainedThumbValues.get(j);
            }

            /*
             * If there are specific boundaries for this thumb, ensure the
             * maximum being calculated is at most the specific maximum.
             */
            Long maxAllowable = allowableMaximumConstrainedThumbValues.get(j);
            if ((maxAllowable != null) && (value > maxAllowable)) {
                value = maxAllowable;
            }

            maxConstrainedThumbValues.add(value);
        }
        Collections.reverse(maxConstrainedThumbValues);

        /*
         * Ensure that for each thumb, the minimum boundaries is not greater
         * than the maximum boundaries.
         */
        for (int j = 0; j < constrainedThumbValues.size(); j++) {
            if (minConstrainedThumbValues.get(j) > maxConstrainedThumbValues
                    .get(j)) {
                return false;
            }
        }

        /*
         * Since the calculations did not yield illegal boundaries, keep the new
         * boundaries.
         */
        this.minConstrainedThumbValues.clear();
        this.maxConstrainedThumbValues.clear();
        this.minConstrainedThumbValues.addAll(minConstrainedThumbValues);
        this.maxConstrainedThumbValues.addAll(maxConstrainedThumbValues);
        return true;
    }

    /**
     * Drag the specified thumb to the specified point, or as close to it as
     * possible, rearranging other thumbs to make room for this one.
     * 
     * @param thumb
     *            Specifier of the thumb to be dragged.
     * @param e
     *            Mouse event that prompted this drag.
     * @param dragEnded
     *            Flag indicating whether or not the drag has ended.
     */
    private void dragThumbToPoint(ThumbSpecifier thumb, MouseEvent e,
            boolean dragEnded) {
        if (thumb.type == ValueType.CONSTRAINED) {

            /*
             * Determine the value at which the thumb would be positioned if it
             * could move freely as far as it wanted; if this is the same value
             * that the thumb has now, do nothing more unless the drag has
             * ended.
             */
            long targetValue = snapValueCalculator.getSnapThumbValue(
                    mapPixelToValue(e.x, true),
                    minConstrainedThumbValues.get(thumb.index),
                    maxConstrainedThumbValues.get(thumb.index));
            if ((targetValue == constrainedThumbValues.get(thumb.index))
                    && (dragEnded == false)) {
                return;
            }

            /*
             * Get a copy of the thumb value so that they may be adjusted if
             * required.
             */
            long[] newValues = getCopyOfValues(constrainedThumbValues);

            /*
             * Handle the adjusting of the thumb values differently depending
             * upon whether the intervals between the thumbs are locked or not.
             */
            if (constrainedThumbIntervalLocked) {
                long delta = targetValue - newValues[thumb.index];
                for (int j = 0; j < newValues.length; j++) {
                    newValues[j] += delta;
                }
            } else {

                /*
                 * If the target value is greater than the current value for the
                 * thumb, then the thumb was dragged right; otherwise, the thumb
                 * was dragged left. Either way, adjust all the thumbs that are
                 * to that side of the moved thumb to ensure that appropriate
                 * spacing between them is kept.
                 */
                if (targetValue > newValues[thumb.index]) {
                    newValues[thumb.index] = targetValue;
                    for (int j = thumb.index + 1; j < newValues.length; j++) {
                        if (newValues[j - 1]
                                + minimumConstrainedThumbGap <= newValues[j]) {
                            break;
                        }
                        newValues[j] = snapValueCalculator
                                .getSnapThumbValue(newValues[j],
                                        newValues[j - 1]
                                                + minimumConstrainedThumbGap,
                                        maxConstrainedThumbValues.get(j));
                    }
                } else {
                    newValues[thumb.index] = targetValue;
                    for (int j = thumb.index - 1; j >= 0; j--) {
                        if (newValues[j + 1]
                                - minimumConstrainedThumbGap >= newValues[j]) {
                            break;
                        }
                        newValues[j] = snapValueCalculator.getSnapThumbValue(
                                newValues[j], minConstrainedThumbValues.get(j),
                                newValues[j + 1] - minimumConstrainedThumbGap);
                    }
                }
            }

            /*
             * Set the values to those calculated.
             */
            setConstrainedThumbValues(newValues,
                    (dragEnded ? ChangeSource.USER_GUI_INTERACTION_COMPLETE
                            : ChangeSource.USER_GUI_INTERACTION_ONGOING));
        } else {

            /*
             * Determine the value at which the thumb would be positioned if it
             * could move freely as far as it wanted; if this is the same value
             * that the thumb has now, do nothing more unless the drag has
             * ended.
             */
            long targetValue = snapValueCalculator.getSnapThumbValue(
                    mapPixelToValue(e.x, true), getMinimumAllowableValue(),
                    getMaximumAllowableValue());
            if ((targetValue == freeThumbValues.get(thumb.index))
                    && (dragEnded == false)) {
                return;
            }

            /*
             * Get a copy of the thumb value so that they may be adjusted, and
             * adjust the target thumb.
             */
            long[] newValues = getCopyOfValues(freeThumbValues);
            newValues[thumb.index] = targetValue;

            /*
             * Set the values to those calculated.
             */
            setFreeThumbValues(newValues,
                    (dragEnded ? ChangeSource.USER_GUI_INTERACTION_COMPLETE
                            : ChangeSource.USER_GUI_INTERACTION_ONGOING));
        }
    }

    /**
     * Ensure that the constrained thumb values are within the allowable value
     * range, and that each such thumb is far enough apart from its neighbors to
     * not be closer than the minimum constrained thumb gap.
     */
    private void correctConstrainedThumbValues() {

        /*
         * Get a copy of the thumb values so that they may be adjusted if
         * required.
         */
        long[] newValues = getCopyOfValues(constrainedThumbValues);

        /*
         * Iterate through the thumbs, ensuring that each in turn falls within
         * the allowable range.
         */
        for (int j = 0; j < newValues.length; j++) {
            if (newValues[j] < getMinimumAllowableValue()) {
                newValues[j] = getMinimumAllowableValue();
            } else if (newValues[j] > getMaximumAllowableValue()) {
                newValues[j] = getMaximumAllowableValue();
            }
        }

        /*
         * Iterate through the thumbs, ensuring that each in turn is at least
         * the minimum thumb gap distance from the previous one.
         */
        for (int j = 1; j < newValues.length; j++) {
            if (newValues[j] - newValues[j - 1] < minimumConstrainedThumbGap) {
                newValues[j] = newValues[j - 1] + minimumConstrainedThumbGap;
            }
        }

        /*
         * The iteration above may have pushed one or more thumbs beyond the
         * maximum allowable value; if so, push them back the other way, still
         * maintaining proper spacing between them.
         */
        if ((newValues.length > 0) && (newValues[newValues.length
                - 1] > getMaximumAllowableValue())) {
            long delta = newValues[newValues.length - 1]
                    - getMaximumAllowableValue();
            for (int j = newValues.length - 1; (j >= 0) && (delta > 0L); j--) {
                newValues[j] -= delta;
                if ((j > 0) && (newValues[j]
                        - newValues[j - 1] < minimumConstrainedThumbGap)) {
                    delta = minimumConstrainedThumbGap
                            - (newValues[j] - newValues[j - 1]);
                }
            }
        }

        /*
         * Set the values to those calculated.
         */
        setConstrainedThumbValues(newValues, ChangeSource.METHOD_INVOCATION);
    }

    /**
     * Ensure that the free thumb values are within the allowable value range.
     */
    private void correctFreeThumbValues() {

        /*
         * Get a copy of the thumb values so that they may be adjusted if
         * required.
         */
        long[] newValues = getCopyOfValues(freeThumbValues);

        /*
         * Iterate through the thumbs, ensuring that each in turn falls within
         * the allowable range.
         */
        for (int j = 0; j < newValues.length; j++) {
            if (newValues[j] < getMinimumAllowableValue()) {
                newValues[j] = getMinimumAllowableValue();
            } else if (newValues[j] > getMaximumAllowableValue()) {
                newValues[j] = getMaximumAllowableValue();
            }
        }

        /*
         * Set the values to those calculated.
         */
        setFreeThumbValues(newValues, ChangeSource.METHOD_INVOCATION);
    }

    /**
     * Ensure that the constrained marked values are within the allowable value
     * range.
     */
    private void correctConstrainedMarkedValues() {

        /*
         * Get a copy of the marked values so that they may be adjusted if
         * required.
         */
        long[] newValues = getCopyOfValues(constrainedMarkedValues);

        /*
         * Iterate through the values, ensuring that each in turn falls within
         * the allowable range.
         */
        for (int j = 0; j < newValues.length; j++) {
            if (newValues[j] < getMinimumAllowableValue()) {
                newValues[j] = getMinimumAllowableValue();
            } else if (newValues[j] > getMaximumAllowableValue()) {
                newValues[j] = getMaximumAllowableValue();
            }
        }

        /*
         * Set the values to those calculated.
         */
        setConstrainedMarkedValues(newValues);
    }

    /**
     * Ensure that the free marked values are within the allowable value range.
     */
    private void correctFreeMarkedValues() {

        /*
         * Get a copy of the marked values so that they may be adjusted if
         * required.
         */
        long[] newValues = getCopyOfValues(freeMarkedValues);

        /*
         * Iterate through the values, ensuring that each in turn falls within
         * the allowable range.
         */
        for (int j = 0; j < newValues.length; j++) {
            if (newValues[j] < getMinimumAllowableValue()) {
                newValues[j] = getMinimumAllowableValue();
            } else if (newValues[j] > getMaximumAllowableValue()) {
                newValues[j] = getMaximumAllowableValue();
            }
        }

        /*
         * Set the values to those calculated.
         */
        setFreeMarkedValues(newValues);
    }

    /**
     * Set the constrained thumb values to those specified.
     * 
     * @param newValues
     *            New constrained thumb values.
     * @param source
     *            Source of the change.
     */
    private void setConstrainedThumbValues(long[] newValues,
            ChangeSource source) {

        /*
         * If the values have had to be adjusted, set the new values.
         */
        boolean changed = (source == ChangeSource.USER_GUI_INTERACTION_COMPLETE);
        for (int j = 0; j < constrainedThumbValues.size(); j++) {
            if (constrainedThumbValues.get(j) != newValues[j]) {
                changed = true;
                break;
            }
        }
        if (changed) {
            constrainedThumbValuesChanged(newValues, source);
        }
    }

    /**
     * Set the free thumb values to those specified.
     * 
     * @param newValues
     *            New free thumb values.
     * @param source
     *            Source of the change.
     */
    private void setFreeThumbValues(long[] newValues, ChangeSource source) {

        /*
         * If the values have had to be adjusted, set the new values.
         */
        boolean changed = (source == ChangeSource.USER_GUI_INTERACTION_COMPLETE);
        for (int j = 0; j < freeThumbValues.size(); j++) {
            if (freeThumbValues.get(j) != newValues[j]) {
                changed = true;
                break;
            }
        }
        if (changed) {
            freeThumbValuesChanged(newValues, source);
        }
    }

    /**
     * Update the constrained marked value color.
     * 
     * @param index
     *            Index of the constrained marked value color being updated;
     *            must be a valid constrained marked value index.
     * @param color
     *            Color to use at the specified index.
     */
    private void updateConstrainedMarkedValueColor(int index, Color color) {
        while (constrainedMarkedValueColors.size() < index) {
            constrainedMarkedValueColors.add(null);
        }
        if (constrainedMarkedValueColors.size() == index) {
            constrainedMarkedValueColors.add(color);
        } else {
            constrainedMarkedValueColors.set(index, color);
        }
    }

    /**
     * Update the free marked value color.
     * 
     * @param index
     *            Index of the free marked value color being updated; must be a
     *            valid free marked value index.
     * @param color
     *            Color to use at the specified index.
     */
    private void updateFreeMarkedValueColor(int index, Color color) {
        while (freeMarkedValueColors.size() < index) {
            freeMarkedValueColors.add(null);
        }
        if (freeMarkedValueColors.size() == index) {
            freeMarkedValueColors.add(color);
        } else {
            freeMarkedValueColors.set(index, color);
        }
    }

    /**
     * Update the range color for the specified list of ranges at the specified
     * index.
     * 
     * @param list
     *            List of range colors to be updated.
     * @param index
     *            Index of the track color being updated; must be between 0 and
     *            the number of track tiles inclusive.
     * @param color
     *            Color to use at the specified index.
     */
    private void updateRangeColor(List<Color> list, int index, Color color) {
        while (list.size() < index) {
            list.add(null);
        }
        if (list.size() == index) {
            list.add(color);
        } else {
            list.set(index, color);
        }
    }

    /**
     * Respond to the visible value range being changed.
     * 
     * @param lowerVisibleValue
     *            New lower visible value boundary.
     * @param upperVisibleValue
     *            New upper visible value boundary.
     * @param source
     *            Source of the change.
     */
    private void visibleValueRangeChanged(long lowerVisibleValue,
            long upperVisibleValue, ChangeSource source) {

        /*
         * Do sanity checks on the new visible range.
         */
        if (lowerVisibleValue > maximumValue) {
            lowerVisibleValue = maximumValue - DEFAULT_VISIBLE_OFFSET;
        }
        if (lowerVisibleValue < minimumValue) {
            lowerVisibleValue = minimumValue;
        }
        if (upperVisibleValue < minimumValue) {
            upperVisibleValue = minimumValue + DEFAULT_VISIBLE_OFFSET;
        }
        if (upperVisibleValue > maximumValue) {
            upperVisibleValue = maximumValue;
        }

        /*
         * Remember the new range, determine the active thumb if any, and
         * redraw.
         */
        this.lowerVisibleValue = lowerVisibleValue;
        this.upperVisibleValue = upperVisibleValue;
        scheduleDetermineActiveThumbIfEnabled();
        redraw();

        /*
         * Notify listeners.
         */
        for (IMultiValueLinearControlListener listener : listeners) {
            listener.visibleValueRangeChanged(this, lowerVisibleValue,
                    upperVisibleValue, source);
        }
    }

    /**
     * Respond to a single constrained thumb value being changed.
     * 
     * @param index
     *            Index of the value being changed.
     * @param newValue
     *            New value.
     * @param source
     *            Source of the change.
     */
    private void constrainedThumbValueChanged(int index, long newValue,
            ChangeSource source) {
        constrainedThumbValues.set(index, newValue);
        scheduleDetermineActiveThumbIfEnabled();
        redraw();
        if (listeners.size() > 0) {
            long[] newValues = getCopyOfValues(constrainedThumbValues);
            for (IMultiValueLinearControlListener listener : listeners) {
                listener.constrainedThumbValuesChanged(this, newValues, source);
            }
        }
    }

    /**
     * Respond to a single free thumb value being changed.
     * 
     * @param index
     *            Index of the value being changed.
     * @param newValue
     *            New value.
     * @param source
     *            Source of the change.
     */
    private void freeThumbValueChanged(int index, long newValue,
            ChangeSource source) {
        freeThumbValues.set(index, newValue);
        scheduleDetermineActiveThumbIfEnabled();
        redraw();
        if (listeners.size() > 0) {
            long[] newValues = getCopyOfValues(freeThumbValues);
            for (IMultiValueLinearControlListener listener : listeners) {
                listener.freeThumbValuesChanged(this, newValues, source);
            }
        }
    }

    /**
     * Respond to one or more constrained thumb values being changed.
     * 
     * @param newValues
     *            New values; this array may hold a different number of values
     *            from the existing values list for constrained values.
     * @param source
     *            Source of the change.
     */
    private void constrainedThumbValuesChanged(long[] newValues,
            ChangeSource source) {
        constrainedThumbValues.clear();
        for (int j = 0; j < newValues.length; j++) {
            constrainedThumbValues.add(newValues[j]);
            if (constrainedThumbValuesEditable.size() <= j) {
                constrainedThumbValuesEditable.add(true);
            }
        }
        while (constrainedThumbValuesEditable.size() > newValues.length) {
            constrainedThumbValuesEditable.remove(newValues.length);
        }
        redraw();
        for (IMultiValueLinearControlListener listener : listeners) {
            listener.constrainedThumbValuesChanged(this, newValues, source);
        }
    }

    /**
     * Respond to one or more free thumb values being changed.
     * 
     * @param newValues
     *            New values; this array may hold a different number of values
     *            from the existing values list for free values.
     * @param source
     *            Source of the change.
     */
    private void freeThumbValuesChanged(long[] newValues, ChangeSource source) {
        freeThumbValues.clear();
        for (int j = 0; j < newValues.length; j++) {
            freeThumbValues.add(newValues[j]);
            if (freeThumbValuesEditable.size() <= j) {
                freeThumbValuesEditable.add(true);
            }
        }
        while (freeThumbValuesEditable.size() > newValues.length) {
            freeThumbValuesEditable.remove(newValues.length);
        }
        redraw();
        for (IMultiValueLinearControlListener listener : listeners) {
            listener.freeThumbValuesChanged(this, newValues, source);
        }
    }

    /**
     * Determine whether or not all constrained thumb values are editable.
     * 
     * @return True if all constrained thumb values are editable, false
     *         otherwise.
     */
    private boolean areAllConstrainedValuesEditable() {
        for (boolean editable : constrainedThumbValuesEditable) {
            if (editable == false) {
                return false;
            }
        }
        return true;
    }
}
