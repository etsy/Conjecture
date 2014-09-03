package com.etsy.conjecture.model;

import com.etsy.conjecture.Utilities;
import com.etsy.conjecture.data.BinaryLabel;
import com.etsy.conjecture.data.LabeledInstance;
import com.etsy.conjecture.data.StringKeyedVector;


/**
 *  Hinge loss for binary classification tasks with y in {-1,1}.
 *  When threshold=1.0, one gets the loss used by SVM.
 *  When threshold=0.0, one gets the loss used by the Perceptron.
 */
public class Hinge extends UpdateableLinearModel<BinaryLabel> {

    private static final long serialVersionUID = 1L;
    private double threshold = 0.0;

    public Hinge(SGDOptimizer optimizer) {
        super(optimizer);
    }

    public Hinge(StringKeyedVector param, SGDOptimizer optimizer) {
        super(param, optimizer);
    }

    @Override
    public BinaryLabel predict(StringKeyedVector instance) {
        double inner = param.dot(instance);
        return new BinaryLabel(Utilities.logistic(inner));
    }

    @Override
    public double loss(LabeledInstance<BinaryLabel> instance) {
        double hypothesis = Utilities.logistic(param.dot(instance.getVector()));
        double label = instance.getLabel().getAsPlusMinus();
        double z = hypothesis * label;
        if (z <= this.threshold) {
            return this.threshold - z;
        } else {
            return 0.0;
        }
    }

    @Override
    public StringKeyedVector getGradients(LabeledInstance<BinaryLabel> instance) {
        StringKeyedVector gradients = instance.getVector().copy();
        double hypothesis = (param.dot(instance.getVector()));
        double label = instance.getLabel().getAsPlusMinus();
        if (hypothesis * label <= this.threshold) {
            gradients.mul(-label);
            return gradients;
        } else {
            return new StringKeyedVector();
        }        
    }

    @Override
    protected String getModelType() {
        return "hinge";
    }

    public Hinge setThreshold(double threshold) {
        this.threshold = threshold;
        return this;
    }

}
