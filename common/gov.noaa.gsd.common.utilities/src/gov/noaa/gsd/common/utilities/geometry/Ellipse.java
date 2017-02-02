/**
 * This software was developed and / or modified by the
 * National Oceanic and Atmospheric Administration (NOAA), 
 * Earth System Research Laboratory (ESRL), 
 * Global Systems Division (GSD), 
 * Evaluation & Decision Support Branch (EDS)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.common.utilities.geometry;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeTypeAdapter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Description: Ellipse, with the center point described as a latitude-longitude
 * coordinate and the width and height described in specified units.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- ------------ --------------------------
 * Aug 31, 2016   15934    Chris.Golden Initial creation.
 * Sep 21, 2016   15934    Chris.Golden Corrected bugs in the use of the width
 *                                      and height (the formula used by the
 *                                      getRadius() method expects horizontal
 *                                      and vertical radii, not diameters, thus
 *                                      they need to each by divided by 2), as
 *                                      well as the rotation.
 * Sep 29, 2016   15928    Chris.Golden Made scaleable, switched to use rotation
 *                                      angles in radians, and added methods to
 *                                      get rotated or scaled copies.
 * Oct 12, 2016   15928    Chris.Golden Changed behavior to allow resizing
 *                                      to cause geometries to flip over the
 *                                      appropriate axis if the user crosses
 *                                      that axis while resizing.
 * </pre>
 * 
 * @author Chris.Golden
 * @version 1.0
 */
@DynamicSerialize
@DynamicSerializeTypeAdapter(factory = AdvancedGeometrySerializationAdapter.class)
public class Ellipse implements IRotatable, IScaleable {

    // Private Static Constants

    /**
     * Number of polygon coordinates to be assumed may be generated by
     * {@link #asGeometry(GeometryFactory, double, int)}.
     */
    private static final int STARTING_NUMBER_OF_POLYGON_COORDINATES = 100;

    /**
     * Angles in radians from the center point of an ellipse at which, at a bare
     * minimum, points will be generated to create a JTS {@link Polygon} by
     * {@link #asGeometry(GeometryFactory, double, int)}.
     */
    private static final double[] ANGLES_IN_RADIANS_OF_POLYGON_POINTS = { 0.0,
            Math.toRadians(90.0), Math.toRadians(180.0), Math.toRadians(270.0) };

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 305523075801044778L;

    // Private Variables

    /**
     * Center point as a latitude-longitude pair.
     */
    private final Coordinate centerPoint;

    /**
     * Width in units given by {@link #units}.
     */
    private final double width;

    /**
     * Height in units given by {@link #units}.
     */
    private final double height;

    /**
     * Units used by {@link #width} and {@link #height}.
     */
    private final LinearUnit units;

    /**
     * Rotation in counterclockwise radians.
     */
    private final double rotation;

    // Public Constructors

    /**
     * Construct a standard instance.
     * 
     * @param centerPoint
     *            Center point of the ellipse as a latitude-longitude pair; must
     *            not be <code>null</code>.
     * @param width
     *            Width in units given by <code>units</code>.
     * @param height
     *            Height in units given by <code>units</code>.
     * @param units
     *            Units for <code>width</code> and <code>height</code>; must not
     *            be <code>null</code>.
     * @param rotation
     *            Rotation in counterclockwise radians.
     */
    @JsonCreator
    public Ellipse(@JsonProperty("centerPoint") Coordinate centerPoint,
            @JsonProperty("width") double width,
            @JsonProperty("height") double height,
            @JsonProperty("units") LinearUnit units,
            @JsonProperty("rotation") double rotation) {
        this.centerPoint = new Coordinate(centerPoint.x, centerPoint.y, 0.0);
        this.width = width;
        this.height = height;
        this.units = units;

        /*
         * Ensure rotation is between 0 (inclusive) and 2 * Pi (exclusive).
         */
        this.rotation = (rotation + (2.0 * Math.PI)) % (2.0 * Math.PI);
    }

    // Public Methods

    @Override
    public Coordinate getCenterPoint() {
        return new Coordinate(centerPoint);
    }

    /**
     * Get the width in the units specified by {@link #getUnits()}.
     * 
     * @return Width.
     */
    public double getWidth() {
        return width;
    }

