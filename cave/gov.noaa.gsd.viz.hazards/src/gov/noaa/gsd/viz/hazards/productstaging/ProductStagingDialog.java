/**
 * This software was developed and / or modified by the National Oceanic and
 * Atmospheric Administration (NOAA), Earth System Research Laboratory (ESRL),
 * Global Systems Division (GSD), Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.hazards.productstaging;

import gov.noaa.gsd.viz.hazards.dialogs.BasicDialog;
import gov.noaa.gsd.viz.hazards.jsonutilities.Dict;
import gov.noaa.gsd.viz.hazards.jsonutilities.DictList;
import gov.noaa.gsd.viz.megawidgets.MegawidgetException;
import gov.noaa.gsd.viz.megawidgets.MegawidgetManager;
import gov.noaa.gsd.viz.megawidgets.MegawidgetSpecificationException;
import gov.noaa.gsd.viz.mvp.widgets.ICommandInvocationHandler;
import gov.noaa.gsd.viz.mvp.widgets.ICommandInvoker;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import com.google.common.collect.Lists;
import com.raytheon.uf.common.dataplugin.events.hazards.HazardConstants;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.viz.ui.dialogs.ModeListener;

/**
 * This Product Staging Dialog handles multiple hazards per product. This is a
 * part of Hazard Life Cycle.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 10, 2012            shouming.wei      Initial creation
 * Feb     2013            Bryon L.    Refactored for use in MVP
 *                                     architecture.  Added JavaDoc.
 *                                     Cleaned up logic.
 * Feb     2013            Bryon L.    Added ability to use megawidgets for
 *                                     user-defined content.
 * Feb-Mar 2013            Chris G.    More refactoring for MVP reasons,
 *                                     general cleanup.
 * Jun 04, 2013            Chris G.    Added support for changing background
 *                                     and foreground colors in order to stay
 *                                     in synch with CAVE mode.
 * Jul 18, 2013    585     Chris G.    Changed to support loading from bundle.
 * </pre>
 * 
 * @author shouming.wei
 * @version 1.0
 */
class ProductStagingDialog extends BasicDialog {

    // Private Static Constants

    /**
     * Dialog title.
     */
    private static final String DIALOG_TITLE = "Product Staging";

    /**
     * Tab text prefix.
     */
    private static final String TAB_TEXT_PREFIX = "Product : ";

    /**
     * OK button text.
     */
    private static final String OK_BUTTON_TEXT = "Continue...";

    /**
     * Events section text.
     */
    private static final String EVENTS_SECTION_TEXT = "Events";

    /**
     * Parameters section text.
     */
    private static final String PARAMETERS_SECTION_TEXT = "Product Generator Parameters";

    /**
     * Logging mechanism.
     */
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(ProductStagingDialog.class);

    // Private Variables

    /**
     * Product staging information dictionary.
     */
    private Dict productStagingInfo;

    /**
     * Flag indicating whether or not the product is to be issued.
     */
    private boolean toBeIssued = false;

    /**
     * Megawidget manager for staging info megawidgets.
     */
    @SuppressWarnings("unused")
    private MegawidgetManager stagingMegawidgetManager = null;

    /**
     * Megawidget manager for dialog info megawidgets.
     */
    @SuppressWarnings("unused")
    private MegawidgetManager dialogMegawidgetManager = null;

    /**
     * Continue command invocation handler.
     */
    private ICommandInvocationHandler continueHandler = null;

    /**
     * Continue command invoker.
     */
    private final ICommandInvoker continueInvoker = new ICommandInvoker() {
        @Override
        public void setCommandInvocationHandler(
                ICommandInvocationHandler handler) {
            continueHandler = handler;
        }
    };

    // Private Classes

    /**
     * Megawidget manager for this dialog.
     */
    private class DialogMegawidgetManager extends MegawidgetManager {

        // Public Constructors

        /**
         * Construct a standard instance.
         * 
         * @param parent
         *            Parent composite in which the megawidgets are to be
         *            created.
         * @param specifiers
         *            List of dictionaries, each of the latter holding the
         *            parameters of a megawidget specifier. Each megawidget
         *            specifier must have an identifier that is unique within
         *            this list.
         * @param state
         *            State to be viewed and/or modified via the megawidgets
         *            that are constructed. Each megawidget specifier defined by
         *            <code>specifiers</code> should have an entry in this
         *            dictionary, mapping the specifier's identifier to the
         *            value that the megawidget will take on.
         * @throws MegawidgetException
         *             If one of the megawidget specifiers is invalid, or if an
         *             error occurs while creating or initializing one of the
         *             megawidgets.
         */
        public DialogMegawidgetManager(Composite parent, List<Dict> specifiers,
                Dict state) throws MegawidgetException {
            super(parent, specifiers, state, 0L, 0L, 0L, 0L);
        }

