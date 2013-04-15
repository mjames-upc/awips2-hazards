/**
 * This software was developed and / or modified by the National Oceanic and
 * Atmospheric Administration (NOAA), Earth System Research Laboratory (ESRL),
 * Global Systems Division (GSD), Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.hazards.productstaging;

import gov.noaa.gsd.viz.hazards.display.HazardServicesPresenter;
import gov.noaa.gsd.viz.hazards.display.IHazardServicesModel;
import gov.noaa.gsd.viz.hazards.display.IHazardServicesModel.Element;
import gov.noaa.gsd.viz.hazards.display.action.ProductStagingAction;
import gov.noaa.gsd.viz.hazards.jsonutilities.Dict;
import gov.noaa.gsd.viz.mvp.widgets.ICommandInvocationHandler;

import java.util.EnumSet;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * Settings presenter, used to mediate between the model and the settings view.
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
 * @author bryon.lawrence
 * @version 1.0
 */
public class ProductStagingPresenter extends
        HazardServicesPresenter<IProductStagingView<?, ?>> {

    // Private Static Constants

    /**
     * Logging mechanism.
     */
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(ProductStagingPresenter.class);

    // Private Variables

    /**
     * Continue command invocation handler.
     */
    private final ICommandInvocationHandler continueHandler = new ICommandInvocationHandler() {
        @Override
        public void commandInvoked(String command) {
            try {
                String issueFlag = (getView().isToBeIssued() ? "True" : "False");
                ProductStagingAction action = new ProductStagingAction(
                        "Continue");
                action.setIssueFlag(issueFlag);
                Dict productInfo = getView().getProductInfo();
                action.setJSONText(productInfo.toJSONString());
                fireAction(action);
            } catch (Exception e1) {
                statusHandler.error("ProductStatingPresenter.bind(): ", e1);
            }
        }
    };

    // Public Constructors

    /**
     * Construct a standard instance of a product staging presenter.
     * 
     * @param model
     *            Model to be handled by this presenter.
     * @param view
     *            Product staging view to be handled by this presenter.
     */
    public ProductStagingPresenter(IHazardServicesModel model,
            IProductStagingView<?, ?> view) {
        super(model, view);
    }

    // Public Methods

    /**
     * Receive notification of a model change. For the moment, the product
     * staging dialog doesn't care about model event.
     * 
     * @param changes
     *            Set of elements within the model that have changed.
     */
    @Override
    public void modelChanged(EnumSet<Element> changed) {

        // No action.
    }

    /**
     * Show a subview providing setting detail for the current setting.
     * 
     * @param issueFlag
     *            Whether or not this is a result of an issue action
     * @param productList
     *            List of products to stage.
     */
    public final void showProductStagingDetail(boolean issueFlag,
            Dict productStagingInfo) {
        getView().showProductStagingDetail(issueFlag, productStagingInfo);
        bind();
    }

    // Protected Methods

    /**
     * Initialize the specified view in a subclass-specific manner.
     * 
     * @param view
     *            View to be initialized.
     */
    @Override
    protected void initialize(IProductStagingView<?, ?> view) {

        // No action.
    }

    /**
     * Binds the presenter to the view which implements the IProductStagingView
     * interface. The interface is the contract, and it is all the presenter
     * needs to know about the view. This allows different views to easily be
     * created and given to this presenter.
     * <p>
     * By binding to the view, the presenter handles all of the view's events.
     */
    private void bind() {
        getView().getContinueInvoker().setCommandInvocationHandler(
                continueHandler);
    }
}