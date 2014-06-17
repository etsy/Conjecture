package com.etsy.conjecture.topics.lda;

import com.etsy.conjecture.Utilities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LDADoc implements Serializable {

    private static final long serialVersionUID = 1536967875771864807L;
    double[] topic_proportions;
    double total_words;
    int[] word_idx;
    double[] word_count;
    double[][] phi;
    boolean phi_dirty;

    public LDADoc(Map<String, Double> word_counts, LDADict dict) {
        total_words = 0.0;
        word_idx = new int[word_counts.size()];
        word_count = new double[word_counts.size()];
        phi_dirty = true;
        int i = 0;
        for (Map.Entry<String, Double> e : word_counts.entrySet()) {
            word_idx[i++] = dict.index(e.getKey());
            total_words += e.getValue();
        }
        // Keep parallel arrays in sorted order of word index, for easier
        // aggregation
        // of partial topic models.
        Arrays.sort(word_idx);
        for (int w = 0; w < word_idx.length; w++) {
            word_count[w] = word_counts.get(dict.word(word_idx[w]));
        }
    }

    public double[] topicProportions() {
        return topic_proportions;
    }

    public double wordCount() {
        return total_words;
    }

    public void updateTopicProportions(LDATopics topics, double alpha) {
        int K = topics.numTopics();
        // reuse old topic proportions unless the topic model has changed.
        if (topic_proportions == null
                || topic_proportions.length != topics.numTopics()) {
            topic_proportions = new double[K];
            for (int k = 0; k < K; k++) {
                topic_proportions[k] = total_words / (double)K;
            }
        }
        if (phi == null || phi[0].length != topics.numTopics()) {
            phi = new double[word_idx.length][K];
        }
        // iterate the update procedure.
        double[] topic_proportions_new = new double[K];
        double[] phi_z = new double[word_idx.length];
        while (true) {
            // Compute phi.
            for (int k = 0; k < K; k++) {
                double digamma_k = LDAUtils.digamma(topic_proportions[k]);
                for (int w = 0; w < word_idx.length; w++) {
                    double wp = Math.log(topics.wordProb(word_idx[w], k));
                    phi[w][k] = digamma_k + wp;
                    if (k == 0) {
                        phi_z[w] = phi[w][k];
                    } else {
                        phi_z[w] = LDAUtils.logSumExp(phi_z[w], phi[w][k]);
                    }
                }
            }
            // Compute updated gamma.
            double conv = 0.0;
            for (int k = 0; k < K; k++) {
                topic_proportions_new[k] = alpha;
                for (int w = 0; w < word_idx.length; w++) {
                    phi[w][k] = Math.exp(phi[w][k] - phi_z[w]) * word_count[w];
                    topic_proportions_new[k] += phi[w][k];
                }
                double diff = topic_proportions[k] - topic_proportions_new[k];
                topic_proportions[k] = topic_proportions_new[k];
                conv += diff * diff;
            }
            // Check convergence.
            if (conv < 1000.0) {
                break;
            }
        }
        phi_dirty = false;
    }

    // You can only call this after calling updateTopicProportions..
    public LDAPartialTopics toPartialTopics() throws Exception {
        if (phi_dirty) {
            throw new Exception(
                    "Called toPartialTopics() on a doc that hasnt been updated");
        }
        return new LDAPartialTopics(word_idx, phi);
    }

    public LDAPartialTopics toPartialTopic(int topic) throws Exception {
        if (phi_dirty) {
            throw new Exception(
                    "Called toPartialTopics() on a doc that hasnt been updated");
        }
        double[][] phi_k = new double[word_idx.length][1]; // duh
        for (int i = 0; i < word_idx.length; i++) {
            phi_k[i][0] = phi[i][topic];
        }
        return new LDAPartialTopics(word_idx, phi_k);
    }

    public LDAPartialSparseTopics toPartialSparseTopics(int n) throws Exception {
        if (phi_dirty) {
            throw new Exception(
                    "Called toPartialTopics() on a doc that hasnt been updated");
        }
        int K = topic_proportions.length;
        Map<Integer, Double> partial_phi = new HashMap<Integer, Double>();
        Map<Integer, Double> word_topic_prob = new HashMap<Integer, Double>();
        for (int w = 0; w < word_idx.length; w++) {
            word_topic_prob.clear();
            for (int k = 0; k < K; k++) {
                word_topic_prob.put(k, phi[w][k]);
            }
            ArrayList<Integer> sorted_topics = Utilities.orderKeysByValue(
                    word_topic_prob, true);
            double z = 0.0;
            for (int i = 0; i < n; i++) {
                z += phi[w][sorted_topics.get(i)];
            }
            word_topic_prob.clear();
            for (int i = 0; i < n; i++) {
                int k = sorted_topics.get(i);
                int v = word_idx[w];
                partial_phi.put(v * K + k, (phi[w][k] / z) * word_count[w]);
            }
        }
        return new LDAPartialSparseTopics(K, partial_phi);
    }

}
