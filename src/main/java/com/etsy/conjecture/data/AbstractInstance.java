package com.etsy.conjecture.data;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class AbstractInstance<T extends AbstractInstance<T>> {

    protected static final String SEP = "___";
    public String id;
    public String supporting_data;
    protected double weight;

    StringKeyedVector vector;

    public AbstractInstance() {
        this(new StringKeyedVector(), 1.0);
    }

    public AbstractInstance(double weight) {
        this(new StringKeyedVector(), weight);
    }

    public AbstractInstance(StringKeyedVector skv) {
        this(skv, 1.0);
    }

    public AbstractInstance(StringKeyedVector skv, double weight) {
        this.vector = skv;
        this.weight = weight;
    }

    public AbstractInstance(Map<String, Double> map) {
        this(map, 1.0);
    }

    public AbstractInstance(Map<String, Double> map, double weight) {
        this.vector = new StringKeyedVector(map);
        this.weight = weight;
    }

    @SuppressWarnings("unchecked")
    public T setWeight(double weight) {
        this.weight = weight;
        return (T)this;
    }

    public double getWeight() {
        return weight;
    }

    public String getId() {
        return id;
    }

    public StringKeyedVector getVector() {
        return vector;
    }

    public void setSupportingData(String s) {
        supporting_data = s;
    }

    public String getSupportingData() {
        return supporting_data;
    }

    @SuppressWarnings("unchecked")
    public T setCoordinate(String id, double value) {
        vector.setCoordinate(id, value);
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T addToCoordinate(String id, double value) {
        vector.addToCoordinate(id, value);
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T setId(String id) {
        this.id = id;
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.etsy.conjecture.data.InstanceInterface#addTerm(java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T addTerm(String term) {
        addTerm(term, 1.);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.etsy.conjecture.data.InstanceInterface#addTerm(java.lang.String,
     * double)
     */

    @SuppressWarnings("unchecked")
    public T addTerm(String term, double featureWeight) {
        addToCoordinate(term, featureWeight);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addTermWithNamespace(java.
     * lang.String, java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T addTermWithNamespace(String term, String namespace) {
        addTermWithNamespace(term, namespace, 1);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addTermWithNamespace(java.
     * lang.String, java.lang.String, double)
     */

    @SuppressWarnings("unchecked")
    public T addTermWithNamespace(String term, String namespace,
            double featureWeight) {
        addToCoordinate(namespace + SEP + term, featureWeight);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addTerms(java.util.Collection,
     * double)
     */

    @SuppressWarnings("unchecked")
    public T addTerms(Collection<String> terms, double featureWeight) {
        for (String term : terms) {
            addToCoordinate(term, featureWeight);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addTerms(java.util.Collection)
     */

    @SuppressWarnings("unchecked")
    public T addTerms(Collection<String> terms) {
        addTerms(terms, 1.);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addTermsWithNamespace(java
     * .util.Collection, java.lang.String, double)
     */

    @SuppressWarnings("unchecked")
    public T addTermsWithNamespace(Collection<String> terms, String namespace,
            double featureWeight) {
        for (String term : terms) {
            addTermWithNamespace(term, namespace, featureWeight);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addTermsWithNamespace(java
     * .util.Collection, java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T addTermsWithNamespace(Collection<String> terms, String namespace) {
        addTermsWithNamespace(terms, namespace, 1.);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addTerms(java.lang.String[],
     * double)
     */

    @SuppressWarnings("unchecked")
    public T addTerms(String[] terms, double featureWeight) {
        for (String term : terms) {
            addToCoordinate(term, featureWeight);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addTerms(java.lang.String[])
     */

    @SuppressWarnings("unchecked")
    public T addTerms(String[] terms) {
        addTerms(terms, 1.);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addTermsWithNamespace(java
     * .lang.String[], java.lang.String, double)
     */

    @SuppressWarnings("unchecked")
    public T addTermsWithNamespace(String[] terms, String namespace,
            double featureWeight) {
        for (String term : terms) {
            addTermWithNamespace(term, namespace, featureWeight);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addTermsWithNamespace(java
     * .lang.String[], java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T addTermsWithNamespace(String[] terms, String namespace) {
        addTermsWithNamespace(terms, namespace, 1.);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addTermsWithWeights(java.util
     * .Map)
     */

    @SuppressWarnings("unchecked")
    public T addTermsWithWeights(Map<String, Double> termsWithWeights) {
        for (String term : termsWithWeights.keySet()) {
            addTerm(term, termsWithWeights.get(term));
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addTermsWithWeightsWithNamespace
     * (java.util.Map, java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T addTermsWithWeightsWithNamespace(
            Map<String, Double> termsWithWeights, String namespace) {
        for (String term : termsWithWeights.keySet()) {
            addTermWithNamespace(term, namespace, termsWithWeights.get(term));
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addNumericArrayWithNamespace
     * (double[], java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T addNumericArrayWithNamespace(double[] array, String namespace) {
        for (int i = 0; i < array.length; i++) {
            addToCoordinate(namespace + SEP + i, array[i]);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.etsy.conjecture.data.InstanceInterface#addNumericArray(double[])
     */

    @SuppressWarnings("unchecked")
    public T addNumericArray(double[] array) {
        for (int i = 0; i < array.length; i++) {
            addToCoordinate("" + i, array[i]);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addNumericArrayWithNamespace
     * (java.lang.Double[], java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T addNumericArrayWithNamespace(Double[] array, String namespace) {
        for (int i = 0; i < array.length; i++) {
            addToCoordinate(namespace + SEP + i, array[i]);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addNumericArray(java.lang.
     * Double[])
     */

    @SuppressWarnings("unchecked")
    public T addNumericArray(Double[] array) {
        for (int i = 0; i < array.length; i++) {
            addToCoordinate("" + i, array[i]);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addNumericArrayWithNamespace
     * (java.util.List, java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T addNumericArrayWithNamespace(List<Double> values, String namespace) {
        for (int i = 0; i < values.size(); i++) {
            addToCoordinate(namespace + SEP + i, values.get(i));
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addNumericArray(java.util.
     * List)
     */

    @SuppressWarnings("unchecked")
    public T addNumericArray(List<Double> values) {
        for (int i = 0; i < values.size(); i++) {
            addToCoordinate("" + i, values.get(i));
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#setNumericArrayWithNamespace
     * (double[], java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T setNumericArrayWithNamespace(double[] array, String namespace) {
        for (int i = 0; i < array.length; i++) {
            addToCoordinate(namespace + SEP + i, array[i]);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.etsy.conjecture.data.InstanceInterface#setNumericArray(double[])
     */

    @SuppressWarnings("unchecked")
    public T setNumericArray(double[] array) {
        for (int i = 0; i < array.length; i++) {
            addToCoordinate("" + i, array[i]);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#setNumericArrayWithNamespace
     * (java.lang.Double[], java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T setNumericArrayWithNamespace(Double[] array, String namespace) {
        for (int i = 0; i < array.length; i++) {
            addToCoordinate(namespace + SEP + i, array[i]);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#setNumericArray(java.lang.
     * Double[])
     */

    @SuppressWarnings("unchecked")
    public T setNumericArray(Double[] array) {
        for (int i = 0; i < array.length; i++) {
            addToCoordinate("" + i, array[i]);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#setNumericArrayWithNamespace
     * (java.util.List, java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T setNumericArrayWithNamespace(List<Double> values, String namespace) {
        for (int i = 0; i < values.size(); i++) {
            addToCoordinate(namespace + SEP + i, values.get(i));
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#setNumericArray(java.util.
     * List)
     */

    @SuppressWarnings("unchecked")
    public T setNumericArray(List<Double> values) {
        for (int i = 0; i < values.size(); i++) {
            addToCoordinate("" + i, values.get(i));
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.etsy.conjecture.data.InstanceInterface#addIdField(long, double)
     */

    @SuppressWarnings("unchecked")
    public T addIdField(long id, double featureWeight) {
        addToCoordinate("" + id, featureWeight);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.etsy.conjecture.data.InstanceInterface#addIdField(long)
     */

    @SuppressWarnings("unchecked")
    public T addIdField(long id) {
        addIdField(id, 1.);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addIdFieldWithNamespace(long,
     * double, java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T addIdFieldWithNamespace(long id, double featureWeight,
            String namespace) {
        addToCoordinate(namespace + SEP + id, featureWeight);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addIdFieldWithNamespace(long,
     * java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T addIdFieldWithNamespace(long id, String namespace) {
        addIdFieldWithNamespace(id, 1., namespace);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.etsy.conjecture.data.InstanceInterface#addIdField(int, double)
     */

    @SuppressWarnings("unchecked")
    public T addIdField(int id, double featureWeight) {
        addToCoordinate("" + id, featureWeight);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.etsy.conjecture.data.InstanceInterface#addIdField(int)
     */

    @SuppressWarnings("unchecked")
    public T addIdField(int id) {
        addIdField(id, 1.);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addIdFieldWithNamespace(int,
     * double, java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T addIdFieldWithNamespace(int id, double featureWeight,
            String namespace) {
        addToCoordinate(namespace + SEP + id, featureWeight);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addIdFieldWithNamespace(int,
     * java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T addIdFieldWithNamespace(int id, String namespace) {
        addIdFieldWithNamespace(id, 1., namespace);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.etsy.conjecture.data.InstanceInterface#addIds(long[], double)
     */

    @SuppressWarnings("unchecked")
    public T addIds(long[] ids, double featureWeight) {
        for (long id : ids) {
            addToCoordinate("" + id, featureWeight);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.etsy.conjecture.data.InstanceInterface#addIds(long[])
     */

    @SuppressWarnings("unchecked")
    public T addIds(long[] ids) {
        addIds(ids, 1.);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.etsy.conjecture.data.InstanceInterface#addIds(int[], double)
     */

    @SuppressWarnings("unchecked")
    public T addIds(int[] ids, double featureWeight) {
        for (long id : ids) {
            addToCoordinate("" + id, featureWeight);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.etsy.conjecture.data.InstanceInterface#addIds(int[])
     */

    @SuppressWarnings("unchecked")
    public T addIds(int[] ids) {
        addIds(ids, 1.);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addIds(java.util.Collection,
     * double)
     */

    @SuppressWarnings("unchecked")
    public T addIds(Collection<Integer> ids, double featureWeight) {
        for (long id : ids) {
            addToCoordinate("" + id, featureWeight);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addIds(java.util.Collection)
     */

    @SuppressWarnings("unchecked")
    public T addIds(Collection<Integer> ids) {
        addIds(ids, 1.);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addIdsWithNamespace(long[],
     * double, java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T addIdsWithNamespace(long[] ids, double featureWeight,
            String namespace) {
        for (long id : ids) {
            addToCoordinate(namespace + SEP + id, featureWeight);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addIdsWithNamespace(long[],
     * java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T addIdsWithNamespace(long[] ids, String namespace) {
        addIdsWithNamespace(ids, 1., namespace);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addIdsWithNamespace(int[],
     * double, java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T addIdsWithNamespace(int[] ids, double featureWeight,
            String namespace) {
        for (int id : ids) {
            addToCoordinate(namespace + SEP + id, featureWeight);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addIdsWithNamespace(int[],
     * java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T addIdsWithNamespace(int[] ids, String namespace) {
        addIdsWithNamespace(ids, 1., namespace);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addIdsWithNamespace(java.util
     * .Collection, double, java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T addIdsWithNamespace(Collection<Long> ids, double featureWeight,
            String namespace) {
        for (Long id : ids) {
            addToCoordinate(namespace + SEP + id, featureWeight);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#addIdsWithNamespace(java.util
     * .Collection, java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T addIdsWithNamespace(Collection<Long> ids, String namespace) {
        addIdsWithNamespace(ids, 1., namespace);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.etsy.conjecture.data.InstanceInterface#setIdField(long, double)
     */

    @SuppressWarnings("unchecked")
    public T setIdField(long id, double featureWeight) {
        addToCoordinate("" + id, featureWeight);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.etsy.conjecture.data.InstanceInterface#setIdField(long)
     */

    @SuppressWarnings("unchecked")
    public T setIdField(long id) {
        setIdField(id, 1.);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#setIdFieldWithNamespace(long,
     * double, java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T setIdFieldWithNamespace(long id, double featureWeight,
            String namespace) {
        addToCoordinate(namespace + SEP + id, featureWeight);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#setIdFieldWithNamespace(long,
     * java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T setIdFieldWithNamespace(long id, String namespace) {
        setIdFieldWithNamespace(id, 1., namespace);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.etsy.conjecture.data.InstanceInterface#setIdField(int, double)
     */

    @SuppressWarnings("unchecked")
    public T setIdField(int id, double featureWeight) {
        addToCoordinate("" + id, featureWeight);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.etsy.conjecture.data.InstanceInterface#setIdField(int)
     */

    @SuppressWarnings("unchecked")
    public T setIdField(int id) {
        setIdField(id, 1.);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#setIdFieldWithNamespace(int,
     * double, java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T setIdFieldWithNamespace(int id, double featureWeight,
            String namespace) {
        addToCoordinate(namespace + SEP + id, featureWeight);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#setIdFieldWithNamespace(int,
     * java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T setIdFieldWithNamespace(int id, String namespace) {
        setIdFieldWithNamespace(id, 1., namespace);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.etsy.conjecture.data.InstanceInterface#setIds(long[], double)
     */

    @SuppressWarnings("unchecked")
    public T setIds(long[] ids, double featureWeight) {
        for (long id : ids) {
            addToCoordinate("" + id, featureWeight);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.etsy.conjecture.data.InstanceInterface#setIds(long[])
     */

    @SuppressWarnings("unchecked")
    public T setIds(long[] ids) {
        setIds(ids, 1.);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.etsy.conjecture.data.InstanceInterface#setIds(int[], double)
     */

    @SuppressWarnings("unchecked")
    public T setIds(int[] ids, double featureWeight) {
        for (long id : ids) {
            addToCoordinate("" + id, featureWeight);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.etsy.conjecture.data.InstanceInterface#setIds(int[])
     */

    @SuppressWarnings("unchecked")
    public T setIds(int[] ids) {
        setIds(ids, 1.);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#setIds(java.util.Collection,
     * double)
     */

    @SuppressWarnings("unchecked")
    public T setIds(Collection<Integer> ids, double featureWeight) {
        for (long id : ids) {
            addToCoordinate("" + id, featureWeight);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#setIds(java.util.Collection)
     */

    @SuppressWarnings("unchecked")
    public T setIds(Collection<Integer> ids) {
        setIds(ids, 1.);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#setIdsWithNamespace(long[],
     * double, java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T setIdsWithNamespace(long[] ids, double featureWeight,
            String namespace) {
        for (long id : ids) {
            addToCoordinate(namespace + SEP + id, featureWeight);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#setIdsWithNamespace(long[],
     * java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T setIdsWithNamespace(long[] ids, String namespace) {
        setIdsWithNamespace(ids, 1., namespace);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#setIdsWithNamespace(int[],
     * double, java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T setIdsWithNamespace(int[] ids, double featureWeight,
            String namespace) {
        for (int id : ids) {
            addToCoordinate(namespace + SEP + id, featureWeight);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#setIdsWithNamespace(int[],
     * java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T setIdsWithNamespace(int[] ids, String namespace) {
        setIdsWithNamespace(ids, 1., namespace);
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#setIdsWithNamespace(java.util
     * .Collection, double, java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T setIdsWithNamespace(Collection<Long> ids, double featureWeight,
            String namespace) {
        for (Long id : ids) {
            addToCoordinate(namespace + SEP + id, featureWeight);
        }
        return (T)this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.etsy.conjecture.data.InstanceInterface#setIdsWithNamespace(java.util
     * .Collection, java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public T setIdsWithNamespace(Collection<Long> ids, String namespace) {
        setIdsWithNamespace(ids, 1., namespace);
        return (T)this;
    }

}
