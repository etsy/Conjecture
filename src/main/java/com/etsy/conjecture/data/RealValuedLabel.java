package com.etsy.conjecture.data;

public class RealValuedLabel extends Label {

    protected final Double value;
    private static final long serialVersionUID = -1L;

    public RealValuedLabel(double value) {
        this.value = new Double(value);
    }

    public Double getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return value + "";
    }
}
