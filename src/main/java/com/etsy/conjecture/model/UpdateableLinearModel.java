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

public abstract class UpdateableLinearModel<L extends Label> implements
        UpdateableModel<L, UpdateableLinearModel<L>>,
        Comparable<UpdateableLinearModel<L>>, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 8549108867384062857L;
    protected LazyVector param;
    protected final String modelType;

    protected long epoch;

    // parameters for gradient truncation
    // for more info, see:
    // http://jmlr.csail.mit.edu/papers/volume10/langford09a/langford09a.pdf
    private int period = 0;
    private double truncationUpdate = 0.1;
    private double truncationThreshold = 0.0;
    private SingleLearningRate truncationLearningRate; 

    private String argString = "NOT SET";

    public void setArgString(String s) {
        argString = s;
    }

    public String getArgString() {
        return argString;
    }

    public double dotWithParam(StringKeyedVector x) {
        return param.dot(x);
    }

    protected UpdateableLinearModel(RegularizationUpdater regularizer) {
        this.param = new LazyVector(100, regularizer);
        epoch = 0;
        modelType = getModelType();
    }

    protected UpdateableLinearModel(StringKeyedVector param, RegularizationUpdater regularizer) {
        this.param = new LazyVector(param, regularizer);
        epoch = 0;
        modelType = getModelType();
    }

    public abstract void updateRule(LabeledInstance<L> instance, double bias);

    public void updateRule(LabeledInstance<L> instance) {
        updateRule(instance, 0.0);
    }

    public abstract L predict(StringKeyedVector instance, double bias);

    public L predict(StringKeyedVector instance) {
        return predict(instance, 0.0);
    }

    protected abstract String getModelType();

    public Iterator<Map.Entry<String, Double>> decompose() {
        return param.iterator();
    }

    public void setParameter(String name, double value) {
        param.setCoordinate(name, value);
    }

    public StringKeyedVector getParam() {
        return param;
    }

    public void reScale(double scale) {
        param.mul(scale);
    }

    public void setFreezeFeatureSet(boolean freeze) {
        param.setFreezeKeySet(freeze);
    }

    public void update(Collection<LabeledInstance<L>> instances) {
        for (LabeledInstance<L> instance : instances) {
            update(instance);
        }
    }

    public void update(LabeledInstance<L> instance) {
        if (epoch > 0) {
            param.incrementIteration();
        }
        updateRule(instance);
        if (period > 0 && epoch > 0 && epoch % period == 0) {
            applyTruncation(instance.getVector());
        }
        epoch++;
    }

    public void update(LabeledInstance<L> instance,
            StringKeyedVector global_model) {
        if (epoch > 0) {
            param.incrementIteration();
        }
        double bias = instance.getVector().dot(global_model);
        updateRule(instance, bias);
        if (period > 0 && epoch > 0 && epoch % period == 0) {
            applyTruncation(instance.getVector());
        }
        epoch++;
    }

    public void merge(UpdateableLinearModel<L> model, double scaling) {
        param.addScaled(model.param, scaling);
        epoch += model.epoch;
    }

    private void applyTruncation(StringKeyedVector instance) {
        final double update = this.truncationLearningRate.computeLearningRate(epoch) * truncationUpdate;
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

        param.transformValues(truncFn);
        param.removeZeroCoordinates();
    }

    public long getEpoch() {
        return epoch;
    }

    public void setEpoch(long e) {
        epoch = e;
    }

    public UpdateableLinearModel<L> setTruncationPeriod(int period) {
        checkArgument(period >= 0, "period must be non-negative, given: %s",
                period);
        this.period = period;
        return this;
    }

    public UpdateableLinearModel<L> setTruncationThreshold(double threshold) {
        checkArgument(threshold >= 0, "update must be non-negative, given: %s",
                threshold);
        this.truncationThreshold = threshold;
        return this;
    }

    public UpdateableLinearModel<L> setTruncationUpdate(double update) {
        checkArgument(update >= 0, "update must be non-negative, given: %s",
                update);
        this.truncationUpdate = update;
        return this;
    }

    public UpdateableLinearModel<L> setTruncationLearningRate(SingleLearningRate rateSchedule) {
        this.truncationLearningRate = rateSchedule;
        return this;
    }

    @Override
    public int compareTo(UpdateableLinearModel<L> inputModel) {
        return (int)Math.signum(inputModel.param.LPNorm(2d) - param.LPNorm(2d));
    }

    public void thresholdParameters(double t) {
        for (Iterator<Map.Entry<String, Double>> it = param.iterator(); it
                .hasNext();) {
            if (Math.abs(it.next().getValue()) < t) {
                it.remove();
            }
        }
    }

    public String explainPrediction(StringKeyedVector x) {
        return explainPrediction(x, -1);
    }

    public String explainPrediction(StringKeyedVector x, int n) {
        StringBuilder out = new StringBuilder();
        Map<String, Double> weights = new HashMap<String, Double>();
        for (String dim : x.keySet()) {
            if (param.getCoordinate(dim) != 0.0) {
                weights.put(
                        dim,
                        Math.abs(x.getCoordinate(dim)
                                * param.getCoordinate(dim)));
            }
        }
        ArrayList<String> keys = com.etsy.conjecture.Utilities
                .orderKeysByValue(weights, true);
        for (int i = 0; (n == -1 || i < n) && i < keys.size(); i++) {
            String k = keys.get(i);
            out.append(k + ":" + String.format("%.2f", x.getCoordinate(k))
                    + "->" + String.format("%.2f", param.getCoordinate(k))
                    + " ");
        }
        return out.toString();
    }
}
