package com.etsy.conjecture.model;

import com.etsy.conjecture.data.LabeledInstance;
import com.etsy.conjecture.data.RealValuedLabel;
import com.etsy.conjecture.data.StringKeyedVector;

public class LeastSquaresRegressionModel extends
        UpdateableLinearModel<RealValuedLabel> {

    private static final long serialVersionUID = 1L;

    public LeastSquaresRegressionModel() {
        super();
    }

    public LeastSquaresRegressionModel(StringKeyedVector param) {
        super(param);
    }

    @Override
    public RealValuedLabel predict(StringKeyedVector instance, double bias) {
        return new RealValuedLabel(param.dot(instance) + bias);
    }

    @Override
    public void updateRule(LabeledInstance<RealValuedLabel> instance,
            double bias) {
        double prediction = param.dot(instance.getVector()) + bias;
        double label = instance.getLabel().getValue();
        param.addScaled(instance.getVector(), 2 * computeLearningRate()
                * (prediction - label));
    }

    @Override
    protected String getModelType() {
        return "least_squares_regression";
    }

}
