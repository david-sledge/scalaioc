package com.example

import scala.collection.immutable.Map

import joptsimple._

package object cli {
  val parseOptions = (c: Map[Any, Any]) => {

    val parser = new OptionParser( "s:a:" )

    val options = parser.parse(c("args").asInstanceOf[Array[String]]:_*)

    val salutation = if (options.has("s")) {
    	options.valueOf("s")
    }
    else {
    	"Hello"
    }

    val addressee = if (options.has("a")) {
    	options.valueOf("a")
    }
    else {
    	"World"
    }

    println(s"$salutation, $addressee!")

  }
}
