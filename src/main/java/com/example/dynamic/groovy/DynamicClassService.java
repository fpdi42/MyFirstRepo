package com.example.dynamic.groovy;

import com.example.dynamic.model.ClassDescriptor;
import com.example.dynamic.security.ClassDescriptorValidator;
import com.example.dynamic.xml.XmlMarshaller;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Iterator;

/**
 * Service för att hantera dynamisk klassgenering, instansiering och XML-marshalling
 */
@Service
public class DynamicClassService {
    private static final Logger logger = LoggerFactory.getLogger(DynamicClassService.class);

    private final DynamicClassLoader dynamicClassLoader;
    private final GroovyCodeGenerator codeGenerator;
    private final ClassDescriptorValidator validator;
    private final XmlMarshaller xmlMarshaller;

    public DynamicClassService() {
        this.dynamicClassLoader = new DynamicClassLoader();
        this.codeGenerator = new GroovyCodeGenerator();
        this.validator = new ClassDescriptorValidator();
        this.xmlMarshaller = new XmlMarshaller();
    }

    /**
     * Huvudmetod: klassgenering från JSON
     * @param descriptor Klasseskriptor från JSON
     * @return Klassresponsobjekt med källkod och metadata
     */
    public GeneratedClassResponse generateClass(ClassDescriptor descriptor) throws Exception {
        // 1. Validera klasseskriptor
        logger.info("Validerar klasseskriptor: {}", descriptor);
        validator.validate(descriptor);

        // 2. Generera Groovy-källkod
        String sourceCode = codeGenerator.generateClassSource(descriptor);
        logger.info("Groovy-källkod genererad för: {}", descriptor.getFullyQualifiedName());

        // 3. Kompilera och ladda klassen
        Class<?> compiledClass = dynamicClassLoader.compileAndLoad(
                descriptor.getFullyQualifiedName(),
                sourceCode
        );

        // 4. Returnera genererad klassresponse
        GeneratedClassResponse response = new GeneratedClassResponse();
        response.setClassId(generateClassId(descriptor));
        response.setClassName(descriptor.getClassName());
        response.setFullyQualifiedName(descriptor.getFullyQualifiedName());
        response.setSourceCode(sourceCode);
        response.setPackageName(descriptor.getPackageName());
        response.setCacheStats(dynamicClassLoader.getCacheStats());

        logger.info("Klass framgångsrikt genererad: {}", response.getFullyQualifiedName());
        return response;
    }

    /**
     * Instansierar klassen och mappar data från JSON
     * @param fullyQualifiedName Klassnamn med paket
     * @param sourceCode Groovy-källkod (för omkompilering om nödvändigt)
     * @param instanceData JSON-data för objektet
     * @return Marshallad XML-representation av objektet
     */
    public String createInstanceAndMarshalToXml(String fullyQualifiedName, String sourceCode, JsonNode instanceData) throws Exception {
        try {
            // 1. Kompilera/ladda klassen (från cache om möjligt)
            logger.info("Laddar klass: {}", fullyQualifiedName);
            Class<?> clazz = dynamicClassLoader.compileAndLoad(fullyQualifiedName, sourceCode);

            // 2. Instansiering
            Object instance = dynamicClassLoader.instantiate(clazz);
            logger.info("Instans skapad för: {}", fullyQualifiedName);

            // 3. Mappa JSON-data till objektet via reflection
            mapJsonToObject(instance, instanceData);
            logger.info("JSON-data mappad till objektet");

            // 4. Marshalla till XML
            String xml = xmlMarshaller.marshallToXmlPretty(instance);
            logger.info("Objekt marshallat till XML framgångsrikt");

            return xml;

        } catch (Exception e) {
            logger.error("Fel vid instansiering och marshalling", e);
            throw new RuntimeException("Misslyckades att skapa instans och marshalla till XML: " + e.getMessage(), e);
        }
    }

    /**
     * Mappar JSON-nod till objektets getter/setter
     */
    private void mapJsonToObject(Object obj, JsonNode jsonNode) throws Exception {
        Iterator<String> fieldNames = jsonNode.fieldNames();

        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode fieldValue = jsonNode.get(fieldName);

            // Generera setter-metodnamn
            String setterName = "set" + capitalizeFirstLetter(fieldName);

            try {
                // Sök efter setter-metod
                Method[] methods = obj.getClass().getDeclaredMethods();
                for (Method method : methods) {
                    if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                        Class<?> paramType = method.getParameterTypes()[0];
                        Object value = convertJsonValue(fieldValue, paramType);
                        method.invoke(obj, value);
                        logger.debug("Mappad {} = {}", fieldName, value);
                        break;
                    }
                }
            } catch (Exception e) {
                logger.warn("Misslyckades att mappa fält '{}' till objektet", fieldName, e);
            }
        }
    }

    /**
     * Konverterar JsonNode-värde till rätt Java-typ
     */
    private Object convertJsonValue(JsonNode node, Class<?> targetType) {
        if (node.isNull()) {
            return null;
        }

        if (targetType == String.class) {
            return node.asText();
        } else if (targetType == int.class || targetType == Integer.class) {
            return node.asInt();
        } else if (targetType == long.class || targetType == Long.class) {
            return node.asLong();
        } else if (targetType == double.class || targetType == Double.class) {
            return node.asDouble();
        } else if (targetType == float.class || targetType == Float.class) {
            return (float) node.asDouble();
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return node.asBoolean();
        } else if (targetType == java.time.LocalDate.class) {
            return java.time.LocalDate.parse(node.asText());
        } else if (targetType == java.time.LocalDateTime.class) {
            return java.time.LocalDateTime.parse(node.asText());
        } else if (targetType == java.math.BigDecimal.class) {
            return new java.math.BigDecimal(node.asText());
        } else {
            logger.warn("Okänd måltyp: {}", targetType.getName());
            return node.asText();
        }
    }

    private String generateClassId(ClassDescriptor descriptor) {
        return descriptor.getPackageName() + "." + descriptor.getClassName() + "_" + System.currentTimeMillis();
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public DynamicClassLoader.CacheStats getCacheStats() {
        return dynamicClassLoader.getCacheStats();
    }

    public void cleanup() {
        dynamicClassLoader.cleanup();
    }

    /**
     * DTO för genererad klassresponse
     */
    public static class GeneratedClassResponse {
        private String classId;
        private String className;
        private String fullyQualifiedName;
        private String packageName;
        private String sourceCode;
        private DynamicClassLoader.CacheStats cacheStats;

        // Getters and setters
        public String getClassId() {
            return classId;
        }

        public void setClassId(String classId) {
            this.classId = classId;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getFullyQualifiedName() {
            return fullyQualifiedName;
        }

        public void setFullyQualifiedName(String fullyQualifiedName) {
            this.fullyQualifiedName = fullyQualifiedName;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public String getSourceCode() {
            return sourceCode;
        }

        public void setSourceCode(String sourceCode) {
            this.sourceCode = sourceCode;
        }

        public DynamicClassLoader.CacheStats getCacheStats() {
            return cacheStats;
        }

        public void setCacheStats(DynamicClassLoader.CacheStats cacheStats) {
            this.cacheStats = cacheStats;
        }
    }
}
