package de.vullriede.codekata;

public enum Unit {
    QUANTITY(false),
    WEIGHT_IN_KG(true),
    VOLUME_IN_L(true);

    private final boolean dividable;


    Unit(boolean dividable) {
        this.dividable = dividable;
    }


    public boolean isDividable() {
        return this.dividable;
    }
}
