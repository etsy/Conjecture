#!/bin/bash

# - make monolithic conjecture jar.
sbt clean assembly
# - make the instances.
java -cp target/conjecture-assembly-*.jar com.twitter.scalding.Tool com.etsy.conjecture.demo.IrisDataToMulticlassLabeledInstances --input_file data/iris.tsv --output_file iris_model/instances --local
# - construct the classifier.
java -cp target/conjecture-assembly-*.jar com.twitter.scalding.Tool com.etsy.conjecture.demo.LearnMulticlassClassifier --input iris_model/instances --output iris_model --class_names Iris-versicolor,Iris-virginica,Iris-setosa --iters 5 --folds 3 --local
