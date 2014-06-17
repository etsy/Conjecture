package com.etsy.conjecture

import cascading.pipe.Pipe
import cascading.flow.FlowDef
import com.twitter.scalding._
import com.etsy.conjecture.data._

import scala.collection.generic
import scala.util.matching.Regex

// Input: line file in VW format
// Writes MulticlassLabeledInstances in JSON

trait VWReader {
    import Dsl._

    def parse(input: String): MulticlassLabeledInstance = {

        // parse header
        val a = input.split("""\s*\|""").toList
        var b = a(0).split("""\s+""").toList
        var label = b(0)
        var importance = 1.0
        var tag = ""

        try {
            if (b.length > 1) importance = b(1).toDouble
            if (b.length > 2) tag = b(2)
        } catch {
            case e: Exception => println("Ignoring header")
        }

        // create inst with header info
        val instObj = new MulticlassLabeledInstance(label)
        instObj.setId(tag)

        // parse remainder
        val remainder = input.split("\\s+").toList
        val pipePattern = """(.*)\|(.*)"""
        val pipeReg = (pipePattern).r

        var pastHeader = false
        var namespace = ""

        remainder.map {
            token: String =>
                if (pipeReg.pattern.matcher(token).matches) {
                    val pipeReg(before, after) = token
                    if (pastHeader)
                        addFeature(instObj, before, namespace)
                    namespace = extractNamespace(after) // will be "" if no namespace 
                    pastHeader = true
                } else {
                    if (pastHeader)
                        addFeature(instObj, token, namespace)
                }
        }
        instObj

    }

    def extractNamespace(token: String): String = {
        val pairReg = ("""(.+)\:(.+)""").r
        var namespace = token
        if (pairReg.pattern.matcher(token).matches) {
            val pairReg(term, value) = token
            namespace = term
            // TODO: return weight for namespace when models can handle that
        }
        namespace
    }

    def setId(instObj: MulticlassLabeledInstance, id: String): Boolean = {
        instObj.setId(id)
        true
    }

    def setImportance(instObj: MulticlassLabeledInstance, token: String): Boolean = {
        // TODO: set importance weighting here once we support it
        true
    }

    def addFeature(instObj: MulticlassLabeledInstance, token: String, namespace: String) {

        val pairPattern = """(.+)\:(.+)"""
        val pairReg = (pairPattern).r

        if (token == "")
            return

        try {
            if (pairReg.pattern.matcher(token).matches) {
                val pairReg(term, value) = token
                if (namespace == "") instObj.addTerm(term, value.toDouble) // catch numberFormatException
                else instObj.addTermWithNamespace(term, namespace, value.toDouble)
            } else {
                if (namespace == "") instObj.addTerm(token)
                else instObj.addTermWithNamespace(token, namespace)
            }
        } catch {
            case e: Exception => println("Ignore line: " + token)
        }
    }

}

