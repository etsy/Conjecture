package com.etsy.conjecture.model;

import com.etsy.conjecture.Utilities;
import com.etsy.conjecture.data.BinaryLabel;
import com.etsy.conjecture.data.LabeledInstance;
import com.etsy.conjecture.data.StringKeyedVector;

public class Perceptron extends UpdateableLinearModel<BinaryLabel> {

    private static final long serialVersionUID = 1L;

    public Perceptron() {
        super();
    }

    public Perceptron(StringKeyedVector param) {
        super(param);
    }

    @Override
    public BinaryLabel predict(StringKeyedVector instance, double bias) {
        double inner = param.dot(instance);
        return new BinaryLabel(Utilities.logistic(inner + bias));
    }

    /**
     * TODO: some annealing schedule on the learning rate.
     */
    @Override
    public void updateRule(LabeledInstance<BinaryLabel> instance, double bias) {
        double label = instance.getLabel().getAsPlusMinus();
        if ((param.dot(instance.getVector()) + bias) * label < 1.0) {
            param.addScaled(instance.getVector(), computeLearningRate() * label);
        }

    }

    @Override
    protected String getModelType() {
        return "perceptron";
    }

}
