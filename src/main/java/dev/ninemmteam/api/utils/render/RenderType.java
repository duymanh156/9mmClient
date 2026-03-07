package dev.ninemmteam.api.utils.render;

public enum RenderType {
    SIDES,
    LINES,
    BOTH;

    public boolean sides() {
        return this == SIDES || this == BOTH;
    }

    public boolean lines() {
        return this == LINES || this == BOTH;
    }
}
