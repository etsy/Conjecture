package com.etsy.conjecture.text

import com.etsy.conjecture.data.{ AbstractInstance, BinaryLabeledInstance, LabeledInstance, StringKeyedVector }

import com.twitter.algebird.Operators._

import cascading.tuple.Fields
import cascading.pipe.Pipe

import scala.collection.JavaConverters._

object FeatureHelper {

    import com.twitter.scalding.Dsl._

    def keepFeaturesWithCountGreaterThan(pipe: Pipe, instance_field: Fields, n: Int): Pipe = {
        val counts = pipe
            .flatMapTo(instance_field -> ('term, '__count)) {
                v: AnyRef =>
                    val vector = v match {
                        case skv: StringKeyedVector => skv
                        case ins: AbstractInstance[_] => ins.getVector
                        case lin: LabeledInstance[_] => lin.getVector
                        case _ => throw new IllegalArgumentException("keepFeaturesWithCountGreaterThan does not expect class: " + v.getClass.getName)
                    }
                    vector.keySet.asScala.map { k => k -> 1 }
            }
            .groupBy('term) { _.sum[Long]('__count) }
            .filter('__count) { c: Long => c > n }
            .mapTo('term -> 'set) { t: String => Set(t) }
            .groupAll { _.plus[Set[String]]('set) }

        pipe
            .crossWithTiny(counts)
            .map(instance_field.append('set) -> instance_field) { x: (AnyRef, Set[String]) =>
                val skv = x._1 match {
                    case s: StringKeyedVector => s
                    case i: AbstractInstance[_] => i.getVector
                    case l: LabeledInstance[_] => l.getVector
                    case _ => throw new IllegalArgumentException("keepFeaturesWithCountGreaterThan does not expect class: " + x._1.getClass.getName)
                }
                val it = skv.iterator
                while (it.hasNext) {
                    val e = it.next
                    if (!x._2.contains(e.getKey)) {
                        it.remove
                    }
                }
                x._1
            }
    }

    def nGramsUpTo(string: String, n: Int = 2, prefix: String = ""): List[String] = {
        val toks = Text(string.toLowerCase).standardTextFilter.toString.split(" ").toList
        val toks_pad = "" +: toks :+ ""
        val grams = (1 to n).map { m => toks_pad.sliding(m).toList.map { p => p.mkString("::") } }.foldLeft(List[String]()) { _ ++ _ }
        grams.filter { g => g != "" }.map { g => prefix + g }
    }

    def stringListToSKV(list: List[String], weight: Double = 1.0): StringKeyedVector = {
        val skv = new StringKeyedVector();
        list.foreach { f => skv.setCoordinate(f, weight) }
        skv
    }

    def getEmailBody(body: String): Option[String] = {
        val p = parseEmailBodyToTextAndType(body)
        if (p._1 != null)
            Some(p._1)
        else
            None
    }

    def parseEmailBodyToTextAndType(body: String): (String, String) = {
        try {
            val email = com.codahale.jerkson.Json.parse[List[Map[String, String]]](body)
            val textParts = email.filter(part => part("type") == "text/plain")
            if (textParts.length > 0)
                (textParts.map(part => part("body")).mkString(" "), "text/plain")
            else {
                val htmlParts = email.filter(part => part("type") == "text/html")
                if (htmlParts.length > 0)
                    (htmlParts.map(part => part("body")).mkString(" "), "text/html")
                else
                    (null, "filter") // Filter this email
            }
        } catch {
            case _ : Exception => (null, "filter")
        }
    }

}
