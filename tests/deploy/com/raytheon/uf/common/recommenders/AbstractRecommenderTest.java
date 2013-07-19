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

import static org.junit.Assert.fail;
import gov.noaa.gsd.viz.hazards.utilities.FileUtilities;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import jep.JepException;

import org.junit.Before;
import org.junit.BeforeClass;

import com.raytheon.uf.common.dataplugin.events.IEvent;
import com.raytheon.uf.common.localization.PathManagerFactoryTest;
import com.raytheon.uf.common.python.concurrent.IPythonJobListener;
import com.raytheon.uf.viz.recommenders.CAVERecommenderEngine;
import com.raytheon.uf.viz.recommenders.CAVERecommenderScriptManager;

/**
 * Tests the recommenders
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 27, 2013            mnash       Initial creation
 * Jul 19, 2013 1257       bsteffen    Convert recommender dialog info to use
 *                                     Serializeables for values instead of
 *                                     Strings.
 * 
 * </pre>
 * 
 * @author mnash
 * @version 1.0
 */

public abstract class AbstractRecommenderTest {

    protected volatile boolean proceed = false;

    /**
     * 
     */
    public AbstractRecommenderTest() {
        FileUtilities.fillFiles();
    }

    private AbstractRecommenderEngine<CAVERecommenderScriptManager> engine;

    @BeforeClass
    public static void classSetUp() {
        PathManagerFactoryTest.initLocalization();
    }

    @Before
    public void setUp() throws JepException {
        engine = new CAVERecommenderEngine();
    }

    public List<IEvent> runRecommender(String name,
            IPythonJobListener<List<IEvent>> listener) {
        try {
            for (EventRecommender rec : engine.getInventory()) {
                if (rec.getName().equals(name)) {
                    engine.runEntireRecommender(rec.getName(), listener);
                }
            }
        } catch (Throwable t) {
            fail("Could not run recommender " + t);
        }
        return null;
    }

    public Map<String, Serializable> getDialogInfo(String name) {
        try {
            for (EventRecommender rec : engine.getInventory()) {
                if (rec.getName().equals(name)) {
                    Map<String, Serializable> vals = engine.getDialogInfo(name);
                    return vals;
                }
            }
        } catch (Throwable t) {
            fail("Could not run get dialog info " + t);
        }
        return null;
    }

    public Map<String, String> getSpatialInfo(String name) {
        try {
            for (EventRecommender rec : engine.getInventory()) {
                if (rec.getName().equals(name)) {
                    Map<String, String> vals = engine.getSpatialInfo(name);
                    return vals;
                }
            }
        } catch (Throwable t) {
            fail("Could not run get dialog info " + t);
        }
        return null;
    }

}
