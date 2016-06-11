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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.raytheon.uf.common.colormap.Color;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeTypeAdapter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;

/**
 * Description: Visual feature, instances of which provide arbitrary drawable,
 * and optionally editable, elements for a spatial display that vary over time.
 * Each visual feature may reference other visual features by identifier and use
 * the them as templates, providing attribute values that are not explicitly
 * defined within the primary visual feature. Temporal variance means that an
 * instance may only be visible within a particular time range, or may have
 * different visual characteristics or geometries at different times. To get a
 * concrete representation of a visual feature at a particular point in time,
 * the
 * {@link #getStateAtTime(SpatialEntity, IIdentifierGenerator, boolean, Date, Color, double, BorderStyle, double, double, double, int)}
 * method is used to generate a {@link SpatialEntity} if appropriate.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- ------------ --------------------------
 * Feb 12, 2016   15676    Chris.Golden Initial creation.
 * Mar 26, 2016   15676    Chris.Golden Added copy constructor, and a method
 *                                      to set the geometry for whichever time
 *                                      range encompasses a given timestamp.
 * Apr 05, 2016   15676    Chris.Golden Added toString() method for debugging.
 * May 05, 2016   15676    Chris.Golden Added ability to be serialized to
 *                                      support Thrift serialiation and
 *                                      deserialization. This in turn allows
 *                                      two H.S. instances sharing an edex
 *                                      to see each other's stored events.
 * Jun 10, 2016   19537    Chris.Golden Combined base and selected visual feature
 *                                      lists for each hazard event into one,
 *                                      replaced by visibility constraints
 *                                      based upon selection state to individual
 *                                      visual features.
 * </pre>
 * 
 * @author Chris.Golden
 * @version 1.0
 */
@DynamicSerialize
@DynamicSerializeTypeAdapter(factory = VisualFeatureSerializationAdapter.class)
public class VisualFeature implements Serializable {

    // Private Static Constants

    /**
     * Serialization version UID.
     */
    private static final long serialVersionUID = 9083255877893928066L;

    // Package Private Static Classes

    /**
     * Serializable color.
     */
    static class SerializableColor extends Color implements Serializable {

        // Private Static Constants

        /**
         * Serialization version UID.
         */
        private static final long serialVersionUID = 5319722295972192334L;

        // Public Constructors

        /**
         * Construct a default instance.
         */
        public SerializableColor() {
        }

        /**
         * Construct an instance that is opaque.
         * 
         * @param red
         *            Red component.
         * @param green
         *            Green component.
         * @param blue
         *            Blue component.
         */
        public SerializableColor(float red, float green, float blue) {
            super(red, green, blue);
        }

        /**
         * Construct an instance.
         * 
         * @param red
         *            Red component.
         * @param green
         *            Green component.
         * @param blue
         *            Blue component.
         * @param alpha
         *            Alpha comnponent.
         */
        public SerializableColor(float red, float green, float blue, float alpha) {
            super(red, green, blue, alpha);
        }

        /**
         * Construct an instance based upon an existing color.
         * 
         * @param color
         *            Color to be copied.
         */
        public SerializableColor(Color color) {
            super(color.getRed(), color.getGreen(), color.getBlue(), color
                    .getAlpha());
        }

        // Private Methods

        /**
         * Write out the object for serialization purposes. This is required
         * because apparently just subclassing a non-serializable class like
         * {@link Color} with private fields, and not providing custom
         * serialization and deserialization, causes all the fields to be set to
         * <code>0.0</code>.
         * 
         * @param stream
         *            Stream to which to write out the object.
         * @throws IOException
         *             If the object cannot be written out.
         */
        private void writeObject(ObjectOutputStream stream) throws IOException {
            stream.writeFloat(getRed());
            stream.writeFloat(getGreen());
            stream.writeFloat(getBlue());
            stream.writeFloat(getAlpha());
        }

        /**
         * Read in the object for deserialization purposes. This is required
         * because apparently just subclassing a non-serializable class like
         * {@link Color} with private fields, and not providing custom
         * serialization and deserialization, causes all the fields to be set to
         * <code>0.0</code>.
         * 
         * @param stream
         *            Stream from which to read in the object.
         * @throws IOException
         *             If the object cannot be read in.
         * @throws ClassNotFoundException
         *             If the class of a serialized object cannot be found.
         */
        private void readObject(ObjectInputStream stream) throws IOException,
                ClassNotFoundException {
            setRed(stream.readFloat());
            setGreen(stream.readFloat());
            setBlue(stream.readFloat());
            setAlpha(stream.readFloat());
        }

    }

    // Private Classes

    /**
     * Property fetcher, each instance of which is used to fetch particular
     * property's value from a specified visual feature at a particular time.
     */
    private interface IPropertyFetcher<P> {

        /**
         * Get the value of the property for the specified visual feature at the
         * specified time.
         * 
         * @param visualFeature
         *            Visual feature to be queried.
         * @param time
         *            Time for which to query the property value.
         */
        P getPropertyValue(VisualFeature visualFeature, Date time);
    }

    /**
     * Serializable array of bytes.
     */
    private class SerializableBytes implements Serializable {

        // Private Static Constants

        /**
         * Serialization version UID.
         */
        private static final long serialVersionUID = -5954561615947845039L;

        // Private Variables

        /**
         * Array of bytes.
         */
        private byte[] bytes;

        // Public Constructors

        /**
         * Construct an instance.
         */
        public SerializableBytes(byte[] bytes) {
            this.bytes = bytes;
        }

        // Public Methods

        /**
         * Get the underlying byte array.
         * 
         * @return Byte array.
         */
        public byte[] getBytes() {
            return bytes;
        }

        // Private Methods

        /**
         * Write out the object for serialization purposes. The length of the
         * byte array is written, then the array itself if not zero-length.
         * 
         * @param stream
         *            Stream to which to write out the object.
         * @throws IOException
         *             If the object cannot be written out.
         */
        private void writeObject(ObjectOutputStream stream) throws IOException {
            if ((bytes == null) || (bytes.length == 0)) {
                stream.writeInt(0);
            } else {
                stream.writeInt(bytes.length);
                stream.write(bytes);
            }
        }

