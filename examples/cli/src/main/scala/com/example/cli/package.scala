package com.example

import joptsimple._

package object cli {
  val parseOptions: Array[String] => Unit = (args: Array[String]) => {

    // two optional arguments "-a arg1" "-s arg2"
    val options = new OptionParser( "s:a:" ).parse(args:_*)

    // if no -s flag, default to "Hello"
    val salutation = if (options.has("s")) {
    	options.valueOf("s")
    }
    else {
    	"Hello"
    }

    // if no -a flag, default to "World"
    val addressee = if (options.has("a")) {
    	options.valueOf("a")
    }
    else {
    	"World"
    }

    println(s"$salutation, $addressee!")

  }
}
