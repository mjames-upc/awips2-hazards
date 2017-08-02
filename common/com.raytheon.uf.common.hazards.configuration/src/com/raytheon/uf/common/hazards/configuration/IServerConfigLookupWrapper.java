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
package com.raytheon.uf.common.hazards.configuration;

/**
 * Provides access to the localization site, host, and port.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 2, 2016  26624      bkowal      Initial creation
 *
 * </pre>
 *
 * @author bkowal
 */

public interface IServerConfigLookupWrapper {

    /**
     * Returns the localization site.
     * 
     * @return the localization site.
     */
    public String getSite();

    /**
     * Returns the localization host.
     * 
     * @return the localization host.
     */
    public String getHost();

    /**
     * Returns the localization port.
     * 
     * @return the localization port.
     */
    public String getPort();

}