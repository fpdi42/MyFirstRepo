package com.example.dynamic.groovy;

import com.example.dynamic.model.ClassDescriptor;
import com.example.dynamic.model.FieldDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Genererar Groovy-klasskällkod från en ClassDescriptor
 */
public class GroovyCodeGenerator {
    private static final Logger logger = LoggerFactory.getLogger(GroovyCodeGenerator.class);

    /**
     * Genererar Groovy-klasskällkod
     */
    public String generateClassSource(ClassDescriptor descriptor) {
        StringBuilder sb = new StringBuilder();

        // Package declaration
        sb.append("package ").append(descriptor.getPackageName()).append("\n\n");

        // Imports
        sb.append("import jakarta.xml.bind.annotation.XmlRootElement\n");
        sb.append("import jakarta.xml.bind.annotation.XmlElement\n");
        sb.append("import com.fasterxml.jackson.annotation.JsonProperty\n");
        sb.append("import java.time.LocalDate\n");
        sb.append("import java.time.LocalDateTime\n");
        sb.append("import java.math.BigDecimal\n\n");

        // Class definition with annotations
        sb.append("@XmlRootElement(name = \"").append(descriptor.getClassName()).append("\")\n");
        sb.append("class ").append(descriptor.getClassName()).append(" {\n\n");

        // Fields with annotations
        List<FieldDefinition> fields = descriptor.getFields();
        for (FieldDefinition field : fields) {
            sb.append("    @JsonProperty(\"").append(field.getName()).append("\")\n");
            sb.append("    @XmlElement(name = \"").append(field.getName()).append("\")\n");
            sb.append("    private ").append(field.getType()).append(" ").append(field.getName()).append("\n\n");
        }

        // Constructor
        sb.append("    ").append(descriptor.getClassName()).append("() {\n");
        sb.append("    }\n\n");

        // Getters and Setters
        for (FieldDefinition field : fields) {
            String capitalizedName = capitalizeFirstLetter(field.getName());

            // Getter
            sb.append("    ").append(field.getType()).append(" get").append(capitalizedName).append("() {\n");
            sb.append("        return this.").append(field.getName()).append("\n");
            sb.append("    }\n\n");

            // Setter
            sb.append("    void set").append(capitalizedName).append("(").append(field.getType())
                    .append(" ").append(field.getName()).append(") {\n");
            sb.append("        this.").append(field.getName()).append(" = ").append(field.getName()).append("\n");
            sb.append("    }\n\n");
        }

        // toString() method
        sb.append("    @Override\n");
        sb.append("    String toString() {\n");
        sb.append("        return \"").append(descriptor.getClassName()).append("{\" +\n");

        for (int i = 0; i < fields.size(); i++) {
            FieldDefinition field = fields.get(i);
            sb.append("            \"").append(field.getName()).append("=\" + ")
                    .append(field.getName());

            if (i < fields.size() - 1) {
                sb.append(" + \", \" +\n");
            } else {
                sb.append(" +\n");
            }
        }
        sb.append("            \"}\"\n");
        sb.append("    }\n\n");

        // equals() method
        sb.append("    @Override\n");
        sb.append("    boolean equals(Object o) {\n");
        sb.append("        if (this === o) return true\n");
        sb.append("        if (!(o instanceof ").append(descriptor.getClassName()).append(")) return false\n");
        sb.append("        ").append(descriptor.getClassName()).append(" that = (").append(descriptor.getClassName()).append(") o\n");

        for (FieldDefinition field : fields) {
            sb.append("        if (").append(field.getName()).append(" != that.").append(field.getName()).append(") return false\n");
        }
        sb.append("        return true\n");
        sb.append("    }\n\n");

        // hashCode() method
        sb.append("    @Override\n");
        sb.append("    int hashCode() {\n");
        sb.append("        return Objects.hash(");
        for (int i = 0; i < fields.size(); i++) {
            sb.append(fields.get(i).getName());
            if (i < fields.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")\n");
        sb.append("    }\n\n");

        // End class
        sb.append("}\n");

        String sourceCode = sb.toString();
        logger.debug("Genererad Groovy-klasskällkod:\n{}", sourceCode);

        return sourceCode;
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
