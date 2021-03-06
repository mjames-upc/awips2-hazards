/**
 * This software was developed and / or modified by the
 * National Oceanic and Atmospheric Administration (NOAA), 
 * Earth System Research Laboratory (ESRL), 
 * Global Systems Division (GSD), 
 * Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.megawidgets;

import gov.noaa.gsd.common.utilities.TimeResolution;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.eclipse.nebula.widgets.cdatetime.CDT;
import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import com.google.common.collect.ImmutableMap;

/**
 * Description: Date-time component, providing an encapsulation of a single pair
 * of widgets, one allowing the viewing and manipulation of a date, the other a
 * time, to be used as part of time-oriented megawidgets.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 16, 2013    2545    Chris.Golden      Initial creation
 * Feb 08, 2014    2161    Chris.Golden      Added ability to display custom strings
 *                                           in place of date-time values. Also
 *                                           forced the minimum width to be large
 *                                           enough for each component to display
 *                                           date-time values. Also corrected
 *                                           Javadoc.
 * Mar 08, 2014    2155    Chris.Golden      Fixed bugs with date-time fields in
 *                                           time megawidgets that caused unexpected
 *                                           date-times to be selected when the user
 *                                           manipulated the drop-down calendar.
 * Apr 24, 2014    2925    Chris.Golden      Changed to work with new validator
 *                                           package, updated Javadoc and other
 *                                           comments.
 * Jun 27, 2014    3512    Chris.Golden      Changed to implement new ITimeComponent
 *                                           interface, and fixed minor visual bug
 *                                           that caused the time spinner up/down
 *                                           buttons to look enabled when the
 *                                           component had editable as true but
 *                                           enabled as false.
 * Mar 29, 2015    6874    Chris.Golden      Fixed bugs that caused some valid values
 *                                           entered by users in the date/time fields
 *                                           to be rejected.
 * Mar 31, 2015    6873    Chris.Golden      Added code to ensure that mouse wheel
 *                                           events are not processed by the
 *                                           megawidget, but are instead passed up
 *                                           to any ancestor that is a scrolled
 *                                           composite.
 * Apr 10, 2015    7394    Robert.Blum       Simplified the code to use one Date object
 *                                           to back both widgets and also made the 
 *                                           values roll correctly.
 * Oct 19, 2016   21873    Chris.Golden      Added time resolution option.
 * </pre>
 * 
 * @author Chris.Golden
 * @version 1.0
 */
public class DateTimeComponent implements ITimeComponent {

    // Private Static Constants

    /**
     * Time zone.
     */
    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("GMT");

    /**
     * Formatter for date-time strings.
     */
    private static final SimpleDateFormat SHORT_DATE_FORMATTER = new SimpleDateFormat(
            "HH:mm dd-MM-yy");
    static {
        SHORT_DATE_FORMATTER.setTimeZone(TIME_ZONE);
    }

    /**
     * Text to display for the date when no date-time has been chosen.
     */
    private static final String NULL_DATE_TEXT = "(no date)";

    /**
     * Text to display for the time when no date-time has been chosen.
     */
    private static final String NULL_TIME_TEXT = "--:--";

    /**
     * Formatter for date strings.
     */
    private static final String DATE_FORMAT = "dd-MMM-yyyy";

    /**
     * Map of time resolutions to the formats of the time strings at those
     * resolutions.
     */
    private static final Map<TimeResolution, String> TIME_FORMATS_FOR_RESOLUTIONS;
    static {
        EnumMap<TimeResolution, String> map = new EnumMap<>(
                TimeResolution.class);
        map.put(TimeResolution.MINUTES, "HH:mm");
        map.put(TimeResolution.SECONDS, "HH:mm:ss");
        TIME_FORMATS_FOR_RESOLUTIONS = ImmutableMap.copyOf(map);
    }

    /**
     * Longest date text that could be displayed.
     */
    private static final String LONGEST_DATE_TEXT = "00-May-0000";

    /**
     * Map of time resolutions to the longest time strings that are possible at
     * those resolutions.
     */
    private static final Map<TimeResolution, String> LONGEST_TIME_TEXTS_FOR_RESOLUTIONS;
    static {
        EnumMap<TimeResolution, String> map = new EnumMap<>(
                TimeResolution.class);
        map.put(TimeResolution.MINUTES, "00:00");
        map.put(TimeResolution.SECONDS, "00:00:00");
        LONGEST_TIME_TEXTS_FOR_RESOLUTIONS = ImmutableMap.copyOf(map);
    }

    private static final long MILLISECONDS_IN_MINUTE = 60000;

