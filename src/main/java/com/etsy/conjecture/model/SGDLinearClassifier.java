package com.etsy.conjecture.model;

import static com.google.common.base.Preconditions.checkArgument;
import gnu.trove.function.TDoubleFunction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.etsy.conjecture.Utilities;
import com.etsy.conjecture.data.Label;
import com.etsy.conjecture.data.LabeledInstance;
import com.etsy.conjecture.data.LazyVector;
import com.etsy.conjecture.data.StringKeyedVector;

/**
 *  Regularized linear models with stochastic gradient descent (SGD) learning.
 *  For each training example, subclassing models estimate the gradient of the
 *  loss, and update the model params with a decreasing strength schedule (aka
 *  learning rate).
 */
public abstract class SGDLinearClassifier<L extends Label>
        extends UpdateableLinearModel<L> {

    protected LearningRateComputation rateComputer;
    protected RegularizationUpdater regularizer;

    protected SGDLinearClassifier(LearningRateComputation rateComputer, RegularizationUpdater regularizer) {
        super(regularizer);
        this.rateComputer = rateComputer;
    }

    protected SGDLinearClassifier(StringKeyedVector param, LearningRateComputation rateComputer, RegularizationUpdater regularizer) {
        super(param, regularizer);
        this.rateComputer = rateComputer;
    }

    public SGDLinearClassifier<L> setLearningRateComputation(LearningRateComputation computation) {
    	this.rateComputer = computation;
    	return this;
    }

    /**
     *  Get a StringKeyedVector holding the gradient of the loss w.r.t. every model parameter.
     */
    public abstract StringKeyedVector getGradients(LabeledInstance<L> instance, double bias);

    /**
     *  Update model weights using the gradients and the
     *  learning rate schedule specified in BinaryModelTrainer.
     */
    @Override
    public void updateRule(LabeledInstance<L> instance, double bias) {
        StringKeyedVector gradients = getGradients(instance, bias);
        Iterator it = gradients.iterator();
        while (it.hasNext()) {
            Map.Entry<String,Double> pairs = (Map.Entry)it.next();
            String feature = pairs.getKey();
            double gradient = pairs.getValue();
            double featureLearningRate = rateComputer.computeFeatureLearningRate(feature, epoch, gradient);
            param.addToCoordinate(feature, featureLearningRate * gradient);
       }
    }
}


