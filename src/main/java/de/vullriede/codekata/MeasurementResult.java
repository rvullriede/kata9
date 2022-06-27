package de.vullriede.codekata;

import java.util.Objects;

/**
 * Carries a measurement result based of a  {@code Unit} and an amount, e.g. from a scale.
 *
 * @param unit   the unit (must not be null)
 * @param amount the amount (must not be null)
 */
public record MeasurementResult(Unit unit, double amount) {

    public MeasurementResult {
        Objects.requireNonNull(unit, "Unit must not be null");
        Objects.requireNonNull(amount, "Amount must not be null.");
        if (amount <= 0) {
            throw new IllegalArgumentException("Only positive amounts are accepted.");
        }
    }
}
