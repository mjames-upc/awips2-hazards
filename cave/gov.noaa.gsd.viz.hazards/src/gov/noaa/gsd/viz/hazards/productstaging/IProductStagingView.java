/**
 * This software was developed and / or modified by the
 * National Oceanic and Atmospheric Administration (NOAA), 
 * Earth System Research Laboratory (ESRL), 
 * Global Systems Division (GSD), 
 * Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.hazards.productstaging;

import gov.noaa.gsd.viz.hazards.jsonutilities.Dict;
import gov.noaa.gsd.viz.mvp.IView;
import gov.noaa.gsd.viz.mvp.widgets.ICommandInvoker;

/**
 * Description: Defines the interface that a concrete ProductStagingView must
 * implement.
 * <p>
 * A product staging view must provide a way of displaying potential products
 * for issuance.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 18, 2013            Bryon.Lawrence      Initial creation
 * 
 * </pre>
 * 
 * @author Bryon.Lawrence
 * @version 1.0
 */
public interface IProductStagingView<C, E extends Enum<E>> extends IView<C, E> {

    // Public Methods

    /**
     * Open the product staging dialog.
     * 
     * @param toBeIssued
     *            Flag indicating whether or not this is a result of an issue
     *            action.
     * @param productList
     *            List of products to stage.
     */
    public void showProductStagingDetail(boolean toBeIssued,
            Dict productStagingInfo);

    /**
     * Get the continue command invoker to allow the presenter to bind with it
     * for notifications.
     * 
     * @return Continue command invoker.
     */
    public ICommandInvoker getContinueInvoker();

    /**
     * Determine whether or not the product should be issued.
     * 
     * @return True if the product should be issued, false otherwise.
     */
    public boolean isToBeIssued();

    /**
     * @return a dictionary representing the product information.
     */
    public Dict getProductInfo();
}