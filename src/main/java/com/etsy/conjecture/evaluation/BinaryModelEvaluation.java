package com.etsy.conjecture.evaluation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.etsy.conjecture.data.BinaryLabel;
import com.etsy.conjecture.PrimitivePair;

/**
 * a basic container for evaluations TODO: add getters for individual metrics
 */
public class BinaryModelEvaluation implements ModelEvaluation<BinaryLabel>,
        Serializable {

    private static final long serialVersionUID = 1L;
    private final ReceiverOperatingCharacteristic ROC;
    private final ConfusionMatrix conf;

    public BinaryModelEvaluation() {
        ROC = new ReceiverOperatingCharacteristic();
        conf = new ConfusionMatrix(2);
    }

    public void merge(ModelEvaluation<BinaryLabel> other) {
      BinaryModelEvaluation tempOther = (BinaryModelEvaluation) other;
      ROC.add(tempOther.ROC);
      conf.add(tempOther.conf);
    }

    public void add(BinaryLabel real, BinaryLabel pred) {
        add(real.getValue(), pred.getValue());
    }

    public void add(double label, double prediction) {
        ROC.add(label, prediction);
        conf.addHard((int)label, prediction);
    }

    public void add(PrimitivePair labelPrediction) {
        ROC.add(labelPrediction);
        conf.addHard((int)labelPrediction.first, labelPrediction.second);
    }

    public double computeAUC() {
        return ROC.binaryAUC();
    }

    public double computeBrier() {
        return ROC.brierScore();
    }

    public double computeAccy() {
        return conf.computeAccuracy();
    }

    public double computeAccy(int dim) {
        return conf.computeAccuracy(dim);
    }

    public double computeFmeasure() {
        return conf.computeAverageFmeasure();
    }

    public double computeFmeasure(int dim) {
        return conf.computeFmeasure(dim);
    }

    public double computePrecision() {
        return conf.computeAveragePrecision();
    }

    public double computePrecision(int dim) {
        return conf.computePrecision(dim);
    }

    public double computeRecall() {
        return conf.computeAverageRecall();
    }

    public double computeRecall(int dim) {
        return conf.computeRecall(dim);
    }

    public Map<String, Double> getStatistics() {
        SortedMap<String, Double> m = new TreeMap<String, Double>();

        m.put("Brier", computeBrier());
        m.put("Acc (avg)", computeAccy());
        m.put("F1 (avg)", computeFmeasure());
        m.put("Prc (avg)", computePrecision());
        m.put("Rec (avg)", computeRecall());

        m.put("0-class Acc", computeAccy(0));
        m.put("0-class F1", computeFmeasure(0));
        m.put("0-class Prc", computePrecision(0));
        m.put("0-class Rec", computeRecall(0));

        m.put("1-class Acc", computeAccy(1));
        m.put("1-class F1", computeFmeasure(1));
        m.put("1-class Prc", computePrecision(1));
        m.put("1-class Rec", computeRecall(1));
        m.put("1-class AUC", computeAUC());
        return m;
    }

    public Map<String, Object> getObjects() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("conf", conf.toString());
        return m;
    }
}
