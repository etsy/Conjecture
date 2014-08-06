package com.etsy.conjecture.model;

import com.etsy.conjecture.data.LazyVector;
import com.etsy.conjecture.data.StringKeyedVector;
import static com.google.common.base.Preconditions.checkArgument;
import com.etsy.conjecture.Utilities;

/**
 *  AdaGrad provides adaptive per-feature learning rates at each time step t.
 *  Described here: http://www.ark.cs.cmu.edu/cdyer/adagrad.pdf
 */
public class AdagradOptimizer extends SGDOptimizer {

    private StringKeyedVector unnormalizedGradients = new StringKeyedVector();
    private StringKeyedVector gradients = new StringKeyedVector();

    /**
     *  Update adaptive feature specific learning rates
     */
    public double updateAndGetFeatureLearningRate(String feature, long t, double gradient) {
        double gradUpdate = 0.0;
        if (gradients.containsKey(feature)) {
            gradUpdate = gradient * gradient;
        } else {
            /**
             *  Unmentioned in the literature, but initializing
             *  the squared gradient at 1.0 rather than 0.0
             *  helps avoid oscillation.
             */
            gradUpdate = 1d+(gradient * gradient);
        }
        gradients.addToCoordinate(feature, gradUpdate);
        unnormalizedGradients.addToCoordinate(feature, gradient);
        return initialLearningRate/Math.sqrt(gradients.getCoordinate(feature));
    }

    public double getFeatureLearningRate(String feature, long t) {
        return initialLearningRate/Math.sqrt(gradients.getCoordinate(feature));
    }

    /**
     *  Lazily calculates and applies the update that minimizes the l1
     *  regularized objective. See "Adding l1 regularization" in
     *  http://www.ark.cs.cmu.edu/cdyer/adagrad.pdf
     */
    public double lazyUpdate(String feature, double param, long start, long end) {
        if (Utilities.floatingPointEquals(laplace, 0.0d)) {
            return param;
        }
        for (long iter = start + 1; iter <= end; iter++) {
            if (Utilities.floatingPointEquals(param, 0.0d)) {
                return 0.0d;
            }
            double eta = getFeatureLearningRate(feature, iter);
            double u = unnormalizedGradients.getCoordinate(feature);
            double normalizedGradient = u/iter;
            if (Math.abs(normalizedGradient) <= laplace) {
                param = 0.0;
            } else {
                param = param - (Math.signum(u) * eta * (normalizedGradient - laplace));
            }
        }
        return param;
    }

    @Override
    public double getUpdate(String feature, long epoch, double gradient) {
        double featureLearningRate = updateAndGetFeatureLearningRate(feature, epoch, gradient);
        return gradient * featureLearningRate;
    }
}
