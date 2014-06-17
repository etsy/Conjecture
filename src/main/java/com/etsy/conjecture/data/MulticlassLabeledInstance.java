package com.etsy.conjecture.data;

import java.util.Map;

public class MulticlassLabeledInstance extends
        AbstractInstance<MulticlassLabeledInstance> implements
        LabeledInstance<MulticlassLabel> {

    protected MulticlassLabel label;

    public MulticlassLabel getLabel() {
        return label;
    }

    public MulticlassLabeledInstance(String label) {
        this(new MulticlassLabel(label), 1.0);
    }

    public MulticlassLabeledInstance(String label, double weight) {
        this(new MulticlassLabel(label), weight);
    }

    public MulticlassLabeledInstance(String label, Map<String, Double> instance) {
        this(new MulticlassLabel(label), instance, 1.0);
    }

    public MulticlassLabeledInstance(String label,
            Map<String, Double> instance, double weight) {
        this(new MulticlassLabel(label), instance, weight);
    }

    public MulticlassLabeledInstance(String label, StringKeyedVector vec) {
        this(new MulticlassLabel(label), vec.getMap(), 1.0);
    }

    public MulticlassLabeledInstance(String label, StringKeyedVector vec,
            double weight) {
        this(new MulticlassLabel(label), vec.getMap(), weight);
    }

    public MulticlassLabeledInstance(MulticlassLabel label) {
        this(label, 1.0);
    }

    public MulticlassLabeledInstance(MulticlassLabel label, double weight) {
        super(weight);
        this.label = label;
    }

    public MulticlassLabeledInstance(MulticlassLabel label,
            Map<String, Double> instance) {
        this(label, instance, 1.0);
    }

    public MulticlassLabeledInstance(MulticlassLabel label,
            Map<String, Double> instance, double weight) {
        super(instance, weight);
        this.label = label;
    }

    public MulticlassLabeledInstance(MulticlassLabel label,
            StringKeyedVector vec) {
        this(label, vec.getMap(), 1.0);
    }

    public MulticlassLabeledInstance(MulticlassLabel label,
            StringKeyedVector vec, double weight) {
        this(label, vec.getMap(), weight);
    }

    public BinaryLabeledInstance toBinaryInstance(String category) {
        double tmpLabel = 0d;
        if (category.equals(this.label.getLabel())) {
            tmpLabel = 1d;
        }
        return new BinaryLabeledInstance(tmpLabel, getVector());
    }
}
