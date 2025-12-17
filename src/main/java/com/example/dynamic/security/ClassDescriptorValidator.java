package com.example.dynamic.security;

import com.example.dynamic.model.ClassDescriptor;
import com.example.dynamic.model.FieldDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validerar ClassDescriptor mot säkerhetskrav för att förhindra injection
 */
public class ClassDescriptorValidator {
    private static final Logger logger = LoggerFactory.getLogger(ClassDescriptorValidator.class);

    // Whitelist över tillåtna fälttyper
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "String", "int", "long", "double", "float", "boolean",
            "Integer", "Long", "Double", "Float", "Boolean",
            "java.time.LocalDate", "java.time.LocalDateTime",
            "java.util.Date", "BigDecimal", "java.math.BigDecimal"
    );

    // Java-reserverade ord
    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "false", "final", "finally", "float", "for", "goto", "if",
            "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "null", "package", "private", "protected", "public", "return",
            "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "true", "try", "void", "volatile", "while"
    );

    // Regex för giltiga klassnamn (börja med bokstav, innehålla bokstäver, siffror, underscore)
    private static final Pattern VALID_IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final Pattern VALID_PACKAGE_PATTERN = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)(\\.([a-zA-Z_][a-zA-Z0-9_]*))*$");

    /**
     * Validerar ClassDescriptor komplett
     * @throws SecurityException om validering misslyckas
     */
    public void validate(ClassDescriptor descriptor) throws SecurityException {
        if (descriptor == null) {
            throw new SecurityException("ClassDescriptor är null");
        }

        validateClassName(descriptor.getClassName());
        validatePackageName(descriptor.getPackageName());
        validateFields(descriptor.getFields());

        logger.info("ClassDescriptor validerad framgångsrikt: {}.{}", 
                descriptor.getPackageName(), descriptor.getClassName());
    }

    private void validateClassName(String className) throws SecurityException {
        if (className == null || className.trim().isEmpty()) {
            throw new SecurityException("Klassnamnet kan inte vara tomt");
        }

        if (!VALID_IDENTIFIER_PATTERN.matcher(className).matches()) {
            throw new SecurityException("Ogiltigt klassnamn: " + className + 
                    ". Måste börja med bokstav, innehålla endast bokstäver, siffror och underscore");
        }

        if (JAVA_KEYWORDS.contains(className.toLowerCase())) {
            throw new SecurityException("Klassnamnet '" + className + 
                    "' är ett reserverat Java-ord");
        }

        // Begränsa längd
        if (className.length() > 255) {
            throw new SecurityException("Klassnamnet är för långt (max 255 tecken)");
        }
    }

    private void validatePackageName(String packageName) throws SecurityException {
        if (packageName == null || packageName.trim().isEmpty()) {
            throw new SecurityException("Paketnamnet kan inte vara tomt");
        }

        if (!VALID_PACKAGE_PATTERN.matcher(packageName).matches()) {
            throw new SecurityException("Ogiltigt paketnamn: " + packageName + 
                    ". Måste följa Java-konventioner (t.ex. com.example.dynamic)");
        }

        if (packageName.length() > 255) {
            throw new SecurityException("Paketnamnet är för långt (max 255 tecken)");
        }
    }

    private void validateFields(java.util.List<FieldDefinition> fields) throws SecurityException {
        if (fields == null || fields.isEmpty()) {
            throw new SecurityException("Minst ett fält måste definieras");
        }

        if (fields.size() > 100) {
            throw new SecurityException("För många fält definierade (max 100)");
        }

        Set<String> usedFieldNames = new HashSet<>();
        for (FieldDefinition field : fields) {
            validateField(field, usedFieldNames);
        }
    }

    private void validateField(FieldDefinition field, Set<String> usedFieldNames) throws SecurityException {
        if (field.getName() == null || field.getName().trim().isEmpty()) {
            throw new SecurityException("Fältnamn kan inte vara tomt");
        }

        String fieldName = field.getName();
        if (!VALID_IDENTIFIER_PATTERN.matcher(fieldName).matches()) {
            throw new SecurityException("Ogiltigt fältnamn: " + fieldName + 
                    ". Måste börja med bokstav, innehålla endast bokstäver, siffror och underscore");
        }

        if (JAVA_KEYWORDS.contains(fieldName.toLowerCase())) {
            throw new SecurityException("Fältnamnet '" + fieldName + 
                    "' är ett reserverat Java-ord");
        }

        if (usedFieldNames.contains(fieldName)) {
            throw new SecurityException("Dubblerat fältnamn: " + fieldName);
        }
        usedFieldNames.add(fieldName);

        if (field.getType() == null || field.getType().trim().isEmpty()) {
            throw new SecurityException("Fälttyp kan inte vara tom för fält: " + fieldName);
        }

        if (!ALLOWED_TYPES.contains(field.getType())) {
            throw new SecurityException("Otillåten fälttyp '" + field.getType() + 
                    "' för fält '" + fieldName + "'. Tillåtna typer: " + ALLOWED_TYPES);
        }
    }
}
