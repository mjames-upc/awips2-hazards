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
package com.raytheon.uf.edex.hazards.interop.gfe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.dataplugin.gfe.GridDataHistory;
import com.raytheon.uf.common.dataplugin.gfe.dataaccess.GFEDataAccessUtil;
import com.raytheon.uf.common.dataplugin.gfe.db.objects.GFERecord;
import com.raytheon.uf.common.dataplugin.gfe.db.objects.GridParmInfo;
import com.raytheon.uf.common.dataplugin.gfe.db.objects.ParmID;
import com.raytheon.uf.common.dataplugin.gfe.request.AbstractGfeRequest;
import com.raytheon.uf.common.dataplugin.gfe.request.CommitGridsRequest;
import com.raytheon.uf.common.dataplugin.gfe.request.GetGridHistoryRequest;
import com.raytheon.uf.common.dataplugin.gfe.request.GetGridInventoryRequest;
import com.raytheon.uf.common.dataplugin.gfe.request.GetGridParmInfoRequest;
import com.raytheon.uf.common.dataplugin.gfe.request.LockChangeRequest;
import com.raytheon.uf.common.dataplugin.gfe.request.SaveGfeGridRequest;
import com.raytheon.uf.common.dataplugin.gfe.server.lock.LockTable.LockMode;
import com.raytheon.uf.common.dataplugin.gfe.server.message.ServerResponse;
import com.raytheon.uf.common.dataplugin.gfe.server.request.CommitGridRequest;
import com.raytheon.uf.common.dataplugin.gfe.server.request.LockRequest;
import com.raytheon.uf.common.dataplugin.gfe.server.request.SaveGridRequest;
import com.raytheon.uf.common.message.WsId;
import com.raytheon.uf.common.serialization.comm.RequestRouter;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.TimeRange;

/**
 * Handles making request to retrieve grid information from the database.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 27, 2013 2277       jsanchez     Initial creation
 * Mar 24, 2013 3323       bkowal       Created methods to retrieve the correct
 *                                      GridParmInfo based on mode.
 * Apr 08, 2014 3357       bkowal       No longer attempt to save to the Official
 *                                      DB for practice mode.
 * Jan 19, 2014 4840       rferrel      Log error when exception getting GFE Record.
 * May 29, 2015 6895       Ben.Phillippe Refactored Hazard Service data access
 * May 06, 2016 18202      Robert.Blum  Changes for operational mode.
 * Sep 14, 2016 15934      Chris.Golden Fixed errors caused when no inventories
 *                                      for the current site can be found.
 * </pre>
 * 
 * @author jsanchez
 * @version 1.0
 */

public class GridRequestHandler {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(GridRequestHandler.class);

    private static final String PARM_ID_PREFIX_FORMAT = "Hazards_SFC:%s_GRID";

    private static final String PARM_ID_SUFFIX_FORMAT = "Fcst_00000000_0000";

    public static final String OPERATIONAL_PARM_ID_FORMAT = PARM_ID_PREFIX_FORMAT
            + "__" + PARM_ID_SUFFIX_FORMAT;

    public static final String PRACTICE_PARM_ID_FORMAT = PARM_ID_PREFIX_FORMAT
            + "_Prac_" + PARM_ID_SUFFIX_FORMAT;

    private String requestRoute;

    public GridRequestHandler() {

    }

    public GridRequestHandler(String requestRoute) {
        this.requestRoute = requestRoute;
    }

    /**
     * Retrieves the GridParmInfo for the siteID.
     * 
     * @param siteID
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public GridParmInfo requestGridParmInfo(String siteID, String parmIDFormat)
            throws Exception {
        GetGridParmInfoRequest gridParmInfoRequest = new GetGridParmInfoRequest();
        gridParmInfoRequest.setParmIds(Arrays.asList(new ParmID[] { new ParmID(
                String.format(parmIDFormat, siteID)) }));

        ServerResponse<List<GridParmInfo>> response = (ServerResponse<List<GridParmInfo>>) makeRequest(
                siteID, gridParmInfoRequest);

        GridParmInfo info = null;
        if (response != null && response.getPayload() != null) {
            List<GridParmInfo> payload = response.getPayload();
            info = payload.get(0);
        }

        return info;
    }

    public GridParmInfo requestGridParmInfo(boolean practice, String siteID)
            throws Exception {
        return (practice ? requestPracticeGridParmInfo(siteID)
                : requestOperationalGridParmInfo(siteID));
    }

    public GridParmInfo requestOperationalGridParmInfo(String siteID)
            throws Exception {
        return requestGridParmInfo(siteID, OPERATIONAL_PARM_ID_FORMAT);
    }

    public GridParmInfo requestPracticeGridParmInfo(String siteID)
            throws Exception {
        return requestGridParmInfo(siteID, PRACTICE_PARM_ID_FORMAT);
    }

    /**
     * Finds hazard GFERecords that intersect with the timeRange.
     * 
     * @param timeRange
     * @return
     */
    public List<GFERecord> findIntersectedGrid(ParmID parmID,
            TimeRange timeRange) throws Exception {
        List<GFERecord> records = new ArrayList<GFERecord>();
        List<TimeRange> inventoryTimeRanges = getGridInventory(parmID);
        List<TimeRange> intersectingTimeRanges = new ArrayList<TimeRange>();
        if (inventoryTimeRanges != null) {
            for (TimeRange inventoryTimeRange : inventoryTimeRanges) {
                if (timeRange.overlaps(inventoryTimeRange)) {
                    intersectingTimeRanges.add(inventoryTimeRange);
                }
            }
        }
        Map<TimeRange, List<GridDataHistory>> histories = getGridHistory(parmID
                .getDbId().getSiteId(), parmID, intersectingTimeRanges);
        for (TimeRange tr : intersectingTimeRanges) {
            GFERecord record = new GFERecord(parmID, tr);
            try {
                record.setGridHistory(histories.get(tr));
                record.setMessageData(GFEDataAccessUtil.getSlice(record));
                records.add(record);
            } catch (Exception ex) {
                if (statusHandler.isPriorityEnabled(Priority.ERROR)) {
                    String message = String
                            .format("Unable to obtain GFE Record for ParamID: %s, in the time range: %s",
                                    parmID.getParmId(), tr.toString());
                    statusHandler.error(message, ex);
                }
            }
        }

        return records;
    }

