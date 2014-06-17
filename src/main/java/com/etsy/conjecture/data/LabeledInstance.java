package com.etsy.conjecture.data;

public interface LabeledInstance<L extends Label> {
    public L getLabel();

    public StringKeyedVector getVector();

    public double getWeight();
}
