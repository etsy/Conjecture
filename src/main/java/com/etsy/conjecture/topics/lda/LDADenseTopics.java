package com.etsy.conjecture.topics.lda;

import java.io.Serializable;
import java.util.Random;

public class LDADenseTopics implements LDATopics, Serializable {

    private static final long serialVersionUID = 8704084406257021101L;
    int num_topics;
    int dict_size;
    double[][] topic_prob;
    LDADict dict;
    Random rnd = new Random();

    public LDADenseTopics(double[][] topic_prob) {
        this.num_topics = topic_prob.length;
        this.dict_size = topic_prob[0].length;
        this.topic_prob = topic_prob;
    }

    public void setTopicProb(int topic, double[] prob) {
        topic_prob[topic] = prob;
    }

    public void setDict(LDADict dict_) throws Exception {
        if (dict_.size() < dict_size)
            throw new Exception("trying to set the dict with size "
                    + dict_.size() + " on a topic model with dict size "
                    + dict_size);
        dict = dict_;
        dict_size = dict.size();
    }

    public LDADict getDict() {
        return dict;
    }

    public double wordProb(int word, int topic) {
        return topic_prob[topic][word];
    }

    public int numTopics() {
        return num_topics;
    }

    public int dictSize() {
        return dict_size;
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        for (int k = 0; k < num_topics; k++) {
            b.append(k + ": ");
            for (int w = 0; w < dict_size; w++) {
                if (dict == null)
                    b.append(w + ":"
                            + String.format("%.3f, ", topic_prob[k][w]));
                else
                    b.append(dict.word(w) + ":"
                            + String.format("%.3f, ", topic_prob[k][w]));
            }
            b.append("\n");
        }
        return b.toString();
    }

}
