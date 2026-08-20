package org.liftoff.thepantry.services;

import org.junit.jupiter.api.Test;
import org.liftoff.thepantry.models.Unit;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuantityNormalizationServiceTests {

    private final QuantityNormalizationService service = new QuantityNormalizationService();

    @Test
    void normalizesFractionsAndCompatibleVolumeUnits() {
        QuantityNormalizationService.NormalizedQuantity halfCup = service
                .normalize("1/2", new Unit("cup"))
                .orElseThrow();
        QuantityNormalizationService.NormalizedQuantity tablespoons = service
                .normalize("8", new Unit("tablespoon"))
                .orElseThrow();

        assertEquals(0, halfCup.getAmount().compareTo(tablespoons.getAmount()));
        assertEquals(QuantityNormalizationService.MeasurementDimension.VOLUME,
                halfCup.getDimension());
    }

    @Test
    void normalizesMixedFractionsAndMassUnits() {
        BigDecimal grams = service.normalize("1 1/2", new Unit("kilogram"))
                .orElseThrow().getAmount();

        assertEquals(new BigDecimal("1500.000000"), grams);
    }

    @Test
    void leavesFreeFormAmountsUnnormalized() {
        assertTrue(service.normalize("to taste", new Unit("teaspoon")).isEmpty());
    }
}
