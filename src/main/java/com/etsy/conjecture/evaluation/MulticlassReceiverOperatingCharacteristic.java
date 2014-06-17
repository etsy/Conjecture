package com.etsy.conjecture.evaluation;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

import com.etsy.conjecture.GenericPair;
import com.etsy.conjecture.data.MulticlassPrediction;
import static com.google.common.base.Preconditions.checkArgument;

public class MulticlassReceiverOperatingCharacteristic implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Num examples in each class. */
    private Map<String, Integer> classCounts;

    /** Num total examples */
    private int numExamples;

    /** Binary ROCs for each class */
    private Map<String, ReceiverOperatingCharacteristic> classROC;

    /**
     * Instantiates a new receiver operating characteristic.
     */
    public MulticlassReceiverOperatingCharacteristic(String[] categories) {
        classROC = new HashMap<String, ReceiverOperatingCharacteristic>();
        classCounts = new HashMap<String, Integer>();
        for (String category : categories) {
            classROC.put(category, new ReceiverOperatingCharacteristic());
            classCounts.put(category, 0);
        }
    }

    public void add(MulticlassReceiverOperatingCharacteristic other) {
        numExamples += other.numExamples;
        for(Map.Entry<String,Integer> entry : other.classCounts.entrySet()) {
            String category = entry.getKey();
            Integer count = entry.getValue();
            classCounts.put(category, classCounts.get(category)+count);
        }

        for(Map.Entry<String,ReceiverOperatingCharacteristic> entry : other.classROC.entrySet()) {
            String category = entry.getKey();
            ReceiverOperatingCharacteristic update = entry.getValue();
            ReceiverOperatingCharacteristic roc = classROC.get(category);
            roc.add(update);
            classROC.put(category, roc);
        }
    }

    public void add(GenericPair<String, MulticlassPrediction> labelPrediction) {
        add(labelPrediction.first, labelPrediction.second);
    }

    public void add(String label, MulticlassPrediction prediction) {
        checkArgument(classCounts.containsKey(label),
                "label is of unknown category: %s", label);
        checkArgument(classROC.containsKey(label),
                "label is of unknown category: %s", label);

        // accum class counts
        int count = classCounts.get(label);
        classCounts.put(label, count + 1);

        // accum total counts;
        numExamples++;

        // add to individual binary ROC classes
        for (String category : classCounts.keySet()) {
            double binaryPrediction = prediction.getMap().get(category);
            double classLabel = category.equals(label) ? 1d : 0d;
            classROC.get(category).add(classLabel, binaryPrediction);
        }
    }

    public double multiclassAUC() {
        double weightedAverageAUC = 0d;
        for (String label : classCounts.keySet()) {
            double classInfluence = (double)classCounts.get(label)
                    / numExamples;
            ReceiverOperatingCharacteristic roc = classROC.get(label);
            double classAUC = roc.binaryAUC();
            weightedAverageAUC += classInfluence * classAUC;
        }
        return weightedAverageAUC;
    }

    public double multiclassBrierScore() {
        double brierScore = 0d;
        int numClasses = classCounts.keySet().size();
        for (String label : classCounts.keySet()) {
            brierScore += (classROC.get(label)).brierScore();
        }
        return brierScore / numClasses;
    }

    public static double computeAUC(
            Collection<GenericPair<String, MulticlassPrediction>> labelsAndPredictions,
            String[] categories) {
        MulticlassReceiverOperatingCharacteristic roc = new MulticlassReceiverOperatingCharacteristic(
                categories);
        for (GenericPair<String, MulticlassPrediction> p : labelsAndPredictions)
            roc.add((String)p.first, (MulticlassPrediction)p.second);
        return roc.multiclassAUC();
    }

    public static double computeBrierScore(
            Collection<GenericPair<String, MulticlassPrediction>> labelsAndPredictions,
            String[] categories) {
        MulticlassReceiverOperatingCharacteristic roc = new MulticlassReceiverOperatingCharacteristic(
                categories);
        for (GenericPair<String, MulticlassPrediction> p : labelsAndPredictions)
            roc.add(p.first, p.second);
        return roc.multiclassBrierScore();
    }

}
