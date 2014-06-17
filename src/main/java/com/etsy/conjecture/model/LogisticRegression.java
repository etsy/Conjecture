package com.etsy.conjecture.model;

import com.etsy.conjecture.Utilities;
import com.etsy.conjecture.data.BinaryLabel;
import com.etsy.conjecture.data.LabeledInstance;
import com.etsy.conjecture.data.StringKeyedVector;

public class LogisticRegression extends UpdateableLinearModel<BinaryLabel> {

    private static final long serialVersionUID = 1L;

    public LogisticRegression() {
        super();
    }

    public LogisticRegression(StringKeyedVector param) {
        super(param);
    }

    public BinaryLabel predict(StringKeyedVector instance, double bias) {
        return new BinaryLabel(Utilities.logistic(instance.dot(param) + bias));
    }

    public void updateRule(LabeledInstance<BinaryLabel> instance, double bias) {
        double hypothesis = Utilities.logistic(instance.getVector().dot(param)
                + bias);
        double label = instance.getLabel().getValue();
        param.addScaled(instance.getVector(), -computeLearningRate()
                * (hypothesis - label));
    }

    protected String getModelType() {
        return "logistic_regression";
    }

}