        /**
         * Read in the object for deserialization purposes. The length of the
         * byte array is read, then the array itself if the length is not zero.
         * 
         * @param stream
         *            Stream from which to read in the object.
         * @throws IOException
         *             If the object cannot be read in.
         * @throws ClassNotFoundException
         *             If the class of a serialized object cannot be found.
         */
        private void readObject(ObjectInputStream stream) throws IOException,
                ClassNotFoundException {

            /*
             * Read in the length needed, and then if it is greater than zero,
             * read in the bytes. Multiple passes may be needed to read in the
             * entire buffer, as the stream's read() methods are not guaranteed
             * to return all the bytes in one pass.
             */
            int length = stream.readInt();
            bytes = new byte[length];
            if (length > 0) {
                for (int count = 0, thisCount = 0; count < length; count += thisCount) {
                    thisCount = stream.read(bytes, count, length - count);
                }
            }
        }
    }

    // Public Static Constants

    /**
     * Special color indicating that the event type's color should be used.
     */
    public static final SerializableColor COLOR_OF_EVENT_TYPE = new SerializableColor(
            -1.0f, -1.0f, -1.0f);

    /**
     * Special double value indicating that a value appropriate to an event type
     * should be used for a property.
     */
    public static final Double DOUBLE_OF_EVENT_TYPE = -1.0;

    /**
     * Special integer value indicating that a value appropriate to an event
     * type should be used for a property.
     */
    public static final Integer INTEGER_OF_EVENT_TYPE = -1;

    /**
     * Default border color.
     */
    public static final SerializableColor DEFAULT_BORDER_COLOR = new SerializableColor(
            1.0f, 1.0f, 1.0f);

    /**
     * Default fill color.
     */
    public static final SerializableColor DEFAULT_FILL_COLOR = new SerializableColor(
            0.0f, 0.0f, 0.0f, 0.0f);

    /**
     * Default border thickness.
     */
    public static final double DEFAULT_BORDER_THICKNESS = 1.0;

    /**
     * Default border style.
     */
    public static final BorderStyle DEFAULT_BORDER_STYLE = BorderStyle.SOLID;

    /**
     * Default diameter.
     */
    public static final double DEFAULT_DIAMETER = 5.0;

    /**
     * Default text offset length.
     */
    public static final double DEFAULT_TEXT_OFFSET_LENGTH = DEFAULT_DIAMETER;

    /**
     * Default text offset direction.
     */
    public static final double DEFAULT_TEXT_OFFSET_DIRECTION = 90.0;

    /**
     * Default text size.
     */
    public static final int DEFAULT_TEXT_SIZE = 10;

    /**
     * Default text color.
     */
    public static final SerializableColor DEFAULT_TEXT_COLOR = new SerializableColor(
            1.0f, 1.0f, 1.0f);

    /**
     * Default drag capability.
     */
    public static final DragCapability DEFAULT_DRAG_CAPABILITY = DragCapability.NONE;

    /**
     * Default rotatable flag.
     */
    public static final boolean DEFAULT_ROTATABLE = false;

    /**
     * Default scaleable flag.
     */
    public static final boolean DEFAULT_SCALEABLE = false;

    // Private Static Constants

    /**
     * Geometry property fetcher.
     */
    private static final IPropertyFetcher<Geometry> GEOMETRY_FETCHER = new IPropertyFetcher<Geometry>() {

        @Override
        public Geometry getPropertyValue(VisualFeature visualFeature, Date time) {
            TemporallyVariantProperty<Geometry> property = visualFeature
                    .getGeometry();
            return (property == null ? null : property.getProperty(time));
        }
    };

    /**
     * Border color fetcher.
     */
    private static final IPropertyFetcher<SerializableColor> BORDER_COLOR_FETCHER = new IPropertyFetcher<SerializableColor>() {

        @Override
        public SerializableColor getPropertyValue(VisualFeature visualFeature,
                Date time) {
            TemporallyVariantProperty<SerializableColor> property = visualFeature
                    .getBorderColor();
            return (property == null ? null : property.getProperty(time));
        }
    };

    /**
     * Fill color fetcher.
     */
    private static final IPropertyFetcher<SerializableColor> FILL_COLOR_FETCHER = new IPropertyFetcher<SerializableColor>() {

        @Override
        public SerializableColor getPropertyValue(VisualFeature visualFeature,
                Date time) {
            TemporallyVariantProperty<SerializableColor> property = visualFeature
                    .getFillColor();
            return (property == null ? null : property.getProperty(time));
        }
    };

    /**
     * Border thickness fetcher.
     */
    private static final IPropertyFetcher<Double> BORDER_THICKNESS_FETCHER = new IPropertyFetcher<Double>() {

        @Override
        public Double getPropertyValue(VisualFeature visualFeature, Date time) {
            TemporallyVariantProperty<Double> property = visualFeature
                    .getBorderThickness();
            return (property == null ? null : property.getProperty(time));
        }
    };

    /**
     * Border style fetcher.
     */
    private static final IPropertyFetcher<BorderStyle> BORDER_STYLE_FETCHER = new IPropertyFetcher<BorderStyle>() {

        @Override
        public BorderStyle getPropertyValue(VisualFeature visualFeature,
                Date time) {
            TemporallyVariantProperty<BorderStyle> property = visualFeature
                    .getBorderStyle();
            return (property == null ? null : property.getProperty(time));
        }
    };

    /**
     * Diameter fetcher.
     */
    private static final IPropertyFetcher<Double> DIAMETER_FETCHER = new IPropertyFetcher<Double>() {

        @Override
        public Double getPropertyValue(VisualFeature visualFeature, Date time) {
            TemporallyVariantProperty<Double> property = visualFeature
                    .getDiameter();
            return (property == null ? null : property.getProperty(time));
        }
    };

    /**
     * Text label fetcher.
     */
    private static final IPropertyFetcher<String> LABEL_FETCHER = new IPropertyFetcher<String>() {

        @Override
        public String getPropertyValue(VisualFeature visualFeature, Date time) {
            TemporallyVariantProperty<String> property = visualFeature
                    .getLabel();
            return (property == null ? null : property.getProperty(time));
        }
    };

    /**
     * Text offset length fetcher.
     */
    private static final IPropertyFetcher<Double> TEXT_OFFSET_LENGTH_FETCHER = new IPropertyFetcher<Double>() {

        @Override
        public Double getPropertyValue(VisualFeature visualFeature, Date time) {
            TemporallyVariantProperty<Double> property = visualFeature
                    .getTextOffsetLength();
            return (property == null ? null : property.getProperty(time));
        }
    };

    /**
     * Text offset direction fetcher.
     */
    private static final IPropertyFetcher<Double> TEXT_OFFSET_DIRECTION_FETCHER = new IPropertyFetcher<Double>() {

        @Override
        public Double getPropertyValue(VisualFeature visualFeature, Date time) {
            TemporallyVariantProperty<Double> property = visualFeature
                    .getTextOffsetDirection();
            return (property == null ? null : property.getProperty(time));
        }
    };

