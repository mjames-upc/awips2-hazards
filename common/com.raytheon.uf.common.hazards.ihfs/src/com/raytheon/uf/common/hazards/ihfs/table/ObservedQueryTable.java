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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.raytheon.uf.common.hazards.ihfs.TableColumnData;
import com.raytheon.uf.common.hazards.ihfs.data.AbstractTableData;

/**
 * This class describes the data query model of the ihfs.HEIGHT table.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 28, 2015 8839       Chris.Cody  Initial Creation
 * Aug 14, 2015 9988       Chris.Cody  Base Database Code changes for Dates
 * 
 * </pre>
 * 
 * @author Chris.Cody
 * @version 1.0
 */
public class ObservedQueryTable extends AbstractQueryTable {

    private static final List<String> ObservedColumnNameList = Arrays.asList(
            "LID", "PE", "DUR", "TS", "EXTREMUM", "OBSTIME", "VALUE",
            "SHEF_QUAL_CODE", "QUALITY_CODE", "REVISION", "PRODUCT_ID",
            "PRODUCTTIME", "POSTINGTIME");

    protected ObservedQueryTable(String tableName,
            Class<? extends AbstractTableData> tableDataClass) {
        super(tableName, ObservedColumnNameList, tableDataClass);

        tableColumnDataMap = new HashMap<>(ObservedColumnNameList.size());
        tableColumnDataMap.put("LID", new TableColumnData(tableName, "LID",
                TableColumnData.STRING_TYPE, 8, 0, true));
        tableColumnDataMap.put("PE", new TableColumnData(tableName, "PE",
                TableColumnData.STRING_TYPE, 2, 1, true));
        tableColumnDataMap.put("DUR", new TableColumnData(tableName, "DUR",
                TableColumnData.INTEGER_TYPE, TableColumnData.INTEGER_LEN, 2,
                true));
        tableColumnDataMap.put("TS", new TableColumnData(tableName, "TS",
                TableColumnData.STRING_TYPE, 2, 3, true));
        tableColumnDataMap.put("EXTREMUM", new TableColumnData(tableName,
                "EXTREMUM", TableColumnData.STRING_TYPE, 1, 4, true));
        tableColumnDataMap.put("OBSTIME", new TableColumnData(tableName,
                "OBSTIME", TableColumnData.DATE_TYPE, TableColumnData.DATE_LEN,
                5, true));
        tableColumnDataMap.put("VALUE", new TableColumnData(tableName, "VALUE",
                TableColumnData.DOUBLE_TYPE, TableColumnData.DOUBLE_LEN, 6,
                false));
        tableColumnDataMap.put("SHEF_QUAL_CODE", new TableColumnData(tableName,
                "SHEF_QUAL_CODE", TableColumnData.STRING_TYPE, 1, 7, false));
        tableColumnDataMap.put("QUALITY_CODE", new TableColumnData(tableName,
                "QUALITY_CODE", TableColumnData.INTEGER_TYPE,
                TableColumnData.INTEGER_LEN, 8, false));
        tableColumnDataMap.put("REVISION", new TableColumnData(tableName,
                "REVISION", TableColumnData.INTEGER_TYPE,
                TableColumnData.INTEGER_LEN, 9, false));
        tableColumnDataMap.put("PRODUCT_ID", new TableColumnData(tableName,
                "PRODUCT_ID", TableColumnData.STRING_TYPE, 10, 10, false));
        tableColumnDataMap.put("PRODUCTTIME", new TableColumnData(tableName,
                "PRODUCTTIME", TableColumnData.DATE_TYPE,
                TableColumnData.DATE_LEN, 11, false));
        tableColumnDataMap.put("POSTINGTIME", new TableColumnData(tableName,
                "POSTINGTIME", TableColumnData.DATE_TYPE,
                TableColumnData.DATE_LEN, 12, false));
    }
}
