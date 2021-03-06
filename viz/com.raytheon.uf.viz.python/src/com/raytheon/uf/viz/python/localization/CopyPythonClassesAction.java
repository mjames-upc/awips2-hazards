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
package com.raytheon.uf.viz.python.localization;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.localization.FileUpdatedMessage;
import com.raytheon.uf.common.localization.FileUpdatedMessage.FileChangeType;
import com.raytheon.uf.common.localization.ILocalizationFileObserver;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.LocalizationUtil;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.protectedfiles.ProtectedFileLookup;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.localization.LocalizationManager;
import com.raytheon.uf.viz.localization.perspective.service.ILocalizationService;
import com.raytheon.uf.viz.localization.perspective.service.LocalizationPerspectiveUtils;
import com.raytheon.viz.ui.VizWorkbenchManager;

/**
 * A file copy action that will copy the Python classes from one Python file to
 * a new user-specified file.
 * 
 * If this capability ever becomes available to any Python file, this class
 * should be relocated to com.raytheon.uf.viz.localization.perspective and it
 * should be updated to extend the AbstractToAction.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 18, 2013 2461       bkowal      Initial creation
 * Nov 25, 2013 2461       bkowal      Refactor
 * Feb 19, 2014 3018       bkowal      Ensure that unusable localization levels
 *                                     are disabled in the sub-menu. Ensure that
 *                                     files are not temporarily overwritten when
 *                                     the save to localization fails.
 * 
 * 
 * </pre>
 * 
 * @author bkowal
 * @version 1.0
 */

public class CopyPythonClassesAction extends Action implements IMenuCreator {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(CopyPythonClassesAction.class);

    private static final String MENU_TEXT = "Create Override File ...";

    private static final String PY_CLASS_PLACEHOLDER = "    pass";

    private static final String PY_CLASS_PATTERN_STRING = "^class[ \t].+:";

    private static final Pattern PY_CLASS_PATTERN = Pattern
            .compile(PY_CLASS_PATTERN_STRING);

    private Menu menu;

    private LocalizationFile file;

    private LocalizationFile newFile;

    /**
     * @param file
     * @param service
     */
    public CopyPythonClassesAction(LocalizationFile file) {
        super(MENU_TEXT, IAction.AS_DROP_DOWN_MENU);
        this.file = file;
    }

    @Override
    public void run() {
    }

    public void run(LocalizationLevel level) {
        String fullFilePath = this.file.getName();
        final String name = FilenameUtils.getName(fullFilePath);

        final String newFullFilePath = FilenameUtils.getFullPathNoEndSeparator(
                fullFilePath) + IOUtils.DIR_SEPARATOR + name;

        IPathManager pm = PathManagerFactory.getPathManager();
        final LocalizationFile originalFile = pm
                .getLocalizationFile(this.file.getContext(), fullFilePath);
        this.newFile = pm.getLocalizationFile(
                pm.getContext(this.file.getContext().getLocalizationType(),
                        level),
                newFullFilePath);
        boolean overwrite = true;
        if (this.newFile.exists()) {
            Shell parent = VizWorkbenchManager.getInstance().getCurrentWindow()
                    .getShell();

            overwrite = MessageDialog.openConfirm(parent, "Override file",
                    "The file: " + newFile.toString() + " exists, "
                            + "are you sure you want to override it?");
        }

        if (overwrite == false) {
            return;
        }

        try {
            this.copyClassesToNewFile(originalFile);
        } catch (IOException e1) {
            statusHandler.handle(Priority.ERROR,
                    "Unable to copy the python classes!", e1);
            return;
        }

        // refresh the localization perspective and open the new file
        final ILocalizationService service = LocalizationPerspectiveUtils
                .changeToLocalizationPerspective();
        try {
            final Runnable select = new Runnable() {
                @Override
                public void run() {
                    service.selectFile(newFile);
                    service.openFile(newFile);
                }
            };
            final ILocalizationFileObserver[] observers = new ILocalizationFileObserver[1];
            ILocalizationFileObserver observer = new ILocalizationFileObserver() {
                @Override
                public void fileUpdated(FileUpdatedMessage message) {
                    if (message.getChangeType() != FileChangeType.DELETED) {
                        service.fileUpdated(message);
                        VizApp.runAsync(select);
                    }
                    newFile.removeFileUpdatedObserver(observers[0]);
                }
            };
            observers[0] = observer;
            newFile.addFileUpdatedObserver(observer);
            newFile.save();
        } catch (LocalizationException e) {
            this.restoreOldFile();
            statusHandler.handle(Priority.ERROR,
                    "Unable to save file to localization", e);
        }
    }