    /**
     * Text size fetcher.
     */
    private static final IPropertyFetcher<Integer> TEXT_SIZE_FETCHER = new IPropertyFetcher<Integer>() {

        @Override
        public Integer getPropertyValue(VisualFeature visualFeature, Date time) {
            TemporallyVariantProperty<Integer> property = visualFeature
                    .getTextSize();
            return (property == null ? null : property.getProperty(time));
        }
    };

    /**
     * Text color fetcher.
     */
    private static final IPropertyFetcher<SerializableColor> TEXT_COLOR_FETCHER = new IPropertyFetcher<SerializableColor>() {

        @Override
        public SerializableColor getPropertyValue(VisualFeature visualFeature,
                Date time) {
            TemporallyVariantProperty<SerializableColor> property = visualFeature
                    .getTextColor();
            return (property == null ? null : property.getProperty(time));
        }
    };

    /**
     * Drag capability fetcher.
     */
    private static final IPropertyFetcher<DragCapability> DRAG_CAPABILITY_FETCHER = new IPropertyFetcher<DragCapability>() {

        @Override
        public DragCapability getPropertyValue(VisualFeature visualFeature,
                Date time) {
            TemporallyVariantProperty<DragCapability> property = visualFeature
                    .getDragCapability();
            return (property == null ? null : property.getProperty(time));
        }
    };

    /**
     * Rotatable flag fetcher.
     */
    private static final IPropertyFetcher<Boolean> ROTATABLE_FETCHER = new IPropertyFetcher<Boolean>() {

        @Override
        public Boolean getPropertyValue(VisualFeature visualFeature, Date time) {
            TemporallyVariantProperty<Boolean> property = visualFeature
                    .getRotatable();
            return (property == null ? null : property.getProperty(time));
        }
    };

    /**
     * Scaleable flag fetcher.
     */
    private static final IPropertyFetcher<Boolean> SCALEABLE_FETCHER = new IPropertyFetcher<Boolean>() {

        @Override
        public Boolean getPropertyValue(VisualFeature visualFeature, Date time) {
            TemporallyVariantProperty<Boolean> property = visualFeature
                    .getScaleable();
            return (property == null ? null : property.getProperty(time));
        }
    };

    // Private Static Variables

    /**
     * Well-Known Binary reader, used for deserializing geometries. It is
     * thread-local because <code>WKBReader</code> is not explicitly declared to
     * be thread-safe. This class's static methods may be called simultaneously
     * by multiple threads, and each thread must be able to deserialize
     * geometries separately in order to avoid cross-thread pollution.
     */
    private static final ThreadLocal<WKBReader> wkbReader = new ThreadLocal<WKBReader>() {

        @Override
        protected WKBReader initialValue() {
            return new WKBReader();
        }
    };

    /**
     * Well-Known Binary writer, used for serializing geometries. It is
     * thread-local because <code>WKBWriter</code> is not explicitly declared to
     * be thread-safe. This class's static methods may be called simultaneously
     * by multiple threads, and each thread must be able to serialize geometries
     * separately in order to avoid cross-thread pollution.
     */
    private static final ThreadLocal<WKBWriter> wkbWriter = new ThreadLocal<WKBWriter>() {

        @Override
        protected WKBWriter initialValue() {
            return new WKBWriter();
        }
    };

    // Private Variables

    /**
     * Identifier of this feature.
     */
    private String identifier;

    /**
     * Visibility constraints.
     */
    private VisibilityConstraints visibilityConstraints;

    /**
     * List of visual features to be treated as templates for this feature, in
     * the order in which they should be checked when querying for property
     * values. This list may be <code>null</code>.
     * <p>
     * <strong>Note</strong>: The visual features listed herein cannot cause
     * potential circular dependencies. Thus, none of them may be a reference to
     * this visual feature, nor may they be references to visual features that
     * via their <code>templates</code> lists, directly or indirectly reference
     * this visual feature. Thus, if feature A's list includes B, and feature
     * B's list includes C, and feature C's list includes A, that would
     * constitute a circular dependency.
     * </p>
     */
    private TemporallyVariantProperty<ImmutableList<VisualFeature>> templates;

    /**
     * Geometry; may be <code>null</code>.
     */
    private TemporallyVariantProperty<Geometry> geometry;

    /**
     * Border color; may be <code>null</code>.
     */
    private TemporallyVariantProperty<SerializableColor> borderColor;

    /**
     * Fill color; may be <code>null</code>.
     */
    private TemporallyVariantProperty<SerializableColor> fillColor;

    /**
     * Border thickness in pixels; may be <code>null</code>.
     */
    private TemporallyVariantProperty<Double> borderThickness;

    /**
     * Border style; may be <code>null</code>.
     */
    private TemporallyVariantProperty<BorderStyle> borderStyle;

    /**
     * Diameter in pixels; may be <code>null</code>.
     */
    private TemporallyVariantProperty<Double> diameter;

    /**
     * Text label; may be <code>null</code>.
     */
    private TemporallyVariantProperty<String> label;

    /**
     * Text offset length in pixels; may be <code>null</code>.
     */
    private TemporallyVariantProperty<Double> textOffsetLength;

    /**
     * Text offset direction in degrees (with 0 being to the right, 90 above,
     * and so on); may be <code>null</code>.
     */
    private TemporallyVariantProperty<Double> textOffsetDirection;

    /**
     * Text size in points; may be <code>null</code>.
     */
    private TemporallyVariantProperty<Integer> textSize;

    /**
     * Text color; may be <code>null</code>.
     */
    private TemporallyVariantProperty<SerializableColor> textColor;

    /**
     * Drag capability; may be <code>null</code>.
     */
    private TemporallyVariantProperty<DragCapability> dragCapability;

    /**
     * Flag indicating whether or not the feature is rotatable; may be
     * <code>null</code>.
     */
    private TemporallyVariantProperty<Boolean> rotatable;

    /**
     * Flag indicating whether or not the feature is scaleable; may be
     * <code>null</code>.
     */
    private TemporallyVariantProperty<Boolean> scaleable;

    // Public Constructors

