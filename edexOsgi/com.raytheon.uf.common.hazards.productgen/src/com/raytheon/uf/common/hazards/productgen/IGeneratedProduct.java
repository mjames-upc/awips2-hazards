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
package com.raytheon.uf.common.hazards.productgen;

import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.dataplugin.events.EventSet;
import com.raytheon.uf.common.dataplugin.events.IEvent;

/**
 * 
 * Generated product interface for the client.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 17, 2012            jsanchez     Initial creation
 * Aug 20, 2013 1360       blawrenc     Added methods to set/get event set
 * 
 * </pre>
 * 
 * @author jsanchez
 * @version 1.0
 */
public interface IGeneratedProduct {

    /** @return Returns a pil */
    public String getProductID();

    /** @return Returns a map of format types to generated products */
    public Map<String, List<Object>> getEntries();

    /**
     * @param format
     *            the format type
     * @return Returns a generated product from a format type
     */
    public List<Object> getEntry(String format);

    /** @return Errors thrown when executing a python class */
    public String getErrors();

    /**
     * 
     * @param
     * @return A set of events
     */
    public EventSet<IEvent> getEventSet();

    /**
     * 
     * @param An
     *            event set
     * @return
     */
    public void setEventSet(EventSet<IEvent> eventSet);

}
