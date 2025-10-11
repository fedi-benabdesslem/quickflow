package com.ai.application.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TemplateType {
    email,
    PV
    ;

    @JsonCreator
    public static TemplateType fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("TemplateType cannot be null");
        }
        String normalized = value.trim().toUpperCase();
        switch (normalized) {
            case "EMAIL":
                return email;
            case "PV":
                return PV;
            default:
                throw new IllegalArgumentException("Unknown TemplateType value: " + normalized + ". Accepted: email, PV");
        }
    }
}