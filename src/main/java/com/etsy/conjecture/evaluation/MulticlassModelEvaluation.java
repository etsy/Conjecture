package com.etsy.conjecture.evaluation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.etsy.conjecture.GenericPair;
import com.etsy.conjecture.data.MulticlassLabel;
import com.etsy.conjecture.data.MulticlassPrediction;

/**
 * a basic container for evaluations TODO: add getters for individual metrics
 */

public class MulticlassModelEvaluation implements Serializable,
        ModelEvaluation<MulticlassLabel> {


    /**
     * 
     */
    private static final long serialVersionUID = 4916724871985109129L;
    private final MulticlassReceiverOperatingCharacteristic ROC;
    private final MulticlassConfusionMatrix conf;
    private final String[] categories;

    public MulticlassModelEvaluation(String[] categories) {
        this.categories = categories;
        ROC = new MulticlassReceiverOperatingCharacteristic(categories);
        conf = new MulticlassConfusionMatrix(categories);
    }

    public void add(String label, MulticlassPrediction prediction) {
        ROC.add(label, prediction);
        conf.addInfo(label, prediction.getLabel());
    }

    public void merge(ModelEvaluation<MulticlassLabel> other) {
        MulticlassModelEvaluation tempOther = (MulticlassModelEvaluation) other;
        ROC.add(tempOther.ROC);
        conf.add(tempOther.conf);
    }
    
    public void add(GenericPair<String, MulticlassPrediction> labelPrediction) {
        add(labelPrediction.first, labelPrediction.second);
    }

    public void add(MulticlassLabel real, MulticlassLabel pred) {
        if (!(pred instanceof MulticlassPrediction)) {
            throw new java.lang.RuntimeException(
                    "MulticlassModelEvaluation needs a MulticlassPrediction");
        }
        add(real.getLabel(), (MulticlassPrediction)pred);
    }

    public double computeAUC() {
        return ROC.multiclassAUC();
    }

    public double computeAUC(String dim) {
        return ROC.singleClassAUC(dim);
    }

    public double computeBrier() {
        return ROC.multiclassBrierScore();
    }

    public double computeAccy() {
        return conf.computeAccuracy();
    }

    public double computeAccy(String dim) {
        return conf.computeAccuracy(dim);
    }

    public double computeFmeasure() {
        return conf.computeAverageFmeasure();
    }

    public double computeFmeasure(String dim) {
        return conf.computeFmeasure(dim);
    }

    public double computePrecision() {
        return conf.computeAveragePrecision();
    }

    public double computePrecision(String dim) {
        return conf.computePrecision(dim);
    }

    public double computeRecall() {
        return conf.computeAverageRecall();
    }

    public double computeRecall(String dim) {
        return conf.computeRecall(dim);
    }

    public double computePercent(String dim) {
        return ROC.computePercent(dim);
    }

    public String printDebug() {
        return conf.printDebug();
    }

    public Map<String, Double> getStatistics() {
        SortedMap<String, Double> m = new TreeMap<String, Double>();

        m.put("AUC (avg)", computeAUC());
        m.put("Brier (avg)", computeBrier());
        m.put("Acc (avg)", computeAccy());
        m.put("F1 (avg)", computeFmeasure());
        m.put("Prc (avg)", computePrecision());
        m.put("Rec (avg)", computeRecall());

        for (String category : categories) {
            m.put(category + ": Pct", computePercent(category));
            m.put(category + ": AUC", computeAUC(category));
            m.put(category + ": Acc", computeAccy(category));
            m.put(category + ": F1", computeFmeasure(category));
            m.put(category + ": Prc", computePrecision(category));
            m.put(category + ": Rec", computeRecall(category));
        }

        return m;
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append("AUC: " + ROC.multiclassAUC() + "\n");
        buff.append("Brier: " + ROC.multiclassBrierScore() + "\n");
        buff.append("IR metrics:\n" + conf.getIR() + "\n");
        buff.append("Confusion Matrix:\n" + conf.toString() + "\n");
        return buff.toString();
    }

    public HashMap<String, Object> getObjects() {
        HashMap<String, Object> m = new HashMap<String, Object>();
        m.put("conf", conf.toString());
        return m;
    }

}
