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
 * list state changes generated by a {@link IListStateChanger}. The generic
 * parameter <code>I</code> provides the type of widget identifier to be used,
 * while <code>E</code> provides the type of element within the list.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- ------------ --------------------------
 * Mar 14, 2016   15676    Chris.Golden Initial creation.
 * Aug 22, 2016   19537    Chris.Golden Removed selection-related method,
 *                                      as it has nothing to do with the
 *                                      list changer widget.
 * </pre>
 * 
 * @author Chris.Golden
 * @version 1.0
 */
public interface IListStateChangeHandler<I, E> {

    // Public Methods

    /**
     * Receive notification that the specified list element was changed in some
     * way.
     * 
     * @param identifier
     *            Identifier of the list that was changed. This may be
     *            <code>null</code> if this object only handles one particular
     *            list's changes.
     * @param element
     *            Element that was changed.
     */
    public void listElementChanged(I identifier, E element);
}
