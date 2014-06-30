package com.etsy.conjecture.model;

import com.etsy.conjecture.data.LazyVector;
import com.etsy.conjecture.data.StringKeyedVector;
import static com.google.common.base.Preconditions.checkArgument;
import com.etsy.conjecture.Utilities;
import com.etsy.conjecture.data.LabeledInstance;
import java.util.Map;
import java.util.Iterator;

/**
 *  Implements  FTRL-Proximal online learning as described 
 *  here: http://static.googleusercontent.com/media/research.google.com/en/us/pubs/archive/41159.pdf
 */
public class FTRLOptimizer extends SGDOptimizer {

    private double alpha;
    private double beta;
    private StringKeyedVector z = new StringKeyedVector();
    private StringKeyedVector n = new StringKeyedVector();

    @Override
    public StringKeyedVector getUpdate(LabeledInstance instance) {
        FTRLRegularization(instance);
        StringKeyedVector gradients = model.getGradients(instance);
        Iterator it = gradients.iterator();
        while (it.hasNext()) {
            Map.Entry<String,Double> pairs = (Map.Entry)it.next();
            String feature = pairs.getKey();
            double gradient = pairs.getValue();
            double eta = getFeatureLearningRate(feature, gradient);
            double z_i = 0.0; // if first round, set z_i to 0.0
            if (z.containsKey(feature)) {
                z_i = z.getCoordinate(feature);
            }
            double update = (z_i + gradient) - eta * model.param.getCoordinate(feature);
            z.setCoordinate(feature, update);
            double n_i = 0.0; // if first round, set n_i to 0.0
            if (n.containsKey(feature)) {
                n_i = n.getCoordinate(feature);
            }
            n.setCoordinate(feature, n_i + gradient * gradient);
       }
       return new StringKeyedVector(); // Model updates happen in the FTRLRegularization step
    }

    public double getFeatureLearningRate(String feature, double gradient) {
        double n_i = 0.0;
        if (n.containsKey(feature)) {
            n_i = n.getCoordinate(feature);
        }
        return 1d/alpha * (Math.sqrt(n_i + gradient * gradient) - Math.sqrt(n_i));
    }


    public void FTRLRegularization(LabeledInstance instance) {
        Iterator it = instance.getVector().iterator();
        while (it.hasNext()) {
            Map.Entry<String,Double> pairs = (Map.Entry)it.next();
            String feature = pairs.getKey();
            double value = pairs.getValue();
            double regularizedWeight = getRegularizedWeight(feature);
            model.param.setCoordinate(feature, regularizedWeight);
       }
    }

    /**
     *  If z doesn't contain the key, it's initialized at 0.0
     *  and therefore less than laplace which is always >= 0.0
     */
    public double getRegularizedWeight(String feature) {
        if (z.containsKey(feature)){
            double z_i = z.getCoordinate(feature);
            if (Math.abs(z_i) <= laplace) {
                return 0.0d;
            } else {
                double n_i = n.getCoordinate(feature);
                double w_i = -(((beta + Math.sqrt(n_i))/alpha) + gaussian) * (z_i - Math.signum(z_i) * laplace);
                return w_i;
            }
        } else {
            return 0.0;
        }
    }

    /**
     *  Since we can do sparse regularization updates, lazyUpdate
     *  does nothing and just returns the feature param.
     */
    @Override
    public double lazyUpdate(String feature, double param, long start, long end) {
        return param;
    }

    public FTRLOptimizer setAlpha(double alpha) {
        checkArgument(alpha > 0, "alpha must be greater than 0. Given: %s", alpha);
        this.alpha = alpha;
        return this;
    }

    public FTRLOptimizer setBeta(double beta) {
        checkArgument(beta > 0, "beta must be greater than 0. Given: %s", beta);
        this.beta = beta;
        return this;
    }

}
