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

import java.util.List;
import java.util.Map;

/**
 * Single page megawidget specifier base class, from which specific types of
 * container megawidget specifiers that have a single page or panel of child
 * megawidgets may be derived.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 04, 2013            Chris.Golden      Initial induction into repo
 * Sep 24, 2013    2168    Chris.Golden      Changed to work with new
 *                                           child specifier manager.
 * Apr 24, 2014    2925    Chris.Golden      Changed to work with new validator
 *                                           package, updated Javadoc and other
 *                                           comments.
 * Oct 20, 2014    4818    Chris.Golden      Added option of providing a
 *                                           scrollable panel for child
 *                                           megawidgets.
 * </pre>
 * 
 * @author Chris.Golden
 * @version 1.0
 * @see ContainerMegawidget
 */
public abstract class SinglePageMegawidgetSpecifier extends
        ContainerMegawidgetSpecifier<IControlSpecifier> implements
        IPotentiallyScrollableContainerSpecifier<IControlSpecifier> {

    // Public Static Constants

    /**
     * Megawidget column count parameter name; a megawidget may include a
     * positive integer associated with this name. This indicates the number of
     * columns that this megawidget provides in which its children may lay
     * themselves out. If not specified, the default is 1.
     */
    public static final String MEGAWIDGET_COLUMN_COUNT = "numColumns";

    /**
     * Child megawidget specifiers parameter name; each single page megawidget
     * may contain a reference to a {@link List} object associated with this
     * name. The provided list must contain zero or more child megawidget
     * specifier parameter maps, each in the form of a {@link Map} from which a
     * megawidget specifier will be constructed.
     */
    public static final String CHILD_MEGAWIDGETS = "fields";

    // Private Variables

    /**
     * Number of columns this megawidget contains within which its children may
     * lay themselves out.
     */
    private final int columnCount;

    /**
     * Flag indicating whether or not the client area is to be scrollable.
     */
    private final boolean scrollable;

    // Public Constructors

    /**
     * Construct a standard instance.
     * 
     * @param parameters
     *            Map holding the parameters that will be used to configure a
     *            megawidget created by this notifier as a set of key-value
     *            pairs.
     * @throws MegawidgetSpecificationException
     *             If the megawidget specifier parameters are invalid.
     */
    public SinglePageMegawidgetSpecifier(Map<String, Object> parameters)
            throws MegawidgetSpecificationException {
        super(IControlSpecifier.class, parameters);

        /*
         * Ensure that the column count, if present, is acceptable, and if not
         * present is assigned a default value.
         */
        columnCount = ConversionUtilities.getSpecifierIntegerValueFromObject(
                getIdentifier(), getType(),
                parameters.get(MEGAWIDGET_COLUMN_COUNT),
                MEGAWIDGET_COLUMN_COUNT, 1);
        if (columnCount < 1) {
            throw new MegawidgetSpecificationException(getIdentifier(),
                    getType(), MEGAWIDGET_COLUMN_COUNT, columnCount,
                    "must be positive integer");
        }

        /*
         * Ensure that the scrollable flag, if present, is valid.
         */
        scrollable = ConversionUtilities.getSpecifierBooleanValueFromObject(
                getIdentifier(), getType(), parameters.get(SCROLLABLE),
                SCROLLABLE, false);

        /*
         * Ensure that child list, if present, is acceptable, and store it.
         */
        List<?> childrenObject = null;
        try {
            childrenObject = (List<?>) parameters.get(CHILD_MEGAWIDGETS);
        } catch (Exception e) {
            throw new MegawidgetSpecificationException(getIdentifier(),
                    getType(), CHILD_MEGAWIDGETS,
                    parameters.get(CHILD_MEGAWIDGETS),
                    "must be list of child megawidget specifiers");
        }
        if (childrenObject != null) {
            try {
                getChildManager().addChildMegawidgetSpecifiers(
                        getChildManager().createMegawidgetSpecifiers(
                                childrenObject, columnCount));
            } catch (Exception e) {
                throw new MegawidgetSpecificationException(getIdentifier(),
                        getType(), CHILD_MEGAWIDGETS, childrenObject,
                        "bad list of child megawidget specifiers", e);
            }
        }
    }

    // Public Methods

    /**
     * Get the column count.
     * 
     * @return Column count.
     */
    public final int getColumnCount() {
        return columnCount;
    }

    @Override
    public boolean isScrollable() {
        return scrollable;
    }
}
