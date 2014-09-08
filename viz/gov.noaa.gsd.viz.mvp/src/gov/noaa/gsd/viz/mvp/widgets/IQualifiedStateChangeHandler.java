/**
 * This software was developed and / or modified by the
 * National Oceanic and Atmospheric Administration (NOAA), 
 * Earth System Research Laboratory (ESRL), 
 * Global Systems Division (GSD), 
 * Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.mvp.widgets;

import java.util.Map;

/**
 * Interface describing the methods that must be implemented by a handler for
 * state changes generated by a {@link IQualifiedStateChanger}. The generic
 * parameter <code>Q</code> provides the type of widget qualifier to be used,
 * <code>I</code> provides the type of widget identifier to be used, and
 * <code>S</code> provides the type of state.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- ------------ --------------------------
 * Aug 15, 2014    4243    Chris.Golden Initial creation.
 * </pre>
 * 
 * @author Chris.Golden
 * @version 1.0
 */
public interface IQualifiedStateChangeHandler<Q, I, S> {

    // Public Methods

    /**
     * Receive notification that the specified state was changed.
     * 
     * @param qualifier
     *            Qualifier of the state that was changed.
     * @param identifier
     *            Identifier of the state that was changed.
     * @param value
     *            New value for the specified state.
     */
    public void stateChanged(Q qualifier, I identifier, S value);

    /**
     * Receive notification that the specified states were changed.
     * 
     * @param qualifier
     *            Qualifier of the states that were changed.
     * @param valuesForIdentifiers
     *            Map pairing state identifiers with their new values.
     */
    public void statesChanged(Q qualifier, Map<I, S> valuesForIdentifiers);
}