/**
 * This software was developed and / or modified by the
 * National Oceanic and Atmospheric Administration (NOAA), 
 * Earth System Research Laboratory (ESRL), 
 * Global Systems Division (GSD), 
 * Evaluation & Decision Support Branch (EDS)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.widgets;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Listener;
import org.eclipse.swt.internal.SWTEventListener;

/**
 * Description: Interface describing the methods that must be implemented by a
 * listener for events generated by a {@link CustomizableTabFolder}.
 * <p>
 * Note that this class is a copy of {@link CTabFolder2Listener} changed to work
 * with a <code>CustomizableTabFolder</code> instead of a {@link CTabFolder}.
 * </p>
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- ------------ --------------------------
 * Feb 05, 2017   15556    Chris.Golden Initial creation.
 * </pre>
 * 
 * @author Chris.Golden
 * @version 1.0
 */
@SuppressWarnings("restriction")
public interface ICustomizableTabFolderListener extends SWTEventListener {

    /**
     * Sent when the user clicks on the close button of an item in the
     * CustomizableTabFolder. The item being closed is specified in the
     * event.item field. Setting the event.doit field to false will stop the
     * CustomizableTabItem from closing. When the CustomizableTabItem is closed,
     * it is disposed. The contents of the CustomizableTabItem (see
     * CustomizableTabItem.setControl) will be made not visible when the
     * CustomizableTabItem is closed.
     * 
     * @param event
     *            an event indicating the item being closed
     */
    public void close(CustomizableTabFolderEvent event);

    /**
     * Sent when the user clicks on the minimize button of a
     * CustomizableTabFolder. The state of the CustomizableTabFolder does not
     * change automatically - it is up to the application to change the state of
     * the CustomizableTabFolder in response to this event using
     * CustomizableTabFolder.setMinimized(true).
     * 
     * @param event
     *            an event containing information about the minimize
     * 
     * @see CustomizableTabFolder#getMinimized()
     * @see CustomizableTabFolder#setMinimized(boolean)
     * @see CustomizableTabFolder#setMinimizeVisible(boolean)
     */
    public void minimize(CustomizableTabFolderEvent event);

    /**
     * Sent when the user clicks on the maximize button of a
     * CustomizableTabFolder. The state of the CustomizableTabFolder does not
     * change automatically - it is up to the application to change the state of
     * the CustomizableTabFolder in response to this event using
     * CustomizableTabFolder.setMaximized(true).
     * 
     * @param event
     *            an event containing information about the maximize
     * 
     * @see CustomizableTabFolder#getMaximized()
     * @see CustomizableTabFolder#setMaximized(boolean)
     * @see CustomizableTabFolder#setMaximizeVisible(boolean)
     */
    public void maximize(CustomizableTabFolderEvent event);

    /**
     * Sent when the user clicks on the restore button of a
     * CustomizableTabFolder. This event is sent either to restore the
     * CustomizableTabFolder from the minimized state or from the maximized
     * state. To determine which restore is requested, use
     * CustomizableTabFolder.getMinimized() or
     * CustomizableTabFolder.getMaximized() to determine the current state. The
     * state of the CustomizableTabFolder does not change automatically - it is
     * up to the application to change the state of the CustomizableTabFolder in
     * response to this event using CustomizableTabFolder.setMaximized(false) or
     * CustomizableTabFolder.setMinimized(false).
     * 
     * @param event
     *            an event containing information about the restore
     * 
     * @see CustomizableTabFolder#getMinimized()
     * @see CustomizableTabFolder#getMaximized()
     * @see CustomizableTabFolder#setMinimized(boolean)
     * @see CustomizableTabFolder#setMinimizeVisible(boolean)
     * @see CustomizableTabFolder#setMaximized(boolean)
     * @see CustomizableTabFolder#setMaximizeVisible(boolean)
     */
    public void restore(CustomizableTabFolderEvent event);

    /**
     * Sent when the user clicks on the chevron button of the
     * CustomizableTabFolder. A chevron appears in the CustomizableTabFolder
     * when there are more tabs than can be displayed at the current widget
     * size. To select a tab that is not currently visible, the user clicks on
     * the chevron and selects a tab item from a list. By default, the
     * CustomizableTabFolder provides a list of all the items that are not
     * currently visible, however, the application can provide its own list by
     * setting the event.doit field to <code>false</code> and displaying a
     * selection list.
     * 
     * @param event
     *            an event containing information about the show list
     * 
     * @see CustomizableTabFolder#setSelection(CustomizableTabItem)
     */
    public void showList(CustomizableTabFolderEvent event);
}