    private static final long MILLISECONDS_IN_HOUR = MILLISECONDS_IN_MINUTE * 60;

    private static final long MILLISECONDS_IN_DAY = MILLISECONDS_IN_HOUR * 24;

    // Private Classes

    /**
     * Date-time widget that correctly sets its component text widget's
     * background color when its background is set, and that forwards adding and
     * removal of key and mouse listeners to the appropriate component SWT
     * widgets.
     */
    private class DateTime extends CDateTime {

        // Public Constructors

        /**
         * Construct a standard instance.
         * 
         * @param parent
         *            Parent composite.
         * @param style
         *            Style.
         */
        public DateTime(Composite parent, int style) {
            super(parent, style);
        }

        // Public Methods

        @Override
        public Color getBackground() {
            return text.getControl().getBackground();
        }

        @Override
        public void setBackground(Color color) {
            text.getControl().setBackground(color);
        }

        @Override
        public void addKeyListener(KeyListener listener) {

            /*
             * This listener is added to the component text field, and then the
             * text field's other listeners are re-added, since the latter
             * removes them and adds them again, allowing the listener specified
             * here to fire before the built-in listeners do.
             */
            text.getControl().addKeyListener(listener);
            addTextListener();
        }

        @Override
        public void removeKeyListener(KeyListener listener) {
            text.getControl().removeKeyListener(listener);
        }

        @Override
        public void addMouseListener(MouseListener listener) {
            Control spinner = getSpinner();
            if (spinner != null) {
                spinner.addMouseListener(listener);
            }
        }

        @Override
        public void removeMouseListener(MouseListener listener) {
            Control spinner = getSpinner();
            if (spinner != null) {
                spinner.removeMouseListener(listener);
            }
        }

        /**
         * Get a calendar instance with the specified date.
         * 
         * @param date
         *            Date to be used for the current time. If <code>null</code>
         *            the current time will be used instead.
         * @return Calendar instance.
         */
        @Override
        public Calendar getCalendarInstance(Date date) {
            long time = (date == null ? holder.getCurrentTime() : date
                    .getTime());
            Calendar calendar = Calendar.getInstance(TIME_ZONE,
                    Locale.getDefault());
            calendar.setTimeInMillis(time);
            return calendar;
        }

        @Override
        public void setSelection(Date selection) {
            super.setSelection(selection);
            if (selection == null) {
                text.getControl().clearSelection();
            }
        }

        /**
         * Set the spinner component's enabled state.
         * 
         * @param enable
         *            Flag indicating whether or not the spinner component
         *            should be enabled.
         */
        public void setSpinnerEnabled(boolean enable) {
            Control spinner = getSpinner();
            if (spinner != null) {
                spinner.setEnabled(enable);
            }
        }

        // Protected Methods

        @Override
        protected void postClose(Shell popup) {
            super.postClose(popup);
            notifyListenersOfEndingStateChange();
        }

        @Override
        protected void addTextListener() {

            /*
             * Ensure that the text listener does not process mouse wheel
             * events.
             */
            super.addTextListener();
            Text control = text.getControl();
            for (Listener listener : control.getListeners(SWT.MouseWheel)) {
                control.removeListener(SWT.MouseWheel, listener);
            }

            /*
             * Ensure that the spinner, if one is being used, does not process
             * mouse wheel events either.
             */
            Control spinner = getSpinner();
            if (spinner != null) {
                UiBuilder.ensureMouseWheelEventsPassedUpToAncestor(spinner);
            }
        }

        // Private Methods

        /**
         * Get the spinner that is a part of this widget, if any.
         * 
         * @return Spinner that is a part of this widget, or <code>null</code>
         *         if there is no such component.
         */
        private Control getSpinner() {

            /*
             * This roundabout way of getting the spinner is required so as to
             * allow access to what is a package-private object within
             * CDateTime. The alternative would be to make our own copy of
             * CDateTime and just make all the mods there, but that seemed
             * messier.
             */
            Control[] children = panel.getComposite().getChildren();
            for (Control child : children) {
                if (child instanceof Spinner) {
                    return child;
                }
            }
            return null;
        }
    }

    // Private Variables

    /**
     * Identifier.
     */
    private final String identifier;

    /**
     * Current value in milliseconds since the epoch.
     */
    private long state;

    /**
     * Previous value in milliseconds since the epoch.
     */
    private long oldState;

    /**
     * Flag indicating whether state changes that occur as a result of a rapidly
     * repeatable action should be forwarded or not.
     */
    private final boolean onlySendEndStateChanges;

    /**
     * Last value that the state change listener knows about; this is used only
     * if {@link #onlySendEndStateChanges} is true.
     */
    private long lastForwardedState;

