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
package gov.noaa.gsd.viz.hazards.display.deprecated;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Reverse engineered to represent product generation result as JSON.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 28, 2013 1257       bsteffen    Initial creation
 * Sep 19, 2013 2046       mnash       Update for product generation.
 * Nov 15, 2013  2182       daniel.s.schaffer@noaa.gov    Refactoring JSON - ProductStagingDialog
 * Feb 18, 2014 2702       jsanchez    Used Serializable for products.
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
@Deprecated
public class ProductGenerationResult {

    private String returnType;

    private GeneratedProduct[] generatedProducts;

    private HazardEventSet[] hazardEventSets;

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public GeneratedProduct[] getGeneratedProducts() {
        return generatedProducts;
    }

    public void setGeneratedProducts(GeneratedProduct[] generatedProducts) {
        this.generatedProducts = generatedProducts;
    }

    public HazardEventSet[] getHazardEventSets() {
        return hazardEventSets;
    }

    public void setHazardEventSets(HazardEventSet[] hazardEventSets) {
        this.hazardEventSets = hazardEventSets;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    /**
     * Json representation of a product
     */
    @Deprecated
    public static class GeneratedProduct {

        private Map<String, List<Serializable>> products = new HashMap<String, List<Serializable>>();

        private String productID;

        private String productGeneratorName;

        private LinkedHashMap<String, Serializable> data;

        private Map<String, List<LinkedHashMap<String, Serializable>>> editableEntries = new LinkedHashMap<String, List<LinkedHashMap<String, Serializable>>>();

        public Map<String, List<Serializable>> getProducts() {
            return products;
        }

        public void setProducts(Map<String, List<Serializable>> products) {
            this.products = products;
        }

        public void addProduct(String format, List<Serializable> product) {
            this.products.put(format, product);
        }

        public String getProductID() {
            return productID;
        }

        public void setProductID(String productID) {
            this.productID = productID;
        }

        public Map<String, List<LinkedHashMap<String, Serializable>>> getEditableEntries() {
            return editableEntries;
        }

        public void setEditableEntries(
                Map<String, List<LinkedHashMap<String, Serializable>>> editableEntries) {
            this.editableEntries = editableEntries;
        }

        public String getProductGeneratorName() {
            return productGeneratorName;
        }

        public void setProductGeneratorName(String productGeneratorName) {
            this.productGeneratorName = productGeneratorName;
        }

        public LinkedHashMap<String, Serializable> getData() {
            return data;
        }

        public void setData(LinkedHashMap<String, Serializable> data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    /**
     * Json representation of a hazard event set
     */
    @Deprecated
    public static class HazardEventSet {

        private StagingInfo stagingInfo;

        private String productGeneratorName;

        private Map<String, String> dialogInfo;

        public StagingInfo getStagingInfo() {
            return stagingInfo;
        }

        public void setStagingInfo(StagingInfo stagingInfo) {
            this.stagingInfo = stagingInfo;
        }

        public String getProductGeneratorName() {
            return productGeneratorName;
        }

        public void setProductGeneratorName(String productGeneratorName) {
            this.productGeneratorName = productGeneratorName;
        }

        public Map<String, String> getDialogInfo() {
            return dialogInfo;
        }

        public void setDialogInfo(Map<String, String> dialogInfo) {
            this.dialogInfo = dialogInfo;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    /**
     * Json representation Staging info
     */
    @Deprecated
    public static class StagingInfo {

        private Map<String, String[]> valueDict;

        private DeprecatedField[] fields;

        public Map<String, String[]> getValueDict() {
            return valueDict;
        }

        public void setValueDict(Map<String, String[]> valueDict) {
            this.valueDict = valueDict;
        }

        public DeprecatedField[] getFields() {
            return fields;
        }

        public void setFields(DeprecatedField[] fields) {
            this.fields = fields;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }

    }

    /**
     * Json representation of a field in Staging info
     */
    @Deprecated
    public static class DeprecatedField {
        private int lines;

        private DeprecatedChoice[] choices;

        private String fieldName;

        private String fieldType;

        private String label;

        public int getLines() {
            return lines;
        }

        public void setLines(int lines) {
            this.lines = lines;
        }

        public DeprecatedChoice[] getChoices() {
            return choices;
        }

        public void setChoices(DeprecatedChoice[] choices) {
            this.choices = choices;
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getFieldType() {
            return fieldType;
        }

        public void setFieldType(String fieldType) {
            this.fieldType = fieldType;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    /**
     * Json representation choice for a field
     */
    @Deprecated
    public static class DeprecatedChoice {
        private String displayString;

        private String identifier;

        public String getDisplayString() {
            return displayString;
        }

        public void setDisplayString(String displayString) {
            this.displayString = displayString;
        }

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }

    }

}