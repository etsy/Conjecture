package com.etsy.conjecture.model;

import com.etsy.conjecture.Utilities;
import com.etsy.conjecture.data.BinaryLabel;
import com.etsy.conjecture.data.LabeledInstance;
import com.etsy.conjecture.data.StringKeyedVector;

/**
 *  Logistic regression loss for binary classification with y in {-1, 1}.
 */
public class LogisticRegression extends UpdateableLinearModel<BinaryLabel> {

    private static final long serialVersionUID = 1L;

    public LogisticRegression(SGDOptimizer optimizer) {
        super(optimizer);
    }

    public LogisticRegression(StringKeyedVector param, SGDOptimizer optimizer) {
        super(param, optimizer);
    }

    @Override
    public BinaryLabel predict(StringKeyedVector instance) {
        return new BinaryLabel(Utilities.logistic(instance.dot(param)));
    }

    @Override
    public double loss(LabeledInstance<BinaryLabel> instance) {
        double inner = instance.getVector().dot(param);
        double label = instance.getLabel().getAsPlusMinus();
        return Math.log(1.0 + Math.exp(-label * inner));
    }

    @Override
    public StringKeyedVector getGradients(LabeledInstance<BinaryLabel> instance) {
        StringKeyedVector gradients = instance.getVector().copy();
        double label = instance.getLabel().getAsPlusMinus();
        double inner = instance.getVector().dot(param);
        double gradient = -label * Utilities.logistic(-label * inner);
        gradients.mul(gradient);
        return gradients;
    }

    protected String getModelType() {
        return "logistic_regression";
    }

}