    /**
     * Label of the megawidget, if any.
     */
    private final Label label;

    /**
     * Date selector.
     */
    private final DateTime date;

    /**
     * Time selector.
     */
    private final DateTime time;

    /**
     * Date object for the both the Date and Time widgets that is reused to
     * avoid constant reallocation.
     */
    private final Date timestamp = new Date();

    /**
     * Calendar object that is reused to avoid constant reallocation.
     */
    private final Calendar calendar = Calendar.getInstance();

    /**
     * Holder of this date-time component.
     */
    private final IDateTimeComponentHolder holder;

    /**
     * Flag indicating whether or not a rapid state change is in progress within
     * the text area of either the date widget or the time widget.
     */
    private boolean rapidStateChangeInTextFieldInProgress;

    // Public Static Methods

    /**
     * Get a description of the specified state.
     * 
     * @param state
     *            State to be rendered in descriptive string form.
     * @return Description in string form of the specified state.
     */
    public static String getStateDescription(long state) {
        return SHORT_DATE_FORMATTER.format(state);
    }

    // Public Constructors

    /**
     * Construct a standard instance.
     * 
     * @param identifier
     *            Identifier of this date-time component.
     * @param parent
     *            Parent composite in which to locate the date-time component's
     *            widgets.
     * @param text
     *            Label text to be used, or <code>null</code> if no label is to
     *            be shown.
     * @param specifier
     *            Control specifier for the megawidget that is the holder of the
     *            this date-time component.
     * @param startingValue
     *            Starting value of the date-time component.
     * @param timeResolution
     *            Time resolution to be used.
     * @param grabExcessHorizontalSpace
     *            Flag indicating whether or not the component's widgets should
     *            grab excess horizontal space.
     * @param verticalIndent
     *            Vertical indent to be applied.
     * @param onlySendEndStateChanges
     *            Flag indicating whether or not only end state changes should
     *            be sent along as notifications.
     * @param holder
     *            Holder of this date-time component.
     */
    public DateTimeComponent(String identifier, Composite parent, String text,
            IControlSpecifier specifier, long startingValue,
            TimeResolution timeResolution, boolean grabExcessHorizontalSpace,
            int verticalIndent, boolean onlySendEndStateChanges,
            IDateTimeComponentHolder holder) {
        this.identifier = identifier;
        this.holder = holder;
        lastForwardedState = state = startingValue;

        /*
         * Set up the calendar to be used for finding the closest previous
         * midnight of any given epoch time.
         */
        calendar.setTimeZone(TIME_ZONE);

        /*
         * Create the composite holding the components, and the label if
         * appropriate. Remove any horizontal spacing from the panel, as it
         * would be too much space between the date and time widgets; instead,
         * remember the spacing that was going to be used so as to use it to the
         * left of the date widget to visually separate it from the label (if
         * the label is used).
         */
        Composite panel = UiBuilder.buildComposite(parent, 3, SWT.NONE,
                UiBuilder.CompositeType.SINGLE_ROW, specifier);
        GridData panelLayoutData = (GridData) panel.getLayoutData();
        panelLayoutData.grabExcessHorizontalSpace = grabExcessHorizontalSpace;
        panelLayoutData.verticalIndent = verticalIndent;
        GridLayout layout = (GridLayout) panel.getLayout();
        int spacingAfterLabel = layout.horizontalSpacing;
        layout.horizontalSpacing = 0;
        label = (text == null ? null : UiBuilder.buildLabel(panel, text,
                specifier));

        /*
         * Set up the timestamp trackers and determine whether or not only
         * ending state changes are to be sent along to listeners.
         */
        timestamp.setTime(state);
        this.onlySendEndStateChanges = onlySendEndStateChanges;

        /*
         * Create the date selector, calculating the minimum width required for
         * it to ensure that it stays at least that wide regardless of what text
         * is displayed by it.
         */
        date = new DateTime(panel, CDT.BORDER | CDT.DROP_DOWN | CDT.TAB_FIELDS
                | CDT.COMPACT);
        date.setNullText(LONGEST_DATE_TEXT);
        date.setSelection(null);
        int dateWidth = date.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        date.setNullText(NULL_DATE_TEXT);
        date.setPattern(DATE_FORMAT);
        date.setTimeZone(TIME_ZONE);
        date.setSelection(timestamp);
        date.setEnabled(specifier.isEnabled());

        /*
         * Place the date selector in the panel's grid.
         */
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, false, false);
        gridData.horizontalSpan = (label == null ? 2 : 1);
        gridData.minimumWidth = dateWidth;
        gridData.widthHint = dateWidth;
        if (label != null) {
            gridData.horizontalIndent = spacingAfterLabel;
        }
        date.setLayoutData(gridData);

