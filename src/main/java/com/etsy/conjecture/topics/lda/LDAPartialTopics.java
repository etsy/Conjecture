package com.etsy.conjecture.topics.lda;

import java.io.Serializable;

public class LDAPartialTopics implements Serializable {

    private static final long serialVersionUID = 3590284302630767864L;
    private int[] word_index;
    private double[][] phi;

    public LDAPartialTopics(int[] word_index, double[][] phi) {
        this.word_index = word_index;
        this.phi = phi;
    }

    private int countUniqueWords(LDAPartialTopics rhs) {
        // First determine the number of unique words in both sides.
        int num_words = 0;
        int lhs_idx = 0;
        int rhs_idx = 0;
        while (lhs_idx < word_index.length && rhs_idx < rhs.word_index.length) {
            int lhs_word = word_index[lhs_idx];
            int rhs_word = rhs.word_index[rhs_idx];
            if (lhs_word <= rhs_word) {
                lhs_idx++;
            }
            if (rhs_word <= lhs_word) {
                rhs_idx++;
            }
            num_words++;
        }
        // add word for whatever pointers not reached the end
        if (lhs_idx != word_index.length) {
            num_words += word_index.length - lhs_idx;
        }
        if (rhs_idx != rhs.word_index.length) {
            num_words += rhs.word_index.length - rhs_idx;
        }
        return num_words;
    }

    public LDAPartialTopics merge(LDAPartialTopics rhs) throws Exception {
        if (phi[0].length != rhs.phi[0].length) {
            throw new Exception(
                    "Try to merge partials with different nubmer of topics: "
                            + phi.length + " and " + rhs.phi.length);
        }
        int K = phi[0].length;
        int num_words = countUniqueWords(rhs);
        int[] word_idx_new = new int[num_words];
        double[][] phi_new = new double[num_words][K];
        int new_idx = 0;
        int lhs_idx = 0;
        int rhs_idx = 0;
        while (lhs_idx < word_index.length && rhs_idx < rhs.word_index.length) {
            int lhs_word = word_index[lhs_idx];
            int rhs_word = rhs.word_index[rhs_idx];
            if (lhs_word < rhs_word) {
                word_idx_new[new_idx] = lhs_word;
                for (int k = 0; k < K; k++) {
                    phi_new[new_idx][k] = phi[lhs_idx][k];
                }
                lhs_idx++;
            } else if (rhs_word < lhs_word) {
                word_idx_new[new_idx] = rhs_word;
                for (int k = 0; k < K; k++) {
                    phi_new[new_idx][k] = rhs.phi[rhs_idx][k];
                }
                rhs_idx++;
            } else {
                word_idx_new[new_idx] = rhs_word;
                for (int k = 0; k < K; k++) {
                    phi_new[new_idx][k] = rhs.phi[rhs_idx][k] + phi[lhs_idx][k];
                }
                rhs_idx++;
                lhs_idx++;
            }
            new_idx++;
        }
        // add word for whatever pointers not reached the end
        for (; lhs_idx < word_index.length; lhs_idx++) {
            int lhs_word = word_index[lhs_idx];
            word_idx_new[new_idx] = lhs_word;
            for (int k = 0; k < K; k++) {
                phi_new[new_idx][k] = phi[lhs_idx][k];
            }
            new_idx++;
        }
        for (; rhs_idx != rhs.word_index.length; rhs_idx++) {
            int rhs_word = rhs.word_index[rhs_idx];
            word_idx_new[new_idx] = rhs_word;
            for (int k = 0; k < K; k++) {
                phi_new[new_idx][k] = rhs.phi[rhs_idx][k];
            }
            new_idx++;
        }
        return new LDAPartialTopics(word_idx_new, phi_new);
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < word_index.length; i++) {
            b.append(i + " - " + word_index[i] + ": ");
            for (int k = 0; k < phi[i].length; k++) {
                b.append(phi[i][k] + ", ");
            }
            b.append("\n");
        }
        return b.toString();
    }

    public double[][] toTopicVectors() {
        // Ensure that words_index has no gaps.
        int word_max = word_index[word_index.length - 1];
        int K = phi[0].length;
        double[][] phi_new = new double[K][word_max + 1];
        for (int k = 0; k < K; k++) {
            double z = 0.0;
            for (int i = 0; i < word_index.length; i++) {
                int w = word_index[i];
                phi_new[k][w] = phi[i][k];
                z += phi[i][k];
            }
            for (int i = 0; i < phi_new[k].length; i++) {
                phi_new[k][i] /= z;
            }
        }
        return phi_new;
    }

    public double[] toTopicVector() throws Exception {
        double[][] phi_new = toTopicVectors();
        if (phi_new.length > 1) {
            throw new Exception(
                    "called toTopicVector() on a thing with multiple vectors");
        }
        return phi_new[0];
    }

    public LDADenseTopics toTopics() {
        return new LDADenseTopics(toTopicVectors());
    }

    public static void main(String[] argv) throws Exception {
        int[] words_lhs = new int[] { 1, 2, 4, 7, 10 };
        double[][] phi_lhs = new double[][] { { 0.3, 0.7 }, { 0.2, 0.8 },
                { 0.1, 0.9 }, { 0.5, 0.5 }, { 0.9, 0.1 } };
        int[] words_rhs = new int[] { 4, 10, 11, 12, 15 };
        double[][] phi_rhs = new double[][] { { 0.4, 0.6 }, { 0.3, 0.7 },
                { 0.3, 0.7 }, { 0.1, 0.9 }, { 0.4, 0.6 }, { 0.5, 0.5 } };
        LDAPartialTopics lhs = new LDAPartialTopics(words_lhs, phi_lhs);
        LDAPartialTopics rhs = new LDAPartialTopics(words_rhs, phi_rhs);
        System.out.println(lhs);
        System.out.println(rhs);
        System.out.println(rhs.merge(lhs));
        System.out.println(lhs.merge(rhs));
        System.out.println(lhs.merge(rhs).toTopics());
    }

}
