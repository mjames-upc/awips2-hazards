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

import gov.noaa.gsd.viz.megawidgets.validators.BoundedFractionValidator;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;

/**
 * Fraction spinner megawidget, allowing the manipulation of a double value.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 23, 2013   2168     Chris.Golden      Initial creation.
 * Apr 24, 2014   2925     Chris.Golden      Changed to work with new validator
 *                                           package, updated Javadoc and other
 *                                           comments.
 * Aug 12, 2015   4123     Chris.Golden      Changed to allow sharing of code
 *                                           with new range megawidgets.
 * </pre>
 * 
 * @author Chris.Golden
 * @version 1.0
 * @see FractionSpinnerSpecifier
 */
public class FractionSpinnerMegawidget extends SpinnerMegawidget<Double> {

    // Protected Constructors

    /**
     * Construct a standard instance.
     * 
     * @param specifier
     *            Specifier.
     * @param parent
     *            Parent of the megawidget.
     * @param paramMap
     *            Hash table mapping megawidget creation time parameter
     *            identifiers to values.
     */
    protected FractionSpinnerMegawidget(FractionSpinnerSpecifier specifier,
            Composite parent, Map<String, Object> paramMap) {
        super(specifier, parent, new DoubleSpinnerAndScaleComponentHelper(
                specifier, specifier.getStateIdentifiers(), parent), paramMap);
    }

    // Protected Methods

    @Override
    protected int getPrecision() {
        return ((BoundedFractionValidator) getStateValidator()).getPrecision();
    }
}