    /**
     * Construct a standard instance that is as copy of the specified visual
     * feature.
     * 
     * @param original
     *            Visual feature to be copied.
     */
    public VisualFeature(VisualFeature original) {

        /*
         * All immutable properties of the original feature are simply
         * referenced by this new object, since their immutability guarantees
         * that sharing the objects will not cause problems.
         */
        this.identifier = original.identifier;
        this.visibilityConstraints = original.visibilityConstraints;
        this.templates = original.templates;
        this.borderColor = original.borderColor;
        this.fillColor = original.fillColor;
        this.borderThickness = original.borderThickness;
        this.borderStyle = original.borderStyle;
        this.diameter = original.diameter;
        this.label = original.label;
        this.textOffsetLength = original.textOffsetLength;
        this.textOffsetDirection = original.textOffsetDirection;
        this.textSize = original.textSize;
        this.textColor = original.textColor;
        this.dragCapability = original.dragCapability;
        this.rotatable = original.rotatable;
        this.scaleable = original.scaleable;

        /*
         * Mutable properties must be copied down to the point where immutable
         * components are encountered. Geometries for all practical purposes
         * immutable.
         */
        this.geometry = new TemporallyVariantProperty<>(
                original.geometry.getDefaultProperty());
        for (Map.Entry<Range<Date>, Geometry> entry : original.geometry
                .getPropertiesForTimeRanges().entrySet()) {
            this.geometry.addPropertyForTimeRange(entry.getKey(),
                    entry.getValue());
        }
    }

    // Package Constructors

    /**
     * Construct a standard instance.
     * 
     * @param identifier
     *            Identifier of this instance.
     */
    VisualFeature(String identifier) {
        this.identifier = identifier;
    }

    // Public Methods

    @Override
    public boolean equals(Object other) {
        if (other instanceof VisualFeature == false) {
            return false;
        }
        VisualFeature otherFeature = (VisualFeature) other;
        return (compare(identifier, otherFeature.identifier)
                && (visibilityConstraints == otherFeature.visibilityConstraints)
                && compare(templates, otherFeature.templates)
                && compare(geometry, otherFeature.geometry)
                && compare(borderColor, otherFeature.borderColor)
                && compare(fillColor, otherFeature.fillColor)
                && compare(borderThickness, otherFeature.borderThickness)
                && compare(borderStyle, otherFeature.borderStyle)
                && compare(diameter, otherFeature.diameter)
                && compare(label, otherFeature.label)
                && compare(textOffsetLength, otherFeature.textOffsetLength)
                && compare(textOffsetDirection,
                        otherFeature.textOffsetDirection)
                && compare(textSize, otherFeature.textSize)
                && compare(textColor, otherFeature.textColor)
                && compare(dragCapability, otherFeature.dragCapability)
                && compare(rotatable, otherFeature.rotatable) && compare(
                    scaleable, otherFeature.scaleable));
    }

    @Override
    public int hashCode() {
        return (int) ((getHashCode(identifier)
                + getHashCode(visibilityConstraints) + getHashCode(templates)
                + getHashCode(geometry) + getHashCode(borderColor)
                + getHashCode(fillColor) + getHashCode(borderThickness)
                + getHashCode(borderStyle) + getHashCode(diameter)
                + getHashCode(label) + getHashCode(textOffsetLength)
                + getHashCode(textOffsetDirection) + getHashCode(textSize)
                + getHashCode(textColor) + getHashCode(dragCapability)
                + getHashCode(rotatable) + getHashCode(scaleable)) % Integer.MAX_VALUE);
    }

    /**
     * Get the identifier.
     * 
     * @return Identifier.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Get the visibility constraints.
     * 
     * @return Visibility constraints.
     */
    public VisibilityConstraints getVisibilityConstraints() {
        return visibilityConstraints;
    }

    /**
     * Get the geometry for the specified time.
     * 
     * @param time
     *            Time for which to check.
     * @return Geometry that applies for the specified time, or
     *         <code>null</code> if none applies.
     */
    public Geometry getGeometry(Date time) {
        return getValue(GEOMETRY_FETCHER, time, null);
    }

    /**
     * Get the border color for the specified time.
     * 
     * @param time
     *            Time for which to check.
     * @return Border color that applies for the specified time; may be
     *         {@link #COLOR_OF_EVENT_TYPE}, indicating that the border should
     *         be the color of the event type.
     */
    public Color getBorderColor(Date time) {
        return getValue(BORDER_COLOR_FETCHER, time, DEFAULT_BORDER_COLOR);
    }

    /**
     * Get the fill color for the specified time.
     * 
     * @param time
     *            Time for which to check.
     * @return Fill color that applies for the specified time; may be
     *         {@link #COLOR_OF_EVENT_TYPE}, indicating that the fill should be
     *         the color of the event type.
     */
    public Color getFillColor(Date time) {
        return getValue(FILL_COLOR_FETCHER, time, DEFAULT_FILL_COLOR);
    }

    /**
     * Get the border thickness in pixels for the specified time.
     * 
     * @param time
     *            Time for which to check.
     * @return Border thickness that applies for the specified time; may be
     *         {@link #DOUBLE_OF_EVENT_TYPE}, indicating that the event type's
     *         border thickness should be used.
     */
    public double getBorderThickness(Date time) {
        return getValue(BORDER_THICKNESS_FETCHER, time,
                DEFAULT_BORDER_THICKNESS);
    }

    /**
     * Get the border style for the specified time.
     * 
     * @param time
     *            Time for which to check.
     * @return Border style that applies for the specified time.
     */
    public BorderStyle getBorderStyle(Date time) {
        return getValue(BORDER_STYLE_FETCHER, time, DEFAULT_BORDER_STYLE);
    }

    /**
     * Get the diameter in pixels for the specified time.
     * 
     * @param time
     *            Time for which to check.
     * @return Diameter that applies for the specified time; may be
     *         {@link #DOUBLE_OF_EVENT_TYPE}, indicating that the event type's
     *         point diameter should be used.
     */
    public double getDiameter(Date time) {
        return getValue(DIAMETER_FETCHER, time, DEFAULT_DIAMETER);
    }

    /**
     * Get the text label for the specified time.
     * 
     * @param time
     *            Time for which to check.
     * @return Text label that applies for the specified time, if any.
     */
    public String getLabel(Date time) {
        return getValue(LABEL_FETCHER, time, null);
    }

    /**
     * Get the text offset length in pixels for the specified time.
     * 
     * @param time
     *            Time for which to check.
     * @return Text offset length that applies for the specified time; may be
     *         {@link #DOUBLE_OF_EVENT_TYPE}, indicating that the event type's
     *         text offset length be used.
     */
    public double getTextOffsetLength(Date time) {
        return getValue(TEXT_OFFSET_LENGTH_FETCHER, time,
                DEFAULT_TEXT_OFFSET_LENGTH);
    }

