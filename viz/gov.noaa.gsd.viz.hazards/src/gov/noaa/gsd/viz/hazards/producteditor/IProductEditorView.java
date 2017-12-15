/**
 * This software was developed and / or modified by the National Oceanic and
 * Atmospheric Administration (NOAA), Earth System Research Laboratory (ESRL),
 * Global Systems Division (GSD), Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.hazards.producteditor;

import java.util.List;

import com.raytheon.uf.common.hazards.configuration.types.HazardTypes;
import com.raytheon.uf.common.hazards.productgen.GeneratedProductList;

import gov.noaa.gsd.viz.mvp.IView;
import gov.noaa.gsd.viz.mvp.widgets.ICommandInvoker;

/**
 * Description: Defines the interface a concrete Product Editor View must
 * fulfill.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 18, 2013            Bryon.Lawrence      Initial creation
 * Sep 19, 2013 2046       mnash        Update for product generation.
 * Jan  7, 2013 2367       jsanchez     Used GeneratedProductList.
 * Feb 7, 2014  2890       bkowal       Product Generation JSON refactor.
 * Feb 18, 2014 2702       jsanchez     Removed unused method.
 * Apr 11, 2014 2819       Chris.Golden Fixed bugs with the Preview and Issue
 *                                      buttons in the HID remaining grayed out
 *                                      when they should be enabled.
 * May 08, 2014 2925       Chris.Golden Changed to work with MVP framework changes.
 * Jul 28, 2014 3214       jsanchez     Added notifySessionEventsModified method.  
 * Feb 26, 2015 6306       mduff        Pass site id to product editor.
 * May 13, 2015 6899       Robert.Blum  Removed notifySessionEventsModified method.
 * Dec 04, 2015 12981      Roger.Ferrel Checks to prevent issuing unwanted
 *                                      expiration product.
 * Mar 30, 2016  8837      Robert.Blum  Added changeSite() for service backup.
 * Dec 12, 2016 21504      Robert.Blum  Updates for hazard locking.
 * Apr 05, 2017 32733      Robert.Blum  Removed unused parameter.
 * Apr 27, 2017 11853      Chris.Golden Made names of methods more consistent, and
 *                                      added a method to check to see if the
 *                                      product editor is open.
 * Dec 17, 2017 20739      Chris.Golden Refactored away access to directly mutable
 *                                      session events.
 * </pre>
 * 
 * @author Bryon.Lawrence
 * @version 1.0
 */
public interface IProductEditorView<C, E extends Enum<E>> extends IView<C, E> {

    // Public Methods

    /**
     * Initialize the view.
     */
    public void initialize();

    public boolean showProductEditor(
            List<GeneratedProductList> generatedProductsList, String siteId,
            HazardTypes hazardTypes);

    /**
     * Close the product editor dialog.
     */
    public void closeProductEditor();

    /**
     * Get the generated products list.
     * 
     * @return Generated products list.
     */
    public List<GeneratedProductList> getGeneratedProductsList();

    /**
     * Get the command invoker associated with the dialog's issue button.
     * 
     * @return Command invoker associated with the dialog's issue button.
     */
    public ICommandInvoker<String> getIssueInvoker();

    /**
     * Get the command invoker associated with the dialog's dismiss button.
     * 
     * @return Command invoker associated with the dialog's dismiss button.
     */
    public ICommandInvoker<String> getDismissInvoker();

    /**
     * Show the actual dialog.
     * 
     * TODO: This should be refactored away at some point, as it should probably
     * be done by {@link #showProductEditor(List, String)}.
     */
    public void openDialog();

    /**
     * Handles changing the site for service backup.
     * 
     * @param site
     */
    public void changeSite(String site);

    /**
     * Return whether or not the Product Editor is currently open.
     * 
     * @return
     */
    public boolean isProductEditorOpen();

    /**
     * Updates the Product Editor to handle the situation when events become
     * locked and the products are no longer valid.
     */
    public void handleHazardEventLock();
}