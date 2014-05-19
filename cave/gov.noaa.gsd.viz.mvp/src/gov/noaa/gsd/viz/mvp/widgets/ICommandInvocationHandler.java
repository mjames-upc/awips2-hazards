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

/**
 * Interface describing the methods that must be implemented by a handler for
 * command invocations generated by a {@link ICommandInvoker}. The generic
 * parameter <code>I</code> provides the type of widget identifier to be used.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- ------------ --------------------------
 * May 08, 2014    2925    Chris.Golden Changed to work with generic identifier.
 * </pre>
 * 
 * @author Chris.Golden
 * @version 1.0
 */
public interface ICommandInvocationHandler<I> {

    // Public Methods

    /**
     * Receive notification that the specified invoker was invoked.
     * 
     * @param identifier
     *            Identifier of the invoker that was invoked. This may be
     *            <code>null</code> if this object only handles one type of
     *            invocation.
     */
    public void commandInvoked(I identifier);
}
