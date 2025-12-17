package com.example.dynamic.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Beskriver en klass som ska genereras dynamiskt
 */
public class ClassDescriptor {
    @JsonProperty("className")
    private String className;

    @JsonProperty("packageName")
    private String packageName;

    @JsonProperty("fields")
    private List<FieldDefinition> fields = new ArrayList<>();

    @JsonProperty("description")
    private String description;

    public ClassDescriptor() {
    }

    public ClassDescriptor(String className, String packageName, List<FieldDefinition> fields) {
        this.className = className;
        this.packageName = packageName;
        this.fields = fields;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public List<FieldDefinition> getFields() {
        return fields;
    }

    public void setFields(List<FieldDefinition> fields) {
        this.fields = fields;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFullyQualifiedName() {
        return packageName + "." + className;
    }

    @Override
    public String toString() {
        return "ClassDescriptor{" +
                "className='" + className + '\'' +
                ", packageName='" + packageName + '\'' +
                ", fields=" + fields +
                '}';
    }
}
