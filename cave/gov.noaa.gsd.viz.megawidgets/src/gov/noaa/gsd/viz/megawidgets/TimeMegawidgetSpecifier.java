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

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Description: Base class for time megawidget specifiers, which provide
 * specifications of megawidgets that allow the selection of one or more
 * timestamps.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 13, 2013    2545    Chris.Golden      Initial creation
 * </pre>
 * 
 * @author Chris.Golden
 * @version 1.0
 */
public abstract class TimeMegawidgetSpecifier extends
        StatefulMegawidgetSpecifier implements IControlSpecifier,
        IRapidlyChangingStatefulSpecifier {

    // Public Static Constants

    /**
     * Current time provider megawidget creation time parameter name; if
     * specified in the map passed to <code>createMegawidget()</code>, its value
     * must be an object of type {@link ICurrentTimeProvider}, which will be
     * used by the megawidget when it needs to know what the current time is. If
     * not specified, the megawidget will use the current system time as the
     * current time.
     */
    public static final String CURRENT_TIME_PROVIDER = "currentTimeProvider";

    /**
     * Minimum time megawidget creation time parameter name; if specified in the
     * map passed to <code>createMegawidget()</code>, its value must be an
     * object of type <code>Long</code> indicating the minimum time in
     * milliseconds that may be specified using the created megawidget.
     */
    public static final String MINIMUM_TIME = "minimumTime";

    /**
     * Maximum time megawidget creation time parameter name; if specified in the
     * map passed to <code>createMegawidget()</code>, its value must be an
     * object of type <code>Long</code> indicating the maximum time in
     * milliseconds that may be specified using the created megawidget.
     */
    public static final String MAXIMUM_TIME = "maximumTime";

    // Protected Static Constants

    /**
     * Default current time provider.
     */
    protected static final ICurrentTimeProvider DEFAULT_CURRENT_TIME_PROVIDER = new ICurrentTimeProvider() {
        @Override
        public long getCurrentTime() {
            return System.currentTimeMillis();
        }
    };

    // Private Variables

    /**
     * Control options manager.
     */
    private final ControlSpecifierOptionsManager optionsManager;

    /**
     * Flag indicating whether or not state changes that are part of a group of
     * rapid changes are to result in notifications to the listener.
     */
    private final boolean sendingEveryChange;

    // Public Constructors

    /**
     * Construct a standard instance.
     * 
     * @param parameters
     *            Map holding the parameters that will be used to configure a
     *            megawidget created by this specifier as a set of key-value
     *            pairs.
     * @throws MegawidgetSpecificationException
     *             If the megawidget specifier parameters are invalid.
     */
    public TimeMegawidgetSpecifier(Map<String, Object> parameters)
            throws MegawidgetSpecificationException {
        super(parameters);
        optionsManager = new ControlSpecifierOptionsManager(this, parameters,
                ControlSpecifierOptionsManager.BooleanSource.FALSE);

        // Ensure that the rapid change notification flag, if
        // provided, is appropriate.
        sendingEveryChange = getSpecifierBooleanValueFromObject(
                parameters.get(MEGAWIDGET_SEND_EVERY_STATE_CHANGE),
                MEGAWIDGET_SEND_EVERY_STATE_CHANGE, true);
    }

    // Public Methods

    @Override
    public final boolean isEditable() {
        return optionsManager.isEditable();
    }

    @Override
    public final int getWidth() {
        return optionsManager.getWidth();
    }

    @Override
    public final boolean isFullWidthOfColumn() {
        return optionsManager.isFullWidthOfColumn();
    }

    @Override
    public final int getSpacing() {
        return optionsManager.getSpacing();
    }

    @Override
    public final boolean isSendingEveryChange() {
        return sendingEveryChange;
    }

    // Protected Methods

    @Override
    protected final Set<Class<?>> getClassesOfState() {
        Set<Class<?>> classes = Sets.newHashSet();
        classes.add(Number.class);
        return classes;
    }
}