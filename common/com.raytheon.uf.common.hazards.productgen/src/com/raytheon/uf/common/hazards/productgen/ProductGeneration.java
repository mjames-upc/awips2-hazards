/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.hazards.productgen;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;

import com.raytheon.uf.common.dataplugin.events.EventSet;
import com.raytheon.uf.common.dataplugin.events.IEvent;
import com.raytheon.uf.common.dataplugin.events.interfaces.IDefineDialog;
import com.raytheon.uf.common.dataplugin.events.interfaces.IProvideMetadata;
import com.raytheon.uf.common.hazards.productgen.executors.GenerateProductExecutor;
import com.raytheon.uf.common.hazards.productgen.executors.GenerateProductFromExecutor;
import com.raytheon.uf.common.hazards.productgen.executors.ProductDialogInfoExecutor;
import com.raytheon.uf.common.hazards.productgen.executors.ProductMetadataExecutor;
import com.raytheon.uf.common.hazards.productgen.executors.UpdateProductExecutor;
import com.raytheon.uf.common.hazards.productgen.product.ProductScript;
import com.raytheon.uf.common.hazards.productgen.product.ProductScriptFactory;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.python.concurrent.IPythonExecutor;
import com.raytheon.uf.common.python.concurrent.IPythonJobListener;
import com.raytheon.uf.common.python.concurrent.PythonJobCoordinator;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * 
 * Generates product into different formats based on a eventSet.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 10, 2013            jsanchez     Initial creation
 * Sep 19, 2013 2046       mnash        Update for less dependencies.
 * Nov  5, 2013 2266       jsanchez     Removed unused method and used GeneratedProductList.
 * Apr 23, 2014 1480       jsanchez     Passed correction flag to update method.
 * Oct 03, 2014 4042       Chris.Golden Added ability to get product script file path.
 * Jan 20, 2015 4476       rferrel      Implement shutdown of PythonJobCoordinator.
 * Feb 15, 2015 2271       Dan Schaffer Incur recommender/product generator init costs immediately
 * Feb 26, 2015 6306       mduff        Pass site id in.
 * Apr 16, 2015 7579       Robert.Blum  Replace prevDataList with keyinfo object.
 * May 07, 2015 6979       Robert.Blum  Added method to update product dictionaries without
 *                                      running the entire generator again.
 * Nov 17, 2015 3473       Robert.Blum  Moved all python files under HazardServices localization
 *                                      dir.
 * Dec 16, 2015 14019      Robert.Blum  Updates for new PythonJobCoordinator API.
 * May 03, 2016 18376      Chris.Golden Changed to support reuse of Jep instance between H.S.
 *                                      sessions in the same CAVE session, since stopping and
 *                                      starting the Jep instances when the latter use numpy is
 *                                      dangerous.
 * Feb 23, 2017 29170      Robert.Blum  Product Editor refactor.
 * Mar 21, 2017 29996      Robert.Blum  Moved getScriptFile() to SessionConfigurationManager.
 * Apr 19, 2017 32734      Kevin.Bisanz Increase NUM_GENERATION_THREADS from 1 to 4.
 * Jun 05, 2017 29996      Robert.Blum  Changed generateFrom() to accept product parts.
 * </pre>
 * 
 * @author jsanchez
 * @version 1.0
 */
public class ProductGeneration implements IDefineDialog, IProvideMetadata {

    // Private Static Constants

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(ProductGeneration.class);

    private static final String GENERATION_THREAD_POOL_NAME = "ProductGenerators";

    private static final int NUM_GENERATION_THREADS = 4;

    /**
     * Instance of this class to be used for all Hazard Services sessions. A
     * single instance is shared, instead of starting up and shutting down one
     * instance per session, because of bad numpy-Jep interactions as described
     * <a href="https://github.com/mrj0/jep/issues/28">here</a>. By keeping a
     * singleton around, Jep with numpy loaded is never shut down.
     */
    private static final ProductGeneration PRODUCT_GENERATION = new ProductGeneration();

    // Private Variables

    /**
     * Manages execution of product script jobs.
     */
    private final PythonJobCoordinator<ProductScript> coordinator;

    private final ProductScriptFactory factory;

    private final IPathManager pathManager = PathManagerFactory
            .getPathManager();

    // Public Static Methods

    /**
     * Get the singleton instance of this class using the specified site
     * identifier.
     * 
     * @param site
     *            Site identifier.
     * @return Singleton instance of this class.
     */
    public static ProductGeneration getInstance(String site) {
        PRODUCT_GENERATION.setSite(site);
        return PRODUCT_GENERATION;
    }

    // Private Constructors

