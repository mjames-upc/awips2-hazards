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
 */
package com.raytheon.uf.common.hazards.ihfs.table;

import com.raytheon.uf.common.hazards.ihfs.data.CurPpTableData;

/**
 * This singleton class describes the data query model of the ihfs.CURPP table.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 28, 2015 8839       Chris.Cody  Initial Creation
 * 
 * </pre>
 * 
 * @author Chris.Cody
 * @version 1.0
 */
public class CurPpQueryTable extends ObservedQueryTable {

    public static final String CurPpTableName = "CURPP";

    private static CurPpQueryTable curPpQueryTable = null;

    private CurPpQueryTable() {
        super(CurPpTableName, CurPpTableData.class);
    }

    public static synchronized final CurPpQueryTable getInstance() {
        if (curPpQueryTable == null) {
            curPpQueryTable = new CurPpQueryTable();
        }

        return (curPpQueryTable);
    }
}
