package com.etsy.conjecture.data;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface InstanceInterface<T extends InstanceInterface<T>> {

    public abstract String getId();

    public abstract T setId(String id);

    public abstract T addTerm(String term);

    public abstract T addTerm(String term, double featureWeight);

    public abstract T addTermWithNamespace(String term, String namespace);

    public abstract T addTermWithNamespace(String term, String namespace,
            double featureWeight);

    public abstract T addTerms(Collection<String> terms, double featureWeight);

    public abstract T addTerms(Collection<String> terms);

    public abstract T addTermsWithNamespace(Collection<String> terms,
            String namespace, double featureWeight);

    public abstract T addTermsWithNamespace(Collection<String> terms,
            String namespace);

    public abstract T addTerms(String[] terms, double featureWeight);

    public abstract T addTerms(String[] terms);

    public abstract T addTermsWithNamespace(String[] terms, String namespace,
            double featureWeight);

    public abstract T addTermsWithNamespace(String[] terms, String namespace);

    public abstract T addTermsWithWeights(Map<String, Double> termsWithWeights);

    public abstract T addTermsWithWeightsWithNamespace(
            Map<String, Double> termsWithWeights, String namespace);

    public abstract T addNumericArrayWithNamespace(double[] array,
            String namespace);

    public abstract T addNumericArray(double[] array);

    public abstract T addNumericArrayWithNamespace(Double[] array,
            String namespace);

    public abstract T addNumericArray(Double[] array);

    public abstract T addNumericArrayWithNamespace(List<Double> values,
            String namespace);

    public abstract T addNumericArray(List<Double> values);

    public abstract T setNumericArrayWithNamespace(double[] array,
            String namespace);

    public abstract T setNumericArray(double[] array);

    public abstract T setNumericArrayWithNamespace(Double[] array,
            String namespace);

    public abstract T setNumericArray(Double[] array);

    public abstract T setNumericArrayWithNamespace(List<Double> values,
            String namespace);

    public abstract T setNumericArray(List<Double> values);

    public abstract T addIdField(long id, double featureWeight);

    public abstract T addIdField(long id);

    public abstract T addIdFieldWithNamespace(long id, double featureWeight,
            String namespace);

    public abstract T addIdFieldWithNamespace(long id, String namespace);

    public abstract T addIdField(int id, double featureWeight);

    public abstract T addIdField(int id);

    public abstract T addIdFieldWithNamespace(int id, double featureWeight,
            String namespace);

    public abstract T addIdFieldWithNamespace(int id, String namespace);

    public abstract T addIds(long[] ids, double featureWeight);

    public abstract T addIds(long[] ids);

    public abstract T addIds(int[] ids, double featureWeight);

    public abstract T addIds(int[] ids);

    public abstract T addIds(Collection<Integer> ids, double featureWeight);

    public abstract T addIds(Collection<Integer> ids);

    public abstract T addIdsWithNamespace(long[] ids, double featureWeight,
            String namespace);

    public abstract T addIdsWithNamespace(long[] ids, String namespace);

    public abstract T addIdsWithNamespace(int[] ids, double featureWeight,
            String namespace);

    public abstract T addIdsWithNamespace(int[] ids, String namespace);

    public abstract T addIdsWithNamespace(Collection<Long> ids,
            double featureWeight, String namespace);

    public abstract T addIdsWithNamespace(Collection<Long> ids, String namespace);

    public abstract T setIdField(long id, double featureWeight);

    public abstract T setIdField(long id);

    public abstract T setIdFieldWithNamespace(long id, double featureWeight,
            String namespace);

    public abstract T setIdFieldWithNamespace(long id, String namespace);

    public abstract T setIdField(int id, double featureWeight);

    public abstract T setIdField(int id);

    public abstract T setIdFieldWithNamespace(int id, double featureWeight,
            String namespace);

    public abstract T setIdFieldWithNamespace(int id, String namespace);

    public abstract T setIds(long[] ids, double featureWeight);

    public abstract T setIds(long[] ids);

    public abstract T setIds(int[] ids, double featureWeight);

    public abstract T setIds(int[] ids);

    public abstract T setIds(Collection<Integer> ids, double featureWeight);

    public abstract T setIds(Collection<Integer> ids);

    public abstract T setIdsWithNamespace(long[] ids, double featureWeight,
            String namespace);

    public abstract T setIdsWithNamespace(long[] ids, String namespace);

    public abstract T setIdsWithNamespace(int[] ids, double featureWeight,
            String namespace);

    public abstract T setIdsWithNamespace(int[] ids, String namespace);

    public abstract T setIdsWithNamespace(Collection<Long> ids,
            double featureWeight, String namespace);

    public abstract T setIdsWithNamespace(Collection<Long> ids, String namespace);

}
