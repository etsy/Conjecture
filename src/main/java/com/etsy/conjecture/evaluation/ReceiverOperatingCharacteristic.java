package com.etsy.conjecture.evaluation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.etsy.conjecture.PrimitivePair;

public class ReceiverOperatingCharacteristic implements Serializable {

    private static class NumComparator implements Comparator<Double>,
            Serializable {
        private static final long serialVersionUID = 6569477679353298040L;

        @Override
        public int compare(Double o1, Double o2) {
            return -o1.compareTo(o2);
        }
    }

    private static final long serialVersionUID = 1L;
    private static NumComparator numComparator = new NumComparator();

    /**
     * map for storing the number of positive and negative labeled examples for
     * each prediction
     */
    private NavigableMap<Double, int[]> examples;

    /** The pos. */
    private double pos = 0;

    /** The neg. */
    private double neg = 0;

    /**
     * Instantiates a new receiver operating characteristic.
     */
    public ReceiverOperatingCharacteristic() {
        examples = new TreeMap<Double, int[]>(numComparator);
    }

    /**
     * Merge two ROCs together.
     */
    public void add(ReceiverOperatingCharacteristic r) {
        for (Map.Entry<Double, int[]> entry : r.examples.entrySet()) {
            increment(entry.getKey(), entry.getValue());
        }
        pos += r.pos;
        neg += r.neg;
    }

    /**
     * increments count values
     */
    private void increment(Double key, int[] value) {
        if (!examples.containsKey(key)) {
            examples.put(key, value);
        } else {
            int[] oldVals = examples.get(key);
            oldVals[0] += value[0];
            oldVals[1] += value[1];
            examples.put(key, oldVals);
        }
    }

    private void increment(Double prediction, double label) {

        if (label > 0.5) {
            pos++;
            increment(prediction, new int[] { 1, 0 });
        } else {
            neg++;
            increment(prediction, new int[] { 0, 1 });
        }
    }

    /**
     * Adds the.
     * 
     * @param label
     *            the label
     * @param prediction
     *            the prediction
     */
    public void add(double label, double prediction) {
        increment(prediction, label);
    }

    /* pair should be in label, prediction order */
    public void add(PrimitivePair pair) {
        add(pair.first, pair.second);
    }

    /**
     * Roc.
     * 
     * @return the double[][]
     */
    public double[][] ROC() {

        // checked: examples are in sorted order.
        // pos and neg are correct

        List<PrimitivePair> curve = new ArrayList<PrimitivePair>();
        double tp = 0;
        double fp = 0;

        for (int[] counts : examples.values()) {
            curve.add(new PrimitivePair(fp / neg, tp / pos));

            tp += counts[0];
            fp += counts[1];
        }
        curve.add(new PrimitivePair(fp / neg, tp / pos));

        double[][] out = new double[curve.size()][2];

        for (int i = 0; i < curve.size(); i++) {
            out[i][0] = curve.get(i).second; // tpr
            out[i][1] = curve.get(i).first; // fpr
        }
        return out;

    }

    /**
     * Brier score.
     * 
     * @return the double
     */
    public double brierScore() {
        double score = 0;
        double total = 0;

        for (Map.Entry<Double, int[]> entry : examples.entrySet()) {

            Double pred = entry.getKey();
            int[] counts = entry.getValue();

            score += counts[0] * (1 - pred) * (1 - pred);
            score += counts[1] * (0 - pred) * (0 - pred);
            total += counts[0] + counts[1];
        }

        return score / total;
    }

    /**
     * bins the predictions. looks at the average label compared to the median
     * prediction for each bin. computes the brier score based on this
     * 
     * @param bins
     *            the bins
     * @return the double
     */
    public double averagedBrierScore(int bins) {
        double score = 0;
        double predBins = Math.min(bins, pos + neg);

        double binWidth = 1. / predBins;
        double bottom = 0.;
        double top = bottom + binWidth;

        for (int i = 0; i < predBins; i++) {
            double num = 0;
            double avgLabel = 0;

            NavigableMap<Double, int[]> subMap = examples.subMap(bottom, true,
                    top, true);
            for (int[] labels : subMap.values()) {
                avgLabel += labels[0];
                num += labels[0] + labels[1];
            }

            double medianscore = (bottom + top) / 2.;

            if (num > 0) {
                avgLabel /= num;
                score += (medianscore - avgLabel) * (medianscore - avgLabel);
            }

            top += binWidth;
            bottom += binWidth;
        }
        return score / predBins;
    }

    /**
     * Binary auc.
     * 
     * @return the double
     */
    public double binaryAUC() {
        double[][] ROC = ROC();
        double area = 0.0;
        for (int i = 1; i < ROC.length; i++) {
            area += trapezoidArea(ROC[i - 1][1], ROC[i][1], ROC[i - 1][0],
                    ROC[i][0]);
        }
        area += trapezoidArea(1, ROC[ROC.length - 1][1], 1,
                ROC[ROC.length - 1][0]);
        return area;
    }

    /**
     * Trapezoid area.
     * 
     * @param x1
     *            the x1
     * @param x2
     *            the x2
     * @param y1
     *            the y1
     * @param y2
     *            the y2
     * @return the double
     */
    private double trapezoidArea(double x1, double x2, double y1, double y2) {
        double base = Math.abs(x1 - x2);
        double avgHeight = (y1 + y2) / 2.0;
        return base * avgHeight;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        double[][] out = this.ROC();
        for (int i = 0; i < out.length; i++) {
            buff.append(out[i][0] + "\t" + out[i][1] + "\n");
        }
        return buff.toString();
    }

    /**
     * Compute auc.
     * 
     * @param labelsAndPredictions
     *            the labels and predictions
     * @return the double
     */
    public static double computeAUC(
            Collection<PrimitivePair> labelsAndPredictions) {
        ReceiverOperatingCharacteristic roc = new ReceiverOperatingCharacteristic();
        for (PrimitivePair p : labelsAndPredictions)
            roc.add(p.first, p.second);
        return roc.binaryAUC();
    }

    /**
     * Compute brier score.
     * 
     * @param labelsAndPredictions
     *            the labels and predictions
     * @return the double
     */
    public static double computeBrierScore(
            Collection<PrimitivePair> labelsAndPredictions) {
        ReceiverOperatingCharacteristic roc = new ReceiverOperatingCharacteristic();
        for (PrimitivePair p : labelsAndPredictions)
            roc.add(p.first, p.second);
        return roc.averagedBrierScore(25);
    }

}
