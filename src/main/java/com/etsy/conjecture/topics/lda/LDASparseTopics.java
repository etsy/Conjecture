package com.etsy.conjecture.topics.lda;

import java.io.Serializable;
import java.util.Map;

public class LDASparseTopics implements LDATopics, Serializable {

    private static final long serialVersionUID = 4878060449289865652L;
    int K;
    Map<Integer, Double> prob;
    LDADict dict;

    public LDASparseTopics(int K, Map<Integer, Double> prob) {
        this.prob = prob;
        this.K = K;
    }

    public void setDict(LDADict dict_) {
        dict = dict_;
    }

    public LDADict getDict() {
        return dict;
    }

    public double wordProb(int word, int topic) {
        int key = word * K + topic;
        if (prob.containsKey(key)) {
            return prob.get(key);
        } else {
            return 0.00000001;
        }
    }

    public int numTopics() {
        return K;
    }

    public int dictSize() {
        return dict.size();
    }

}
