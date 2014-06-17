package com.etsy.conjecture.data;

import static com.google.common.base.Preconditions.checkArgument;

public class BinaryLabel extends RealValuedLabel {

    private static final long serialVersionUID = 1L;

    public BinaryLabel() {
        super(0.0);
    }

    public BinaryLabel(double value) {
        super(checkBinaryValue(value));

    }

    private static double checkBinaryValue(double value) {
        checkArgument(value >= 0 && value <= 1,
                "value must be in [0, 1], given: %s", value);
        return value;
    }

    // {0,+1} -> {-1,+1}
    public double getAsPlusMinus() {
        return 2.0 * (getValue() - 0.5);
    }
}
