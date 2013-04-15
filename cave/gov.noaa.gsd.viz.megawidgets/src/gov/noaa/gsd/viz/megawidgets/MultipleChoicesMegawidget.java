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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Stateful megawidget created by a megawidget specifier that has a set of zero
 * or more choices as its state.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 04, 2013            Chris.Golden      Initial induction into repo
 * 
 * </pre>
 * 
 * @author Chris.Golden
 * @version 1.0
 * @see ChoicesMegawidgetSpecifier
 */
public abstract class MultipleChoicesMegawidget extends StatefulMegawidget {

    // Protected Variables

    /**
     * List of strings making up the current state; the strings are the choices
     * currently selected.
     */
    protected List<String> state = new ArrayList<String>();

    // Protected Constructors

    /**
     * Construct a standard instance.
     * 
     * @param specifier
     *            Specifier.
     * @param paramMap
     *            Hash table mapping megawidget creation time parameter
     *            identifiers to values.
     */
    protected MultipleChoicesMegawidget(MegawidgetSpecifier specifier,
            Map<String, Object> paramMap) {
        super(specifier, paramMap);
    }

    // Protected Methods

    /**
     * Get the current state for the specified identifier. This method is called
     * by <code>getState()</code> only after the latter has ensured that the
     * supplied state identifier is valid.
     * 
     * @param identifier
     *            Identifier for which state is desired. Implementations may
     *            assume that the state identifier supplied by this parameter is
     *            valid for this megawidget.
     * @return Object making up the current state for the specified identifier.
     */
    @Override
    protected final Object doGetState(String identifier) {
        return state;
    }

    /**
     * Set the current state for the specified identifier. This method is called
     * by <code>setState()</code> only after the latter has ensured that the
     * supplied state identifier is valid, and has set a flag that indicates
     * that this setting of the state will not trigger the widget to notify its
     * listener of an invocation.
     * 
     * @param identifier
     *            Identifier for which state is to be set. Implementations may
     *            assume that the state identifier supplied by this parameter is
     *            valid for this megawidget.
     * @param state
     *            Object making up the state to be used for this identifier, or
     *            <code>null</code> if this state should be reset.
     * @throws MegawidgetStateException
     *             If new state is not of a valid type for this <code>
     *             StatefulMegawWidget</code> implementation.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected final void doSetState(String identifier, Object state)
            throws MegawidgetStateException {

        // Set the state to that supplied.
        if (state == null) {
            if (this.state == null) {
                this.state = new ArrayList<String>();
            } else {
                this.state.clear();
            }
        } else if (state instanceof String) {
            this.state = new ArrayList<String>();
            this.state.add((String) state);
        } else if (state instanceof List) {
            try {
                this.state = (List<String>) state;
            } catch (Exception e) {
                throw new MegawidgetStateException(identifier, getSpecifier()
                        .getType(), state, "must be list of choices");
            }
        } else {
            try {
                this.state = new ArrayList<String>((Collection<String>) state);
            } catch (Exception e) {
                throw new MegawidgetStateException(identifier, getSpecifier()
                        .getType(), state, "must be list of choices");
            }
        }

        // Notify the widget itself that the state has
        // changed.
        megawidgetStateChanged(this.state);
    }

    /**
     * Get a shortened description of the specified state for the specified
     * identifier. This method is called by
     * <code>getStateDescription() only after
     * the latter has ensured that the supplied state
     * identifier is valid.
     * 
     * @param identifier
     *            Identifier to which the state would be assigned.
     *            Implementations may assume that the state identifier supplied
     *            by this parameter is valid for this megawidget.
     * @param state
     *            State for which to generate a shortened description.
     * @return Description of the specified state.
     * @throws MegawidgetStateException
     *             If the specified state is not of a valid type for this <code>
     *             StatefulMegawidget</code> implementation.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected final String doGetStateDescription(String identifier, Object state)
            throws MegawidgetStateException {
        if (state instanceof String) {
            return (String) state;
        } else {
            try {
                StringBuffer description = new StringBuffer();
                for (String element : (Collection<String>) state) {
                    if (description.length() > 0) {
                        description.append("; ");
                    }
                    description.append(element);
                }
                return description.toString();
            } catch (Exception e) {
                throw new MegawidgetStateException(identifier, getSpecifier()
                        .getType(), state, "must be list of choices");
            }
        }
    }

    /**
     * Receive notification that the megawidget's state has changed.
     * 
     * @param state
     *            New state.
     */
    protected abstract void megawidgetStateChanged(List<String> state);
}