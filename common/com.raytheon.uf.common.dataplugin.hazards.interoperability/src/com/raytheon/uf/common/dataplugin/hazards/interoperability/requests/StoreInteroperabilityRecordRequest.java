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
package com.raytheon.uf.common.dataplugin.hazards.interoperability.requests;

import java.util.List;

import com.raytheon.uf.common.dataplugin.events.hazards.request.HazardRequest;
import com.raytheon.uf.common.dataplugin.hazards.interoperability.HazardInteroperabilityRecord;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * 
 * Request used for storing interoperability records
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 20, 2015 6895     Ben.Phillippe Routing registry requests through request server
 * 
 * </pre>
 * 
 * @author bphillip
 * @version 1.0
 */
@DynamicSerialize
public class StoreInteroperabilityRecordRequest extends HazardRequest {

    /**
     * The interoperability events to store
     */
    @DynamicSerializeElement
    private List<HazardInteroperabilityRecord> events;

    /**
     * Creates a new StoreInteroperabilityRecordRequest
     */
    public StoreInteroperabilityRecordRequest() {

    }

    /**
     * Creates a new StoreInteroperabilityRecordRequest
     * 
     * @param events
     *            The events to store
     * @param practice
     *            Practice mode
     */
    public StoreInteroperabilityRecordRequest(
            List<HazardInteroperabilityRecord> events, boolean practice) {
        super(practice);
        this.events = events;
    }

    /**
     * @return the events
     */
    public List<HazardInteroperabilityRecord> getEvents() {
        return events;
    }

    /**
     * @param events
     *            the events to set
     */
    public void setEvents(List<HazardInteroperabilityRecord> events) {
        this.events = events;
    }
}
