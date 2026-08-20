package org.liftoff.thepantry.services;

import org.liftoff.thepantry.models.Unit;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class QuantityNormalizationService {

    private static final int SCALE = 6;
    private final Map<String, UnitDefinition> unitDefinitions = new HashMap<>();

    public QuantityNormalizationService() {
        register(MeasurementDimension.MASS, "1", "gram", "grams", "g");
        register(MeasurementDimension.MASS, "1000", "kilogram", "kilograms", "kg");
        register(MeasurementDimension.MASS, "28.349523125", "ounce", "ounces", "oz");
        register(MeasurementDimension.MASS, "453.59237", "pound", "pounds", "lb", "lbs");

        register(MeasurementDimension.VOLUME, "1", "milliliter", "milliliters", "ml");
        register(MeasurementDimension.VOLUME, "1000", "liter", "liters", "l");
        register(MeasurementDimension.VOLUME, "4.92892159375", "teaspoon", "teaspoons", "tsp");
        register(MeasurementDimension.VOLUME, "14.78676478125", "tablespoon", "tablespoons", "tbsp");
        register(MeasurementDimension.VOLUME, "236.5882365", "cup", "cups");
        register(MeasurementDimension.VOLUME, "946.352946", "quart", "quarts", "qt");
        register(MeasurementDimension.VOLUME, "29.5735295625", "fluid ounce", "fluid ounces", "fl oz");
        register(MeasurementDimension.VOLUME, "0.3080576", "pinch", "pinches");

        register(MeasurementDimension.COUNT, "1", "item", "items", "slice", "slices", "can", "cans",
                "clove", "cloves", "piece", "pieces");
    }

    public Optional<NormalizedQuantity> normalize(String displayAmount, Unit unit) {
        Optional<BigDecimal> parsedAmount = parseAmount(displayAmount);
        if (parsedAmount.isEmpty()) {
            return Optional.empty();
        }

        UnitDefinition definition = unit == null
                ? new UnitDefinition(MeasurementDimension.COUNT, BigDecimal.ONE)
                : unitDefinitions.get(normalizeUnitName(unit.getName()));
        if (definition == null) {
            return Optional.empty();
        }

        BigDecimal baseAmount = parsedAmount.get()
                .multiply(definition.factorToBase)
                .setScale(SCALE, RoundingMode.HALF_UP);
        return Optional.of(new NormalizedQuantity(baseAmount, definition.dimension));
    }

    public boolean supports(Unit unit) {
        return unit == null || unitDefinitions.containsKey(normalizeUnitName(unit.getName()));
    }

    Optional<BigDecimal> parseAmount(String displayAmount) {
        if (displayAmount == null || displayAmount.isBlank()) {
            return Optional.empty();
        }

        String normalized = displayAmount.trim()
                .replace("½", "1/2")
                .replace("¼", "1/4")
                .replace("¾", "3/4")
                .replace("⅓", "1/3")
                .replace("⅔", "2/3");
        String[] parts = normalized.split("\\s+");
        try {
            if (parts.length == 1) {
                return Optional.of(parseNumberOrFraction(parts[0]));
            }
            if (parts.length == 2 && parts[1].contains("/")) {
                return Optional.of(new BigDecimal(parts[0]).add(parseNumberOrFraction(parts[1])));
            }
        } catch (ArithmeticException | NumberFormatException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private BigDecimal parseNumberOrFraction(String value) {
        if (!value.contains("/")) {
            return new BigDecimal(value);
        }
        String[] fraction = value.split("/");
        if (fraction.length != 2) {
            throw new NumberFormatException("Invalid fraction");
        }
        return new BigDecimal(fraction[0]).divide(
                new BigDecimal(fraction[1]), SCALE, RoundingMode.HALF_UP);
    }

    private void register(MeasurementDimension dimension, String factor, String... names) {
        UnitDefinition definition = new UnitDefinition(dimension, new BigDecimal(factor));
        for (String name : names) {
            unitDefinitions.put(normalizeUnitName(name), definition);
        }
    }

    private String normalizeUnitName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    public enum MeasurementDimension {
        MASS,
        VOLUME,
        COUNT
    }

    public static class NormalizedQuantity {
        private final BigDecimal amount;
        private final MeasurementDimension dimension;

        public NormalizedQuantity(BigDecimal amount, MeasurementDimension dimension) {
            this.amount = amount;
            this.dimension = dimension;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public MeasurementDimension getDimension() {
            return dimension;
        }
    }

    private static class UnitDefinition {
        private final MeasurementDimension dimension;
        private final BigDecimal factorToBase;

        private UnitDefinition(MeasurementDimension dimension, BigDecimal factorToBase) {
            this.dimension = dimension;
            this.factorToBase = factorToBase;
        }
    }
}
