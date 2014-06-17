package com.etsy.conjecture.data;

public class InstanceFactory {

    private InstanceFactory() {
    };

    public static Instance buildInstance() {
        return new Instance();
    }

    public static Instance copyInstance(Instance inst) {
        return new Instance(inst.getVector());
    }

    public static BinaryLabeledInstance toBinaryLabeledInstance(double label,
            Instance instance) {
        return new BinaryLabeledInstance(label, instance.getVector());
    }

    public static BinaryLabeledInstance toBinaryLabeledInstance(
            BinaryLabel label, Instance instance) {
        return new BinaryLabeledInstance(label, instance.getVector());
    }

    public static RealValueLabeledInstance toRealValueLabeledInstance(
            double label, Instance instance) {
        return new RealValueLabeledInstance(label, instance.getVector());
    }

    public static RealValueLabeledInstance toRealValueLabeledInstance(
            RealValuedLabel label, Instance instance) {
        return new RealValueLabeledInstance(label, instance.getVector());
    }
}
