# Conjecture [![Build Status](https://travis-ci.org/etsy/Conjecture.svg?branch=master)](https://travis-ci.org/etsy/Conjecture)

Conjecture is a framework for building machine learning models in Hadoop using the Scalding DSL.
The goal of this project is to enable the development of statistical models as viable components
in a wide range of product settings. Applications include classification and categorization,
recommender systems, ranking, filtering, and regression (predicting real-valued numbers).
Conjecture has been designed with a primary emphasis on flexibility and can handle a wide variety of inputs.
Integration with Hadoop and scalding enable seamless handling of extremely large data volumes,
and integration with established ETL processes. Predicted labels can either be consumed directly
by the web stack using the dataset loader, or models can be deployed and consumed by live web code.
Currently, binary classification (assigning one of two possible labels to input data points)
is the most mature component of the Conjecture package.

# Tutorial
There are a few stages involved in training a machine learning model using Conjecture.

## Create Training Data
We represent the training data as "feature vectors" which are just mappings of feature names to real values.
In this case we represent them as a java map of strings to doubles
(although we have a class StringKeyedVector which provides convenience methods for feature vector construction).
We also need the true label of each instance, which we represent as 0 and 1
(the mapping of these binary labels to e.g., "male" and "female" is up to the user).
We construct BinaryLabeledInstances, which are just wrappers for a feature vector and a label.

    val bl = new BinaryLabeledInstance(0.0)
    bl.addTerm("bias", 1.0)
    bl.addTerm("some_feature", 0.5)

## Training a Classifier
Classifiers are essentially trained by presenting the labeled instances to them.  There are several kinds 
of linear classifiers we implement, among them:

* Logistic regression,
* Perceptron,
* MIRA (a large margin perceptron model),
* Passive aggressive.

These models all have several options, such as learning rate, regularization parameters and so on.  We supply
reasonable defaults for these parameters although they can be changed readily.  To train a linear model
simply call the update function with the labeled instance:

    val p = new LogisticRegression()
    p.update(bl)

In order to make this procedure tractable for large datasets, we provided scalding wrappers for the training.
These operate by training several small models on mappers, then aggregating them into a final complete model
on the reducers.  This wrapper is called like so:

    new BinaryModelTrainer(args)
      .train(instances, 'instance, 'model)
      .write(SequenceFile("model"))
      .map('model -> 'model){ x : UpdateableBinaryModel => new com.google.gson.Gson.toJson(x) }
      .write(Tsv("model_json"))

This code segment will train a model using a pipe called instances which has a field called instance which contains
the BinaryLabeledInstance objects.  It produces a pipe with a single field containing the completed model, which can
then be written to disk.

This class uses the command line args object from scalding, in order to let you set some options on the command line.
Some useful options are:

| Argument                            | Possible values                               | Default            | Meaning                                          |
|-------------------------------------|-----------------------------------------------|--------------------|--------------------------------------------------|
| --model                             | mira, logistic_regression, passive_aggressive | passive_aggressive | The type of model to use.                        |
| --iters                             | 1, 2, 3...                                    | 1                  | The number of iterations of training to perform. |
| --zero_class_prob, --one_class_prob | [0, 1]                                        | 1                  |                                                  |

To see all the command line options, see the BinaryModelTrainer class.

## Evaluating a Classifier
It is important to get a sense of the performance you can expect out of your classifier on unseen data.
In order to do this we recommend to use cross validation.
In essence, your input set of instances is split up into testing and training portions (multiple different ways),
then a classifier is trained on each training portion, and evaluated (against the true labels which are present)
using the testing portion.
This is all wrapped up in a class called BinaryCrossValidator, it is used like so:

    new BinaryCrossValidator(args, 5)
      .crossValidate(instances, 'instance)
      .write(Tsv("model_xval"))

This class also takes the command line arguments, which it passes to a model trainer for each fold.
This allows the specification of options to the cross validated models on the command line.
The output contains statistics about the performance of the model as well as the confusion matrices
for each fold.

A script is included which cross validates a logistic regression model on the iris dataset.