    /**
     * Retrieves the grid history for the list of time ranges.
     * 
     * @param siteID
     *            the siteId of the histories needed
     * @param parmID
     *            the parmId of the histories needed
     * @param timeRanges
     *            the time range of histories needed
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private Map<TimeRange, List<GridDataHistory>> getGridHistory(String siteID,
            ParmID parmID, List<TimeRange> timeRanges) throws Exception {
        try {
            GetGridHistoryRequest request = new GetGridHistoryRequest();
            request.setParmID(parmID);
            request.setTimeRanges(timeRanges);
            ServerResponse<?> sr = makeRequest(siteID, request);
            Map<TimeRange, List<GridDataHistory>> histories = (Map<TimeRange, List<GridDataHistory>>) sr
                    .getPayload();
            return histories;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    /**
     * Retrieves the grid inventory for the site's grid parm info.
     * 
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public List<TimeRange> getGridInventory(ParmID parmID) throws Exception {
        GetGridInventoryRequest request = new GetGridInventoryRequest();
        request.setParmIds(Arrays.asList(new ParmID[] { parmID }));
        try {
            ServerResponse<Map<ParmID, List<TimeRange>>> response = (ServerResponse<Map<ParmID, List<TimeRange>>>) makeRequest(
                    parmID.getDbId().getSiteId(), request);
            Map<ParmID, List<TimeRange>> map = response.getPayload();
            return map.get(parmID);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private ServerResponse<?> makeRequest(String siteID,
            AbstractGfeRequest request) throws Exception {
        request.setSiteID(siteID);
        request.setWorkstationID(new WsId(null, null, "CAVE"));
        if (requestRoute == null) {
            return (ServerResponse<?>) RequestRouter.route(request);
        } else {
            return (ServerResponse<?>) RequestRouter.route(request,
                    requestRoute);
        }
    }

    /**
     * Store the new grid(s). If records is 'null' then the grids for the
     * replacementTimeRange will be deleted.
     * 
     * @param records
     * @param gridParmInfo
     *            TODO
     * @param replacementTimeRange
     * @throws Exception
     */
    public void store(List<GFERecord> records, GridParmInfo gridParmInfo,
            TimeRange replacementTimeRange, boolean practice) throws Exception {
        saveToForecastDB(gridParmInfo, records, replacementTimeRange);
        if (practice == false) {
            saveToOfficialDB(gridParmInfo, replacementTimeRange);
        }
    }

    /**
     * Stores the GFERecord objects in the gfe tables.
     * 
     * @param records
     * @return
     * @throws Exception
     */
    private ServerResponse<?> saveToForecastDB(GridParmInfo gridParmInfo,
            List<GFERecord> records, TimeRange replacementTimeRange)
            throws Exception {
        ParmID parmID = gridParmInfo.getParmID();
        String siteID = parmID.getDbId().getSiteId();
        // Make a request to lock the time range of the grid. Otherwise, the
        // grid would be prevented from being saved if locked by someone else.
        LockRequest lreq = new LockRequest(parmID, replacementTimeRange,
                LockMode.LOCK);
        List<LockRequest> requests = new ArrayList<LockRequest>(1);
        requests.add(lreq);
        LockChangeRequest request = new LockChangeRequest();
        request.setRequests(requests);
        makeRequest(siteID, request);
        ServerResponse<?> response = null;
        try {
            // Make a request to actually save the grid.
            List<SaveGridRequest> sgr = new ArrayList<SaveGridRequest>();
            sgr.add(new SaveGridRequest(gridParmInfo.getParmID(),
                    replacementTimeRange, records));
            SaveGfeGridRequest saveGfeGridRequest = new SaveGfeGridRequest();
            saveGfeGridRequest.setSaveRequests(sgr);
            saveGfeGridRequest.setClientSendStatus(false);
            response = makeRequest(siteID, saveGfeGridRequest);
        } finally {
            // Unlock
            requests.clear();
            requests.add(new LockRequest(gridParmInfo.getParmID(),
                    replacementTimeRange, LockMode.UNLOCK));
            request.setRequests(requests);
            makeRequest(siteID, request);
        }
        return response;
    }

    private void saveToOfficialDB(GridParmInfo gridParmInfo,
            TimeRange commitTime) throws Exception {
        ParmID parmID = gridParmInfo.getParmID();
        String siteID = parmID.getDbId().getSiteId();

        CommitGridRequest req = new CommitGridRequest(parmID, commitTime);
        CommitGridsRequest request = new CommitGridsRequest();
        request.setCommits(Arrays.asList(new CommitGridRequest[] { req }));
        makeRequest(siteID, request);
    }

    public List<TimeRange> requestAdjacentTimeRanges(ParmID parmID,
            TimeRange timeRange) throws Exception {
        // find adjacent timeRanges
        List<TimeRange> inventoryTimeRanges = getGridInventory(parmID);
        List<TimeRange> adjacentTimeRanges = new ArrayList<TimeRange>();
        for (TimeRange tr : inventoryTimeRanges) {
            if (tr.isAdjacentTo(timeRange)) {
                adjacentTimeRanges.add(tr);
            }
        }
        return adjacentTimeRanges;
    }

}
