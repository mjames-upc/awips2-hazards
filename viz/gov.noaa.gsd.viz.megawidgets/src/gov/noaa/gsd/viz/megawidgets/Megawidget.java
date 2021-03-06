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

import gov.noaa.gsd.viz.megawidgets.displaysettings.IDisplaySettings;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Base class for any megawidget created by a megawidget specifier.
 * 
 * All concrete subclasses must have a constructor taking the following
 * parameters as arguments, in the given order:
 * <dl>
 * <dt>specifier</dt>
 * <dd>Instance of a subclass of <code>MegawidgetSpecifier</code> that is
 * creating the megawidget. The subclass must have the same name as that of the
 * megawidget subclass, except with "Specifier" appended instead of
 * "Megawidget", and should be in the same package as the megawidget's subclass.
 * </dd>
 * <dt>parent</dt>
 * <dd>Subclass of SWT <code>Widget</code> in which the megawidget is to be
 * placed (such as <code>Composite</code> for window-based megawidgets, or
 * <code>Menu</code> for menu-based ones).</dd>
 * <dt>paramMap</dt>
 * <dd>Map pairing megawidget creation time parameter identifiers with
 * corresponding values.</dd>
 * </dl>
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 04, 2013            Chris.Golden      Initial induction into repo
 * Apr 30, 2013   1277     Chris.Golden      Added support for mutable properties.
 * Oct 23, 2013   2168     Chris.Golden      Removed functionality that belonged
 *                                           in implementations of the new
 *                                           IControl interface, placing it
 *                                           instead in said implementations.
 *                                           Also added helper methods for
 *                                           getting floats and doubles from
 *                                           arbitrary property value objects.
 * Apr 24, 2014   2925     Chris.Golden      Changed to work with new validator
 *                                           package, updated Javadoc and other
 *                                           comments.
 * Jun 24, 2014   4009     Chris.Golden      Added extra data functionality.
 * Oct 20, 2014   4818     Chris.Golden      Added default implemenations of the
 *                                           methods used to save and restore
 *                                           display state.
 * </pre>
 * 
 * @author Chris.Golden
 * @version 1.0
 * @see MegawidgetSpecifier
 */
public abstract class Megawidget implements IMegawidget {

    // Protected Static Constants

    /**
     * Set of all mutable property names for instances of this class.
     */
    protected static final Set<String> MUTABLE_PROPERTY_NAMES;
    static {
        MUTABLE_PROPERTY_NAMES = ImmutableSet.of(
                MegawidgetSpecifier.MEGAWIDGET_ENABLED,
                MegawidgetSpecifier.MEGAWIDGET_EXTRA_DATA);
    };

    // Private Variables

    /**
     * Specifier for this megawidget.
     */
    private final ISpecifier specifier;

    /**
     * Flag indicating whether the megawidget is currently enabled.
     */
    private boolean enabled;

    /**
     * Map of extra data.
     */
    private final Map<String, Object> extraData;

    // Protected Constructors

    /**
     * Construct a standard instance.
     * 
     * @param specifier
     *            Specifier for this megawidget.
     */
    protected Megawidget(ISpecifier specifier) {
        this.specifier = specifier;
        enabled = getSpecifier().isEnabled();
        extraData = new HashMap<>(getSpecifier().getExtraData());
    }

    // Public Methods

    @SuppressWarnings("unchecked")
    @Override
    public final <S extends MegawidgetSpecifier> S getSpecifier() {
        return (S) specifier;
    }

    @Override
    public Set<String> getMutablePropertyNames() {
        return MUTABLE_PROPERTY_NAMES;
    }

    @Override
    public Object getMutableProperty(String name)
            throws MegawidgetPropertyException {
        if (name.equals(MegawidgetSpecifier.MEGAWIDGET_ENABLED)) {
            return isEnabled();
        } else if (name.equals(MegawidgetSpecifier.MEGAWIDGET_EXTRA_DATA)) {
            return getExtraData();
        }
        throw new MegawidgetPropertyException(specifier.getIdentifier(), name,
                specifier.getType(), null, "nonexistent property");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setMutableProperty(String name, Object value)
            throws MegawidgetPropertyException {
        if (name.equals(MegawidgetSpecifier.MEGAWIDGET_ENABLED)) {
            setEnabled(ConversionUtilities.getPropertyBooleanValueFromObject(
                    getSpecifier().getIdentifier(), getSpecifier().getType(),
                    value, name, null));
        } else if (name.equals(MegawidgetSpecifier.MEGAWIDGET_EXTRA_DATA)) {

            /*
             * Ensure that the value is a map with string keys.
             */
            Map<String, Object> map = null;
            try {
                map = (HashMap<String, Object>) value;
                if (map == null) {
                    throw new NullPointerException();
                }
            } catch (Exception e) {
                throw new MegawidgetPropertyException(getSpecifier()
                        .getIdentifier(), name, getSpecifier().getType(),
                        value, "bad map of extra data", e);
            }
            setExtraData(map);
        } else {
            throw new MegawidgetPropertyException(specifier.getIdentifier(),
                    name, specifier.getType(), null, "nonexistent property");
        }
    }

    @Override
    public final Map<String, Object> getMutableProperties() {
        Map<String, Object> map = new HashMap<>();
        try {
            for (String name : getMutablePropertyNames()) {
                map.put(name, getMutableProperty(name));
            }
        } catch (MegawidgetPropertyException e) {
            throw new IllegalStateException(
                    "querying valid mutable property \"" + e.getName()
                            + "\" caused internal error", e);
        }
        return map;
    }

    @Override
    public void setMutableProperties(Map<String, Object> properties)
            throws MegawidgetPropertyException {
        for (String name : properties.keySet()) {
            setMutableProperty(name, properties.get(name));
        }
    }

    @Override
    public final boolean isEnabled() {
        return enabled;
    }

    @Override
    public final void setEnabled(boolean enable) {
        enabled = enable;
        doSetEnabled(enabled);
    }

    @Override
    public final Map<String, Object> getExtraData() {
        return new HashMap<>(extraData);
    }

    @Override
    public final void setExtraData(Map<String, Object> extraData) {
        this.extraData.clear();
        this.extraData.putAll(extraData);
    }

    /**
     * Get the display settings for this megawidget. This implementation simply
     * returns {@link IDisplaySettings#NULL_DISPLAY_SETTINGS}; subclasses must
     * override this if they are to allow their display settings to be queried.
     * 
     * @return Display settings.
     */
    @Override
    public IDisplaySettings getDisplaySettings() {
        return IDisplaySettings.NULL_DISPLAY_SETTINGS;
    }

    /**
     * Set the display settings for this megawidget to those specified. If the
     * display settings are incompatible with this megawidget, they are ignored.
     * This implementation ignores the display settings regardless; subclasses
     * must override this if they are to allow their display settings to be set.
     * 
     * @param displaySettings
     *            New display settings.
     */
    @Override
    public void setDisplaySettings(IDisplaySettings displaySettings) {

        /*
         * No action.
         */
    }

    // Protected Methods

    /**
     * Change the component widgets to ensure their state matches that of the
     * enabled flag.
     * 
     * @param enable
     *            Flag indicating whether the component widgets are to be
     *            enabled or disabled.
     */
    protected abstract void doSetEnabled(boolean enable);
}