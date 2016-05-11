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
import java.text.SimpleDateFormat;
import java.util.List;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.map.module.SimpleModule;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.raytheon.uf.common.colormap.Color;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Description: Class providing methods allowing the conversion of
 * {@link VisualFeaturesList} instances to JSON and back.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- ------------ --------------------------
 * Feb 18, 2016   15676    Chris.Golden Initial creation.
 * 
 * </pre>
 * 
 * @author Chris.Golden
 * @version 1.0
 */
public class VisualFeaturesListJsonConverter {

    // Public Static Constants

    /**
     * Key for the identifier in a JSON object representing a visual feature.
     */
    public static final String KEY_IDENTIFIER = "identifier";

    /**
     * Key for the templates in a JSON object representing a visual feature.
     */
    public static final String KEY_TEMPLATES = "templates";

    /**
     * Key for the geometry in a JSON object representing a visual feature.
     */
    public static final String KEY_GEOMETRY = "geometry";

    /**
     * Key for the border color in a JSON object representing a visual feature.
     */
    public static final String KEY_BORDER_COLOR = "borderColor";

    /**
     * Key for the fill color in a JSON object representing a visual feature.
     */
    public static final String KEY_FILL_COLOR = "fillColor";

    /**
     * Key for the border thickness in a JSON object representing a visual
     * feature.
     */
    public static final String KEY_BORDER_THICKNESS = "borderThickness";

    /**
     * Key for the border style in a JSON object representing a visual feature.
     */
    public static final String KEY_BORDER_STYLE = "borderStyle";

    /**
     * Key for the diameter in a JSON object representing a visual feature.
     */
    public static final String KEY_DIAMETER = "diameter";

    /**
     * Key for the label in a JSON object representing a visual feature.
     */
    public static final String KEY_LABEL = "label";

    /**
     * Key for the text offset length in a JSON object representing a visual
     * feature.
     */
    public static final String KEY_TEXT_OFFSET_LENGTH = "textOffsetLength";

    /**
     * Key for the text offset direction in a JSON object representing a visual
     * feature.
     */
    public static final String KEY_TEXT_OFFSET_DIR = "textOffsetDirection";

    /**
     * Key for the text size in a JSON object representing a visual feature.
     */
    public static final String KEY_TEXT_SIZE = "textSize";

    /**
     * Key for the text color in a JSON object representing a visual feature.
     */
    public static final String KEY_TEXT_COLOR = "textColor";

    /**
     * Key for the drag capability in a JSON object representing a visual
     * feature.
     */
    public static final String KEY_DRAGGABILITY = "dragCapability";

    /**
     * Key for the rotatable flag in a JSON object representing a visual
     * feature.
     */
    public static final String KEY_ROTATABLE = "rotatable";

    /**
     * Key for the scaleable flag in a JSON object representing a visual
     * feature.
     */
    public static final String KEY_SCALEABLE = "scaleable";

    /**
     * Key for the temporally variant property default value.
     */
    public static final String TEMPORALLY_VARIANT_KEY_DEFAULT = "default";

    /**
     * String that may be used in place of a color, double, or integer value
     * definition to indicate that a border, fill, or text color, border
     * thickness, diameter, text offset length, text offset direction, or text
     * size is to be the same as that of the type of the hazard associated with
     * the visual feature.
     */
    public static final String PROPERTY_VALUE_EVENT_TYPE = "eventType";

    /**
     * Pattern used to format and parse timestamps.
     */
    public static final String TIMESTAMP_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    // Package Static Constants

    /**
     * Type of a boolean.
     */
    static final TypeToken<?> TYPE_BOOLEAN = TypeToken.of(Boolean.class);

    /**
     * Type of an integer.
     */
    static final TypeToken<?> TYPE_INTEGER = TypeToken.of(Integer.class);

    /**
     * Type of a double.
     */
    static final TypeToken<?> TYPE_DOUBLE = TypeToken.of(Double.class);

    /**
     * Type of a string.
     */
    static final TypeToken<?> TYPE_STRING = TypeToken.of(String.class);

    /**
     * Type of a list of strings.
     */
    @SuppressWarnings("serial")
    static final TypeToken<?> TYPE_LIST_OF_STRINGS = new TypeToken<List<String>>() {
    };

    /**
     * Type of a color.
     */
    static final TypeToken<?> TYPE_COLOR = TypeToken.of(Color.class);

    /**
     * Type of a geometry.
     */
    static final TypeToken<?> TYPE_GEOMETRY = TypeToken.of(Geometry.class);

    /**
     * Type of a border style.
     */
    static final TypeToken<?> TYPE_BORDER_STYLE = TypeToken
            .of(BorderStyle.class);

    /**
     * Type of a drag capability.
     */
    static final TypeToken<?> TYPE_DRAGGABILITY = TypeToken
            .of(DragCapability.class);

