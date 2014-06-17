package com.etsy.conjecture.topics.lda;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class LDAPartialSparseTopics implements Serializable {

    private static final long serialVersionUID = -5073459183590344302L;
    private int K;
    private Map<Integer, Double> phi;

    public LDAPartialSparseTopics(int K, Map<Integer, Double> phi) {
        this.K = K;
        this.phi = phi;
    }

    public LDAPartialSparseTopics merge(LDAPartialSparseTopics rhs)
            throws Exception {
        if (K != rhs.K) {
            throw new Exception(
                    "Try to merge partials with different nubmer of topics: "
                            + K + " and " + rhs.K);
        }
        Map<Integer, Double> a = phi.size() < rhs.phi.size() ? phi : rhs.phi;
        Map<Integer, Double> b = phi.size() < rhs.phi.size() ? rhs.phi : phi;
        for (Map.Entry<Integer, Double> e : a.entrySet()) {
            if (b.containsKey(e.getKey())) {
                b.put(e.getKey(), e.getValue() + b.get(e.getKey()));
            } else {
                b.put(e.getKey(), e.getValue());
            }
        }
        return new LDAPartialSparseTopics(K, b);
    }

    public LDASparseTopics toTopics() {
        // renormalize.
        double[] z = new double[K];
        for (Map.Entry<Integer, Double> e : phi.entrySet()) {
            z[e.getKey() % K] += e.getValue();
        }
        Set<Integer> keys = phi.keySet();
        for (int i : keys) {
            phi.put(i, phi.get(i) / z[i % K]);
        }
        return new LDASparseTopics(K, phi);
    }

}
