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
package com.raytheon.uf.common.recommenders;

import com.raytheon.uf.common.localization.LocalizationFile;

/**
 * POJO containing pertinent information about recommenders
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- ------------ --------------------------
 * Jan 21, 2013            mnash        Initial creation
 * Dec 16, 2015 14019     Robert.Blum   Updates for new PythonJobCoordinator API.
 * 
 * </pre>
 * 
 * @author mnash
 * @version 1.0
 */

public class EventRecommender {

    public static final String DESCRIPTION = "description";

    public static final String AUTHOR = "author";

    public static final String VERSION = "version";

    private LocalizationFile file;

    private String name;

    private String author;

    private String description;

    private String version;

    /**
     * @return the file
     */
    public LocalizationFile getFile() {
        return file;
    }

    /**
     * @param file
     *            the file to set
     */
    protected void setFile(LocalizationFile file) {
        this.file = file;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    protected void setName(String name) {
        this.name = name;
    }

    /**
     * @return the author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * @param author
     *            the author to set
     */
    protected void setAuthor(String author) {
        this.author = author;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     *            the description to set
     */
    protected void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version
     *            the version to set
     */
    protected void setVersion(String version) {
        this.version = version;
    }
}
