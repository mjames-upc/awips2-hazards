/**
 * This software was developed and / or modified by the
 * National Oceanic and Atmospheric Administration (NOAA), 
 * Earth System Research Laboratory (ESRL), 
 * Global Systems Division (GSD), 
 * Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.megawidgets;

import java.util.Map;

/**
 * Description: Adapter implementation of the megawidget manager listener
 * interface. This provides methods that do nothing, and may be used as a base
 * class for any listeners that do not need to implement all of the methods.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- ------------ --------------------------
 * Jul 08, 2014    3512    Chris.Golden Initial creation.
 * </pre>
 * 
 * @author Chris.Golden
 * @version 1.0
 */
public class MegawidgetManagerAdapter implements IMegawidgetManagerListener {

    @Override
    public void commandInvoked(MegawidgetManager manager, String identifier) {

        /*
         * No action.
         */
    }

    @Override
    public void stateElementChanged(MegawidgetManager manager,
            String identifier, Object state) {

        /*
         * No action.
         */
    }

    @Override
    public void stateElementsChanged(MegawidgetManager manager,
            Map<String, Object> statesForIdentifiers) {

        /*
         * No action.
         */
    }

    @Override
    public void sizeChanged(MegawidgetManager manager, String identifier) {

        /*
         * No action.
         */
    }

    @Override
    public void sideEffectMutablePropertyChangeErrorOccurred(
            MegawidgetManager manager, MegawidgetPropertyException exception) {

        /*
         * No action.
         */
    }
}
