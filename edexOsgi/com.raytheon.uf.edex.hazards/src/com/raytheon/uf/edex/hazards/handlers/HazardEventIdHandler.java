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
package com.raytheon.uf.edex.hazards.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.raytheon.uf.common.dataplugin.events.hazards.HazardConstants;
import com.raytheon.uf.common.dataplugin.events.hazards.datastorage.HazardQueryBuilder;
import com.raytheon.uf.common.dataplugin.events.hazards.requests.HazardEventIdRequest;
import com.raytheon.uf.common.dataplugin.events.hazards.requests.HazardRetrieveRequest;
import com.raytheon.uf.common.dataplugin.events.hazards.requests.HazardRetrieveRequestResponse;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.common.serialization.comm.RequestRouter;
import com.raytheon.uf.edex.database.cluster.ClusterLockUtils;
import com.raytheon.uf.edex.database.cluster.ClusterTask;

/**
 * Retrieves the next available hazard event id for the site given.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 11, 2013            mnash     Initial creation
 * 
 * </pre>
 * 
 * @author mnash
 * @version 1.0
 */

public class HazardEventIdHandler implements
        IRequestHandler<HazardEventIdRequest> {

    private static final String OPERATIONAL_LOCK_NAME = "Operational Hazard Services Event Id";

    private static final String PRACTICE_LOCK_NAME = "Practice Hazard Services Event Id";

    /**
     * 
     */
    public HazardEventIdHandler() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.serialization.comm.IRequestHandler#handleRequest
     * (com.raytheon.uf.common.serialization.comm.IServerRequest)
     */
    @Override
    public Object handleRequest(HazardEventIdRequest request) throws Exception {
        // have different numbering depending on practice/operational hazards
        String lockName = request.isPractice() ? PRACTICE_LOCK_NAME
                : OPERATIONAL_LOCK_NAME;
        ClusterTask task = ClusterLockUtils.lookupLock(lockName,
                request.getSiteId());

        task = ClusterLockUtils.lock(lockName, request.getSiteId(),
                task.getExtraInfo(), 15, true);
        Integer eventId = 0;
        if (task.getExtraInfo() == null || task.getExtraInfo().isEmpty()) {
            HazardRetrieveRequest req = new HazardRetrieveRequest();
            req.setPractice(request.isPractice());
            HazardQueryBuilder builder = new HazardQueryBuilder();
            builder.addKey(HazardConstants.SITE_ID, request.getSiteId());
            req.setFilters(builder.getQuery());
            HazardRetrieveRequestResponse response = (HazardRetrieveRequestResponse) RequestRouter
                    .route(req);
            if (response.getEvents().isEmpty()) {
                // starting at 1 if none exists in the database
                eventId = 1;
            } else {
                // we don't find the site in the cluster_task table, but we have
                // some in the hazards table, we want to make sure we start from
                // that value
                List<String> list = new ArrayList<String>(response.getEvents()
                        .keySet());
                List<Integer> ints = new ArrayList<Integer>();
                for (String l : list) {
                    ints.add(Integer.valueOf(l));
                }
                Collections.sort(ints);
                eventId = ints.get(ints.size() - 1);
            }
        } else {
            eventId = Integer.parseInt(task.getExtraInfo()) + 1;
        }
        ClusterLockUtils.updateExtraInfo(lockName, request.getSiteId(),
                String.valueOf(eventId));
        ClusterLockUtils.unlock(task, false);
        return eventId;
    }
}
