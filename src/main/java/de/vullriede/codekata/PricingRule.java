package de.vullriede.codekata;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A PricingRule defines the how a an article (identifed by its SKU) is priced, based on a Unit (e.g. QUANTITY or WEIGHT) and an amount.
 *
 * @param sku    the SKU (must not be null).
 * @param unit   the unit (must not be null).
 * @param amount the amount (must not be null).
 * @param price  the price (must not be bull).
 */
public record PricingRule(Sku sku, Unit unit, double amount, BigDecimal price) {
    public PricingRule {
        Objects.requireNonNull(sku, "sku must not be null.");
        Objects.requireNonNull(unit, "unit must not be null.");
        Objects.requireNonNull(amount, "amount must not be null.");
        Objects.requireNonNull(price, "price must not be null.");

        if (amount <= 0) {
            throw new IllegalArgumentException("Only positive amounts are accepted.");
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Only positive prices are accepted.");
        }
    }

    public PricingRule(Sku sku, int amount, BigDecimal price) {
        this(sku, Unit.QUANTITY, amount, price);
    }
}
