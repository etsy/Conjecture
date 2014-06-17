package com.etsy.conjecture.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.etsy.conjecture.Utilities;
import com.etsy.conjecture.data.LabeledInstance;
import com.etsy.conjecture.data.MulticlassLabel;
import com.etsy.conjecture.data.MulticlassPrediction;
import com.etsy.conjecture.data.StringKeyedVector;

public class MulticlassLogisticRegression implements
        UpdateableModel<MulticlassLabel, MulticlassLogisticRegression> {

    static final long serialVersionUID = 666L;

    protected Map<String, StringKeyedVector> param = new HashMap<String, StringKeyedVector>();

    public MulticlassLogisticRegression(String[] categories) {
        for (String s : categories) {
            param.put(s, new StringKeyedVector());
        }
    }

    public MulticlassPrediction predict(StringKeyedVector instance) {
        Map<String, Double> scores = new HashMap<String, Double>();
        for (Map.Entry<String, StringKeyedVector> e : param.entrySet()) {
            scores.put(e.getKey(), e.getValue().dot(instance));
        }
        // Give scores rather than class probabilities.
        // TODO: some kind of soft-max bs.
        return new MulticlassPrediction(scores);
    }

    public void setFreezeFeatureSet(boolean freeze) {
        for (Map.Entry<String, StringKeyedVector> e : param.entrySet()) {
            e.getValue().setFreezeKeySet(freeze);
        }
    }

    public void update(Collection<LabeledInstance<MulticlassLabel>> instances) {
        for (LabeledInstance<MulticlassLabel> instance : instances) {
            update(instance);
        }
    }

    public void update(LabeledInstance<MulticlassLabel> li) {
        double normalization = 0.0;
        Map<String, Double> scores = new HashMap<String, Double>();
        for (Map.Entry<String, StringKeyedVector> e : param.entrySet()) {
            double score = Utilities.logistic(e.getValue().dot(li.getVector()));
            scores.put(e.getKey(), score);
            normalization += score;
        }
        for (Map.Entry<String, StringKeyedVector> e : param.entrySet()) {
            double label = e.getKey().equals(li.getLabel().getLabel()) ? 1.0
                    : 0.0;
            e.getValue().addScaled(li.getVector(),
                    0.01 * (label - scores.get(e.getKey()) / normalization));
        }
    }

    public void reScale(double scale) {
        for (String cat : param.keySet()) {
            param.get(cat).mul(scale);
        }
    }

    public void merge(MulticlassLogisticRegression model, double scale) {
        for (String cat : param.keySet()) {
            param.get(cat).addScaled(model.param.get(cat), scale);
        }
    }

    public Iterator<Map.Entry<String, Double>> decompose() {
        throw new UnsupportedOperationException("not done yet");
    }

    public void setParameter(String name, double value) {
        throw new UnsupportedOperationException("not done yet");
    }

    public long getEpoch() {
        return 0;
    }

    public void setEpoch(long epoch) {
        // this class doesnt care about epoch.
    }
}
