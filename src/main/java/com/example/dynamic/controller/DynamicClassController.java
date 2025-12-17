package com.example.dynamic.controller;

import com.example.dynamic.groovy.DynamicClassService;
import com.example.dynamic.model.ClassDescriptor;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST-controller för dynamisk klassgenering
 */
@RestController
@RequestMapping("/api/dynamic")
public class DynamicClassController {
    private static final Logger logger = LoggerFactory.getLogger(DynamicClassController.class);

    @Autowired
    private DynamicClassService dynamicClassService;

    /**
     * POST /api/dynamic/class
     * Genererar en klass baserat på klasseskriptor
     *
     * Exempel JSON:
     * {
     *   "className": "Person",
     *   "packageName": "com.example.dynamic.generated",
     *   "fields": [
     *     {"name": "firstName", "type": "String", "required": true},
     *     {"name": "lastName", "type": "String", "required": true},
     *     {"name": "age", "type": "int", "required": false},
     *     {"name": "address", "type": "String", "required": false}
     *   ]
     * }
     */
    @PostMapping("/class")
    public ResponseEntity<?> generateClass(@RequestBody ClassDescriptor descriptor) {
        try {
            logger.info("Klassgenering startad för: {}", descriptor.getFullyQualifiedName());
            DynamicClassService.GeneratedClassResponse response = dynamicClassService.generateClass(descriptor);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            logger.warn("Säkerhetsvalidering misslyckades: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Säkerhetsfel", e.getMessage()));
        } catch (Exception e) {
            logger.error("Fel vid klassgenering", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Klassgenering misslyckades", e.getMessage()));
        }
    }

    /**
     * POST /api/dynamic/instance/{classId}
     * Instansierar klassen och marshaller till XML
     *
     * Exempel JSON:
     * {
     *   "sourceCode": "package com.example.dynamic.generated;\n...",
     *   "fullyQualifiedName": "com.example.dynamic.generated.Person",
     *   "data": {
     *     "firstName": "John",
     *     "lastName": "Doe",
     *     "age": 30,
     *     "address": "123 Main St"
     *   }
     * }
     */
    @PostMapping("/instance")
    public ResponseEntity<?> createInstanceAndMarshalXml(@RequestBody InstanceRequest request) {
        try {
            if (request.getSourceCode() == null || request.getSourceCode().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Felaktig förfrågan", "sourceCode är obligatorisk"));
            }

            if (request.getFullyQualifiedName() == null || request.getFullyQualifiedName().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Felaktig förfrågan", "fullyQualifiedName är obligatorisk"));
            }

            if (request.getData() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Felaktig förfrågan", "data är obligatorisk"));
            }

            logger.info("Instansiering startad för: {}", request.getFullyQualifiedName());
            String xmlOutput = dynamicClassService.createInstanceAndMarshalToXml(
                    request.getFullyQualifiedName(),
                    request.getSourceCode(),
                    request.getData()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fullyQualifiedName", request.getFullyQualifiedName());
            response.put("xml", xmlOutput);
            response.put("cacheStats", dynamicClassService.getCacheStats());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Fel vid instansiering och marshalling", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Instansiering misslyckades", e.getMessage()));
        }
    }

    /**
     * GET /api/dynamic/cache-stats
     * Hämtar cache-statistik
     */
    @GetMapping("/cache-stats")
    public ResponseEntity<?> getCacheStats() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cacheStats", dynamicClassService.getCacheStats());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Fel vid hämtning av cache-stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Cache-stats hämtning misslyckades", e.getMessage()));
        }
    }

    /**
     * DELETE /api/dynamic/cache
     * Rensar cache
     */
    @DeleteMapping("/cache")
    public ResponseEntity<?> clearCache() {
        try {
            dynamicClassService.cleanup();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cache rensat framgångsrikt");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Fel vid cache-rensning", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Cache-rensning misslyckades", e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String error, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        return errorResponse;
    }

    /**
     * DTO för instanieringsförfrågan
     */
    public static class InstanceRequest {
        private String fullyQualifiedName;
        private String sourceCode;
        private JsonNode data;

        public String getFullyQualifiedName() {
            return fullyQualifiedName;
        }

        public void setFullyQualifiedName(String fullyQualifiedName) {
            this.fullyQualifiedName = fullyQualifiedName;
        }

        public String getSourceCode() {
            return sourceCode;
        }

        public void setSourceCode(String sourceCode) {
            this.sourceCode = sourceCode;
        }

        public JsonNode getData() {
            return data;
        }

        public void setData(JsonNode data) {
            this.data = data;
        }
    }
}