    /**
     * Immutable map pairing property names with the types of the corresponding
     * values.
     */
    static final ImmutableMap<String, TypeToken<?>> TYPES_FOR_PROPERTIES = ImmutableMap
            .<String, TypeToken<?>> builder()
            .put(KEY_TEMPLATES, TYPE_LIST_OF_STRINGS)
            .put(KEY_GEOMETRY, TYPE_GEOMETRY).put(KEY_BORDER_COLOR, TYPE_COLOR)
            .put(KEY_FILL_COLOR, TYPE_COLOR)
            .put(KEY_BORDER_THICKNESS, TYPE_DOUBLE)
            .put(KEY_BORDER_STYLE, TYPE_BORDER_STYLE)
            .put(KEY_DIAMETER, TYPE_DOUBLE).put(KEY_LABEL, TYPE_STRING)
            .put(KEY_TEXT_OFFSET_LENGTH, TYPE_DOUBLE)
            .put(KEY_TEXT_OFFSET_DIR, TYPE_DOUBLE)
            .put(KEY_TEXT_SIZE, TYPE_INTEGER).put(KEY_TEXT_COLOR, TYPE_COLOR)
            .put(KEY_DRAGGABILITY, TYPE_DRAGGABILITY)
            .put(KEY_ROTATABLE, TYPE_BOOLEAN).put(KEY_SCALEABLE, TYPE_BOOLEAN)
            .build();

    /**
     * Object mapper used to convert visual feature lists to and from JSON.
     * (Note that this class <a href="http://wiki.fasterxml.com/JacksonFAQ">is
     * thread-safe</a> as long as it is configured here and not during use, and
     * thus is safe for multiple threads to use to serialize or deserialize
     * visual features simultaneously.)
     */
    static final ObjectMapper CONVERTER = new ObjectMapper();
    static {

        /*
         * Configure the converter to ignore unknown properties, and to include
         * in serialization all non-null members of an object.
         */
        CONVERTER
                .configure(
                        DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,
                        false);
        CONVERTER.getSerializationConfig().setSerializationInclusion(
                Inclusion.NON_NULL);

        /*
         * Configure the converter to serialize and deserialize objects expected
         * to be of type VisualFeaturesList using custom routines.
         */
        SimpleModule module = new SimpleModule("VisualFeaturesList",
                new Version(1, 0, 0, null));
        module.addSerializer(VisualFeaturesList.class,
                new JsonSerializer<VisualFeaturesList>() {

                    @Override
                    public void serialize(VisualFeaturesList value,
                            JsonGenerator jsonGenerator,
                            SerializerProvider provider) throws IOException,
                            JsonProcessingException {
                        VisualFeaturesListJsonSerializer.serialize(value,
                                jsonGenerator, provider);
                    }
                });
        module.addDeserializer(VisualFeaturesList.class,
                new JsonDeserializer<VisualFeaturesList>() {

                    @Override
                    public VisualFeaturesList deserialize(
                            JsonParser jsonParser,
                            DeserializationContext context) throws IOException,
                            JsonProcessingException {
                        return VisualFeaturesListJsonDeserializer.deserialize(
                                jsonParser, context);
                    }
                });
        CONVERTER.registerModule(module);
    };

    // Package Static Variables

    /**
     * Date formatter used for parsing timestamp strings into dates. It is
     * thread-local because this class's static methods may be called
     * simultaneously by multiple threads, and according to the JDK
     * documentation, <code>SimpleDateFormat</code> is not thread-safe.
     */
    static final ThreadLocal<SimpleDateFormat> timestampFormat = new ThreadLocal<SimpleDateFormat>() {

        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat(
                    VisualFeaturesListJsonConverter.TIMESTAMP_FORMAT_PATTERN);
        }
    };

    // Public Static Methods

    /**
     * Convert the specified JSON string into a visual features list.
     * 
     * @param json
     *            Text providing the JSON.
     * @return Visual features list.
     * @throws IOException
     *             If an error occurs when reading the nodes from the parser.
     * @throws JsonProcessingException
     *             If the JSON is malformed.
     */
    public static VisualFeaturesList fromJson(String json) throws IOException,
            JsonProcessingException {
        return CONVERTER.readValue(json, VisualFeaturesList.class);
    }

    /**
     * Convert the specified visual featuers list into a JSON string.
     * 
     * @param visualFeatures
     *            Visual features list.
     * @return JSON string.
     * @throws IOException
     *             If an error occurs when reading the nodes from the parser.
     * @throws JsonProcessingException
     *             If the JSON is malformed.
     */
    public static String toJson(VisualFeaturesList visualFeatures)
            throws IOException, JsonProcessingException {
        return CONVERTER.writeValueAsString(visualFeatures);
    }
}