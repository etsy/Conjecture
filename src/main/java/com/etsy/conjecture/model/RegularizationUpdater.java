// package com.etsy.conjecture.model;

// import com.etsy.conjecture.data.LazyVector;
// import com.etsy.conjecture.Utilities;


// public class RegularizationUpdater implements LazyVector.UpdateFunction {

//     private static final long serialVersionUID = 9153480933266800474L;
//     double laplace = 0.0;
//     double gaussian = 0.0;
//     LearningRateComputation computer = null;

//     public RegularizationUpdater() {}

//     public RegularizationUpdater(LearningRateComputation c) {
//         computer = c;
//     }

//     public RegularizationUpdater(LearningRateComputation c, double g, double l) {
//         computer = c;
//         gaussian = g;
//         laplace = l;
//     }

//     public double lazyUpdate(String feature, double param, long start, long end) {
//         if (Utilities.floatingPointEquals(laplace, 0.0d)
//             && Utilities.floatingPointEquals(gaussian, 0.0d)) {
//             return param;
//         }
//         for (long iter = start + 1; iter <= end; iter++) {
//             if (Utilities.floatingPointEquals(param, 0.0d)) {
//                 return 0.0d;
//             }
//             double eta = computer.getFeatureLearningRate(feature, iter);
//             /**
//              * TODO: patch so that param cannot cross 0.0 during gaussian update
//              */
//             param -= eta * gaussian * param;
//             if (param > 0.0) {
//                 param = Math.max(0.0, param - eta * laplace);
//             } else {
//                 param = Math.min(0.0, param + eta * laplace);
//             }
//         }
//         return param;
//     }

//     public RegularizationUpdater setLearningRateComputation(SingleLearningRate computer) {
//         this.computer = computer;
//         return this;
//     }

// }
