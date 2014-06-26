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
import com.etsy.conjecture.data.MulticlassLabel;
import com.etsy.conjecture.data.LabeledInstance;
import com.etsy.conjecture.data.LazyVector;
import com.etsy.conjecture.data.StringKeyedVector;

public abstract class UpdateableMulticlassLinearModel implements
    UpdateableModel<MulticlassLabel, UpdateableMulticlassLinearModel>,
    Comparable<UpdateableMulticlassLinearModel>, Serializable {

    private static final long serialVersionUID = 8549108867384062857L;
    protected final String modelType;

    // parameters for gradient truncation
    // for more info, see:
    // http://jmlr.csail.mit.edu/papers/volume10/langford09a/langford09a.pdf
    private int period = 0;
    private double truncationUpdate = 0.1;
    private double truncationThreshold = 0.0;

    private String argString = "NOT SET";

    protected long epoch;

    protected Map<String, LazyVector> param = new HashMap<String, LazyVector>();

    protected LearningRateComputation rateComputer = new LearningRateComputation();
    protected RegularizationUpdater regularizer = new RegularizationUpdater(
            rateComputer);

    protected UpdateableMulticlassLinearModel(String[] categories) {
        for (String category : categories) {
            this.param.put(category, new LazyVector(100, regularizer));
        }

        this.epoch = 0;
        this.modelType = this.getModelType();
    }

    public void setArgString(String s) {
        argString = s;
    }

    public String getArgString() {
        return argString;
    }

    public abstract void updateRule(LabeledInstance<MulticlassLabel> instance);

    protected abstract String getModelType();

    public Iterator<Map.Entry<String, Double>> decompose() {
        throw new UnsupportedOperationException("not done yet");
    }

    public void setParameter(String name, double value) {
        throw new UnsupportedOperationException("not done yet");
    }

    public void reScale(double scale) {
        for (String cat : param.keySet()) {
            param.get(cat).mul(scale);
        }
    }

    public void setFreezeFeatureSet(boolean freeze) {
        for (Map.Entry<String, LazyVector> e : param.entrySet()) {
            e.getValue().setFreezeKeySet(freeze);
        }
    }

    public void update(Collection<LabeledInstance<MulticlassLabel>> instances) {
        for (LabeledInstance<MulticlassLabel> instance : instances) {
            update(instance);
        }
    }

    public void update(LabeledInstance<MulticlassLabel> instance) {
        if (epoch > 0) {
            for (LazyVector vec : param.values()) {
                vec.incrementIteration();
            }
        }

        updateRule(instance);

        if (period > 0 && epoch > 0 && epoch % period == 0) {
            applyTruncation(instance.getVector());
        }
        epoch++;
    }

    public void merge(UpdateableMulticlassLinearModel model, double scale) {
        for (String cat : param.keySet()) {
            param.get(cat).addScaled(model.param.get(cat), scale);
        }
        epoch += model.epoch;
    }

    public double computeLearningRate() {
        return rateComputer.computeLearningRate(epoch);
    }

    private void applyTruncation(StringKeyedVector instance) {
        final double update = computeLearningRate() * truncationUpdate;
        final double threshold = truncationThreshold;

        TDoubleFunction truncFn = new TDoubleFunction() {
            public double execute(double parameter) {
                if (parameter > 0 && parameter < threshold) {
                    return Math.max(0, parameter - update);
                } else if (parameter < 0 && parameter > -threshold) {
                    return Math.min(0, parameter + update);
                } else {
                    return parameter;
                }
            }
        };

        for(LazyVector vec : param.values()) {
            vec.transformValues(truncFn);
            vec.removeZeroCoordinates();
        }
    }

    public long getEpoch() {
        return epoch;
    }

    public void setEpoch(long e) {
        epoch = e;
    }

    public UpdateableMulticlassLinearModel setInitialLearningRate(double rate) {
        checkArgument(rate > 0, "period must be positive, given: %s", rate);
        rateComputer.initialLearningRate = rate;
        return this;
    }

    public UpdateableMulticlassLinearModel setUseExponentialLearningRate(boolean b) {
        rateComputer.useExponentialLearningRate = b;
        return this;
    }

    public UpdateableMulticlassLinearModel setExponentialLearningRateBase(double base) {
        checkArgument(base > 0,
                "exponential learning rate base must be positive, given: %f",
                base);
        checkArgument(
                base <= 1.0,
                "exponential learning rate base must be at most 1.0, given: %f",
                base);
        rateComputer.exponentialLearningRateBase = base;
        return this;
    }

    public UpdateableMulticlassLinearModel setExamplesPerEpoch(double examples) {
        checkArgument(examples > 0,
                "examples per epoch musht be positive, given %f", examples);
        rateComputer.examplesPerEpoch = examples;
        return this;
    }

    public UpdateableMulticlassLinearModel setTruncationPeriod(int period) {
        checkArgument(period >= 0, "period must be non-negative, given: %s",
                period);
        this.period = period;
        return this;
    }

    public UpdateableMulticlassLinearModel setTruncationThreshold(double threshold) {
        checkArgument(threshold >= 0, "update must be non-negative, given: %s",
                threshold);
        this.truncationThreshold = threshold;
        return this;
    }

    public UpdateableMulticlassLinearModel setTruncationUpdate(double update) {
        checkArgument(update >= 0, "update must be non-negative, given: %s",
                update);
        this.truncationUpdate = update;
        return this;
    }

    public UpdateableMulticlassLinearModel setLaplaceRegularizationWeight(double weight) {
        regularizer.laplace = weight;
        return this;
    }

    public UpdateableMulticlassLinearModel setGaussianRegularizationWeight(
            double weight) {
        regularizer.gaussian = weight;
        return this;
    }

    // what to do here?
    @Override
    public int compareTo(UpdateableMulticlassLinearModel inputModel) {
        return (int)Math.signum(inputModel.getEpoch() - getEpoch());
    }

    public void thresholdParameters(double t) {
        for (LazyVector vec : param.values()) {
            for (Iterator<Map.Entry<String, Double>> it = vec.iterator(); it
                     .hasNext();) {
                if (Math.abs(it.next().getValue()) < t) {
                    it.remove();
                }
            }
        }
    }

    public String explainPrediction(StringKeyedVector x) {
        return explainPrediction(x, -1);
    }

    public String explainPrediction(StringKeyedVector x, int n) {
        throw new UnsupportedOperationException("not done yet");
    }
}
