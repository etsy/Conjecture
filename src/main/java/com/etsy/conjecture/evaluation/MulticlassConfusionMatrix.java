package com.etsy.conjecture.evaluation;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * class representing a confusion matrix for representing misclassification
 * errors.
 * {@link <a href="http://en.wikipedia.org/wiki/Confusion_matrix">Confusion Matrix</a>}
 * 
 * @author jattenberg
 */
public class MulticlassConfusionMatrix implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The data structure representing the confusion matrix. rows correspond to
     * labels, columns to predictions
     */
    private final SortedMap<String, SortedMap<String, Double>> confusionMatrix;

    /** The num_classes represented in the confusion matrix */
    private final int numClasses;

    /** The number of label / prediction pairs observed */
    double obs;

    /**
     * Instantiates a new confusion matrix.
     * 
     * @param classes
     *            the number of target classes in the problem being considered
     */
    public MulticlassConfusionMatrix(String[] categories) {
        obs = 0;
        this.numClasses = categories.length;
        confusionMatrix = initializeMatrix(categories);
    }

    public void add(MulticlassConfusionMatrix m) {
        obs += m.obs;
        for(Map.Entry<String,SortedMap<String, Double>> entry : m.confusionMatrix.entrySet()) {
            String label = entry.getKey();
            SortedMap<String, Double> value = entry.getValue();
            for(Map.Entry<String,Double> inner_entry : value.entrySet()) {
                String inner_label = inner_entry.getKey();
                Double update = inner_entry.getValue();
                confusionMatrix.get(label).put(inner_label, update + getValue(label, inner_label));
            }
        }
    }

    private static SortedMap<String, SortedMap<String, Double>> initializeMatrix(
            Set<String> categories) {
        String[] catArray = new String[categories.size()];
        int ct = 0;
        for (String category : categories) {
            catArray[ct++] = category;
        }
        return initializeMatrix(catArray);
    }

    private static SortedMap<String, SortedMap<String, Double>> initializeMatrix(
            String[] categories) {
        SortedMap<String, SortedMap<String, Double>> conf = new TreeMap<String, SortedMap<String, Double>>();

        for (String categoryOuter : categories) {
            conf.put(categoryOuter, new TreeMap<String, Double>());
            for (String categoryInner : categories) {
                conf.get(categoryOuter).put(categoryInner, 0d);
            }
        }
        return conf;
    }

    private Double getValue(String label, String guess) {
        return confusionMatrix.get(label).get(guess);
    }

    private void updateConfusionMatrix(String label, String guess, double value) {
        confusionMatrix.get(label).put(guess, value + getValue(label, guess));
    }

    private Map<String, Double> initializeProbabilityMatrix() {
        Map<String, Double> probs = new TreeMap<String, Double>();
        for (String category : confusionMatrix.keySet()) {
            probs.put(category, 0d);
        }
        return probs;
    }

    /**
     * Adds a label / prediction pair to the confusion matrix
     * 
     * @param label
     *            the index of the actual class
     * @param guess
     *            the index of the predicted class
     */
    public void addInfo(String label, String guess) {
        obs++;
        updateConfusionMatrix(label, guess, 1d);
    }

    public void addInfo(String label, String guess, double freq) {
        obs += freq;
        updateConfusionMatrix(label, guess, freq);
    }

    /**
     * Adds a label / prediction pair to the confusion matrix with soft labels
     * 
     * @param label
     *            the index of the actual class
     * @param guess
     *            the predicted distribution over classes.
     */
    public void addInfo(String label, Map<String, Double> guesses) {
        addInfo(label, guesses, 1d);
    }

    /**
     * Adds a label / prediction pair to the confusion matrix with soft labels
     * 
     * @param label
     *            the index of the actual class
     * @param guess
     *            the predicted distribution over classes.
     * @param freq
     *            the number of times to consider the input label / prediction
     *            pair
     */
    public void addInfo(String label, Map<String, Double> predictions,
            double freq) {
        // TODO: ensure that sets match
        obs += freq;
        for (String category : predictions.keySet()) {
            updateConfusionMatrix(label, category, predictions.get(category));
        }
    }

    /**
     * Adds a label / prediction pair to the confusion matrix with soft labels
     * 
     * @param softlabels
     *            actual distribution of target class memberships
     * @param guess
     *            the predicted distribution of class memberships
     */
    public void addInfo(Map<String, Double> labels,
            Map<String, Double> predictions) {
        addInfo(labels, predictions, 1d);
    }

    /**
     * Adds a label / prediction pair to the confusion matrix with soft labels
     * 
     * @param softlabels
     *            actual distribution of target class memberships
     * @param guess
     *            the predicted distribution of class memberships
     * @param freq
     *            the number of times to consider this label / prediction pair
     */
    public void addInfo(Map<String, Double> labels,
            Map<String, Double> predictions, double freq) {
        obs += freq;
        for (String categoryLabel : labels.keySet()) {
            for (String categoryGuess : predictions.keySet()) {
                updateConfusionMatrix(categoryLabel, categoryGuess, freq);
            }
        }
    }

    /**
     * Computes the actual distribution over labels
     * 
     * @return the Map<String, Double encoding probabilities in each class.
     */
    public Map<String, Double> classDistribution() {
        Map<String, Double> dists = initializeProbabilityMatrix();
        for (String category : dists.keySet()) {
            dists.put(category, classDistribution(category));
        }
        return dists;
    }

    /**
     * Computes the actual probability of mambership in a particular class
     * denoted by the input index TODO: implement this more efficiently
     * 
     * @param num
     *            index of the class of interest
     * @return the probability of membership in the requested class
     */
    public double classDistribution(String category) {
        double classSum = 0;
        double totSum = 0;
        for (String categoryLabel : confusionMatrix.keySet()) {
            for (String categoryPrediction : confusionMatrix.keySet()) {
                if (categoryPrediction.equals(categoryLabel)) {
                    classSum += getValue(categoryLabel, categoryPrediction);
                }
                totSum += getValue(categoryLabel, categoryPrediction);
            }
        }
        return totSum > 0d ? classSum : 0d;
    }

    /**
     * Adds a label / prediction pair to the confusion matrix with hard (most
     * likely class) labels
     * 
     * @param softlabels
     *            actual distribution of target class memberships
     * @param guess
     *            the predicted distribution of class memberships
     * @param freq
     *            the number of times to consider this label / prediction pair
     */
    public void addHard(Map<String, Double> softlabels,
            Map<String, Double> predictions, double weight) {
        addInfo(softToHard(softlabels), softToHard(predictions), weight);
    }

    /**
     * Adds a label / prediction pair to the confusion matrix with hard (most
     * likely class) labels
     * 
     * @param softlabels
     *            actual distribution of target class memberships
     * @param guess
     *            the predicted distribution of class memberships
     */
    public void addHard(Map<String, Double> softlabels,
            Map<String, Double> predictions) {
        addInfo(softToHard(softlabels), softToHard(predictions), 1d);
    }

    /**
     * Adds a label / prediction pair to the confusion matrix with hard (most
     * likely class) labels note, only applicable for binary classification (2
     * class) problems
     * 
     * @param label
     *            the index of the actual class of membership
     * @param prediction
     *            the predicted probability of membership in the positive class
     */
    public void addHard(String label, Map<String, Double> guess) {
        addInfo(label, softToHard(guess), 1d);
    }

    /**
     * Adds a label / prediction pair to the confusion matrix with hard (most
     * likely class) labels note, only applicable for binary classification (2
     * class) problems
     * 
     * @param label
     *            the index of the actual class of membership
     * @param prediction
     *            the predicted probability of membership in the positive class
     * @param freq
     *            the number of times this label / prediction pair should be
     *            considered.
     */
    public void addHard(String label, Map<String, Double> guess, double freq) {
        addInfo(label, softToHard(guess), 1d);
    }

    /**
     * converts a soft prediction of probability estimates into a categorical
     * indicator for the most likely class
     * 
     * @param scores
     *            probabilities of label class membership
     * @return the categorical values, 0's for all target classes with a 1 for
     *         the most likely class
     */
    private static String softToHard(Map<String, Double> scores) {
        String maxindex = null;
        double max = Double.NEGATIVE_INFINITY;
        for (String category : scores.keySet()) {
            if (scores.get(category) > max) {
                max = scores.get(category);
                maxindex = category;
            }
        }
        return maxindex;
    }

    public String printDebug() {
        return "";
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append("predicted:\t");
        for (String category : confusionMatrix.keySet()) {
            buff.append(category + "\t");
        }
        buff.append("\n");
        for (String categoryLabel : confusionMatrix.keySet()) {
            buff.append("actually " + categoryLabel + ":\t");
            for (String categoryPrediction : confusionMatrix.keySet()) {
                buff.append(String.format("%.4f\t",
                        getValue(categoryLabel, categoryPrediction)));
            }
            buff.append("\n");
        }
        return buff.toString();
    }

    /**
     * To string row normalized (divided by the sum of each row)
     * 
     * @return the string representation of the confusion matrix that has been
     *         row normalized
     */
    public String toStringRowNormalized() {
        StringBuilder buff = new StringBuilder();
        buff.append("predicted:\t");
        for (String category : confusionMatrix.keySet()) {
            buff.append(category + "\t");
        }
        Map<String, Double> rowSums = rowSums();
        buff.append("\n");
        for (String categoryLabel : confusionMatrix.keySet()) {
            buff.append("actually " + categoryLabel + ":\t");
            for (String categoryPrediction : confusionMatrix.keySet()) {
                String s = String.format(
                        "%.4f\t",
                        getValue(categoryLabel, categoryPrediction)
                                / rowSums.get(categoryLabel));
                buff.append(s);
            }
            buff.append("\n");
        }
        return buff.toString();
    }

    /**
     * To string column normalized (divided by the sum of each column)
     * 
     * @return the string representation of the confusion matrix that has been
     *         column normalized
     */
    public String toStringColNormalized() {
        StringBuilder buff = new StringBuilder();
        buff.append("predicted:\t");
        for (String category : confusionMatrix.keySet()) {
            buff.append(category + "\t");
        }
        Map<String, Double> colSums = colSums();
        buff.append("\n");
        for (String categoryLabel : confusionMatrix.keySet()) {
            buff.append("actually " + categoryLabel + ":\t");
            for (String categoryPrediction : confusionMatrix.keySet()) {
                String s = String.format(
                        "%.4f\t",
                        getValue(categoryLabel, categoryPrediction)
                                / colSums.get(categoryLabel));
                buff.append(s);
            }
            buff.append("\n");
        }
        return buff.toString();
    }

    /**
     * Compute the sum of each row
     * 
     * @return an array containing the sum of each row.
     */
    public Map<String, Double> rowSums() {
        Map<String, Double> sums = initializeProbabilityMatrix();

        for (String cateogryLabel : confusionMatrix.keySet()) {
            for (String cateogryPrediction : confusionMatrix.keySet()) {
                sums.put(
                        cateogryLabel,
                        sums.get(cateogryLabel)
                                + getValue(cateogryLabel, cateogryPrediction));
            }
        }
        return sums;
    }

    /**
     * Compute the accuracy for a given class; the % of examples that have been
     * correctly classifieed.
     * 
     * @param classid
     *            the index of the class where accuracy has been requested
     * @return the % of correctly classified examples for the requested class
     */
    public double computeAccuracy(String classId) {
        double tn = 0.;
        for (String categoryLabel : confusionMatrix.keySet()) {
            for (String categoryPrediction : confusionMatrix.keySet()) {
                if (!categoryLabel.equals(classId)
                        && !categoryPrediction.equals(classId))
                    tn += getValue(categoryLabel, categoryPrediction);
            }
        }

        double tp = getValue(classId, classId);
        return (tn + tp) / obs;
    }

    public double computeAverageFmeasure() {
        Map<String, Double> rowSums = rowSums();
        double total = total(rowSums);
        double fmeasure = 0.;

        for (String category : confusionMatrix.keySet()) {
            fmeasure += rowSums.get(category) * computeFmeasure(category);
        }
        return fmeasure / total;
    }

    public double computeAveragePrecision() {
        Map<String, Double> rowSums = rowSums();
        double total = total(rowSums);
        double precision = 0.;

        for (String category : confusionMatrix.keySet()) {
            double pre = computePrecision(category);
            if (!Double.isNaN(pre)) { // when nothing is predicted as category,
                                      // pre is NaN
                precision += rowSums.get(category) * pre;
            }
        }
        return precision / total;
    }

    public double computeAverageRecall() {
        Map<String, Double> rowSums = rowSums();
        double total = total(rowSums);
        double recall = 0.;

        for (String category : confusionMatrix.keySet()) {
            double re = computeRecall(category);
            if (!Double.isNaN(re)) { // re is NaN when there are 0 examples with
                                     // label of category
                recall += rowSums.get(category) * re;
            }
        }
        return recall / total;
    }

    /**
     * Compute the sums of each column
     * 
     * @return an array containing the sum of each column.
     */
    public Map<String, Double> colSums() {
        Map<String, Double> sums = initializeProbabilityMatrix();
        for (String categoryLabel : confusionMatrix.keySet()) {
            for (String categoryPrediction : confusionMatrix.keySet()) {
                sums.put(categoryPrediction, sums.get(categoryPrediction)
                        + getValue(categoryLabel, categoryPrediction));
            }
        }
        return sums;
    }

    /**
     * Return the confusion matrix as a 2d array
     * 
     * @return the confusion matrix data structure
     */
    public SortedMap<String, SortedMap<String, Double>> getMatrix() {
        SortedMap<String, SortedMap<String, Double>> out = initializeMatrix(confusionMatrix
                .keySet());
        for (String categoryLabel : confusionMatrix.keySet()) {
            for (String categoryPrediction : confusionMatrix.keySet()) {
                out.get(categoryLabel).put(categoryPrediction,
                        getValue(categoryLabel, categoryPrediction));
            }
        }
        return out;
    }

    /**
     * Gets the number of classes in the confusion matrix
     * 
     * @return the number of classes considered
     */
    public int getDim() {
        return this.numClasses;
    }

    /**
     * Computes the accuracy over all observations for all classes (% of
     * correctly labeled examples).
     * 
     * @return accuracy over all classes.
     */
    public double computeAccuracy() {
        double accy = 0d;
        double tot = 0d;
        double right = 0d;
        Map<String, Double> rowSums = rowSums();
        for (String category : confusionMatrix.keySet()) {
            tot += rowSums.get(category);
            right += getValue(category, category);
        }
        if (tot > 0) {
            accy = right / tot;
        }

        return accy;
    }

    /**
     * Compute the precision for each class; the % of members of labeled as
     * belonging to each class who were actually members of that class
     * 
     * @return an array containing the precision values for each class.
     */
    public Map<String, Double> computePrecision() {
        Map<String, Double> precision = initializeProbabilityMatrix();
        for (String categoryLabel : confusionMatrix.keySet()) {
            precision.put(categoryLabel, computePrecision(categoryLabel));
        }
        return precision;
    }

    /**
     * Compute the recall for each class; the % of members of belonging to each
     * class that were labeled as class members
     * 
     * @return an array containing the recall values for each class.
     */
    public Map<String, Double> computeRecall() {
        Map<String, Double> recall = initializeProbabilityMatrix();
        for (String categoryLabel : confusionMatrix.keySet()) {
            recall.put(categoryLabel, computeRecall(categoryLabel));
        }
        return recall;
    }

    /**
     * Computes the F-measure for each class; the harmonic mean of precision and
     * recall
     * {@link <a href="http://en.wikipedia.org/wiki/F_measure">F-Measure</a>}
     * for more info
     * 
     * @return the array containing the F-measure for each class
     */
    public Map<String, Double> computeFmeasure() {
        Map<String, Double> fmeasure = initializeProbabilityMatrix();
        Map<String, Double> precision = this.computePrecision();
        Map<String, Double> recall = this.computeRecall();

        for (String category : confusionMatrix.keySet()) {
            if (recall.get(category) + precision.get(category) != 0)
                fmeasure.put(
                        category,
                        2.0
                                * (precision.get(category) * recall
                                        .get(category))
                                / (precision.get(category) + recall
                                        .get(category)));
        }
        return fmeasure;
    }

    /**
     * Builds A String Table Containing The common IR measures, precision,
     * recall, and F measure for each class
     * 
     * @return the string with performance stats
     */
    public String getIR() {
        StringBuffer buff = new StringBuffer();
        buff.append("class\t" + "precision\t" + "recall\t" + "F measure\n");
        Map<String, Double> precision = this.computePrecision();
        Map<String, Double> recall = this.computeRecall();
        Map<String, Double> fmeasure = this.computeFmeasure();

        for (String category : confusionMatrix.keySet()) {
            buff.append(category + "\t" + precision.get(category) + "\t"
                    + recall.get(category) + "\t" + fmeasure.get(category)
                    + "\n");
        }
        return buff.toString();

    }

    /**
     * Computes precision for a given class; the % of members of belonging to
     * each class that were labeled as class members
     * 
     * @param dim
     *            class of interest
     * @return the precision for the requested class
     */
    public double computePrecision(String category) {
        double tot = 0;
        for (String label : confusionMatrix.keySet()) {
            tot += getValue(label, category);
        }
        return getValue(category, category) / tot;
    }

    /**
     * Compute the recall for a given class; the % of members of belonging to
     * each class that were labeled as class members
     * 
     * @param dim
     *            the class of interest
     * @return the recall for the requested class
     */
    public double computeRecall(String category) {
        double tot = 0;
        for (String prediction : confusionMatrix.keySet())
            tot += getValue(category, prediction);
        return getValue(category, category) / tot;
    }

    /**
     * Computes the F-measure for a given class; the harmonic mean of precision
     * and recall
     * {@link <a href="http://en.wikipedia.org/wiki/F_measure">F-Measure</a>}
     * for more info
     * 
     * 
     * @param dim
     *            the class of interest
     * @return the F-Measure of the requested class
     */
    public double computeFmeasure(String category) {
        double pre = computePrecision(category);
        double rec = computeRecall(category);
        return 2 * (pre * rec) / (pre + rec);
    }

    /**
     * Total.
     * 
     * @param arr
     *            the arr
     * @return the double
     */
    private double total(Map<String, Double> probs) {
        double total = 0;
        for (String category : probs.keySet())
            total += probs.get(category);
        return total;
    }
}
