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
    public BinaryLabel predict(StringKeyedVector instance, double bias) {
        double inner = param.dot(instance);
        return new BinaryLabel(Utilities.logistic(inner + bias));
    }

    // @Override
    // public void updateRule(LabeledInstance<BinaryLabel> instance, double bias) {
    //     // Override this instead of update rule to prevent against some
    //     // truncation.
    //     double label = instance.getLabel().getAsPlusMinus();
    //     double prediction = param.dot(instance.getVector()) + bias;

    //     double loss = Math.max(0, 1d - label * prediction);
    //     if (loss > 0) {
    //         double norm = instance.getVector().LPNorm(2d);
    //         double tau = loss / (norm * norm);
    //         param.addScaled(instance.getVector(), tau * label);
    //     }
    // }

    @Override
    public StringKeyedVector getGradients(LabeledInstance<BinaryLabel> instance, double bias) {
        double label = instance.getLabel().getAsPlusMinus();
        double prediction = param.dot(instance.getVector()) + bias;

        double loss = Math.max(0, 1d - label * prediction);
        if (loss > 0) {
            double norm = instance.getVector().LPNorm(2d);
            double tau = loss / (norm * norm);
            StringKeyedVector gradients = instance.getVector();
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
