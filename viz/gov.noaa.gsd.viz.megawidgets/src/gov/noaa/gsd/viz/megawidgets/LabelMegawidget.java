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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import com.google.common.collect.ImmutableSet;

/**
 * Label megawidget created by a label megawidget specifier.
 * 
 * <pre>
 * `
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 04, 2013            Chris.Golden      Initial induction into repo
 * Sep 26, 2013    2168    Chris.Golden      Added ability to handle new wrap
 *                                           flag, and to implement new
 *                                           IControl interface.
 * Nov 04, 2013    2336    Chris.Golden      Added bold and italic options.
 * Apr 24, 2014    2925    Chris.Golden      Changed to work with new validator
 *                                           package, updated Javadoc and other
 *                                           comments.
 * Oct 10, 2014    4042    Chris.Golden      Added "preferredWidth" parameter.
 * Jun 07, 2016   19464    Chris.Golden      Added "color" parameter.
 * </pre>
 * 
 * @author Chris.Golden
 * @version 1.0
 * @see LabelSpecifier
 */
public class LabelMegawidget extends Megawidget implements IControl {

    // Protected Static Constants

    /**
     * Set of all mutable property names for instances of this class.
     */
    protected static final Set<String> MUTABLE_PROPERTY_NAMES;
    static {
        Set<String> names = new HashSet<>(Megawidget.MUTABLE_PROPERTY_NAMES);
        names.add(IControlSpecifier.MEGAWIDGET_EDITABLE);
        MUTABLE_PROPERTY_NAMES = ImmutableSet.copyOf(names);
    };

    // Private Variables

    /**
     * Label.
     */
    private final Label label;

    /**
     * Control component helper.
     */
    private final ControlComponentHelper helper;

    /**
     * Font, if a custom font was created.
     */
    private final Font font;

    /**
     * Color, if a custom color was created.
     */
    private final Color color;

    // Protected Constructors

    /**
     * Construct a standard instance.
     * 
     * @param specifier
     *            Specifier.
     * @param parent
     *            Parent of this megawidget.
     * @param paramMap
     *            Hash table mapping megawidget creation time parameter
     *            identifiers to values.
     */
    protected LabelMegawidget(LabelSpecifier specifier, Composite parent,
            Map<String, Object> paramMap) {
        super(specifier);
        helper = new ControlComponentHelper(specifier);

        /*
         * Create a label widget, setting its font to a bold and/or italic one
         * if those options are specified. If a font is created, set up a
         * listener to dispose of said font when the label is disposed of.
         */
        label = new Label(parent, (specifier.isToWrap() ? SWT.WRAP : SWT.NONE));
        label.setText(specifier.getLabel());
        if (specifier.isBold() || specifier.isItalic()) {
            FontData fontData = label.getFont().getFontData()[0];
            font = new Font(label.getDisplay(), new FontData(
                    fontData.getName(), fontData.getHeight(),
                    (specifier.isBold() ? SWT.BOLD : SWT.NORMAL)
                            + (specifier.isItalic() ? SWT.ITALIC : SWT.NORMAL)));
            label.setFont(font);
        } else {
            font = null;
        }
        Map<String, Double> colorMap = specifier.getColor();
        if (colorMap.isEmpty() == false) {
            color = new Color(Display.getDefault(), getColorComponent(colorMap,
                    ConversionUtilities.COLOR_AS_MAP_RED), getColorComponent(
                    colorMap, ConversionUtilities.COLOR_AS_MAP_GREEN),
                    getColorComponent(colorMap,
                            ConversionUtilities.COLOR_AS_MAP_BLUE));
            label.setForeground(color);
        } else {
            color = null;
        }
        if ((font != null) || (color != null)) {
            label.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e) {
                    if (font != null) {
                        font.dispose();
                    }
                    if (color != null) {
                        color.dispose();
                    }
                }
            });
        }
        label.setEnabled(specifier.isEnabled());

        /*
         * Place the widget in the grid. If the widget may end up wrapping, then
         * it must be registered as a listener for its parent's resize events so
         * that it can have its width hint set each time the parent is resized.
         * Use the preferred width if one was specified.
         */
        GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gridData.horizontalSpan = specifier.getWidth();
        if (specifier.isToWrap()) {
            specifier.ensureChildIsResizedWithParent(parent, label);
        }
        if (specifier.getPreferredWidth() > 0) {
            gridData.widthHint = specifier.getPreferredWidth();
        }
        gridData.verticalIndent = specifier.getSpacing();
        label.setLayoutData(gridData);
    }

    // Public Methods

    @Override
    public Set<String> getMutablePropertyNames() {
        return MUTABLE_PROPERTY_NAMES;
    }

    @Override
    public Object getMutableProperty(String name)
            throws MegawidgetPropertyException {
        if (name.equals(IControlSpecifier.MEGAWIDGET_EDITABLE)) {
            return isEditable();
        }
        return super.getMutableProperty(name);
    }

    @Override
    public void setMutableProperty(String name, Object value)
            throws MegawidgetPropertyException {
        if (name.equals(IControlSpecifier.MEGAWIDGET_EDITABLE)) {
            setEditable(ConversionUtilities.getPropertyBooleanValueFromObject(
                    getSpecifier().getIdentifier(), getSpecifier().getType(),
                    value, name, null));
        } else {
            super.setMutableProperty(name, value);
        }
    }

    @Override
    public final boolean isEditable() {
        return helper.isEditable();
    }

    @Override
    public final void setEditable(boolean editable) {
        helper.setEditable(editable);
        doSetEditable(editable);
    }

    @Override
    public final int getLeftDecorationWidth() {
        return 0;
    }

    @Override
    public final void setLeftDecorationWidth(int width) {

        /*
         * No action.
         */
    }

    @Override
    public final int getRightDecorationWidth() {
        return 0;
    }

    @Override
    public final void setRightDecorationWidth(int width) {

        /*
         * No action.
         */
    }

    // Protected Methods

    @Override
    protected void doSetEnabled(boolean enable) {
        label.setEnabled(enable);
    }

    // Private Methods

    /**
     * Change the component widgets to ensure their state matches that of the
     * editable flag.
     * 
     * @param editable
     *            Flag indicating whether the component widgets are to be
     *            editable or read-only.
     */
    private void doSetEditable(boolean editable) {

        /*
         * No action.
         */
    }

    /**
     * Get the floating-point value (which must be between 0.0 and 1.0
     * inclusive), in the specified map at the specified key, and convert it to
     * a color component between 0 and 255 inclusive.
     * 
     * @param map
     *            Map holding the color component.
     * @param key
     *            Key under which the value is to be found in the map.
     * @return Color component value, between 0 and 255 inclusive.
     */
    private int getColorComponent(Map<String, Double> map, String key) {
        return (int) ((map.get(key) * 255.0) + 0.5);
    }
}