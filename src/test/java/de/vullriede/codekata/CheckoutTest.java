package de.vullriede.codekata;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CheckoutTest {


    private static final List<PricingRule> DEFAULT_PRICING_RULES = List.of(

            new PricingRule(new Sku("A"), 1, BigDecimal.valueOf(50)),
            new PricingRule(new Sku("A"), 3, BigDecimal.valueOf(130)),

            new PricingRule(new Sku("B"), 1, BigDecimal.valueOf(30)),
            new PricingRule(new Sku("B"), 2, BigDecimal.valueOf(45)),


            new PricingRule(new Sku("C"), 1, BigDecimal.valueOf(20)),

            new PricingRule(new Sku("D"), 1, BigDecimal.valueOf(15)),

            new PricingRule(new Sku("A"), Unit.WEIGHT_IN_KG, 1, new BigDecimal("0.99")),
            new PricingRule(new Sku("A"), Unit.WEIGHT_IN_KG, 5, new BigDecimal("3.99"))
    );

    private static List<Sku> convert(String codeList) {
        if (codeList == null || codeList.length() == 0) {
            return Collections.emptyList();
        }
        String[] split = codeList.split("");
        return Arrays.stream(split).map(s -> new Sku(s)).collect(Collectors.toList());
    }

    private static BigDecimal getTotalPrice(String codeList) {
        return new Checkout(DEFAULT_PRICING_RULES).scan(convert(codeList)).getTotalPrice();
    }

    private static BigDecimal getTotalPrice(List<PricingRule> pricingRules, String codeList) {
        return new Checkout(pricingRules).scan(convert(codeList)).getTotalPrice();
    }


    @Test
    public void testValidCodesForDefaultUnitQuantity() {

        assertThat(getTotalPrice(""), comparesEqualTo(BigDecimal.ZERO));
        assertThat(getTotalPrice("A"), comparesEqualTo(BigDecimal.valueOf(50)));
        assertThat(getTotalPrice("AB"), comparesEqualTo(BigDecimal.valueOf(80)));
        assertThat(getTotalPrice("CDBA"), comparesEqualTo(BigDecimal.valueOf(115)));

        assertThat(getTotalPrice("AA"), comparesEqualTo(BigDecimal.valueOf(100)));
        assertThat(getTotalPrice("AAA"), comparesEqualTo(BigDecimal.valueOf(130)));
        assertThat(getTotalPrice("AAAA"), comparesEqualTo(BigDecimal.valueOf(180)));
        assertThat(getTotalPrice("AAAAA"), comparesEqualTo(BigDecimal.valueOf(230)));
        assertThat(getTotalPrice("AAAAAA"), comparesEqualTo(BigDecimal.valueOf(260)));

        assertThat(getTotalPrice("AAAB"), comparesEqualTo(BigDecimal.valueOf(160)));
        assertThat(getTotalPrice("AAABB"), comparesEqualTo(BigDecimal.valueOf(175)));
        assertThat(getTotalPrice("AAABBD"), comparesEqualTo(BigDecimal.valueOf(190)));
        assertThat(getTotalPrice("DABABA"), comparesEqualTo(BigDecimal.valueOf(190)));
    }


    @Test
    public void testValidCodeForUnitWeight() {
        Checkout checkout = new Checkout(DEFAULT_PRICING_RULES);

        checkout.scan(new Sku("A"), new MeasurementResult(Unit.WEIGHT_IN_KG, 0.234));
        assertEquals(new BigDecimal("0.23"), checkout.getTotalPrice()); // 0,23166, round down

        checkout.reset();

        checkout.scan(new Sku("A"), new MeasurementResult(Unit.WEIGHT_IN_KG, 0.238));
        assertEquals(new BigDecimal("0.24"), checkout.getTotalPrice()); // 0,23562, round up

    }


    @Test
    public void testMissingPricingRulesForSku() {
        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> {
            getTotalPrice("E");
        });
        assertEquals("No pricing rule(s) found for SKU 'E', price calculation not possible.", illegalStateException.getMessage());
    }


    @Test
    public void testMissingPricingRulesForSkuAndUnit() {
        List<PricingRule> pricingRules = List.of(new PricingRule(new Sku("A"), Unit.WEIGHT_IN_KG, 1, BigDecimal.valueOf(130)));

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> {
            getTotalPrice(pricingRules, "A");
        });
        assertEquals("No pricing rule(s) found for SKU 'A' and unit 'QUANTITY', price calculation not possible.", illegalStateException.getMessage());
    }


    @Test
    public void testMissingPricingRulesForAmount() {
        List<PricingRule> pricingRules = List.of(new PricingRule(new Sku("A"), 2, BigDecimal.valueOf(130)));

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> {
            getTotalPrice(pricingRules, "AAA");
        });

        assertEquals("No pricing rule found for SKU 'A', unit QUANTITY and amount 1.0, price calculation not possible.", illegalStateException.getMessage());
    }

}
