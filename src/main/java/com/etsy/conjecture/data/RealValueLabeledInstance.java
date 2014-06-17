package com.etsy.conjecture.data;

import java.util.Map;

public class RealValueLabeledInstance extends
        AbstractInstance<RealValueLabeledInstance> implements
        LabeledInstance<RealValuedLabel> {

    private final RealValuedLabel label;

    public RealValuedLabel getLabel() {
        return label;
    }

    public RealValueLabeledInstance() {
        this(0.0);
    }

    public RealValueLabeledInstance(RealValuedLabel label) {
        this(label, 1.0);
    }

    public RealValueLabeledInstance(RealValuedLabel label, double weight) {
        super(weight);
        this.label = label;
    }

    public RealValueLabeledInstance(double label) {
        this(new RealValuedLabel(label), 1.0);
    }

    public RealValueLabeledInstance(double label, double weight) {
        this(new RealValuedLabel(label), weight);
    }

    public RealValueLabeledInstance(double label, Map<String, Double> instance) {
        this(new RealValuedLabel(label), instance, 1.0);
    }

    public RealValueLabeledInstance(double label, Map<String, Double> instance,
            double weight) {
        this(new RealValuedLabel(label), instance, weight);
    }

    public RealValueLabeledInstance(double label, StringKeyedVector vec) {
        this(new RealValuedLabel(label), vec.getMap(), 1.0);
    }

    public RealValueLabeledInstance(double label, StringKeyedVector vec,
            double weight) {
        this(new RealValuedLabel(label), vec.getMap(), weight);
    }

    public RealValueLabeledInstance(RealValuedLabel label,
            Map<String, Double> instance) {
        this(label, instance, 1.0);
    }

    public RealValueLabeledInstance(RealValuedLabel label,
            Map<String, Double> instance, double weight) {
        super(instance, weight);
        this.label = label;
    }

    public RealValueLabeledInstance(RealValuedLabel label, StringKeyedVector vec) {
        this(label, vec, 1.0);
    }

    public RealValueLabeledInstance(RealValuedLabel label,
            StringKeyedVector vec, double weight) {
        super(vec.getMap(), weight);
        this.label = label;
    }

}
