package com.etsy.conjecture.model;

import com.etsy.conjecture.data.LabeledInstance;
import com.etsy.conjecture.data.RealValuedLabel;
import com.etsy.conjecture.data.StringKeyedVector;

public class LeastSquaresRegressionModel extends
        SGDLinearClassifier<RealValuedLabel> {

    private static final long serialVersionUID = 1L;

    public LeastSquaresRegressionModel(LearningRateComputation rateComputer, 
                                       RegularizationUpdater regularizer) {
        super(rateComputer, regularizer);
    }

    public LeastSquaresRegressionModel(StringKeyedVector param, 
                                       LearningRateComputation rateComputer,
                                       RegularizationUpdater regularizer) {
        super(param, rateComputer, regularizer);
    }

    @Override
    public RealValuedLabel predict(StringKeyedVector instance, double bias) {
        return new RealValuedLabel(param.dot(instance) + bias);
    }

    @Override
    public StringKeyedVector getGradients(LabeledInstance<RealValuedLabel> instance,
                                          double bias) {
        StringKeyedVector gradients = instance.getVector();
        double hypothesis = param.dot(instance.getVector()) + bias;
        double label = instance.getLabel().getValue();
        gradients.mul((2 * (hypothesis-label)));
        return gradients;
    }

    @Override
    protected String getModelType() {
        return "least_squares_regression";
    }

}
