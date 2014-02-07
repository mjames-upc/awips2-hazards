/**
 * This software was developed and / or modified by the
 * National Oceanic and Atmospheric Administration (NOAA), 
 * Earth System Research Laboratory (ESRL), 
 * Global Systems Division (GSD), 
 * Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.hazards.display.test;

import gov.noaa.gsd.viz.hazards.producteditor.IProductEditorView;
import gov.noaa.gsd.viz.mvp.widgets.ICommandInvocationHandler;
import gov.noaa.gsd.viz.mvp.widgets.ICommandInvoker;

import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import com.raytheon.uf.common.hazards.productgen.GeneratedProductList;

/**
 * Description: Mock {@link IProductEditorView} used for testing.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 22, 2013 2166       daniel.s.schaffer@noaa.gov      Initial creation
 * Feb 07, 2014 2890       bkowal      Product Generation JSON refactor.
 * 
 * </pre>
 * 
 * @author daniel.s.schaffer@noaa.gov
 * @version 1.0
 */
@SuppressWarnings("rawtypes")
public class ProductEditorViewForTesting implements IProductEditorView {

    private GeneratedProductList generatedProducts;

    private List<GeneratedProductList> generatedProductsList;

    @Override
    public void dispose() {
    }

    @Override
    public List contributeToMainUI(Enum type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void initialize() {
    }

    @Override
    public void closeProductEditorDialog() {
    }

    @Override
    public GeneratedProductList getGeneratedProductList() {
        return generatedProducts;
    }

    @Override
    public ICommandInvoker getIssueInvoker() {
        return new ICommandInvoker() {

            @Override
            public void setCommandInvocationHandler(
                    ICommandInvocationHandler handler) {
            }

        };
    }

    @Override
    public ICommandInvoker getDismissInvoker() {
        return new ICommandInvoker() {

            @Override
            public void setCommandInvocationHandler(
                    ICommandInvocationHandler handler) {
            }

        };
    }

    @Override
    public ICommandInvoker getShellClosedInvoker() {
        return new ICommandInvoker() {

            @Override
            public void setCommandInvocationHandler(
                    ICommandInvocationHandler handler) {
            }

        };
    }

    @Override
    public void openDialog() {
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public List<GeneratedProductList> getGeneratedProductsList() {
        return this.generatedProductsList;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean showProductEditorDetail(List generatedProductsList) {
        this.generatedProductsList = generatedProductsList;
        this.generatedProducts = new GeneratedProductList();
        for (GeneratedProductList productList : this.generatedProductsList) {
            this.generatedProducts.addAll(productList);
        }

        return true;
    }
}