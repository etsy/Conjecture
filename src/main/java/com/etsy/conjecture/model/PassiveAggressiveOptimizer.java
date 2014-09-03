package com.etsy.conjecture.model;

import com.etsy.conjecture.data.LazyVector;
import com.etsy.conjecture.data.StringKeyedVector;
import static com.google.common.base.Preconditions.checkArgument;
import com.etsy.conjecture.Utilities;
import com.etsy.conjecture.data.Label;
import com.etsy.conjecture.data.LabeledInstance;
import com.etsy.conjecture.data.RealValuedLabel;

/**
 *  See http://eprints.pascal-network.org/archive/00002147/01/CrammerDeKeShSi06.pdf
 *  for a discussion of PA Regression.
 */
public class PassiveAggressiveOptimizer extends SGDOptimizer<RealValuedLabel> {

    private double C;
    private boolean isHinge;

    @Override
    public StringKeyedVector getUpdate(LabeledInstance<RealValuedLabel> instance) {
        double norm = instance.getVector().LPNorm(2d);
        double update = model.loss(instance) / (norm * norm + 0.5 / C);
        if(isHinge) {
            /**
             *  Classification. Scale update by label in {-1, 1}.
             */
            update = update * (2.0 * (instance.getLabel().getValue() - 0.5));
        } else if (instance.getLabel().getValue() - ((RealValuedLabel)model.predict(instance.getVector())).getValue() < 0.0) {
            /** Regression **/
            update = update * -1;
        }
        StringKeyedVector updateVec = instance.getVector().copy();
        updateVec.mul(update);
        return updateVec;
    }

    public PassiveAggressiveOptimizer setC(double C) {
        checkArgument(C > 0, "C must be greater than 0. Given: %s", C);
        this.C = C;
        return this;
    }

    public PassiveAggressiveOptimizer isHinge(boolean isHinge) {
        this.isHinge = isHinge;
        return this;
    }

}