    /**
     * Get the text offset direction in degrees (with 0 being to the right, 90
     * above, and so on) for the specified time.
     * 
     * @param time
     *            Time for which to check.
     * @return Text offset direction that applies for the specified time; may be
     *         {@link #DOUBLE_OF_EVENT_TYPE}, indicating that the event type's
     *         text offset direction should be used.
     */
    public double getTextOffsetDirection(Date time) {
        return getValue(TEXT_OFFSET_DIRECTION_FETCHER, time,
                DEFAULT_TEXT_OFFSET_DIRECTION);
    }

    /**
     * Get the text size in pixels for the specified time.
     * 
     * @param time
     *            Time for which to check.
     * @return Text size that applies for the specified time; may be
     *         {@link #INTEGER_OF_EVENT_TYPE}, indicating that the event type's
     *         text size should be used.
     */
    public int getTextSize(Date time) {
        return getValue(TEXT_SIZE_FETCHER, time, DEFAULT_TEXT_SIZE);
    }

    /**
     * Get the text color for the specified time.
     * 
     * @param time
     *            Time for which to check.
     * @return Text color that applies for the specified time; may be
     *         {@link #COLOR_OF_EVENT_TYPE}, indicating that the text should be
     *         the color of the event type.
     */
    public Color getTextColor(Date time) {
        return getValue(TEXT_COLOR_FETCHER, time, DEFAULT_TEXT_COLOR);
    }

    /**
     * Get the drag capability for the specified time.
     * 
     * @param time
     *            Time for which to check.
     * @return Drag capability that applies for the specified time.
     */
    public DragCapability getDragCapability(Date time) {
        return getValue(DRAG_CAPABILITY_FETCHER, time, DEFAULT_DRAG_CAPABILITY);
    }

    /**
     * Determine whether the feature is rotatable for the specified time.
     * 
     * @param time
     *            Time for which to check.
     * @return True if the feature is rotatable at the specified time, otherwise
     *         false.
     */
    public boolean isRotatable(Date time) {
        return getValue(ROTATABLE_FETCHER, time, DEFAULT_ROTATABLE);
    }

    /**
     * Determine whether the feature is scaleable for the specified time.
     * 
     * @param time
     *            Time for which to check.
     * @return True if the feature is scaleable at the specified time, otherwise
     *         false.
     */
    public boolean isScaleable(Date time) {
        return getValue(SCALEABLE_FETCHER, time, DEFAULT_SCALEABLE);
    }

    /**
     * Get the state of this object as a spatial entity for the specified time.
     * 
     * @param spatialEntity
     *            Spatial entity to be updated to reflect this object's state at
     *            the given time, or <code>null</code> if a spatial entity is to
     *            be created if necessary by this invocation.
     * @param identifierGenerator
     *            Identifier generator, used if <code>spatialEntity</code> is
     *            specified as <code>null</code> to generate the identifier for
     *            the newly-created spatial entity.
     * @param selected
     *            Flag indicating whether or not the item represented by this
     *            object is currently selected. Depending upon the
     * @param time
     *            Time for which to get the state of this object.
     * @param hazardColor
     *            Color of the hazard type associated with this visual feature;
     *            this is used if any of the visual feature's colors are found
     *            to be {@link #COLOR_OF_EVENT_TYPE}.
     * @param hazardBorderThickness
     *            Border thickness of the hazard type associated with this
     *            visual feature; this is used if the border thickness is found
     *            to be {@link #DOUBLE_OF_EVENT_TYPE}.
     * @param hazardBorderStyle
     *            Border style of the hazard type associated with this visual
     *            feature; this is used if the border style is found to be
     *            {@link BorderStyle#EVENT_TYPE}.
     * @param hazardPointDiameter
     *            Point diameter of the hazard type associated with this visual
     *            feature; this is used if the diameter is found to be
     *            {@link #DOUBLE_OF_EVENT_TYPE}.
     * @param hazardTextOffsetLength
     *            Text offset length of the hazard type associated with this
     *            visual feature; this is used if the text offset length is
     *            found to be {@link #DOUBLE_OF_EVENT_TYPE}.
     * @param hazardTextOffsetDirection
     *            Text offset direction of the hazard type associated with this
     *            visual feature; this is used if the text offset direction is
     *            found to be {@link #DOUBLE_OF_EVENT_TYPE}.
     * @param hazardTextSize
     *            Text size of the hazard type associated with this visual
     *            feature; this is used if the text size is found to be
     *            {@link #INTEGER_OF_EVENT_TYPE}.
     * @return Spatial entity representing the state of this object at the
     *         specified time, or <code>null</code> if this object is not
     *         visible at that time. If a visual entity is returned, the flag
     *         returned by its {@link SpatialEntity#checkAndResetModified()}
     *         will indicate whether the entity was updated in the course of the
     *         execution of this method; if it was created and/or modified by
     *         this method, the flag will be true.
     */
    public <I> SpatialEntity<I> getStateAtTime(SpatialEntity<I> spatialEntity,
            IIdentifierGenerator<I> identifierGenerator, boolean selected,
            Date time, Color hazardColor, double hazardBorderThickness,
            BorderStyle hazardBorderStyle, double hazardPointDiameter,
            double hazardTextOffsetLength, double hazardTextOffsetDirection,
            int hazardTextSize) {
        if ((visibilityConstraints == VisibilityConstraints.NEVER)
                || ((visibilityConstraints == VisibilityConstraints.UNSELECTED) && selected)
                || ((visibilityConstraints == VisibilityConstraints.SELECTED) && (selected == false))) {
            return null;
        }
        Geometry geometry = getGeometry(time);
        if (geometry == null) {
            return null;
        }
        if (spatialEntity == null) {
            spatialEntity = new SpatialEntity<>(
                    identifierGenerator.generate(identifier));
        }
        spatialEntity.setGeometry(geometry);
        spatialEntity
                .setBorderColor(getColor(getBorderColor(time), hazardColor));
        spatialEntity.setFillColor(getColor(getFillColor(time), hazardColor));
        spatialEntity.setBorderThickness(getDouble(getBorderThickness(time),
                hazardBorderThickness));
        spatialEntity.setBorderStyle(getBorderStyle(getBorderStyle(time),
                hazardBorderStyle));
        spatialEntity.setDiameter(getDouble(getDiameter(time),
                hazardPointDiameter));
        spatialEntity.setLabel(getLabel(time));
        spatialEntity.setTextOffsetLength(getDouble(getTextOffsetLength(time),
                hazardTextOffsetLength));
        spatialEntity.setTextOffsetDirection(getDouble(
                getTextOffsetDirection(time), hazardTextOffsetDirection));
        spatialEntity
                .setTextSize(getInteger(getTextSize(time), hazardTextSize));
        spatialEntity.setTextColor(getColor(getTextColor(time), hazardColor));
        spatialEntity.setDragCapability(getDragCapability(time));
        spatialEntity.setRotatable(isRotatable(time));
        spatialEntity.setScaleable(isScaleable(time));
        return spatialEntity;
    }

