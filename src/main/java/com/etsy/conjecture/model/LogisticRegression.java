package com.etsy.conjecture.model;

import com.etsy.conjecture.Utilities;
import com.etsy.conjecture.data.BinaryLabel;
import com.etsy.conjecture.data.LabeledInstance;
import com.etsy.conjecture.data.StringKeyedVector;

public class LogisticRegression extends SGDLinearClassifier<BinaryLabel> {

    private static final long serialVersionUID = 1L;

    public LogisticRegression(LearningRateComputation rateComputer, RegularizationUpdater regularizer) {
        super(rateComputer, regularizer);
    }

    public LogisticRegression(StringKeyedVector param, LearningRateComputation rateComputer, RegularizationUpdater regularizer) {
        super(param, rateComputer, regularizer);
    }

    public BinaryLabel predict(StringKeyedVector instance, double bias) {
        return new BinaryLabel(Utilities.logistic(instance.dot(param) + bias));
    }

    @Override
    public StringKeyedVector getGradients(LabeledInstance<BinaryLabel> instance, double bias) {
        StringKeyedVector gradients = instance.getVector();
        double hypothesis = Utilities.logistic(instance.getVector().dot(param)
                + bias);
        double label = instance.getLabel().getValue();
        gradients.mul((hypothesis-label));
        return gradients;
    }

    protected String getModelType() {
        return "logistic_regression";
    }

}
