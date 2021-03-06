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
package com.raytheon.uf.common.dataplugin.events.hazards.request;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.serialization.comm.IServerRequest;

/**
 * 
 * Base class used for all hazard services requests to the registry via the
 * request instance
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer      Description
 * ------------ ---------- ------------- --------------------------
 * Aug 20, 2015    6895    Ben.Phillippe Routing registry requests through request server.
 * May 06, 2016   18202    Robert.Blum   Changes for operational mode.
 * Oct 02, 2017   38506    Chris.Golden  Added equals() and hashCode() methods.
 * </pre>
 * 
 * @author bphillip
 * @version 1.0
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public abstract class HazardRequest implements IServerRequest {

    /** Practice mode flag */
    @XmlElement
    @DynamicSerializeElement
    protected boolean practice;

    /**
     * Creates a new HazardRequest
     */
    protected HazardRequest() {
    }

    /**
     * Creates a new HazardRequest for the given mode
     * 
     * @param practice
     *            True if in practice mode, else false if in operational mode
     */
    protected HazardRequest(boolean practice) {
        this.practice = practice;
    }

    /**
     * Returns true if this request is in practice mode
     * 
     * @return True if this request is in practice mode
     */
    public boolean isPractice() {
        return practice;
    }

    /**
     * Sets the practice mode flag
     * 
     * @param practice
     *            True if in practice mode, else false for operational
     */
    public void setPractice(boolean practice) {
        this.practice = practice;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (practice ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        if (practice != ((HazardRequest) obj).practice) {
            return false;
        }
        return true;
    }
}
