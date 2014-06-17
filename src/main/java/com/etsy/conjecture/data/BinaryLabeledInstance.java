package com.etsy.conjecture.data;

import java.util.Map;

/**
 * TODO: when using method string all methods return a RealValueLabeledInstance
 * think about how to avoid this while not using generic types
 */
public class BinaryLabeledInstance extends
        AbstractInstance<BinaryLabeledInstance> implements
        LabeledInstance<BinaryLabel> {

    protected BinaryLabel label;

    public BinaryLabel getLabel() {
        return label;
    }

    public BinaryLabeledInstance() {
        this(new BinaryLabel(0.0), 1.0);
    }

    public BinaryLabeledInstance(double label, Map<String, Double> instance) {
        this(new BinaryLabel(label), instance, 1.0);
    }

    public BinaryLabeledInstance(double label, Map<String, Double> instance,
            double weight) {
        this(new BinaryLabel(label), instance, weight);
    }

    public BinaryLabeledInstance(double label, StringKeyedVector vec) {
        this(new BinaryLabel(label), vec.getMap(), 1.0);
    }

    public BinaryLabeledInstance(double label, StringKeyedVector vec,
            double weight) {
        this(new BinaryLabel(label), vec.getMap(), weight);
    }

    public BinaryLabeledInstance(BinaryLabel label, Map<String, Double> instance) {
        this(label, instance, 1.0);
    }

    public BinaryLabeledInstance(BinaryLabel label,
            Map<String, Double> instance, double weight) {
        super(instance, weight);
        this.label = label;
    }

    public BinaryLabeledInstance(BinaryLabel label, StringKeyedVector vec) {
        this(label, vec.getMap(), 1.0);
    }

    public BinaryLabeledInstance(BinaryLabel label, StringKeyedVector vec,
            double weight) {
        this(label, vec.getMap(), weight);
    }

    public BinaryLabeledInstance(double label) {
        this(new BinaryLabel(label), 1.0);
    }

    public BinaryLabeledInstance(double label, double weight) {
        this(new BinaryLabel(label), weight);
    }

    public BinaryLabeledInstance(BinaryLabel label) {
        this(label, 1.0);
    }

    public BinaryLabeledInstance(BinaryLabel label, double weight) {
        super(weight);
        this.label = label;
    }

}