        /*
         * Create the time selector; as with the date selector, the minimum
         * width is calculated and applied so that it stays at least that wide.
         */
        time = new DateTime(panel, CDT.BORDER | CDT.SPINNER | CDT.TAB_FIELDS);
        time.setNullText(LONGEST_TIME_TEXTS_FOR_RESOLUTIONS.get(timeResolution));
        time.setSelection(null);
        int timeWidth = time.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        time.setNullText(NULL_TIME_TEXT);
        time.setPattern(TIME_FORMATS_FOR_RESOLUTIONS.get(timeResolution));
        time.setTimeZone(TIME_ZONE);
        time.setSelection(timestamp);
        time.setEnabled(specifier.isEnabled());

        /*
         * Place the time selector in the panel's grid.
         */
        gridData = new GridData(SWT.FILL, SWT.CENTER, false, false);
        gridData.minimumWidth = timeWidth;
        gridData.widthHint = timeWidth;
        time.setLayoutData(gridData);

        /*
         * If only ending state changes are to result in notifications, bind the
         * date-time increment and decrement key presses to indicate that a
         * rapid state change is starting, and corresponding key releases to
         * mean that the rapid state change has ended, and that any ending state
         * change should result in a notification for the state change listener.
         * Do the same for mouse presses and releases on the time widget's Up
         * and Down buttons.
         */
        if (onlySendEndStateChanges) {
            KeyListener keyListener = new KeyListener() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (UiBuilder.isDateTimeValueChanger(e)) {
                        rapidStateChangeInTextFieldInProgress = true;
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    if (UiBuilder.isDateTimeValueChanger(e)) {
                        rapidStateChangeInTextFieldInProgress = false;
                        notifyListenersOfEndingStateChange();
                    }
                }
            };
            date.addKeyListener(keyListener);
            time.addKeyListener(keyListener);
            time.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseDown(MouseEvent e) {
                    rapidStateChangeInTextFieldInProgress = true;
                }

