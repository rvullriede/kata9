package de.vullriede.codekata;


import java.util.Objects;

public record Sku(String code) {

    public Sku {
        Objects.requireNonNull(code, "code must not be null.");
    }
}
