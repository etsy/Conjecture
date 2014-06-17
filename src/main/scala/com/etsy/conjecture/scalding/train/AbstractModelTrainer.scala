package com.etsy.conjecture.scalding.train

import cascading.pipe.Pipe

import com.etsy.conjecture.data._
import com.etsy.conjecture.model._

import com.twitter.scalding._

trait AbstractModelTrainer[L <: Label, M <: UpdateableModel[L, M]] extends Serializable {

    def train(instances: Pipe, instanceField: Symbol, modelField: Symbol): Pipe

    def reTrain(instances: Pipe, instanceField: Symbol, model: Pipe, modelField: Symbol): Pipe
}
