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
package com.raytheon.uf.viz.recommenders.executors;

import java.util.Map;

import com.raytheon.uf.viz.recommenders.CAVERecommenderScriptManager;

/**
 * Off-loads the getScriptMetadata method using the Python concurrent
 * functionality
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 6, 2013            mnash     Initial creation
 * 
 * </pre>
 * 
 * @author mnash
 * @version 1.0
 */

public class CAVERecommenderGetMetadataExecutor extends
        AbstractRecommenderExecutor<Map<String, String>> {

    /**
     * @param recommenderName
     */
    public CAVERecommenderGetMetadataExecutor(String recommenderName) {
        super(recommenderName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.python.concurrent.IPythonExecutor#execute(com.
     * raytheon.uf.common.python.PythonInterpreter)
     */
    @Override
    public Map<String, String> execute(CAVERecommenderScriptManager script) {
        return script.getInfo(recommenderName, "getScriptMetadata");
    }

}
