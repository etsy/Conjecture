// package com.etsy.conjecture.model;

// import java.util.Collection;
// import java.util.HashMap;
// import java.util.Iterator;
// import java.util.Map;

// import com.etsy.conjecture.Utilities;
// import com.etsy.conjecture.data.LabeledInstance;
// import com.etsy.conjecture.data.MulticlassLabel;
// import com.etsy.conjecture.data.MulticlassPrediction;
// import com.etsy.conjecture.data.StringKeyedVector;
// import com.etsy.conjecture.data.LazyVector;

// public class MulticlassLogisticRegression extends UpdateableMulticlassLinearModel {

//     static final long serialVersionUID = 666L;

//     public MulticlassLogisticRegression(String[] categories) {
//         super(categories);
//     }

//     @Override
//     public MulticlassPrediction predict(StringKeyedVector instance) {
//         Map<String, Double> scores = new HashMap<String, Double>();
//         double normalization = 0;

//         for (Map.Entry<String, LazyVector> e : param.entrySet()) {
//             double innerProduct = Math.exp(e.getValue().dot(instance));
//             scores.put(e.getKey(), innerProduct);
//             normalization += innerProduct;
//         }

//         for (Map.Entry<String, Double> e : scores.entrySet()) {
//             scores.put(e.getKey(), e.getValue() / normalization);
//         }

//         return new MulticlassPrediction(scores);
//     }

//     @Override
//     public void updateRule(LabeledInstance<MulticlassLabel> li) {
//         double normalization = 0.0;
//         Map<String, Double> scores = new HashMap<String, Double>();
//         for (Map.Entry<String, LazyVector> e : param.entrySet()) {
//             double score = Utilities.logistic(e.getValue().dot(li.getVector()));
//             scores.put(e.getKey(), score);
//             normalization += score;
//         }

//         for (Map.Entry<String, LazyVector> e : param.entrySet()) {
//             double label = e.getKey().equals(li.getLabel().getLabel()) ? 1.0
//                     : 0.0;
//             e.getValue().addScaled(li.getVector(),
//                     0.01 * (label - scores.get(e.getKey()) / normalization));
//         }
//     }

//     @Override
//     public String getModelType() {
//         return "multiclass_logistic_regression";
//     }

// }