        // Protected Methods

        @Override
        protected final void commandInvoked(String identifier,
                String extraCallback) {

            // No action.
        }

        @Override
        protected final void stateElementChanged(String identifier, Object state) {

            // No action.
        }
    }

    // Public Constructors

    /**
     * Construct a standard instance.
     * 
     * @param parent
     *            Parent shell for this dialog.
     */
    public ProductStagingDialog(Shell parent) {
        super(parent);
        setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        setBlockOnOpen(false);
    }

    // Public Methods

    /**
     * Initialize the content.
     * 
     * @param isToBeIssued
     *            Flag indicating whether or not the product staging dialog is
     *            being called as the result of a product issue action.
     * @param productStagingInfo
     *            Dictionary containing a list containing information about the
     *            potential products for staging as well as possible megawidgets
     *            allowing for user-defined portions of the product staging
     *            dialog.
     */
    public void initialize(boolean isToBeIssued, Dict productStagingInfo) {
        this.toBeIssued = isToBeIssued;
        this.productStagingInfo = productStagingInfo;
    }

    /**
     * Get the continue command invoker.
     * 
     * @return Continue command invoker.
     */
    public ICommandInvoker getContinueInvoker() {
        return continueInvoker;
    }

    /**
     * Determine whether or not the product staging dialog is or was being
     * displayed as a result of an issue action.
     * 
     * @return True if the product staging dialog is or was being displayed as
     *         the result of an issue action, false otherwise.
     */
    public boolean isToBeIssued() {
        return toBeIssued;
    }

    /**
     * Get the dictionary holding the list of products and any megawidgets that
     * were used.
     * 
     * @return Dictionary holding the list of products and megawidgets.
     */
    public Dict getProductList() {
        return productStagingInfo;
    }

    // Protected Methods

    @Override
    protected int getDialogBoundsStrategy() {
        return DIALOG_PERSISTLOCATION;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(DIALOG_TITLE);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite top = (Composite) super.createDialogArea(parent);
        top.setLayout(new FillLayout());
        CTabFolder tabFolder = new CTabFolder(top, SWT.TOP);
        tabFolder.setBorderVisible(true);
        new ModeListener(tabFolder);
        List<Dict> hazardEventSetList = productStagingInfo
                .getDynamicallyTypedValue(HazardConstants.HAZARD_EVENT_SETS);
        for (int i = 0; i < hazardEventSetList.size(); i++) {
            Dict tabInfo = hazardEventSetList.get(i);
            CTabItem tabItem = new CTabItem(tabFolder, SWT.NONE);
            tabItem.setText(TAB_TEXT_PREFIX
                    + tabInfo.get("productGenerator").toString());
            Control control = createTabFolderPage(tabFolder, tabInfo);
            tabItem.setControl(control);
        }
        return top;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        Button ok = getButton(IDialogConstants.OK_ID);
        ok.setText(OK_BUTTON_TEXT);
        setButtonLayoutData(ok);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        super.buttonPressed(buttonId);
        if ((buttonId == IDialogConstants.OK_ID) && (continueHandler != null)) {
            continueHandler.commandInvoked("Continue");
        }
    }

    // Private Methods

    /**
     * Creates the contents of one product tab.
     * 
     * @param tabFolder
     *            The tabFolder to populate.
     * @param tabInfo
     *            Dictionary containing information for this tab page.
     * @return Created tab folder page.
     */
    private Control createTabFolderPage(CTabFolder tabFolder, Dict tabInfo) {
        ScrolledComposite scrolledComposite = new ScrolledComposite(tabFolder,
                SWT.V_SCROLL);
        Composite tabFolderPage = new Composite(scrolledComposite, SWT.NONE);

        // Create the layout for the main panel.
        GridLayout tabLayout = new GridLayout(1, true);
        tabLayout.marginRight = 5;
        tabLayout.marginLeft = 5;
        tabLayout.marginBottom = 5;
        tabLayout.marginHeight = 3;
        tabLayout.marginWidth = 3;
        tabFolderPage.setLayout(tabLayout);

        // Create the user-configurable portions of the dialog.
        try {
            createStagingInfoComposite(tabFolderPage, tabInfo);
            createProductInfoComposite(tabFolderPage, tabInfo);
        } catch (MegawidgetSpecificationException e) {
            statusHandler.error("ProductStagingDialog."
                    + "createTabFolderPage(): Megawidget creation error.", e);
        }

        // Configure the scrolled composite.
        scrolledComposite.setContent(tabFolderPage);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setShowFocusedControl(true);
        tabFolderPage.pack();
        scrolledComposite.setMinHeight(tabFolderPage.computeSize(SWT.DEFAULT,
                SWT.DEFAULT).y);
        return scrolledComposite;
    }

