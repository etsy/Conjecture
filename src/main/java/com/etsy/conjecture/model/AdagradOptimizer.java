package com.etsy.conjecture.model;

import com.etsy.conjecture.data.LazyVector;
import com.etsy.conjecture.data.StringKeyedVector;
import static com.google.common.base.Preconditions.checkArgument;
import com.etsy.conjecture.Utilities;
import com.etsy.conjecture.data.LabeledInstance;
import java.util.Map;
import java.util.Iterator;

/**
 *  AdaGrad provides adaptive per-feature learning rates at each time step t.
 *  Described here: http://www.ark.cs.cmu.edu/cdyer/adagrad.pdf
 */
public class AdagradOptimizer extends SGDOptimizer {

    private StringKeyedVector unnormalizedGradients = new StringKeyedVector();
    private StringKeyedVector gradients = new StringKeyedVector();

    @Override
    public StringKeyedVector getUpdate(LabeledInstance instance) {
        StringKeyedVector gradients = model.getGradients(instance, 0.0);
        Iterator it = gradients.iterator();
        while (it.hasNext()) {
            Map.Entry<String,Double> pairs = (Map.Entry)it.next();
            String feature = pairs.getKey();
            double gradient = pairs.getValue();
            double featureLearningRate = updateAndGetFeatureLearningRate(feature, gradient);
            gradients.setCoordinate(feature, gradient * featureLearningRate);
       }
       return gradients;
    }

    /**
     *  Update adaptive feature specific learning rates
     */
    public double updateAndGetFeatureLearningRate(String feature, double gradient) {
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
        return getFeatureLearningRate(feature);
    }

    public double getFeatureLearningRate(String feature) {
        return initialLearningRate/Math.sqrt(gradients.getCoordinate(feature));
    }

    /**
     *  Overrides the lazy l1 and l2 regularization in the base class
     *  to do adagrad l1.
     *
     *  Lazily calculates and applies the update that minimizes the l1
     *  regularized objective. See "Adding l1 regularization" in
     *  http://www.ark.cs.cmu.edu/cdyer/adagrad.pdf
     */
    @Override
    public double lazyUpdate(String feature, double param, long start, long end) {
        if (Utilities.floatingPointEquals(laplace, 0.0d)) {
            return param;
        }
        for (long iter = start + 1; iter <= end; iter++) {
            if (Utilities.floatingPointEquals(param, 0.0d)) {
                return 0.0d;
            }
            double eta = getFeatureLearningRate(feature);
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
}
