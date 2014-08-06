package com.etsy.conjecture.model;

import com.etsy.conjecture.Utilities;
import com.etsy.conjecture.data.BinaryLabel;
import com.etsy.conjecture.data.LabeledInstance;
import com.etsy.conjecture.data.StringKeyedVector;

public class Perceptron extends UpdateableLinearModel<BinaryLabel> {

    private static final long serialVersionUID = 1L;

    public Perceptron(SGDOptimizer optimizer) {
        super(optimizer);
    }

    public Perceptron(StringKeyedVector param, SGDOptimizer optimizer) {
        super(param, optimizer);
    }

    @Override
    public BinaryLabel predict(StringKeyedVector instance, double bias) {
        double inner = param.dot(instance);
        return new BinaryLabel(Utilities.logistic(inner + bias));
    }

    @Override
    public StringKeyedVector getGradients(LabeledInstance<BinaryLabel> instance, double bias) {
        StringKeyedVector gradients = instance.getVector();
        double hypothesis = (param.dot(instance.getVector()) + bias);
        double label = instance.getLabel().getAsPlusMinus();
        if (hypothesis * label < 1.0) {
            gradients.mul(label);
            return gradients;
        } else {
            return new StringKeyedVector();
        }        
    }

    @Override
    protected String getModelType() {
        return "perceptron";
    }

}
