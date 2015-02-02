/**
 * This software was developed and / or modified by the National Oceanic and
 * Atmospheric Administration (NOAA), Earth System Research Laboratory (ESRL),
 * Global Systems Division (GSD), Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.viz.hazards.spatialdisplay.drawableelements;

import gov.noaa.gsd.viz.hazards.spatialdisplay.HazardServicesDrawingAttributes;
import gov.noaa.nws.ncep.ui.pgen.elements.Layer;

import java.util.List;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Base class for polygon Hazard-Geometries in Hazard Services.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 09, 2015 6260       Dan Schaffer        Initial creation.                                        points.
 * </pre>
 * 
 * @author daniel.s.schaffer
 */
public class HazardServicesMultiPolygon extends HazardServicesShape {
    private final Geometry geometry;

    /**
     * 
     * @param drawingAttributes
     *            Attributes associated with this drawable.
     * @param pgenCategory
     *            The PGEN category of this drawable. Not used by Hazard
     *            Services but required by PGEN.
     * @param pgenType
     *            The PGEN type of this drawable. Not used by Hazard Services
     *            but required by PGEN.
     * @param geometry
     *            The geometry defining this drawable.
     * @param activeLayer
     *            The PGEN layer this will be drawn to.
     * @param id
     *            The id associated with this drawable.
     */
    public HazardServicesMultiPolygon(
            HazardServicesDrawingAttributes drawingAttributes,
            String pgenCategory, String pgenType, Geometry geometry,
            Layer activeLayer, String id) {
        super(id, drawingAttributes);
        List<Coordinate> points = Lists.newArrayList(geometry.getCoordinates());
        setLinePoints(points);
        update(drawingAttributes);
        setPgenCategory(pgenCategory);
        setPgenType(pgenType);
        setParent(activeLayer);
        this.geometry = geometry;
    }

    @Override
    public Geometry getGeometry() {
        return geometry;
    }

}