    /**
     * Construct a standard instance.
     */
    private ProductGeneration() {
        factory = new ProductScriptFactory();
        coordinator = new PythonJobCoordinator<>(NUM_GENERATION_THREADS,
                GENERATION_THREAD_POOL_NAME, factory);
        if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
            String c = coordinator.toString();
            statusHandler
                    .debug("ProductGeneration.init incrementing reference count for "
                            + c.substring(c.lastIndexOf('.') + 1));
        }
    }

    // Public Methods

    /**
     * Set the site identifier. This must be done at least once before any
     * products are generated.
     * 
     * @param site
     *            Site identifier.
     */
    public void setSite(String site) {
        factory.setSite(site);
    }

    /**
     * Shutdown.
     */
    public void shutdown() {

        /*
         * For now, do nothing; since Jep and numpy do not play well together
         * when a Jep instance is shut down and then another one started that
         * also uses numpy, this instance needs to be kept around and functional
         * in case H.S. starts up again.
         */
        // if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
        // String c = coordinator.toString();
        // statusHandler
        // .debug("ProductGeneration.shutdown decrementing reference count for "
        // + c.substring(c.lastIndexOf('.') + 1));
        // }
        // coordinator.shutdown();
    }

    /**
     * Generate the event set into different formats. The job is performed
     * asynchronously and will be passed to the session manager.
     * 
     * @param productGeneratorName
     *            name of the product generator. "ExampleFFW" refers to the
     *            python class "ExampleFFW.py" which should be in the
     *            /common_static/base/python/events/productgen/products
     *            directory
     * @param eventSet
     *            the EventSet<IEvent> object that will provide the information
     *            for the product generator
     * @param productFormats
     *            array of formats to be generated (i.e. "XML", "ASCII")
     * @param listener
     *            the listener to the aysnc job
     */
    public void generate(String productGeneratorName, EventSet<IEvent> eventSet,
            Map<String, Serializable> dialogInfo, String[] productFormats,
            IPythonJobListener<GeneratedProductList> listener) {

        /*
         * Validate the parameter values.
         */
        validate(productFormats, productGeneratorName, eventSet, listener);

        IPythonExecutor<ProductScript, GeneratedProductList> executor = new GenerateProductExecutor(
                productGeneratorName, eventSet, dialogInfo, productFormats);

        try {
            coordinator.submitJobWithCallback(executor, listener);
        } catch (Exception e) {
            statusHandler.error("Error executing async job", e);
        }
    }

    /**
     * Accept an updated data list and passes it to the 'executeFrom' of the
     * product generator.
     * 
     * @param productGeneratorName
     * @param generatedProducts
     * @param productParts
     * @param productFormats
     * @param listener
     */
    public void generateFrom(String productGeneratorName,
            GeneratedProductList generatedProducts,
            List<ProductPart> productParts, String[] productFormats,
            IPythonJobListener<GeneratedProductList> listener) {
        IPythonExecutor<ProductScript, GeneratedProductList> executor = new GenerateProductFromExecutor(
                productGeneratorName, generatedProducts, productParts,
                productFormats);

        try {
            coordinator.submitJobWithCallback(executor, listener);
        } catch (Exception e) {
            statusHandler.error("Error executing async job", e);
        }
    }

    /**
     * Update the updated data list, then pass it to the different formats. The
     * job is performed asynchronously and will be passed to the session
     * manager.
     * 
     * @param productGeneratorName
     *            name of the product generator. "ExampleFFW" refers to the
     *            python class "ExampleFFW.py" which should be in the
     *            /common_static/base/python/events/productgen/products
     *            directory
     * @param eventSet
     *            Event set object that will provide the information for the
     *            product generator.
     * @param updatedDataList
     *            the previously generated dictionaries that will be updated.
     * @param productFormats
     *            array of formats to be generated (i.e. "XML", "ASCII")
     * @param listener
     *            the listener to the aysnc job
     */
    public void update(String productGeneratorName, EventSet<IEvent> eventSet,
            List<Map<String, Serializable>> updatedDataList,
            String[] productFormats,
            IPythonJobListener<GeneratedProductList> listener) {

        /*
         * Validate the parameter values.
         */
        validate(productFormats, productGeneratorName, eventSet, listener);

        IPythonExecutor<ProductScript, GeneratedProductList> executor = new UpdateProductExecutor(
                productGeneratorName, eventSet, updatedDataList,
                productFormats);

        try {
            coordinator.submitJobWithCallback(executor, listener);
        } catch (Exception e) {
            statusHandler.error("Error executing async job", e);
        }
    }

    @Override
    public Map<String, Serializable> getDialogInfo(String product,
            EventSet<IEvent> eventSet) {
        IPythonExecutor<ProductScript, Map<String, Serializable>> executor = new ProductDialogInfoExecutor(
                product, eventSet);
        Map<String, Serializable> retVal = null;
        try {
            retVal = coordinator.submitJob(executor).get();
        } catch (Exception e) {
            statusHandler.error("Error executing job", e);
        }

        return retVal;
    }

    @Override
    public Map<String, Serializable> getMetadata(String product,
            EventSet<IEvent> eventSet) {
        IPythonExecutor<ProductScript, Map<String, Serializable>> executor = new ProductMetadataExecutor(
                product, eventSet);
        Map<String, Serializable> retVal = null;
        try {
            retVal = coordinator.submitJob(executor).get();
        } catch (Exception e) {
            statusHandler.error("Error executing job", e);
        }

        return retVal;
    }

    // Private Methods

    /**
     * Validates 1) if 'formats' is not null, 2) if 'product' is not null, and
     * 3) if hazardEvent set is not null and not empty.
     * 
     * @param formats
     * @param product
     * @param eventSet
     */
    private void validate(String[] formats, String product,
            EventSet<IEvent> eventSet,
            IPythonJobListener<GeneratedProductList> listener) {
        Validate.notNull(formats, "'FORMATS' must be set.");
        Validate.notNull(product, "'PRODUCT' must be set.");
        Validate.notNull(eventSet, "'HAZARD EVENT SET' must be set");
        Validate.notNull(listener, "'listener' must be set.");
    }
}
