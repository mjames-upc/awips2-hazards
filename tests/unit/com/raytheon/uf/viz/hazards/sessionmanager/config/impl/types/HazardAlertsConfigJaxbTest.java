/**
 * This software was developed and / or modified by the
 * National Oceanic and Atmospheric Administration (NOAA), 
 * Earth System Research Laboratory (ESRL), 
 * Global Systems Division (GSD), 
 * Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package com.raytheon.uf.viz.hazards.sessionmanager.config.impl.types;

import static org.junit.Assert.*;

import java.util.List;

import javax.xml.bind.JAXBException;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.raytheon.uf.common.colormap.Color;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.viz.hazards.sessionmanager.impl.HazardType;

/**
 * Description: Alerts configuration test
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 26, 2013  1325     daniel.s.schaffer@noaa.gov      Initial creation
 * 
 * </pre>
 * 
 * @author daniel.s.schaffer@noaa.gov
 * @version 1.0
 */
public class HazardAlertsConfigJaxbTest {

    @Test
    public void basic() {
        try {
            HazardAlertsConfig config = new HazardAlertsConfig();
            HazardEventExpirationAlertsConfig eventExpiration = new HazardEventExpirationAlertsConfig();
            List<HazardAlertCategoryConfig> configByCategory = Lists
                    .newArrayList();
            HazardAlertCategoryConfig categoryConfig = new HazardAlertCategoryConfig(
                    "conv long term");
            categoryConfig.setHazardTypes(Sets
                    .newHashSet(new HazardType("FF", "A", "Convective"),
                            new HazardType("FF", "W", "Convective")));
            List<HazardAlertTimerConfigCriterion> alertConfiguration = Lists
                    .newArrayList();
            Color color = new Color(10, 20, 30);
            HazardAlertTimerConfigCriterion criterion = new HazardAlertTimerConfigCriterion(
                    "10 min", HazardAlertTimerConfigCriterion.Units.MINUTES,
                    HazardAlertTimerConfigCriterion.Location.CONSOLE,
                    10 * TimeUtil.MILLIS_PER_MINUTE, color, true, false, true);
            alertConfiguration.add(criterion);
            categoryConfig.setConfiguration(alertConfiguration);
            configByCategory.add(categoryConfig);
            eventExpiration.setConfigByCategory(configByCategory);
            config.setEventExpirationConfig(eventExpiration);
            JAXBManager manager = new JAXBManager(HazardAlertsConfig.class);
            String xml = manager.marshalToXml(config);

            /**
             * TODO {@link JAXBManager#unmarshalFromXml} should be a generic
             * method so as to avoid need to cast.
             */
            HazardAlertsConfig newConfig = (HazardAlertsConfig) manager
                    .unmarshalFromXml(xml);
            assertEquals(config, newConfig);
            String newXml = manager.marshalToXml(newConfig);
            assertEquals(xml, newXml);
        } catch (JAXBException e) {
            fail(e.toString());
        }

    }

}
