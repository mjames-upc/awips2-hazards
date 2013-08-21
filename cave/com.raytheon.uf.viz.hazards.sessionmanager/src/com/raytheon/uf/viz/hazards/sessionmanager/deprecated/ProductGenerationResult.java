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
package com.raytheon.uf.viz.hazards.sessionmanager.deprecated;

import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonProperty;

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

        @JsonProperty("Legacy")
        private String legacy;

        private String productID;

        public String getLegacy() {
            return legacy;
        }

        public void setLegacy(String legacy) {
            this.legacy = legacy;
        }

        public String getProductID() {
            return productID;
        }

        public void setProductID(String productID) {
            this.productID = productID;
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

        private String productGenerator;

        private Map<String, String> dialogInfo;

        public StagingInfo getStagingInfo() {
            return stagingInfo;
        }

        public void setStagingInfo(StagingInfo stagingInfo) {
            this.stagingInfo = stagingInfo;
        }

        public String getProductGenerator() {
            return productGenerator;
        }

        public void setProductGenerator(String productGenerator) {
            this.productGenerator = productGenerator;
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

        private Field[] fields;

        public Map<String, String[]> getValueDict() {
            return valueDict;
        }

        public void setValueDict(Map<String, String[]> valueDict) {
            this.valueDict = valueDict;
        }

        public Field[] getFields() {
            return fields;
        }

        public void setFields(Field[] fields) {
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
    public static class Field {
        private int lines;

        private Choice[] choices;

        private String fieldName;

        private String fieldType;

        private String label;

        public int getLines() {
            return lines;
        }

        public void setLines(int lines) {
            this.lines = lines;
        }

        public Choice[] getChoices() {
            return choices;
        }

        public void setChoices(Choice[] choices) {
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
    public static class Choice {
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