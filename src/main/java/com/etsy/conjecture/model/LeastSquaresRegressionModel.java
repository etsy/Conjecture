package com.etsy.conjecture.model;

import com.etsy.conjecture.data.LabeledInstance;
import com.etsy.conjecture.data.RealValuedLabel;
import com.etsy.conjecture.data.StringKeyedVector;

public class LeastSquaresRegressionModel extends
        UpdateableLinearModel<RealValuedLabel> {

    private static final long serialVersionUID = 1L;

    public LeastSquaresRegressionModel(SGDOptimizer optimizer) {
        super(optimizer);
    }

    public LeastSquaresRegressionModel(StringKeyedVector param, SGDOptimizer optimizer) {
        super(param, optimizer);
    }

    @Override
    public RealValuedLabel predict(StringKeyedVector instance) {
        return new RealValuedLabel(param.dot(instance));
    }

    @Override
    public double loss (LabeledInstance<RealValuedLabel> instance) {
        double label = instance.getLabel().getValue();
        double hypothesis = param.dot(instance.getVector());
        return 0.5 * (hypothesis - label) * (hypothesis - label);
    }

    @Override
    public StringKeyedVector getGradients(LabeledInstance<RealValuedLabel> instance) {
        StringKeyedVector gradients = instance.getVector().copy();
        double hypothesis = param.dot(instance.getVector());
        double label = instance.getLabel().getValue();
        gradients.mul((2 * (hypothesis-label)));
        return gradients;
    }

    @Override
    protected String getModelType() {
        return "least_squares_regression";
    }

}
