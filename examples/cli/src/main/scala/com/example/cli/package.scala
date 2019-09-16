package com.example

import scala.collection.immutable.Map

import joptsimple._

package object cli {
  val parseOptions = (args: Array[String]) => {

    val parser = new OptionParser( "s:a:" )

    val options = parser.parse(args:_*)

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
