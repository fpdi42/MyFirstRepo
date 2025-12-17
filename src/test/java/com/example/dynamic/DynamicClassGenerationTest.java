package com.example.dynamic;

import com.example.dynamic.groovy.DynamicClassService;
import com.example.dynamic.model.ClassDescriptor;
import com.example.dynamic.model.FieldDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integrationtest för dynamisk klassgenering
 */
@SpringBootTest
@DisplayName("Dynamisk klassgenering tests")
public class DynamicClassGenerationTest {

    @Test
    @DisplayName("Ska generera Person-klass från JSON och marshalla till XML")
    public void testPersonClassGenerationAndXmlMarshalling() throws Exception {
        // 1. Skapa ClassDescriptor för Person-klass
        ClassDescriptor descriptor = createPersonDescriptor();

        // 2. Skapa service
        DynamicClassService service = new DynamicClassService();

        // 3. Generera klassen
        DynamicClassService.GeneratedClassResponse response = service.generateClass(descriptor);

        // Assertions
        assertNotNull(response, "GeneratedClassResponse ska inte vara null");
        assertEquals("Person", response.getClassName());
        assertEquals("com.example.dynamic.generated.Person", response.getFullyQualifiedName());
        assertNotNull(response.getSourceCode(), "Källkod ska genererats");
        assertTrue(response.getSourceCode().contains("class Person"));

        System.out.println("\n=== GENERERAD GROOVY-KÄLLKOD ===");
        System.out.println(response.getSourceCode());

        // 4. Skapa instans och marshalla till XML
        String personJson = "{\n" +
                "  \"firstName\": \"John\",\n" +
                "  \"lastName\": \"Doe\",\n" +
                "  \"age\": 30,\n" +
                "  \"address\": \"123 Main Street\"\n" +
                "}";

        ObjectMapper mapper = new ObjectMapper();
        String xml = service.createInstanceAndMarshalToXml(
                response.getFullyQualifiedName(),
                response.getSourceCode(),
                mapper.readTree(personJson)
        );

        assertNotNull(xml, "XML-output ska inte vara null");
        assertTrue(xml.contains("John"), "XML ska innehålla firstName");
        assertTrue(xml.contains("Doe"), "XML ska innehålla lastName");
        assertTrue(xml.contains("30"), "XML ska innehålla age");

        System.out.println("\n=== MARSHALLAD XML ===");
        System.out.println(xml);

        // 5. Kontrollera cache-statistik
        assertNotNull(response.getCacheStats(), "CacheStats ska inte vara null");
        assertTrue(response.getCacheStats().getClassCount() > 0, "Cache ska innehålla minst en klass");

        System.out.println("\n=== CACHE STATISTICS ===");
        System.out.println(response.getCacheStats());

        service.cleanup();
    }

    @Test
    @DisplayName("Ska validera klasseskriptor och kastera SecurityException för ogiltigt klassnamn")
    public void testSecurityValidationForInvalidClassName() throws Exception {
        ClassDescriptor descriptor = createPersonDescriptor();
        descriptor.setClassName("123Invalid"); // Klassnamn får inte börja med siffra

        DynamicClassService service = new DynamicClassService();

        assertThrows(Exception.class, () -> {
            service.generateClass(descriptor);
        }, "Ska kasta exception för ogiltigt klassnamn");

        service.cleanup();
    }

    @Test
    @DisplayName("Ska validera att otillåten fälttyp inte accepteras")
    public void testSecurityValidationForInvalidFieldType() throws Exception {
        ClassDescriptor descriptor = createPersonDescriptor();
        FieldDefinition invalidField = new FieldDefinition("dangerous", "Object", false);
        descriptor.getFields().add(invalidField);

        DynamicClassService service = new DynamicClassService();

        assertThrows(Exception.class, () -> {
            service.generateClass(descriptor);
        }, "Ska kasta exception för otillåten fälttyp");

        service.cleanup();
    }

    @Test
    @DisplayName("Ska generera flera klasser och cachea dem korrekt")
    public void testMultipleClassGenerationWithCaching() throws Exception {
        DynamicClassService service = new DynamicClassService();

        // Generera flera klasser
        for (int i = 0; i < 3; i++) {
            ClassDescriptor descriptor = new ClassDescriptor();
            descriptor.setClassName("TestClass" + i);
            descriptor.setPackageName("com.example.dynamic.generated.test");

            List<FieldDefinition> fields = new ArrayList<>();
            fields.add(new FieldDefinition("field" + i, "String", true));
            descriptor.setFields(fields);

            DynamicClassService.GeneratedClassResponse response = service.generateClass(descriptor);
            assertNotNull(response);
        }

        // Kontrollera att klasserna är cachade
        DynamicClassService.GeneratedClassResponse response1 = service.generateClass(createPersonDescriptor());
        assertNotNull(response1.getCacheStats());
        assertTrue(response1.getCacheStats().getClassCount() >= 3, "Cache ska innehålla minst 3 klasser");

        System.out.println("\n=== CACHE EFTER GENERERING AV 4 KLASSER ===");
        System.out.println(response1.getCacheStats());

        service.cleanup();
    }

    private ClassDescriptor createPersonDescriptor() {
        ClassDescriptor descriptor = new ClassDescriptor();
        descriptor.setClassName("Person");
        descriptor.setPackageName("com.example.dynamic.generated");
        descriptor.setDescription("En Person-klass");

        List<FieldDefinition> fields = new ArrayList<>();
        fields.add(new FieldDefinition("firstName", "String", true));
        fields.add(new FieldDefinition("lastName", "String", true));
        fields.add(new FieldDefinition("age", "int", false));
        fields.add(new FieldDefinition("address", "String", false));

        descriptor.setFields(fields);
        return descriptor;
    }
}
