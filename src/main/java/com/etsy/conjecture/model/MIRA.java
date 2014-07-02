package com.etsy.conjecture.model;

import com.etsy.conjecture.Utilities;
import com.etsy.conjecture.data.BinaryLabel;
import com.etsy.conjecture.data.LabeledInstance;
import com.etsy.conjecture.data.StringKeyedVector;

public class MIRA extends UpdateableLinearModel<BinaryLabel> {

    private static final long serialVersionUID = 1L;

    public MIRA(SGDOptimizer optimizer) {
        super(optimizer);
    }

    public MIRA(StringKeyedVector param, SGDOptimizer optimizer) {
        super(param, optimizer);
    }

    @Override
    public double loss(LabeledInstance<BinaryLabel> instance) {
        double label = instance.getLabel().getAsPlusMinus();
        double prediction = param.dot(instance.getVector());
        double loss = Math.max(0, 1d - label * prediction);
        return loss;
    }

    @Override
    public BinaryLabel predict(StringKeyedVector instance) {
        double inner = param.dot(instance);
        return new BinaryLabel(Utilities.logistic(inner));
    }

    @Override
    public StringKeyedVector getGradients(LabeledInstance<BinaryLabel> instance) {
        StringKeyedVector gradients = instance.getVector();
        double label = instance.getLabel().getAsPlusMinus();
        double prediction = param.dot(instance.getVector());
        double loss = Math.max(0, 1d - label * prediction);
        if (loss > 0) {
            double norm = instance.getVector().LPNorm(2d);
            double tau = loss / (norm * norm);
            gradients.mul(tau * label);
            return gradients;
        } else {
        	return new StringKeyedVector();
        }
    }

    @Override
    protected String getModelType() {
        return "MIRA";
    }

}