package br.com.wallet.project.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtil {
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_DOWN;

    private MoneyUtil() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    public static BigDecimal format(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING_MODE);
    }
}