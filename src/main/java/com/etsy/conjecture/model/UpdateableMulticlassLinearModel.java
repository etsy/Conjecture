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
import com.etsy.conjecture.data.BinaryLabeledInstance;
import com.etsy.conjecture.data.MulticlassLabeledInstance;
import com.etsy.conjecture.data.MulticlassPrediction;
import com.etsy.conjecture.data.LazyVector;
import com.etsy.conjecture.data.StringKeyedVector;
import com.etsy.conjecture.data.RealValuedLabel;
import com.etsy.conjecture.data.BinaryLabel;

public class UpdateableMulticlassLinearModel implements
    UpdateableModel<MulticlassLabel, UpdateableMulticlassLinearModel>,
    Comparable<UpdateableMulticlassLinearModel>, Serializable {

    private static final long serialVersionUID = 8549108867384062857L;
    protected String modelType;
    private Map<String, UpdateableLinearModel<BinaryLabel>> categoryModels;

    private String argString = "NOT SET";

    protected long epoch;

    protected Map<String, UpdateableLinearModel<BinaryLabel>> param = new HashMap<String, UpdateableLinearModel<BinaryLabel>>();

    public UpdateableMulticlassLinearModel(Map<String, UpdateableLinearModel<BinaryLabel>> categoryModels) {
        this.categoryModels = categoryModels;
        this.epoch = 0;
        this.modelType = this.getModelType();
    }

    public void setArgString(String s) {
        argString = s;
    }

    public String getArgString() {
        return argString;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    public String getModelType() {
        return modelType;
    }

    public Iterator<Map.Entry<String, Double>> decompose() {
        throw new UnsupportedOperationException("not done yet");
    }

    public void setParameter(String name, double value) {
        throw new UnsupportedOperationException("not done yet");
    }

    public void reScale(double scale) {
        for (String cat : param.keySet()) {
            param.get(cat).param.mul(scale);
        }
    }

    public void setFreezeFeatureSet(boolean freeze) {
        for (Map.Entry<String, UpdateableLinearModel<BinaryLabel>> e : param.entrySet()) {
            e.getValue().param.setFreezeKeySet(freeze);
        }
    }

    /**
     *  Minibatch gradient update
     */
    public void update(Collection<LabeledInstance<MulticlassLabel>> instances) {
        for (LabeledInstance<MulticlassLabel> instance : instances) {
            update(instance);
        }
    }

    /**
     *  Single gradient update.
     */
    public void update(LabeledInstance<MulticlassLabel> instance) {
        for (Map.Entry<String, UpdateableLinearModel<BinaryLabel>> e : param.entrySet()) {
            String category = e.getKey();
            UpdateableLinearModel<BinaryLabel> model = e.getValue();
            double label = e.getKey().equals(instance.getLabel().getLabel()) ? 1.0 : 0.0;
            BinaryLabeledInstance blInstance = new BinaryLabeledInstance(label, instance.getVector());
            model.update(blInstance);
        }
        epoch++;
    }

    @Override
    public MulticlassPrediction predict(StringKeyedVector instance) {
        Map<String, Double> scores = new HashMap<String, Double>();
        double normalization = 0;

        for (Map.Entry<String, UpdateableLinearModel<BinaryLabel>> e : param.entrySet()) {
            double prediction = ((RealValuedLabel)e.getValue().predict(instance)).getValue();
            scores.put(e.getKey(), prediction);
            normalization += prediction;
        }

        for (Map.Entry<String, Double> e : scores.entrySet()) {
            scores.put(e.getKey(), e.getValue() / normalization);
        }

        return new MulticlassPrediction(scores);
    }

    public void merge(UpdateableMulticlassLinearModel model, double scale) {
        for (String cat : param.keySet()) {
            param.get(cat).param.addScaled(model.param.get(cat).param, scale);
        }
        epoch += model.epoch;
    }

    public void teardown() {
        for (Map.Entry<String, UpdateableLinearModel<BinaryLabel>> e : param.entrySet()) {
            e.getValue().teardown();
        }
    }

    public long getEpoch() {
        return epoch;
    }

    public void setEpoch(long e) {
        epoch = e;
    }

    // what to do here?
    @Override
    public int compareTo(UpdateableMulticlassLinearModel inputModel) {
        return (int)Math.signum(inputModel.getEpoch() - getEpoch());
    }

    public void thresholdParameters(double t) {
        for (UpdateableLinearModel<BinaryLabel> m : param.values()) {
            for (Iterator<Map.Entry<String, Double>> it = m.param.iterator(); it
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
