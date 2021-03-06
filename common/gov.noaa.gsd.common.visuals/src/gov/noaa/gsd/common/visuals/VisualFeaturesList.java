/**
 * This software was developed and / or modified by the
 * National Oceanic and Atmospheric Administration (NOAA), 
 * Earth System Research Laboratory (ESRL), 
 * Global Systems Division (GSD), 
 * Evaluation & Decision Support Branch (EDS)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.common.visuals;

import java.util.ArrayList;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeTypeAdapter;

/**
 * Description: List of visual features. Each of the elements must have an
 * identifier that is unique within the list.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- ------------ --------------------------
 * Feb 16, 2016   15676    Chris.Golden Initial creation.
 * Mar 26, 2016   15676    Chris.Golden Added convenience lookup methods.
 * Jun 10, 2016   19537    Chris.Golden Changed convenience method to just do
 *                                      a replace instead of a lookup.
 * Feb 13, 2017   28892    Chris.Golden Added annotations concerning
 *                                      serialization, since the latter is
 *                                      done at this level now, instead of at
 *                                      the level of individual visual
 *                                      features.
 * Feb 21, 2018   46736    Chris.Golden Added copy constructor.
 * </pre>
 * 
 * @author Chris.Golden
 * @version 1.0
 */
@DynamicSerialize
@DynamicSerializeTypeAdapter(factory = VisualFeaturesListSerializationAdapter.class)
public class VisualFeaturesList extends ArrayList<VisualFeature> {

    // Private Static Constants

    /**
     * Serialization version UID.
     */
    private static final long serialVersionUID = -2655616243967777587L;

    // Public Constructors

    /**
     * Construct a standard instance with the default capacity that is empty.
     */
    public VisualFeaturesList() {

        /*
         * No action.
         */
    }

    /**
     * Construct a standard instance with the specified capacity that is empty.
     * 
     * @param capacity
     *            Initial capacity.
     */
    public VisualFeaturesList(int capacity) {
        super(capacity);
    }

    /**
     * Construct a standard instance as a copy of the specified list. The copy
     * is shallow, that is, the visual features are not copied, only the list.
     * 
     * @param other
     *            Visual features list to be copied.
     */
    public VisualFeaturesList(VisualFeaturesList other) {
        super(other);
    }

    // Public Methods

    /**
     * Get a copy of the visual feature with the specified identifier.
     * 
     * @param identifier
     *            Identifier of the desired visual feature.
     * @return Visual feature, or <code>null</code> if none is found with the
     *         specified identifier.
     */
    public VisualFeature getByIdentifier(String identifier) {
        for (VisualFeature visualFeature : this) {
            if (visualFeature.getIdentifier().equals(identifier)) {
                return new VisualFeature(visualFeature);
            }
        }
        return null;
    }

    /**
     * Replace the visual feature in the list with an identifier matching the
     * specified visual feature's identifier with the latter.
     * 
     * @param visualFeature
     *            Visual feature to be used as a replacement.
     * @return True if a visual feature was found to be replaced, false
     *         otherwise.
     */
    public boolean replace(VisualFeature visualFeature) {
        for (int j = 0; j < size(); j++) {
            if (get(j).getIdentifier().equals(visualFeature.getIdentifier())) {
                set(j, visualFeature);
                return true;
            }
        }
        return false;
    }
}
