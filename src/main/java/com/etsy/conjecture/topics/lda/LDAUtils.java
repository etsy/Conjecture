package com.etsy.conjecture.topics.lda;

import java.io.Serializable;

public class LDAUtils implements Serializable {

    private static final long serialVersionUID = -1142647262716539345L;

    public static double digamma(double x) {
        if (x > 6.0) {
            double x2 = x * x;
            double x4 = x2 * x2;
            double x6 = x2 * x4;
            double x8 = x4 * x4;
            double x10 = x6 * x4;
            double x12 = x6 * x6;
            double x14 = x10 * x4;
            return Math.log(x) - 1.0 / (2 * x) - 1.0 / (12 * x2) - 1.0
                    / (120 * x4) - 1.0 / (252 * x6) + 1.0 / (240 * x8) - 5.0
                    / (660 * x10) + 691.0 / (32760 * x12) - 1.0 / (12 * x14);
        } else {
            return digamma(x + 1.0) - (1.0 / x);
        }
    }

    public static double logSumExp(double a, double b) {
        double x = (a < b) ? a : b;
        double y = (a < b) ? b : a;
        if (y - x > 50) {
            return y;
        } else {
            return x + Math.log(1.0 + Math.exp(y - x));
        }
    }
}
