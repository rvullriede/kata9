package de.vullriede.codekata;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A Checkout provides a total price of a list of articles based on a list of pricing rules.
 * Pricing rules can be based on different units (e.g. QUANTITY, WEIGHT etc.) and can be (optionally) even be mixed for the same article.
 *
 * <p> A Checkout is thread-safe and can be used from multiple threads.
 * However, for certain use-case external synchronization might be required.
 *
 * <p> Consider an example when an article is scanned from <i>Thread A</i>
 * while <i>Thread B</i> is asking for the total price. While both methods
 * itself will work as expected, <i>Thread A<i> won't block</i> and <i>Thread B</i>
 * will return the total price based of a snapshot that might not yet contain
 * the simultaneously added article.
 */
public class Checkout {

    Map<Sku, Map<Unit, Double>> items = new ConcurrentHashMap<>();

    List<PricingRule> pricingRules = null;


    /**
     * Initializes a new Checkout object.
     *
     * @param pricingRules list of pricing rules that shall be used to calculate the total price.
     *                     At least one rule is required (list size >= 1)
     */
    public Checkout(List<PricingRule> pricingRules) {
        if (pricingRules == null) {
            throw new IllegalArgumentException("At least one pricing rule is required.");
        }
        this.pricingRules = pricingRules;
    }


    /**
     * Adds an article with a {@code MeasurementResult} to the checkout cart.
     * For a pricing per article count, you can use the simpler method {@link #scan(Sku sku)}.
     *
     * @param sku               the article that shall be added
     * @param measurementResult a {@code MeasurementResult}, that carries a {@code Unit} and an amount, e.g. from a scale
     * @return this.
     */
    public Checkout scan(Sku sku, MeasurementResult measurementResult) {
        Objects.requireNonNull(sku, "sku must not be null.");
        Objects.requireNonNull(measurementResult, "measurementResult must not be null.");
        this.items.computeIfAbsent(sku, s -> new ConcurrentHashMap<>()).merge(measurementResult.unit(), measurementResult.amount(), (sumAmount, amount) -> sumAmount + amount);
        return this;
    }


    /**
     * Adds an article with a default {@code Unit} of QUANTITY and amount of 1 to the checkout card.
     * For articles with more complex pricing (e.g. based on WEIGHT)
     * you can use {@link #scan(Sku sku, MeasurementResult result)}.
     *
     * @param sku the article that shall be added
     * @return this.
     */
    public Checkout scan(Sku sku) {
        return this.scan(sku, new MeasurementResult(Unit.QUANTITY, 1));
    }


    /**
     * Adds a list of articles with a default {@code Unit} of QUANTITY and amount of 1 to the checkout card.
     *
     * @param skuList list of articles (must not be null)
     * @return this.
     */
    public Checkout scan(List<Sku> skuList) {
        if (skuList == null) {
            throw new IllegalArgumentException("The provided SKU list cannot be empty.");
        }
        skuList.forEach(sku -> this.scan(sku));
        return this;
    }


    /**
     * Provides the total amount based on the articles currently in the checkout card.
     * The amount is "ready to pay", means rounded (half-) up to 2 decimal places.
     *
     * @return the total (rounded) amount
     */
    public BigDecimal getTotalPrice() {

        BigDecimal total = BigDecimal.ZERO;

        // we will iterate over all articles and calculate subtotals for each article/unit combination and then sum them up to the total.
        for (Map.Entry<Sku, Map<Unit, Double>> skuEntry : this.items.entrySet()) {
            Sku sku = skuEntry.getKey();

            // filter by SKU and sort by the price rules amount DESC. We will then apply the rules from top (largest amount) until no remaining amount is left.
            List<PricingRule> filteredBySku = pricingRules.stream().filter(p -> p.sku().equals(sku)).sorted(Comparator.comparing(PricingRule::amount).reversed()).collect(Collectors.toList());

            if (filteredBySku.isEmpty()) {
                throw new IllegalStateException("No pricing rule(s) found for SKU '" + sku.code() + "', price calculation not possible.");
            }

            BigDecimal subTotalForSku = BigDecimal.ZERO;

            for (Map.Entry<Unit, Double> skuAndUnitEntry : skuEntry.getValue().entrySet()) {
                Unit unit = skuAndUnitEntry.getKey();
                Double amount = skuAndUnitEntry.getValue();

                // already sorted descending as we need it, no 2nd sorting required.
                List<PricingRule> filteredBySkuAndUnit = filteredBySku.stream().filter(p -> p.unit().equals(unit)).collect(Collectors.toList());

                if (filteredBySkuAndUnit.isEmpty()) {
                    throw new IllegalStateException("No pricing rule(s) found for SKU '" + sku.code() + "' and unit '" + unit + "', price calculation not possible.");
                }

                double remainingAmount = amount;

                BigDecimal subTotalForSkuAndQuantity = BigDecimal.ZERO;

                for (PricingRule pricingRule : filteredBySkuAndUnit) {
                    if (remainingAmount >= pricingRule.amount()) {

                        // check how often we can apply the current rule to the remaining amount, apply it and multiple accordingly
                        int ruleApplicable = (int) (remainingAmount / pricingRule.amount());
                        subTotalForSkuAndQuantity = subTotalForSkuAndQuantity.add(pricingRule.price().multiply(BigDecimal.valueOf(ruleApplicable)));
                        remainingAmount = remainingAmount - (pricingRule.amount() * ruleApplicable);
                    }
                }

                // if we have a left-over amount and the unit is dividable (e.g. WEIGHT) we use the last rule (with the smallest amount), otherwise we can't complete the calculation.
                if (remainingAmount > 0) {
                    if (unit.isDividable()) {
                        PricingRule lastPricingRule = filteredBySkuAndUnit.get(filteredBySkuAndUnit.size() - 1);
                        subTotalForSkuAndQuantity = subTotalForSkuAndQuantity.add(lastPricingRule.price().multiply(BigDecimal.valueOf(remainingAmount)));
                    } else {
                        throw new IllegalStateException("No pricing rule found for SKU '" + sku.code() + "', unit " + unit + " and amount " + remainingAmount + ", price calculation not possible.");
                    }
                }

                subTotalForSku = subTotalForSku.add(subTotalForSkuAndQuantity);
            }

            total = total.add(subTotalForSku);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }


    /**
     * Empties the checkout cart. Useful for e.g. wrongly added scans ("Storno").
     */
    public void reset() {
        this.items = new ConcurrentHashMap<>();
    }
}