    /**
     * Build staging-specific megawidgets for the product display dialog.
     * 
     * @param tabPage
     *            Page in which to create the composite.
     * @param tabInfo
     *            Dictionary containing information for this tab page.
     * @throws MegawidgetSpecificationException
     *             An exception encountered while building megawidgets.
     */
    private void createStagingInfoComposite(Composite tabPage, Dict tabInfo)
            throws MegawidgetSpecificationException {
        Dict stagingInfoDict = tabInfo.getDynamicallyTypedValue("stagingInfo");
        if (stagingInfoDict != null && stagingInfoDict.size() > 0) {
            Group fieldsGroup = new Group(tabPage, SWT.NONE);
            fieldsGroup.setText(EVENTS_SECTION_TEXT);
            fieldsGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
                    false));
            stagingMegawidgetManager = buildMegawidgets(fieldsGroup,
                    stagingInfoDict);
        }
    }

    /**
     * Builds megawidgets specific to the product generators. These are added to
     * the dialog and collect any additional information required by the product
     * generators.
     * 
     * @param tabPage
     *            Page in which to create the composite.
     * @param tabInfo
     *            Dictionary containing information for this tab page.
     * @throws MegawidgetSpecificationException
     *             An exception encountered while building the megawidgets.
     */
    private void createProductInfoComposite(Composite tabPage, Dict tabInfo)
            throws MegawidgetSpecificationException {
        Dict productInfoDict = tabInfo.getDynamicallyTypedValue("dialogInfo");
        if (productInfoDict != null && productInfoDict.size() > 0) {
            Group productInfoGroup = new Group(tabPage, SWT.NONE);
            productInfoGroup.setText(PARAMETERS_SECTION_TEXT);
            productInfoGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
                    true, false));
            dialogMegawidgetManager = buildMegawidgets(productInfoGroup,
                    productInfoDict);
        }
    }

    /**
     * Constructs one or more megawidgets based on the contents of the supplied
     * dictionary. These megawidgets will be constructed in the supplied panel.
     * 
     * @param panel
     *            The container (parent) for the constructed megawidgets
     * @param megawidgetDict
     *            Dictionary containing an entry for the megawidgets (either a
     *            dictionary defining a single megawidget, or a list of
     *            dictionaries defining multiple megawidgets), and an entry for
     *            the dictionary holding the values for the megawidget(s).
     * @return Megawidget manager that was created.
     */
    private MegawidgetManager buildMegawidgets(Composite panel,
            Dict megawidgetDict) {

        // Get the dictionary or list of dictionaries defining the mega-
        // widget(s) to be built; if only a dictionary is provided, put it
        // in a list.
        Object specifiersObj = megawidgetDict
                .getDynamicallyTypedValue("fields");
        DictList specifiers = null;
        if (specifiersObj == null) {
            statusHandler.debug("ProductStagingDialog.buildMegawidgets(): "
                    + "Warning: no widgets specified for staging info.");
            return null;
        } else if (specifiersObj instanceof DictList) {
            specifiers = megawidgetDict.getDynamicallyTypedValue("fields");
        } else if (specifiersObj instanceof List) {
            specifiers = new DictList();
            for (Object specifier : (List<?>) specifiersObj) {
                specifiers.add(specifier);
            }
        } else {
            Dict specifier = megawidgetDict.getDynamicallyTypedValue("fields");
            specifiers = new DictList();
            specifiers.add(specifier);
        }

        // Get the dictionary holding the values for the megawidgets.
        Dict values = megawidgetDict.getDynamicallyTypedValue("valueDict");

        // Create the megawidget manager, which will in turn create the mega-
        // widgets and bind them to the values dictionary, and return it.
        List<Dict> specifiersList = Lists.newArrayList();
        for (Object specifier : specifiers) {
            specifiersList.add((Dict) specifier);
        }
        try {
            return new DialogMegawidgetManager(panel, specifiersList, values);
        } catch (MegawidgetException e) {
            statusHandler
                    .error("ProductStagingDialog.buildMegawidgets(): Unable to create "
                            + "megawidget manager due to megawidget construction problem.",
                            e);
            return null;
        }
    }
}
