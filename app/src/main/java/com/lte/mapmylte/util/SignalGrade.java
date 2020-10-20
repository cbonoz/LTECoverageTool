package com.lte.mapmylte.util;

public enum SignalGrade {
    TOP("top"),
    MIDDLE_LOW("middlelow"),
    MIDDLE("middle"),
    LOW("low"),
    UNKNOWN("");

    private final String value;

    SignalGrade(String top) {
        value = top;
    }

    public String getValue() {
        return value;
    }
}