    /**
     * Set the geometry for the specified time. Note that this method only
     * succeeds in setting the geometry if this object has a geometry for a time
     * range encompassing the given time; if it does not, but a template from
     * which this object inherits geometry does, this method will fail.
     * 
     * @param time
     *            Time for which to set the geometry of this object.
     * @param geometry
     *            New geometry to be used.
     * @return True if the geometry was set successfully, false otherwise. The
     *         latter will occur if <code>time</code> does not fall within a
     *         time range for which a geometry has already been defined.
     */
    public boolean setGeometry(Date time, Geometry geometry) {
        return this.geometry.addPropertyForTimeRangeEncompassingTime(time,
                geometry);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(identifier);
        stringBuilder.append(" [selection-based visibility: ");
        stringBuilder.append(visibilityConstraints.getDescription());
        stringBuilder.append("] (");
        boolean never = false;
        if (geometry != null) {
            if (geometry.getDefaultProperty() != null) {
                stringBuilder.append("always visible");
            } else {
                Map<Range<Date>, Geometry> geometriesForTimeRanges = geometry
                        .getPropertiesForTimeRanges();
                if (geometriesForTimeRanges.isEmpty()) {
                    never = true;
                } else {
                    stringBuilder.append("visible ");
                    boolean first = true;
                    for (Range<Date> range : geometriesForTimeRanges.keySet()) {
                        if (first) {
                            first = false;
                        } else {
                            stringBuilder.append(", ");
                        }
                        stringBuilder.append(range.lowerEndpoint() + " to "
                                + range.upperEndpoint());
                    }
                }
            }
        } else {
            never = true;
        }
        if (never) {
            stringBuilder.append("never visible");
        }
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    // Package Methods

    /**
     * Get the list of visual features to be treated as templates for this
     * feature, in the order in which they are to be checked when querying for
     * property values.
     * 
     * @return List of visual features; may be <code>null</code>.
     */
    TemporallyVariantProperty<ImmutableList<VisualFeature>> getTemplates() {
        return templates;
    }

    /**
     * Get the geometry.
     * 
     * @return Geometry; may be <code>null</code>.
     */
    TemporallyVariantProperty<Geometry> getGeometry() {
        return geometry;
    }

    /**
     * Get the border color.
     * 
     * @return Border color; may be <code>null</code>.
     */
    TemporallyVariantProperty<SerializableColor> getBorderColor() {
        return borderColor;
    }

    /**
     * Get the fill color.
     * 
     * @return Fill color; may be <code>null</code>.
     */
    TemporallyVariantProperty<SerializableColor> getFillColor() {
        return fillColor;
    }

    /**
     * Get the border thickness in pixels.
     * 
     * @return Border thickness; may be <code>null</code>.
     */
    TemporallyVariantProperty<Double> getBorderThickness() {
        return borderThickness;
    }

    /**
     * Get the border style.
     * 
     * @return Border style;; may be <code>null</code>.
     */
    TemporallyVariantProperty<BorderStyle> getBorderStyle() {
        return borderStyle;
    }

    /**
     * Get the diameter in pixels.
     * 
     * @return Diameter; may be <code>null</code>.
     */
    TemporallyVariantProperty<Double> getDiameter() {
        return diameter;
    }

    /**
     * Get the text label.
     * 
     * @return Text label; may be <code>null</code>.
     */
    TemporallyVariantProperty<String> getLabel() {
        return label;
    }

    /**
     * Get the text offset length in pixels.
     * 
     * @return Text offset length; may be <code>null</code>.
     */
    TemporallyVariantProperty<Double> getTextOffsetLength() {
        return textOffsetLength;
    }

    /**
     * Get the text offset direction in degrees (with 0 being to the right, 90
     * above, and so on).
     * 
     * @return Text offset direction; may be <code>null</code>.
     */
    TemporallyVariantProperty<Double> getTextOffsetDirection() {
        return textOffsetDirection;
    }

    /**
     * Get the text size in points.
     * 
     * @return Text size; may be <code>null</code>.
     */
    TemporallyVariantProperty<Integer> getTextSize() {
        return textSize;
    }

    /**
     * Get the text color.
     * 
     * @return Text color; may be <code>null</code>.
     */
    TemporallyVariantProperty<SerializableColor> getTextColor() {
        return textColor;
    }

    /**
     * Get the drag capability.
     * 
     * @return Drag capability; may be <code>null</code>.
     */
    TemporallyVariantProperty<DragCapability> getDragCapability() {
        return dragCapability;
    }

    /**
     * Get the flag indicating whether or not the feature is rotatable.
     * 
     * @return Flag indicating whether or not the feature is rotatable; may be
     *         <code>null</code>.
     */
    TemporallyVariantProperty<Boolean> getRotatable() {
        return rotatable;
    }

    /**
     * Get the flag indicating whether or not the feature is scaleable.
     * 
     * @return Flag indicating whether or not the feature is scaleable; may be
     *         <code>null</code>.
     */
    TemporallyVariantProperty<Boolean> getScaleable() {
        return scaleable;
    }

    /**
     * Set the visibility constraints for this feature.
     */
    void setVisibilityConstraints(VisibilityConstraints visibilityConstraints) {
        this.visibilityConstraints = visibilityConstraints;
    }

    /**
     * Set the list of visual features to be treated as templates for this
     * feature, in the order in which they are to be checked first when querying
     * for property values.
     * 
     * @param templates
     *            New value; may be <code>null</code>.
     */
    void setTemplates(
            TemporallyVariantProperty<ImmutableList<VisualFeature>> templates) {
        this.templates = templates;
    }

    /**
     * Set the geometry.
     * 
     * @param geometry
     *            New value; may be <code>null</code>.
     */
    void setGeometry(TemporallyVariantProperty<Geometry> geometry) {
        this.geometry = geometry;
    }

    /**
     * Set the border color.
     * 
     * @param borderColor
     *            New value; may be <code>null</code>.
     */
    void setBorderColor(TemporallyVariantProperty<SerializableColor> borderColor) {
        this.borderColor = borderColor;
    }

    /**
     * Set the fill color.
     * 
     * @param fillColor
     *            New value; may be <code>null</code>.
     */
    void setFillColor(TemporallyVariantProperty<SerializableColor> fillColor) {
        this.fillColor = fillColor;
    }

    /**
     * Set the border thickness in pixels.
     * 
     * @param borderThickness
     *            New value; may be <code>null</code>.
     */
    void setBorderThickness(TemporallyVariantProperty<Double> borderThickness) {
        this.borderThickness = borderThickness;
    }

    /**
     * Set the border style.
     * 
     * @param borderStyle
     *            New value; may be <code>null</code>.
     */
    void setBorderStyle(TemporallyVariantProperty<BorderStyle> borderStyle) {
        this.borderStyle = borderStyle;
    }

    /**
     * Set the diameter in pixels.
     * 
     * @param diameter
     *            New value; may be <code>null</code>.
     */
    void setDiameter(TemporallyVariantProperty<Double> diameter) {
        this.diameter = diameter;
    }

    /**
     * Set the text label.
     * 
     * @param label
     *            New value; may be <code>null</code>.
     */
    void setLabel(TemporallyVariantProperty<String> label) {
        this.label = label;
    }

    /**
     * Set the text offset length in pixels.
     * 
     * @param textOffsetLength
     *            New value; may be <code>null</code>.
     */
    void setTextOffsetLength(TemporallyVariantProperty<Double> textOffsetLength) {
        this.textOffsetLength = textOffsetLength;
    }

    /**
     * Set the text offset direction in degrees (with 0 being to the right, 90
     * above, and so on).
     * 
     * @param textOffsetDirection
     *            New value; may be <code>null</code>.
     */
    void setTextOffsetDirection(
            TemporallyVariantProperty<Double> textOffsetDirection) {
        this.textOffsetDirection = textOffsetDirection;
    }

    /**
     * Set the text size in points.
     * 
     * @param textSize
     *            New value; may be <code>null</code>.
     */
    void setTextSize(TemporallyVariantProperty<Integer> textSize) {
        this.textSize = textSize;
    }

    /**
     * Set the text color.
     * 
     * @param textColor
     *            New value; may be <code>null</code>.
     */
    void setTextColor(TemporallyVariantProperty<SerializableColor> textColor) {
        this.textColor = textColor;
    }

    /**
     * Set the drag capability.
     * 
     * @param dragCapability
     *            New value; may be <code>null</code>.
     */
    void setDragCapability(
            TemporallyVariantProperty<DragCapability> dragCapability) {
        this.dragCapability = dragCapability;
    }

    /**
     * Set the flag indicating whether or not the feature is rotatable.
     * 
     * @param rotatable
     *            New value; may be <code>null</code>.
     */
    void setRotatable(TemporallyVariantProperty<Boolean> rotatable) {
        this.rotatable = rotatable;
    }

    /**
     * Set the flag indicating whether or not the feature is scaleable.
     * 
     * @param scaleable
     *            New value; may be <code>null</code>.
     */
    void setScaleable(TemporallyVariantProperty<Boolean> scaleable) {
        this.scaleable = scaleable;
    }

    // Private Methods

    /**
     * Write out the object for serialization purposes. This is required because
     * the {@link Geometry} objects found within {@link #geometry} cannot easily
     * be deserialized (sometimes resulting in
     * <code>ClassNotFoundException</code> being thrown).
     * 
     * @param stream
     *            Stream to which to write out the object.
     * @throws IOException
     *             If the object cannot be written out.
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {

        /*
         * Perform standard serialization for most of the components of this
         * object.
         */
        stream.writeObject(identifier);
        stream.writeObject(visibilityConstraints);
        stream.writeObject(templates);
        stream.writeObject(borderColor);
        stream.writeObject(fillColor);
        stream.writeObject(borderThickness);
        stream.writeObject(borderStyle);
        stream.writeObject(diameter);
        stream.writeObject(label);
        stream.writeObject(textOffsetLength);
        stream.writeObject(textOffsetDirection);
        stream.writeObject(textSize);
        stream.writeObject(textColor);
        stream.writeObject(dragCapability);
        stream.writeObject(rotatable);
        stream.writeObject(scaleable);

        /*
         * Convert all Geometry objects to arrays of bytes in Well-Known Binary
         * format, then write out the resulting container object.
         */
        if (geometry == null) {
            stream.writeObject(null);
        } else {
            WKBWriter writer = wkbWriter.get();
            Geometry defaultGeom = geometry.getDefaultProperty();
            SerializableBytes bytes = null;
            if (defaultGeom != null) {
                bytes = new SerializableBytes(writer.write(defaultGeom));
            }
            TemporallyVariantProperty<SerializableBytes> semiSerializedGeometry = new TemporallyVariantProperty<>(
                    bytes);
            for (Map.Entry<Range<Date>, Geometry> entry : geometry
                    .getPropertiesForTimeRanges().entrySet()) {
                semiSerializedGeometry.addPropertyForTimeRange(entry.getKey(),
                        new SerializableBytes(writer.write(entry.getValue())));
            }
            stream.writeObject(semiSerializedGeometry);
        }
    }

