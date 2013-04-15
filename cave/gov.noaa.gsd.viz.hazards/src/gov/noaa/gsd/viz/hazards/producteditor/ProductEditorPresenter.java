/**
 * This software was developed and / or modified by the National Oceanic and
 * Atmospheric Administration (NOAA), Earth System Research Laboratory (ESRL),
 * Global Systems Division (GSD), Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.hazards.producteditor;

import gov.noaa.gsd.viz.hazards.display.HazardServicesPresenter;
import gov.noaa.gsd.viz.hazards.display.IHazardServicesModel;
import gov.noaa.gsd.viz.hazards.display.IHazardServicesModel.Element;
import gov.noaa.gsd.viz.hazards.display.action.ProductEditorAction;
import gov.noaa.gsd.viz.hazards.jsonutilities.Dict;
import gov.noaa.gsd.viz.mvp.widgets.ICommandInvocationHandler;

import java.util.EnumSet;
import java.util.List;

/**
 * Description: Product Editor presenter, used to mediate between the model and
 * the product editor view.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer       Description
 * ------------ ---------- -------------- --------------------------
 * Feb 19, 2013            bryon.lawrence Initial creation
 * 
 * </pre>
 * 
 * @author Bryon.Lawrence
 * @version 1.0
 */
public class ProductEditorPresenter extends
        HazardServicesPresenter<IProductEditorView<?, ?>> {

    // Public Constructors

    /**
     * Construct a standard instance of a ProductEditorPresenter.
     * 
     * @param model
     *            Model to be handled by this presenter.
     * @param view
     *            Product editor view to be handled by this presenter.
     */
    public ProductEditorPresenter(IHazardServicesModel model,
            IProductEditorView<?, ?> view) {
        super(model, view);
    }

    // Public Methods

    /**
     * Receive notification of a model change. For the moment, the product
     * editor dialog doesn't care about model events.
     * 
     * @param changes
     *            Set of elements within the model that have changed.
     */
    @Override
    public void modelChanged(EnumSet<Element> changed) {

        // No action.
    }

    /**
     * Show a view of the product editor.
     * 
     * @param productInfo
     *            Product information in as a JSON string.
     */
    public final void showProductEditorDetail(String productInfo) {
        getView().showProductEditorDetail(productInfo);
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
    protected void initialize(IProductEditorView<?, ?> view) {
        view.initialize();
    }

    // Private Methods

    /**
     * Binds the presenter to the view which implements the IProductEditorView
     * interface. The interface is the contract, and it is all the presenter
     * needs to know about the view. This allows different views to easily be
     * created and given to this presenter.
     * <p>
     * By binding to the view, the presenter handles all of the view's events.
     * 
     * @param
     * @return
     */
    private void bind() {
        getView().getIssueInvoker().setCommandInvocationHandler(
                new ICommandInvocationHandler() {

                    @Override
                    public void commandInvoked(String command) {
                        ProductEditorAction action = new ProductEditorAction(
                                "Issue");

                        List<Dict> hazardEventSetsList = getView()
                                .getHazardEventSetsList();
                        List<Dict> generatedProductsDictList = getView()
                                .getGeneratedProductsDictList();
                        Dict returnDict = new Dict();
                        returnDict.put("generatedProducts",
                                generatedProductsDictList);
                        returnDict.put("hazardEventSets", hazardEventSetsList);

                        action.setJSONText(returnDict.toJSONString());
                        ProductEditorPresenter.this.fireAction(action);
                        getView().closeProductEditorDialog();
                    }
                });

        getView().getDismissInvoker().setCommandInvocationHandler(
                new ICommandInvocationHandler() {

                    @Override
                    public void commandInvoked(String command) {
                        ProductEditorAction action = new ProductEditorAction(
                                "Dismiss");

                        List<Dict> hazardEventSetsList = getView()
                                .getHazardEventSetsList();
                        List<Dict> generatedProductsDictList = getView()
                                .getGeneratedProductsDictList();
                        Dict returnDict = new Dict();
                        returnDict.put("generatedProducts",
                                generatedProductsDictList);
                        returnDict.put("hazardEventSets", hazardEventSetsList);

                        action.setJSONText(returnDict.toJSONString());
                        ProductEditorPresenter.this.fireAction(action);
                        getView().closeProductEditorDialog();
                    }
                });

        getView().getShellClosedInvoker().setCommandInvocationHandler(
                new ICommandInvocationHandler() {
                    @Override
                    public void commandInvoked(String command) {
                        ProductEditorPresenter.this
                                .fireAction(new ProductEditorAction("Dismiss"));
                        getView().closeProductEditorDialog();

                    }
                });
    }

}