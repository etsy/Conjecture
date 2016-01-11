package com.etsy.conjecture.scalding.util

import java.io.Serializable
import com.twitter.scalding.Args
import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.reflect.runtime.universe._

class DynamicOptions(args: Args) extends Serializable {
  private val opts = new HashMap[String, DynamicOption[_]]
  parse(args)

  def get(key: String) = opts.get(key)

  def size = opts.size

  var strict = true

  def values = opts.values


  def +=(c: DynamicOption[_]): this.type = {
    if (opts.contains(c.name)) throw new Error("DynamicOption " + c.name + " already exists.")
    opts(c.name) = c
    this
  }

  def -=(c: DynamicOption[_]): this.type = {
    opts -= c.name
    this
  }

  /** The arguments that were unqualified by dashed options.
    * Currently unused but held for future work.
    */
  private val _remaining = new ArrayBuffer[String]
  def remaining: Seq[String] = _remaining

  /** Parse sequence of command-line arguments. */
  def parse(args: Args): Unit = {
    args.m.filter(!_._2.isEmpty).foreach {
      case (key, listValues) =>
      //Only take first value from the values
      val value = listValues.head
      key match {
        case k: String if (opts.contains(k)) => opts(k).parseValue(value)
        case "" =>  _remaining += value
        case _ => {
          opts.+=((key, new DynamicOption(key, value)))
          opts(key).parseValue(value)
        }
      }
    }
  }

  def unParse: Args = {
      val newM = opts.map{cmd => cmd._1->List(cmd._2.value.toString())}.toMap
      new Args(newM)
  }


  class DynamicOption[T](val name:String, val defaultValue:T)(implicit m: Manifest[T]) extends com.etsy.conjecture.scalding.util.DynamicOption[T] with Serializable {
    def this(name: String)(implicit m: Manifest[T]) = this(name, null.asInstanceOf[T])

    DynamicOptions.this += this
    private def valueClass: Class[_] = m.runtimeClass
    private def valueType = m.runtimeClass

    private def matches(str: String): Boolean = str == ("--" + name)

    var _value = defaultValue

    def value: T = {
      _value
    }

    def setValue(v: T) {
      _value = v
    }

    def hasValue = !(valueType eq classOf[Nothing])

    var setCount = 0

    /** Parses each of the supported value, increments set counter, and sets the value */
    def parseValue(args: String) = {
      setCount += 1
      setValue(valueType match {
        case t if t eq classOf[List[String]] => args.asInstanceOf[T]
        case t if t eq classOf[List[Int]] => args.map(_.toInt).asInstanceOf[T]
        case t if t eq classOf[List[Double]] => args.map(_.toDouble).asInstanceOf[T]
        case t if t eq classOf[Char] => args.head.asInstanceOf[T]
        case t if t eq classOf[String] => args.asInstanceOf[T]
        case t if t eq classOf[Short] => args.toShort.asInstanceOf[T]
        case t if t eq classOf[Int] => args.toInt.asInstanceOf[T]
        case t if t eq classOf[Long] => args.toLong.asInstanceOf[T]
        case t if t eq classOf[Double] => args.toDouble.asInstanceOf[T]
        case t if t eq classOf[Float] => args.toFloat.asInstanceOf[T]
        case t if t eq classOf[Boolean] => args.toBoolean.asInstanceOf[T]
        case otw => throw new Error("DynamicOption does not handle values of type " + otw)
      })
    }
  }

  override def toString: String = values.map(_.toString).mkString("\t")
}

trait DynamicOption[T] {
  def name: String
  def defaultValue: T
  def value: T
  def setValue(v: T): Unit
  def hasValue: Boolean
  def setCount: Int
  def wasSet = setCount > 0
  override def toString: String = {
    if (hasValue)
      value match {
        case a: Seq[_] => Seq("--" + name + " ") ++ a.map(_.toString)
        case "" => Seq()
        case a: Any => Seq("--" + name + " " + value.toString)
      }
    else
      Seq()
  }.mkString("; ")
  override def hashCode = name.hashCode
  override def equals(other:Any) = name.equals(other)
}
