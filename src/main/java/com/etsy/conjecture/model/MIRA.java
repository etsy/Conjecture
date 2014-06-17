package com.etsy.conjecture.model;

import com.etsy.conjecture.Utilities;
import com.etsy.conjecture.data.BinaryLabel;
import com.etsy.conjecture.data.LabeledInstance;
import com.etsy.conjecture.data.StringKeyedVector;

public class MIRA extends UpdateableLinearModel<BinaryLabel> {

    private static final long serialVersionUID = 1L;

    public MIRA() {
        super();
    }

    public MIRA(StringKeyedVector param) {
        super(param);
    }

    @Override
    public BinaryLabel predict(StringKeyedVector instance, double bias) {
        double inner = param.dot(instance);
        return new BinaryLabel(Utilities.logistic(inner + bias));
    }

    @Override
    public void updateRule(LabeledInstance<BinaryLabel> instance, double bias) {
        // Override this instead of update rule to prevent against some
        // truncation.
        double label = instance.getLabel().getAsPlusMinus();
        double prediction = param.dot(instance.getVector()) + bias;

        double loss = Math.max(0, 1d - label * prediction);
        if (loss > 0) {
            double norm = instance.getVector().LPNorm(2d);
            double tau = loss / (norm * norm);
            param.addScaled(instance.getVector(), tau * label);
        }
    }

    @Override
    protected String getModelType() {
        return "MIRA";
    }

}
