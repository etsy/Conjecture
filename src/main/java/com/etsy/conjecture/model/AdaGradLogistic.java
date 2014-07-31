// package com.etsy.conjecture.model;

// import com.etsy.conjecture.Utilities;
// import com.etsy.conjecture.data.BinaryLabel;
// import com.etsy.conjecture.data.LabeledInstance;
// import com.etsy.conjecture.data.StringKeyedVector;
// import java.util.Iterator;
// import java.util.Map;
// import static com.google.common.base.Preconditions.checkArgument;

// /**
//  * Implements adagrad for logistic regression as described here: 
//  * http://www.ark.cs.cmu.edu/cdyer/adagrad.pdf 
//  */ 
// public class AdaGradLogistic extends UpdateableLinearModel<BinaryLabel> {

//     private static final long serialVersionUID = 1L;
//     private StringKeyedVector gradients = new StringKeyedVector();
//     private double n = 1d;

//     public AdaGradLogistic() {
//         super();
//     }

//     public AdaGradLogistic setGradients(StringKeyedVector assignment) {
//         gradients = assignment;
//         return this;
//     }

//     public AdaGradLogistic(StringKeyedVector param) {
//         super(param);
//     }

//     public BinaryLabel predict(StringKeyedVector instance, double bias) {
//         return new BinaryLabel(Utilities.logistic(instance.dot(param) + bias));
//     }

//     public void updateRule(LabeledInstance<BinaryLabel> instance, double bias) {
//         double hypothesis = Utilities.logistic(instance.getVector().dot(param)
//                 + bias);
//         double label = instance.getLabel().getValue();
//         double diff = label - hypothesis;
//         adagradUpdate(instance.getVector(), diff);
//     }

//     /**
//      * Computes a per feature learning rate and does the update
//      */
//     public void adagradUpdate(StringKeyedVector instance, double diff) {
//         Iterator it = instance.iterator();
//         while (it.hasNext()) {
//             Map.Entry<String,Double> pairs = (Map.Entry)it.next();
//             String key = pairs.getKey();
//             double value = pairs.getValue();
//             double gradient = value*diff;
//             if (gradients.containsKey(key)) {
//                 gradients.addToCoordinate(key, gradient*gradient);
//                 double featureLearningRate = n/Math.sqrt(gradients.getCoordinate(key));
//                 param.addToCoordinate(key, featureLearningRate * gradient);
//             } else {
//                 gradients.addToCoordinate(key, 1d+(gradient * gradient));
//                 double featureLearningRate = n/Math.sqrt(gradients.getCoordinate(key));
//                 param.addToCoordinate(key, featureLearningRate * gradient);
//             }
//        }
//     }

//     public AdaGradLogistic setLearningRate(double n) {
//         checkArgument(n > 0, "n must be greater than 0. Given: %s", n);
//         this.n = n;
//         return this;
//     }

//     protected String getModelType() {
//         return "adagrad_logistic";
//     }
// }
