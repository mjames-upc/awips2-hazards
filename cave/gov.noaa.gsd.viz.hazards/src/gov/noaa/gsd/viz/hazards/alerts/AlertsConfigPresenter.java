/**
 * This software was developed and / or modified by the
 * National Oceanic and Atmospheric Administration (NOAA), 
 * Earth System Research Laboratory (ESRL), 
 * Global Systems Division (GSD), 
 * Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.hazards.alerts;

import gov.noaa.gsd.common.eventbus.BoundedReceptionEventBus;
import gov.noaa.gsd.viz.hazards.display.HazardServicesPresenter;

import java.util.EnumSet;

import com.raytheon.uf.common.dataplugin.events.hazards.HazardConstants;
import com.raytheon.uf.viz.hazards.sessionmanager.ISessionManager;
import com.raytheon.uf.viz.hazards.sessionmanager.events.impl.ObservedHazardEvent;

/**
 * Alerts presenter, used to mediate between the model and the alerts view.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 04, 2013            Chris.Golden      Initial induction into repo
 * Jul 15, 2013     585    Chris.Golden      Changed to support loading from bundle,
 *                                           including the passing in of the event
 *                                           bus so that the latter is no longer a
 *                                           singleton.
 * Nov 04, 2013 2182     daniel.s.schaffer@noaa.gov      Started refactoring
 * 
 * Dec 03, 2013 2182     daniel.s.schaffer@noaa.gov Refactoring - eliminated IHazardsIF
 * May 17, 2014 2925       Chris.Golden      Added newly required implementation of
 *                                           reinitialize(), and made initialize()
 *                                           protected as it is called by setView().
 * </pre>
 * 
 * @author Chris.Golden
 * @version 1.0
 */
public class AlertsConfigPresenter extends
        HazardServicesPresenter<IAlertsConfigView<?, ?>> {

    // Public Constructors

    /**
     * Construct a standard instance.
     * 
     * @param model
     *            Model to be handled by this presenter.
     * @param eventBus
     *            Event bus used to signal changes.
     */
    public AlertsConfigPresenter(ISessionManager<ObservedHazardEvent> model,
            BoundedReceptionEventBus<Object> eventBus) {
        super(model, eventBus);
    }

    // Public Methods

    @Override
    public final void modelChanged(EnumSet<HazardConstants.Element> changed) {

        // No action.
    }

    /**
     * Show a subview providing setting detail for the current alert.
     */
    public final void showAlertDetail() {

        throw new UnsupportedOperationException("Not yet implemented");
    }

    // Protected Methods

    @Override
    protected final void initialize(IAlertsConfigView<?, ?> view) {
        getView().initialize(this);
    }

    @Override
    protected final void reinitialize(IAlertsConfigView<?, ?> view) {

        /*
         * No action.
         */
    }
}
