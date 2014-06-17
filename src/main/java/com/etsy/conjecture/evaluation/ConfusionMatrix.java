package com.etsy.conjecture.evaluation;

import java.io.Serializable;
import java.util.Collection;

import com.etsy.conjecture.PrimitivePair;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * class representing a confusion matrix for representing misclassification
 * errors.
 * {@link <a href="http://en.wikipedia.org/wiki/Confusion_matrix">Confusion Matrix</a>}
 * 
 * @author jattenberg
 */
public class ConfusionMatrix implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The data structure representing the confusion matrix. rows correspond to
     * labels, columns to predictions
     */
    private double[][] confMatrix;

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
    public ConfusionMatrix(int classes) {
        obs = 0;
        this.numClasses = classes;
        this.confMatrix = new double[numClasses][numClasses];
    }

    public void add(ConfusionMatrix m) {
        obs += m.obs;
        for (int i = 0; i < numClasses; i++) {
            for (int j = 0; j < numClasses; j++) {
                confMatrix[i][j] += m.confMatrix[i][j];
            }
        }
    }

    /**
     * Instantiates a new confusion matrix and adds some initial data
     * 
     * @param classes
     *            - the number of target classes in the problem being considered
     * @param labelsAndPredictions
     *            the labels and predictions
     */
    public ConfusionMatrix(int classes,
            Collection<PrimitivePair> labelsAndPredictions) {
        this(classes);
        for (PrimitivePair p : labelsAndPredictions)
            addInfo(p.first, p.second);
    }

    /**
     * Instantiates a new confusion matrix and adds some initial data
     * 
     * @param classes
     *            - the number of target classes in the problem being considered
     * @param labelsAndPredictions
     *            the labels and predictions
     */
    public ConfusionMatrix(int classes, PrimitivePair[] labelsAndPredictions) {
        this(classes);
        for (PrimitivePair p : labelsAndPredictions)
            addInfo(p.first, p.second);
    }

    /**
     * Instantiates a new confusion matrix and adds some initial data
     * 
     * @param classes
     *            - the number of target classes in the problem being considered
     * @param labelsAndPredictions
     *            the labels and predictions
     */
    public ConfusionMatrix(int classes, double[] labels, double[] predictions) {
        this(classes);
        checkArgument(
                labels.length == predictions.length,
                "labels and predictions must be of the same length! (%s vs %s)",
                labels.length, predictions.length);
        for (int i = 0; i < labels.length; i++) {
            addInfo(labels[i], predictions[i]);
        }
    }

    /**
     * Adds a label / prediction pair to the confusion matrix
     * 
     * @param label
     *            the index of the actual class
     * @param guess
     *            the index of the predicted class
     */
    public void addInfo(int label, int guess) {
        obs++;
        this.confMatrix[label][guess]++;
    }

    /**
     * Adds a label / prediction pair to the confusion matrix with soft labels
     * 
     * @param label
     *            the index of the actual class
     * @param guess
     *            the predicted distribution over classes.
     */
    public void addInfo(int label, double[] guess) {
        addInfo(label, guess, 1);
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
    public void addInfo(int label, double[] guess, double freq) {
        checkArgument(
                guess.length == numClasses,
                "input lenght (%d) must match num classes in confusion matrix (%d) ",
                guess.length, numClasses);
        obs += freq;
        for (int i = 0; i < numClasses; i++) {
            confMatrix[label][i] += freq * guess[i];
        }
    }

    /**
     * Adds a label / prediction pair to the confusion matrix with soft labels
     * note, only applicable for binary classification (2 class) problems
     * 
     * @param label
     *            the actual probability of membership in the positive class
     * @param prediction
     *            the predicted probability of membership in the positive class
     */
    public void addInfo(double label, double prediction) {
        checkArgument(
                2 == numClasses,
                "num classes in confusion matrix (%d) must be 2 for this method",
                numClasses);
        addInfo(new double[] { 1. - label, label }, new double[] {
                1. - prediction, prediction });
    }

    /**
     * Adds a label / prediction pair to the confusion matrix with soft labels
     * 
     * @param softlabels
     *            actual distribution of target class memberships
     * @param guess
     *            the predicted distribution of class memberships
     */
    public void addInfo(double[] softlabels, double[] guess) {
        obs++;
        for (int i = 0; i < numClasses; i++) {
            for (int j = 0; j < numClasses; j++) {
                confMatrix[i][j] += softlabels[i] * guess[j];
            }
        }
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
    public void addInfo(double[] softlabels, double[] guess, double freq) {
        obs += freq;
        for (int i = 0; i < numClasses; i++) {
            for (int j = 0; j < numClasses; j++) {
                confMatrix[i][j] += softlabels[i] * guess[j] * freq;
            }
        }
    }

    /**
     * Computes the actual distribution over labels
     * 
     * @return the double[] encoding probabilities in each class.
     */
    public double[] classDistribution() {
        double[] dists = new double[this.numClasses];
        for (int i = 0; i < numClasses; i++) {
            dists[i] = classDistribution(i);
        }
        return dists;
    }

    /**
     * Computes the actual probability of mambership in a particular class
     * denoted by the input index
     * 
     * @param num
     *            index of the class of interest
     * @return the probability of membership in the requested class
     */
    public double classDistribution(int num) {
        double classSum = 0;
        double totSum = 0;
        for (int i = 0; i < numClasses; i++) {
            for (int j = 0; j < numClasses; j++) {
                if (i == num)
                    classSum += confMatrix[i][j];
                totSum += confMatrix[i][j];
            }
        }
        return classSum / totSum;
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
    public void addHard(double[] softlabels, double[] guess, double weight) {
        addInfo(softToHard(softlabels), softToHard(guess), weight);
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
    public void addHard(double[] softlabels, double[] guess) {
        addInfo(softToHard(softlabels), softToHard(guess));
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
    public void addHard(int label, double[] guess) {
        addInfo(label, softToHard(guess));
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
    public void addHard(int label, double prediction) {
        addInfo(label,
                softToHard(new double[] { 1.0 - prediction, prediction }));
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
    public void addHard(int label, double[] guess, double freq) {
        addInfo(label, softToHard(guess));
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
    private static double[] softToHard(double[] scores) {
        int maxindex = 0;
        double max = 0;
        double[] out = new double[scores.length];
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > max) {
                maxindex = i;
                max = scores[i];
            }
        }
        out[maxindex] = 1;
        return out;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append("predicted:\t");
        for (int i = 0; i < numClasses - 1; i++) {
            buff.append(i + "\t");
        }
        buff.append((numClasses - 1) + "\n");
        for (int i = 0; i < numClasses; i++) {
            buff.append("actually " + i + ":\t");
            for (int j = 0; j < numClasses; j++) {
                buff.append(String.format("%.4f\t", confMatrix[i][j]));
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
        for (int i = 0; i < numClasses - 1; i++) {
            buff.append(i + "\t");
        }
        double[] rowSums = this.rowSums();
        buff.append((numClasses - 1) + "\n");
        for (int i = 0; i < numClasses; i++) {
            buff.append("actually " + i + ":\t");
            for (int j = 0; j < numClasses; j++) {
                String s = String.format("%.4f\t", confMatrix[i][j]
                        / rowSums[i]);
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
        for (int i = 0; i < numClasses - 1; i++) {
            buff.append(i + "\t");
        }
        double[] colSums = this.colSums();
        buff.append((numClasses - 1) + "\n");
        for (int i = 0; i < numClasses; i++) {
            buff.append("actually " + i + ":\t");
            for (int j = 0; j < numClasses; j++) {
                String s = String.format("%.4f\t", confMatrix[i][j]
                        / colSums[i]);
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
    public double[] rowSums() {
        double[] sums = new double[numClasses];
        for (int i = 0; i < numClasses; i++) {
            for (int j = 0; j < numClasses; j++) {
                sums[i] += confMatrix[i][j];
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
    public double computeAccuracy(int classid) {
        double tn = 0.;
        for (int i = 0; i < numClasses; i++)
            for (int j = 0; j < numClasses; j++)
                if (j != classid && i != classid)
                    tn += confMatrix[i][j];
        double tp = confMatrix[classid][classid];
        return (tn + tp) / obs;
    }

    public double computeAverageFmeasure() {
        double[] rowSums = rowSums();
        double total = total(rowSums);
        double fmeasure = 0.;

        for (int i = 0; i < numClasses; i++) {
            fmeasure += rowSums[i] * computeFmeasure(i);
        }
        return fmeasure / total;
    }

    public double computeAveragePrecision() {
        double[] rowSums = rowSums();
        double total = total(rowSums);
        double precision = 0.;

        for (int i = 0; i < numClasses; i++) {
            precision += rowSums[i] * computePrecision(i);
        }
        return precision / total;
    }

    public double computeAverageRecall() {
        double[] rowSums = rowSums();
        double total = total(rowSums);
        double recall = 0.;

        for (int i = 0; i < numClasses; i++) {
            recall += rowSums[i] * computeRecall(i);
        }
        return recall / total;
    }

    /**
     * Compute the sums of each column
     * 
     * @return an array containing the sum of each column.
     */
    public double[] colSums() {
        double[] sums = new double[numClasses];
        for (int i = 0; i < numClasses; i++) {
            for (int j = 0; j < numClasses; j++) {
                sums[j] += confMatrix[i][j];
            }
        }
        return sums;
    }

    /**
     * Return the confusion matrix as a 2d array
     * 
     * @return the confusion matrix data structure
     */
    public double[][] getMatrix() {
        double[][] out = new double[numClasses][numClasses];
        for (int i = 0; i < numClasses; i++)
            for (int j = 0; j < numClasses; j++)
                out[i][j] = confMatrix[i][j];
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
        double accy = 0;
        double tot = 0;
        double right = 0;
        for (int i = 0; i < this.numClasses; i++) {
            tot += total(this.confMatrix[i]);
            right += this.confMatrix[i][i];
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
    public double[] computePrecision() {
        double[] precision = new double[this.numClasses];
        for (int i = 0; i < this.numClasses; i++) {
            double yes = 0;
            double no = 0;
            for (int j = 0; j < this.numClasses; j++) {
                if (i == j)
                    yes += confMatrix[i][j];
                else
                    no += confMatrix[i][j];
            }
            if (yes + no != 0)
                precision[i] = yes / (yes + no);
        }
        return precision;
    }

    /**
     * Compute the recall for each class; the % of members of belonging to each
     * class that were labeled as class members
     * 
     * @return an array containing the recall values for each class.
     */
    public double[] computeRecall() {
        double[] recall = new double[this.numClasses];
        double yes[] = new double[this.numClasses];
        double no[] = new double[this.numClasses];
        for (int i = 0; i < this.numClasses; i++) {
            for (int j = 0; j < this.numClasses; j++) {
                if (i == j)
                    yes[j] += confMatrix[i][j];
                else
                    no[j] += confMatrix[i][j];
            }
        }
        for (int i = 0; i < numClasses; i++) {
            if (yes[i] + no[i] != 0)
                recall[i] = yes[i] / (yes[i] + no[i]);
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
    public double[] computeFmeasure() {
        double[] fmeasure = new double[numClasses];
        double[] precision = this.computePrecision();
        double[] recall = this.computeRecall();

        for (int i = 0; i < this.numClasses; i++) {
            if (recall[i] + precision[i] != 0)
                fmeasure[i] = 2.0 * (precision[i] * recall[i])
                        / (precision[i] + recall[i]);
        }
        return fmeasure;
    }

    /**
     * Builds a string table containing the common IR measures, precision,
     * recall, and F measure for each class
     * 
     * @return the string with performance stats
     */
    public String getIR() {
        StringBuffer buff = new StringBuffer();
        buff.append("class\t" + "precision\t" + "recall\t" + "F measure\n");
        double[] precision = this.computePrecision();
        double[] recall = this.computeRecall();
        double[] fmeasure = this.computeFmeasure();

        for (int i = 0; i < numClasses; i++) {
            buff.append(i + "\t" + precision[i] + "\t" + recall[i] + "\t"
                    + fmeasure[i] + "\n");
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
    public double computePrecision(int dim) {
        double tot = 0;
        for (int i = 0; i < numClasses; i++)
            tot += confMatrix[i][dim];
        return confMatrix[dim][dim] / tot;
    }

    /**
     * Compute the recall for a given class; the % of members of belonging to
     * each class that were labeled as class members
     * 
     * @param dim
     *            the class of interest
     * @return the recall for the requested class
     */
    public double computeRecall(int dim) {
        double tot = 0;
        for (int i = 0; i < numClasses; i++)
            tot += confMatrix[dim][i];
        return confMatrix[dim][dim] / tot;
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
    public double computeFmeasure(int dim) {
        double pre = computePrecision(dim);
        double rec = computeRecall(dim);
        return 2 * (pre * rec) / (pre + rec);
    }

    /**
     * Total.
     * 
     * @param arr
     *            the arr
     * @return the double
     */
    private double total(double[] arr) {
        double total = 0;
        for (int i = 0; i < arr.length; i++)
            total += arr[i];
        return total;
    }

    /**
     * Builds a confusion matrix with the input observations and computes the
     * accuracy over all observations for all classes (% of correctly labeled
     * examples).
     * 
     * 
     * @param input
     *            the input label / prediction pairs
     * @return the accuracy of the input values
     */
    public static double computeAccuracy(Collection<PrimitivePair> input) {
        ConfusionMatrix conf = new ConfusionMatrix(2);
        for (PrimitivePair p : input)
            conf.addInfo(new double[] { 1. - p.first, p.first }, new double[] {
                    1. - p.second, p.second });
        return conf.computeAccuracy();
    }
}