    /**
     * Get the height in the units specified by {@link #getUnits()}.
     * 
     * @return Height.
     */
    public double getHeight() {
        return height;
    }

    /**
     * Get the units in which the values provided by {@link #getWidth()} and
     * {@link #getHeight()} are specified.
     * 
     * @return Units.
     */
    public LinearUnit getUnits() {
        return units;
    }

    @Override
    public double getRotation() {
        return rotation;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Ellipse == false) {
            return false;
        }
        Ellipse otherEllipse = (Ellipse) other;
        return (centerPoint.equals(otherEllipse.centerPoint)
                && (width == otherEllipse.width)
                && (height == otherEllipse.height)
                && (units == otherEllipse.units) && (rotation == otherEllipse.rotation));
    }

    @Override
    public int hashCode() {
        return (int) ((((long) centerPoint.hashCode())
                + ((long) Double.valueOf(width).hashCode())
                + (Double.valueOf(height).hashCode()) + (units.ordinal()) + (Double
                    .valueOf(rotation).hashCode())) % Integer.MAX_VALUE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <G extends IAdvancedGeometry> G copyOf() {
        return (G) new Ellipse(new Coordinate(centerPoint), width, height,
                units, rotation);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <G extends IRotatable> G rotatedCopyOf(double delta) {
        return (G) new Ellipse(new Coordinate(centerPoint), width, height,
                units, (rotation + delta + (2.0 * Math.PI)) % (2.0 * Math.PI));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <G extends IScaleable> G scaledCopyOf(double horizontalMultiplier,
            double verticalMultiplier) {
        if ((horizontalMultiplier == 0.0) || (verticalMultiplier == 0.0)) {
            throw new IllegalArgumentException(
                    "scale multipliers must be non-zero numbers");
        }
        return (G) new Ellipse(new Coordinate(centerPoint), width
                * Math.abs(horizontalMultiplier), height
                * Math.abs(verticalMultiplier), units, rotation);
    }

    @Override
    public boolean isPunctual() {
        return false;
    }

    @Override
    public boolean isLineal() {
        return false;
    }

    @Override
    public boolean isPolygonal() {
        return true;
    }

    @Override
    public boolean isPotentiallyCurved() {
        return true;
    }

    /**
     * Get a JTS {@link Polygon} describing a flattened version of the ellipse.
     * Note that <code>limit</code> must be at least 2, meaning there will be at
     * least four distinct points (plus the first point repeated at the end) in
     * the resulting polygon.
     */
    @Override
    public Geometry asGeometry(GeometryFactory geometryFactory,
            double flatness, int limit) {

        /*
         * Ensure that the limit is large enough.
         */
        if (limit < 2) {
            throw new IllegalArgumentException("limit must be 2 or more");
        }

        /*
         * Add the first point to the list of points being compiled.
         */
        List<Coordinate> coordinates = new ArrayList<>(
                STARTING_NUMBER_OF_POLYGON_COORDINATES);
        coordinates
                .add(getCircumferentialPoint(ANGLES_IN_RADIANS_OF_POLYGON_POINTS[0]));

        /*
         * For each of the four quadrants, add the necessary points. Each
         * iteration adds all points needed up to and including the point at the
         * boundary with the next quadrant, e.g. for the first iteration, the
         * result will leave points up to and including the 90 degree angle
         * point.
         */
        for (int j = 0; j < ANGLES_IN_RADIANS_OF_POLYGON_POINTS.length; j++) {
            double endAngleRadians = ANGLES_IN_RADIANS_OF_POLYGON_POINTS[(j + 1)
                    % ANGLES_IN_RADIANS_OF_POLYGON_POINTS.length];
            addLatLonCoordinatesApproximatingCurve(coordinates,
                    ANGLES_IN_RADIANS_OF_POLYGON_POINTS[j], endAngleRadians,
                    coordinates.get(coordinates.size() - 1),
                    getCircumferentialPoint(endAngleRadians), flatness,
                    limit - 2);
        }

        /*
         * Create a polygon from the resulting points.
         */
        return geometryFactory.createPolygon(coordinates
                .toArray(new Coordinate[coordinates.size()]));
    }

    @Override
    public Coordinate getCentroid(GeometryFactory geometryFactory,
            double flatness, int limit) {
        return new Coordinate(centerPoint);
    }

    @Override
    public boolean isValid() {
        return ((width > 0.0) && (height > 0.0) && (units != null));
    }

    @Override
    public String getValidityProblemDescription() {
        if (isValid()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        boolean nonEmpty = false;
        if (width <= 0.0) {
            builder.append("width not positive number");
            nonEmpty = true;
        }
        if (width <= 0.0) {
            if (nonEmpty) {
                builder.append(", ");
            }
            builder.append("height not positive number");
            nonEmpty = true;
        }
        if (units == null) {
            if (nonEmpty) {
                builder.append(", ");
            }
            builder.append("no units specified");
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return "ELLIPSE(CENTER(" + centerPoint.x + ", " + centerPoint.y
                + ") SIZE(" + width + " x " + height + ") UNITS(" + units
                + ") ROTATION(" + rotation + "))";
    }

    // Private Methods

    /**
     * Get the radius of the ellipse in {@link #units} at the specified angle.
     * 
     * @param angleRadians
     *            Angle in radians.
     * @return Radius of the ellipse.
     */
    private double getRadius(double angleRadians) {
        return (width * height / 4.0)
                / Math.sqrt(Math.pow(Math.sin(angleRadians) * (width / 2.0),
                        2.0)
                        + Math.pow(Math.cos(angleRadians) * (height / 2.0), 2.0));
    }

    /**
     * Get the latitude-longitude point along the circumference of the ellipse
     * at the specified angle in radians.
     * 
     * @param angleRadians
     *            Angle in radians.
     * @return Point along the circumference.
     */
    private Coordinate getCircumferentialPoint(double angleRadians) {

        /*
         * Get the radius at the specified angle, but then use the sum of this
         * angle and the rotation angle when providing a direction in which to
         * offset the center point by the calculated radius. This yields the
         * circumferential point rotated around the center point into position.
         */
        return units.getLatLonOffsetBy(centerPoint, getRadius(angleRadians),
                (angleRadians + rotation) % (2.0 * Math.PI));
    }

    /**
     * Add latitude-longitude coordinates to the specified list to approximate
     * the elliptical curve between the specified angles, flattening it as per
     * the given parameters.
     * 
     * @param coordinates
     *            List to which to add the coordinates.
     * @param startAngleRadians
     *            Starting angle in radians.
     * @param endAngleRadians
     *            Ending angle in radians.
     * @param startPoint
     *            Coordinate of the point along the circumference at
     *            <code>startAngleRadians</code>.
     * @param endPoint
     *            Coordinate of the point along the circumference at
     *            <code>endAngleRadians</code>.
     * @param flatness
     *            Maximum allowable distance between the control points and the
     *            flattened curve, unless <code>limit</code> is exceeded.
     * @param limit
     *            Maximum remaining number of recursive subdivisions allowed to
     *            this invocation.
     */
    private void addLatLonCoordinatesApproximatingCurve(
            List<Coordinate> coordinates, double startAngleRadians,
            double endAngleRadians, Coordinate startPoint, Coordinate endPoint,
            double flatness, int limit) {

        /*
         * If the limit allows for more recursion, get the point along the curve
         * at the angle midway between the two specified angles, and determine
         * whether its distance from the line between the two specified points
         * is greater than is allowed; if so, recursively call this method once
         * for the start angle to the middle angle, and once for the middle
         * angle to the end angle.
         */
        boolean noRecursion = true;
        if (limit > 0) {
            LineSegment lineSegment = new LineSegment(startPoint, endPoint);
            double middleAngleRadians = (startAngleRadians + endAngleRadians + (endAngleRadians < startAngleRadians ? 2.0 * Math.PI
                    : 0.0)) / 2.0;
            Coordinate middlePoint = getCircumferentialPoint(middleAngleRadians);
            if (lineSegment.distance(middlePoint) > flatness) {
                addLatLonCoordinatesApproximatingCurve(coordinates,
                        startAngleRadians, middleAngleRadians, startPoint,
                        middlePoint, flatness, limit - 1);
                addLatLonCoordinatesApproximatingCurve(coordinates,
                        middleAngleRadians, endAngleRadians, middlePoint,
                        endPoint, flatness, limit - 1);
                noRecursion = false;
            }
        }

        /*
         * If no recursion is allowed or none was needed in the check above,
         * just add the end point.
         */
        if (noRecursion) {
            coordinates.add(endPoint);
        }
    }
}
