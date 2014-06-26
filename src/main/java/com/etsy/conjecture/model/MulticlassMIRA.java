package com.etsy.conjecture.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.etsy.conjecture.data.LabeledInstance;
import com.etsy.conjecture.data.MulticlassLabel;
import com.etsy.conjecture.data.MulticlassPrediction;
import com.etsy.conjecture.data.StringKeyedVector;
import com.etsy.conjecture.data.LazyVector;

public class MulticlassMIRA extends UpdateableMulticlassLinearModel {

    static final long serialVersionUID = 666L;

    public MulticlassMIRA(String[] categories) {
        super(categories);
    }

    @Override
    public MulticlassPrediction predict(StringKeyedVector instance) {
        Map<String, Double> scores = new HashMap<String, Double>();
        for (Map.Entry<String, LazyVector> e : param.entrySet()) {
            scores.put(e.getKey(), e.getValue().dot(instance));
        }
        // Give scores rather than class probabilities.
        // TODO: some kind of soft-max bs.
        return new MulticlassPrediction(scores);
    }

    @Override
    public void updateRule(LabeledInstance<MulticlassLabel> li) {
        MulticlassPrediction pred = predict(li.getVector());
        String ltrue = li.getLabel().getLabel();
        String lpred = pred.getLabel();
        if (!ltrue.equals(lpred)) {
            // Update both models involved.
            // make the smallest update to each parameter vector so that
            // the score of the true label >= score of the preicted label + 1
            double loss = pred.getMap().get(lpred) - pred.getMap().get(ltrue)
                    + 1.0;
            double norm = li.getVector().LPNorm(2d);
            double tau = loss / (2.0 * norm * norm);
            param.get(ltrue).addScaled(li.getVector(), tau);
            param.get(lpred).addScaled(li.getVector(), -tau);
        }
    }

    @Override
    public String getModelType() {
        return "multiclass_mira";
    }
}
