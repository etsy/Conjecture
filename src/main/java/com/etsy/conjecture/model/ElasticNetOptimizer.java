package com.etsy.conjecture.model;

import com.etsy.conjecture.data.LazyVector;
import com.etsy.conjecture.Utilities;


public class ElasticNetOptimizer extends SGDOptimizer {

    /**
     *  Since learning rate is independent of feature and gradient,
     *  these params are ignored.
     */
    @Override
    public double getUpdate(String feature, long epoch, double gradient) {
        double featureLearningRate = getDecreasingLearningRate(epoch);
        return gradient * featureLearningRate;
    }

    /**
     *  Lazily calculates and applies the update that minimizes a combination
     *  of a l1 and l2 regularization objective, aka elastic net.
     */
    public double lazyUpdate(String feature, double param, long start, long end) {
        if (Utilities.floatingPointEquals(laplace, 0.0d)
            && Utilities.floatingPointEquals(gaussian, 0.0d)) {
            return param;
        }
        for (long iter = start + 1; iter <= end; iter++) {
            if (Utilities.floatingPointEquals(param, 0.0d)) {
                return 0.0d;
            }
            double eta = getDecreasingLearningRate(iter);
            /**
             * TODO: patch so that param cannot cross 0.0 during gaussian update
             */
            param -= eta * gaussian * param;
            if (param > 0.0) {
                param = Math.max(0.0, param - eta * laplace);
            } else {
                param = Math.min(0.0, param + eta * laplace);
            }
        }
        return param;
    }
}