    private void restoreOldFile() {
        try {
            /* Replace the "new" file with the old file. */
            FileUtils.moveFile(this.file.getFile(), this.newFile.getFile());
        } catch (IOException e) {
            /*
             * If the replacement fails, a simple open and close of the file
             * will restore the old file.
             */
            statusHandler.handle(Priority.ERROR,
                    "Failed to restore the original localization file: "
                            + this.file.getName()
                            + ". Please close and re-open the file.",
                    e);
        }
    }

    private void copyClassesToNewFile(LocalizationFile originalFile)
            throws IOException {
        List<String> pythonLines = FileUtils.readLines(originalFile.getFile());
        List<String> outputLines = new LinkedList<String>();
        int classCount = 0;

        // identify lines that indicate a class declaration
        for (String pythonLine : pythonLines) {
            if (PY_CLASS_PATTERN.matcher(pythonLine).matches()) {
                if (classCount > 0) {
                    // add spaces between multiple classes.
                    outputLines.add(IOUtils.LINE_SEPARATOR);
                    outputLines.add(IOUtils.LINE_SEPARATOR);
                }

                outputLines.add(pythonLine + IOUtils.LINE_SEPARATOR);
                // class definition placeholder to prevent pydev from
                // complaining
                outputLines.add(PY_CLASS_PLACEHOLDER);
                ++classCount;
            }
        }

        // write the output file.
        FileUtils.writeLines(this.newFile.getFile(), outputLines,
                StringUtils.EMPTY, false);
    }

    /**
     * 
     */
    protected void fillMenu(Menu menu) {
        LocalizationLevel[] levels = PathManagerFactory.getPathManager()
                .getAvailableLevels();
        for (int i = 0; i < levels.length; ++i) {
            LocalizationLevel level = levels[i];
            if (level.isSystemLevel() == false) {
                new ActionContributionItem(
                        new CopyPythonClassesInternalAction(level)).fill(menu,
                                -1);
            }
        }
    }

    /**
     * Determines if the action for this level is enabled. By default, checks if
     * the level is the same as the file level
     * 
     * @param level
     * @return
     */
    protected boolean isLevelEnabled(LocalizationLevel level) {
        boolean enabled = true;
        /*
         * This is based on the isLevelEnabled method in AbstractToAction
         * because we cannot directly invoke the method due to access
         * restrictions on the plugin containing the AbstractToAction class.
         */
        if (level == this.file.getContext().getLocalizationLevel()) {
            String fileCtxName = this.file.getContext().getContextName();
            String levelCtxName = LocalizationManager.getContextName(level);
            if ((fileCtxName == null && levelCtxName == null)
                    || (fileCtxName != null
                            && fileCtxName.equals(levelCtxName))) {
                // same context name
                enabled = false;
            }
        }
        if (enabled && ProtectedFileLookup.isProtected(this.file)) {
            return ProtectedFileLookup.getProtectedLevel(file).compareTo(level) >= 0;
        }

        return enabled;
    }

    @Override
    public IMenuCreator getMenuCreator() {
        return this;
    }

    @Override
    public void dispose() {
        if (this.menu != null) {
            this.menu.dispose();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets
     * .Menu)
     */
    @Override
    public Menu getMenu(Menu parent) {
        if (this.menu != null) {
            this.menu.dispose();
        }

        this.menu = new Menu(parent);

        fillMenu(this.menu);
        return this.menu;
    }

    @Override
    public Menu getMenu(Control parent) {
        if (this.menu != null) {
            this.menu.dispose();
        }

        this.menu = new Menu(parent);

        fillMenu(this.menu);

        return this.menu;
    }

    private class CopyPythonClassesInternalAction extends Action {

        private LocalizationLevel level;

        public CopyPythonClassesInternalAction(LocalizationLevel level) {
            this.level = level;
            this.setEnabled(isLevelEnabled(level));
        }

        @Override
        public String getText() {
            String name = LocalizationUtil.getProperName(level);
            String context = LocalizationManager.getContextName(level);
            if (context != null) {
                name += " (" + context + ")";
            }
            return name;
        }

        @Override
        public void run() {
            CopyPythonClassesAction.this.run(level);
        }

    }
}