                @Override
                public void mouseUp(MouseEvent e) {
                    rapidStateChangeInTextFieldInProgress = false;
                    notifyListenersOfEndingStateChange();
                }
            });
        }

        /*
         * Bind changes to the widgets to trigger state changes and
         * corresponding notifications, and if only ending state changes are to
         * result in notifications of state changes, fire off such a
         * notification when the default selection (Enter key) occurs.
         */
        SelectionListener selectionListener = new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DateTime widget = (DateTime) e.widget;

                /*
                 * Remember the previous state in case the new state is
                 * rejected.
                 */
                oldState = state;
                synchronizeStateToWidgets(widget);
                if (((DateTimeComponent.this.onlySendEndStateChanges == false) || isWidgetUndergoingRapidStateChange(widget))
                        && (oldState != state)) {
                    notifyListenersOfRapidStateChange();
                } else {
                    notifyListenersOfEndingStateChange();
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                notifyListenersOfEndingStateChange();
            }
        };
        date.addSelectionListener(selectionListener);
        time.addSelectionListener(selectionListener);
    }

    // Public Methods

    /**
     * Get the label, if any, for this date-time component.
     * 
     * @return Label for this date-time component, or <code>null</code> if there
     *         is no such label.
     */
    @Override
    public Label getLabel() {
        return label;
    }

    /**
     * Get the width of the date and time widgets portion of the component.
     * 
     * @return Width of the date and time widgets portion of the component.
     */
    public int getDateTimeWidth() {
        return ((GridData) date.getLayoutData()).widthHint
                + ((GridData) time.getLayoutData()).widthHint;
    }

    /**
     * Set the height of the component.
     * 
     * @param height
     *            Height in pixels of the component. The component widgets will
     *            be vertically centered within this height.
     */
    @Override
    public void setHeight(int height) {
        ((GridData) label.getParent().getLayoutData()).minimumHeight = height;
    }

    /**
     * Set the date-time component to be enabled or disabled.
     * 
     * @param enable
     *            Flag indicating whether or not the component should be
     *            enabled.
     */
    @Override
    public void setEnabled(boolean enable) {
        if (label != null) {
            label.setEnabled(enable);
        }
        if ((enable == false) && date.isOpen()) {
            date.setOpen(false);
            notifyListenersOfEndingStateChange();
        }
        date.setEnabled(enable);
        time.setEnabled(enable);
        time.setSpinnerEnabled(enable && date.getParent().isEnabled());
    }

    /**
     * Set the date-time component to be editable or read-only.
     * 
     * @param editable
     *            Flag indicating whether the component widgets are to be
     *            editable or read-only.
     * @param helper
     *            Control component helper to be used to determine what
     *            background color is appropriate for the widgets' fields.
     */
    @Override
    public void setEditable(boolean editable, ControlComponentHelper helper) {
        if ((editable == false) && date.isOpen()) {
            date.setOpen(false);
            notifyListenersOfEndingStateChange();
        }
        date.getParent().setEnabled(editable);
        Color color = helper.getBackgroundColor(editable, date, label);
        date.setBackground(color);
        time.setBackground(color);
        time.setSpinnerEnabled(date.isEnabled() && editable);
    }

    /**
     * Set the null text to that specified.
     * 
     * @param text
     *            Text to be displayed in both fields when the state is
     *            <code>null</code>.
     */
    public void setNullText(String text) {
        date.setNullText(text);
        time.setNullText(text);
    }

    /**
     * Set the flag indicating whether or not null text should currently be
     * displayed.
     * 
     * @param show
     *            Flag indicating whether or not null text should currently be
     *            displayed.
     */
    public void setShowNullText(boolean show) {
        date.setSelection(show ? null : timestamp);
        time.setSelection(show ? null : timestamp);
    }

    /**
     * Set the specified value as the that held by the date-time component.
     * 
     * @param value
     *            Value to be held by the date-time component.
     */
    public void setState(long value) {

        /*
         * Record the state.
         */
        state = value;
        recordLastNotifiedState();

        /*
         * Synchronize the timestamp to the new state.
         */
        timestamp.setTime(state);

        /*
         * Set the date and time widgets to match the new state.
         */
        date.setSelection(timestamp);
        time.setSelection(timestamp);
    }

    // Private Methods

    /**
     * Synchronize the state to the current values of the widgets.
     * 
     * @param changed
     *            Widget that experienced a change in its value.
     */
    private void synchronizeStateToWidgets(DateTime changed) {

        /*
         * If the selection is now null, undo the change, as this is not a valid
         * selection.
         */
        if (changed.getSelection() == null) {
            date.setSelection(timestamp);
            time.setSelection(timestamp);
            return;
        }

        long rawState = (changed == date ? date : time).getSelection()
                .getTime();

        // Check if it was the time that changed
        if (changed.getSelection() == time.getSelection()) {
            // Check for minute/hour roll overs
            if (oldState - rawState == MILLISECONDS_IN_HOUR * 23) {
                rawState = rawState + MILLISECONDS_IN_DAY;
            } else if (rawState - oldState == MILLISECONDS_IN_HOUR * 23) {
                rawState = rawState - MILLISECONDS_IN_DAY;
            } else if (oldState - rawState == MILLISECONDS_IN_MINUTE * 59) {
                rawState = rawState + MILLISECONDS_IN_HOUR;
            } else if (rawState - oldState == MILLISECONDS_IN_MINUTE * 59) {
                rawState = rawState - MILLISECONDS_IN_HOUR;
            }
        }

        /*
         * If the holder does not like the new state, restore the old state;
         */
        state = holder.renderValueChangeAcceptable(identifier, rawState);
        if (state == -1L) {
            state = oldState;
        }
        timestamp.setTime(state);

        /*
         * Set both the date widget and time widget to match the now-current
         * state.
         */
        date.setSelection(timestamp);
        time.setSelection(timestamp);
    }

    /**
     * Record the current state as one of which the state change listener is
     * assumed to be aware.
     */
    private void recordLastNotifiedState() {
        lastForwardedState = state;
    }

    /**
     * Determine whether or not the specified widget is undergoing rapid state
     * change.
     * 
     * @param changed
     *            Widget that has experienced a state change.
     * @return True if the widget is undergoing rapid state change, otherwise
     *         false.
     */
    private boolean isWidgetUndergoingRapidStateChange(DateTime changed) {
        return (rapidStateChangeInTextFieldInProgress || ((changed == date) && date
                .isOpen()));
    }

    /**
     * Notify the state change and notification listeners of a state change that
     * is part of a set of rapidly-occurring changes if necessary.
     */
    private void notifyListenersOfRapidStateChange() {
        holder.valueChanged(identifier, state, true);
    }

    /**
     * Notify the state change and notification listeners of a state change if
     * the current state is not the same as the last state of which the state
     * change listener is assumed to be aware.
     */
    private void notifyListenersOfEndingStateChange() {
        if (onlySendEndStateChanges && (lastForwardedState != state)) {
            recordLastNotifiedState();
            holder.valueChanged(identifier, state, false);
        }
    }
}