    /**
     * Read in the object for deserialization purposes. This is required because
     * the {@link Geometry} objects found within {@link #geometry} cannot easily
     * be deserialized (sometimes resulting in
     * <code>ClassNotFoundException</code> being thrown).
     * 
     * @param stream
     *            Stream from which to read in the object.
     * @throws IOException
     *             If the object cannot be read in.
     * @throws ClassNotFoundException
     *             If the class of a serialized object cannot be found.
     */
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream stream) throws IOException,
            ClassNotFoundException {

        /*
         * Perform standard deserialization for most of the components of this
         * object.
         */
        identifier = (String) stream.readObject();
        visibilityConstraints = (VisibilityConstraints) stream.readObject();
        templates = (TemporallyVariantProperty<ImmutableList<VisualFeature>>) stream
                .readObject();
        borderColor = (TemporallyVariantProperty<SerializableColor>) stream
                .readObject();
        fillColor = (TemporallyVariantProperty<SerializableColor>) stream
                .readObject();
        borderThickness = (TemporallyVariantProperty<Double>) stream
                .readObject();
        borderStyle = (TemporallyVariantProperty<BorderStyle>) stream
                .readObject();
        diameter = (TemporallyVariantProperty<Double>) stream.readObject();
        label = (TemporallyVariantProperty<String>) stream.readObject();
        textOffsetLength = (TemporallyVariantProperty<Double>) stream
                .readObject();
        textOffsetDirection = (TemporallyVariantProperty<Double>) stream
                .readObject();
        textSize = (TemporallyVariantProperty<Integer>) stream.readObject();
        textColor = (TemporallyVariantProperty<SerializableColor>) stream
                .readObject();
        dragCapability = (TemporallyVariantProperty<DragCapability>) stream
                .readObject();
        rotatable = (TemporallyVariantProperty<Boolean>) stream.readObject();
        scaleable = (TemporallyVariantProperty<Boolean>) stream.readObject();

        /*
         * Read in the container object holding the geometries in Well-Known
         * Binary format, then convert the latter to Geometry objects in a new
         * container object.
         */
        TemporallyVariantProperty<SerializableBytes> semiSerializedGeometry = (TemporallyVariantProperty<SerializableBytes>) stream
                .readObject();
        if (semiSerializedGeometry == null) {
            geometry = null;
        } else {
            try {
                WKBReader reader = wkbReader.get();
                SerializableBytes bytes = semiSerializedGeometry
                        .getDefaultProperty();
                Geometry defaultGeom = null;
                if (bytes != null) {
                    defaultGeom = reader.read(bytes.getBytes());
                }
                geometry = new TemporallyVariantProperty<>(defaultGeom);
                for (Map.Entry<Range<Date>, SerializableBytes> entry : semiSerializedGeometry
                        .getPropertiesForTimeRanges().entrySet()) {
                    geometry.addPropertyForTimeRange(entry.getKey(),
                            reader.read(entry.getValue().getBytes()));
                }
            } catch (ParseException e) {
                throw new IOException("could not read in geometry", e);
            }
        }
    }

    /**
     * Compare the specified objects to see if they are equivalent, or are both
     * <code>null</code>.
     * 
     * @param object1
     *            First object to be compared; may be <code>null</code>.
     * @param object2
     *            Second object to be compared; may be <code>null</code>.
     * @return True if the two objects are equivalent or are both
     *         <code>null</code>, false otherwise.
     */
    private boolean compare(Object object1, Object object2) {
        return (object1 == null ? object2 == null : object1.equals(object2));
    }

    /**
     * Get the hash code of the specified object.
     * 
     * @param object
     *            Object for which the hash code is to be generated, or
     *            <code>null</code>.
     * @return Hash code, or 0 if the object is <code>null</code>.
     */
    private long getHashCode(Object object) {
        return (object == null ? 0L : object.hashCode());
    }

    /**
     * Get a property value using the specified fetcher, checking first this
     * instance, then any templates. If the value is not found, use the
     * specified default.
     * 
     * @param propertyFetcher
     *            Property fetcher to be used.
     * @param time
     *            Time for which to get the property.
     * @param defaultValue
     *            Default value.
     */
    private <P> P getValue(IPropertyFetcher<P> propertyFetcher, Date time,
            P defaultProperty) {

        /*
         * If the property is provided by this visual feature, use it.
         */
        P property = propertyFetcher.getPropertyValue(this, time);
        if (property != null) {
            return property;
        }

        /*
         * If no value was found for the property within this visual feature,
         * iterate through any templates provided, querying each in turn for the
         * property value, and using the first one that provides it.
         */
        if (templates != null) {
            List<VisualFeature> visualFeatures = templates.getProperty(time);
            if (visualFeatures != null) {
                for (VisualFeature visualFeature : visualFeatures) {
                    property = propertyFetcher.getPropertyValue(visualFeature,
                            time);
                    if (property != null) {
                        return property;
                    }
                }
            }
        }

        /*
         * If no property value was found anywhere, use the default value.
         */
        return defaultProperty;
    }

    /**
     * Get the specified integer value, unless said value is equal to
     * {@link #INTEGER_OF_EVENT_TYPE}, in which case get the specified hazard
     * type integer value.
     * 
     * @param value
     *            Value to be used, unless it is {@link #INTEGER_OF_EVENT_TYPE}.
     * @param hazardTypeValue
     *            Value to be used if <code>value</code> is
     *            {@link #INTEGER_OF_EVENT_TYPE}.
     * @return Value.
     */
    private int getInteger(Integer value, int hazardTypeValue) {
        return (value.equals(INTEGER_OF_EVENT_TYPE) ? hazardTypeValue : value);
    }

    /**
     * Get the specified double value, unless said value is equal to
     * {@link #DOUBLE_OF_EVENT_TYPE}, in which case get the specified hazard
     * type double value.
     * 
     * @param value
     *            Value to be used, unless it is {@link #DOUBLE_OF_EVENT_TYPE}.
     * @param hazardTypeValue
     *            Value to be used if <code>value</code> is
     *            {@link #DOUBLE_OF_EVENT_TYPE}.
     * @return Value.
     */
    private double getDouble(Double value, double hazardTypeValue) {
        return (value.equals(DOUBLE_OF_EVENT_TYPE) ? hazardTypeValue : value);
    }

    /**
     * Get the specified border style, unless said style is equal to
     * {@link BorderStyle#EVENT_TYPE}, in which case get the specified hazard
     * type border style.
     * 
     * @param value
     *            Border style to be used, unless it is
     *            {@link BorderStyle#EVENT_TYPE}.
     * @param hazardTypeValue
     *            Border style to be used if <code>value</code> is
     *            {@link BorderStyle#EVENT_TYPE}.
     * @return Border style.
     */
    private BorderStyle getBorderStyle(BorderStyle value,
            BorderStyle hazardTypeValue) {
        return (value == BorderStyle.EVENT_TYPE ? hazardTypeValue : value);
    }

    /**
     * Get the specified color, unless said color is equal to
     * {@link #COLOR_OF_EVENT_TYPE}, in which case get the specified hazard type
     * color.
     * 
     * @param color
     *            Color to be used, unless it is {@link #COLOR_OF_EVENT_TYPE}.
     * @param hazardTypeColor
     *            Color to be used if <code>color</code> is
     *            {@link #COLOR_OF_EVENT_TYPE}.
     * @return Color.
     */
    private Color getColor(Color color, Color hazardTypeColor) {
        return (color.equals(COLOR_OF_EVENT_TYPE) ? hazardTypeColor : color);
    }
}