# scalaioc
IoC/DI Framework Written in Scala

[![Build Status](https://travis-ci.org/david-sledge/scalaioc.svg?branch=master)](https://travis-ci.org/david-sledge/scalaioc)

## Motivation

TBD

## Goals

* First and foremost support IoC and DI for Scala applications.
  - Facilitate unit testing by allowing components to be instantiated with mock dependencies or inject test implementations.
  - Eliminate the need for global static state.
* Non-invasive: Do not require imports from the framework's library in application code in order to configure the application.
  - Allow configuration to be fully external to application code.
  - Eliminate the need for annotation-based configuration.
  - Eliminate the need to extend framework classes and interfaces in application code.
* Allow the framework to be extended using the framework. Developers can create their own worker definitions.

## Setup

Add the following dependency to your `build.sbt` file:

```scala
libraryDependencies += "io.github.david-sledge" % "scalaioc_2.12" % "v1.0.0-alpha.1"
```

## Simple Tutorial

Let's start with a plain-ol'-scala-object. (The following examples can be found [here](https://github.com/david-sledge/scalaioc/tree/master/examples/simple/ "simple example").)

`HelloWorld.scala`:

```scala
package com.example.hello

class HelloWorld(message: String)
{
  def printMessage = {
    println(s"Your Message : $message")
  }
}

object HelloWorld {
  def apply(message: String) = new HelloWorld(message)
}
```

Configuration file; factory staff plan.

`staff.fsp`:

```
`namespace|scala.ioc`

// this is a factory manager
"helloWorld" `#=` com.example.hello.HelloWorld("Hello World!")
```

Main entry point to the application (`MainApp.scala`).

```scala
package com.example.hello

import scala.ioc.ppm._
import scala.ioc.Factory

object MainApp extends App {

  // build the factory and staff it
  val (factory, _) = staffFactoryFromResource("staff.fsp")
  // put the "helloWorld" manager to work and get the fruits of her labor
  val obj = factory.putToWork("helloWorld").asInstanceOf[HelloWorld]
  obj.printMessage
}
```

### Factory managers

factory managers are specified using `` `#=` `` (lazy) and `` `#=>` `` (productive).

* `` `#=` `` executes the first time and caches the result for later calls.
* `` `#=>` `` executes without caching every time.

`staff.fsp`:

```
`namespace|scala.ioc`

// this is a factory manager
"helloWorld" `#=` com.example.hello.HelloWorld("Hello World!")

// Simple (lazy) factory manager
// the first time a lazy manager is put to work, their result is stored.
// On subsequent requests for the manager to do some work, they just give you
// what's stored from the first time.
"lazyManager" `#=` java.time.ZonedDateTime.now

// Simple (way-too-eager) factory managers
// produce a new result each time they're put to work (even if their work
// results in the exact same thing they produced last time).
"eagerManager" `#=>` java.time.ZonedDateTime.now
```

`MainApp.scala`:

```scala
...

import java.time.ZonedDateTime

object MainApp extends App {

  // build the factory and staff it
  val (factory, _) = staffFactoryFromResource("staff.fsp")
  ...

  // gimme the time, ya lazy bum!
  val dateTime = factory.putToWork("lazyManager").asInstanceOf[ZonedDateTime]
  // you, people-pleaser, gimme the time, too.
  val anotherDateTime = factory.putToWork("eagerManager").asInstanceOf[ZonedDateTime]

  // let's wait half a second
  Thread sleep 500

  // who will give the same ol', and who will give something new?
  println(s"Will the lazy manager produce the same as before?"
      + s" ${if (factory.putToWork("lazyManager").equals(dateTime)) "Yes" else "No"}")
  println(s"Will the eager manager produce the same as before?"
      + s" ${if (factory.putToWork("eagerManager").equals(anotherDateTime)) "Yes" else "No"}")
}
```

### Managers referencing other managers

Managers can task other managers with `` `#ref` `` and `` `#ref!` ``. Both operate the same except that latter will force the lazy (caching) managers to produce something new instead of a cached result. The new product will be cached in place of the old.

`DateTimeHelloWorld.scala`:

```scala
package com.example.hello

import java.time.ZonedDateTime

class DateTimeHelloWorld(message: String, val dateTime: ZonedDateTime)
{
  def printMessage = {
    println(s"Your Message : $message\nI was given the specific time of $dateTime.")
  }
}

object DateTimeHelloWorld {
  def apply(message: String, dateTime: ZonedDateTime) = new DateTimeHelloWorld(message, dateTime)
}
```

`staff.fsp`:

```
`namespace|scala.ioc`

// this is a factory manager
"helloWorld" `#=` com.example.hello.HelloWorld("Hello World!")

// Simple (lazy) factory manager
// the first time a lazy manager is put to work, their result is stored.
// On subsequent requests for the manager to do some work, they just give you
// what's stored from the first time.
// This manager is so lazy they're having another manager do all the work.
"lazyManager" `#=` `#ref`("eagerManager")

// Simple (way-too-eager) factory managers
// produce a new result each time they're put to work (even if their work
// results in the exact same thing they produced last time).
"eagerManager" `#=>` java.time.ZonedDateTime.now

// This guy's such a workhorse, even other managers are more productive.
"productiveSeniorManager" `#=>`
  com.example.hello.DateTimeHelloWorld(
  	"Hello World!",
  	`#ref!`("lazyManager").asInstanceOf[java.time.ZonedDateTime]
  )
```

`MainApp.scala`:

```scala
...

  // build the factory and staff it
  val (factory, _) = staffFactoryFromResource("staff.fsp")

  ...

  // The productive senior manager puts the lazy one to work.
  val dateTimeHelloWorld =
    factory.putToWork("productiveSeniorManager").asInstanceOf[DateTimeHelloWorld]
  dateTimeHelloWorld.printMessage
  // Will the lazy manager give the productive one the same ol'?
  println("The senior manager will make even the lazy manager perform some work.")
  println(s"Will the lazy manager give senior manager the same as before?"
      + s" ${if (dateTimeHelloWorld.dateTime.equals(dateTime)) "Yes" else "No"}")

...
```

## Context parameters/variables

Objects and values can be passed to managers when they're put to work.

### Command-Line Arguments Example

If the application utilizes command line arguments they can be past to a worker to be processed.  (The following examples can be found [here](https://github.com/david-sledge/scalaioc/tree/master/examples/cli/ "cli example").)

The method Factory.putToWork has a second (optional) argument of type scala.collection.immutable.Map[Any, Any]. When not specified it defaults to an empty map. In this example, the second argument is used to pass the application's command-line arguments to the worker named "init".

`MainApp.scala`:

```scala
package com.example.cli

import scala.ioc.ppm._
import scala.ioc.Factory

import java.time.ZonedDateTime

object MainApp extends App {

  // build the factory and staff it
  val (factory, _) = staffFactoryFromResource("staff.fsp")
  // put the "helloWorld" manager to work and get the fruits of her labor
  val obj = factory.putToWork("init", Map("args" -> args))

}
```

And the definition of the worker named "init"...

`staff.fsp`:

```
`namespace|scala.ioc`

"init" `#=>`
  com.example.cli.parseOptions(
	c("args").asInstanceOf[Array[String]],
  )
```

This example uses jopt-simple to parse command line arguments. However, any other arg-parser library could be used.

`com/example/cli/package.scala`:

```scala
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
```

### Passing Context Variables between Workers

`` `#let`(<key>, <value>, <worker>)``

## Namespaces

### Namespace Declarations

* `` `namespace|scala.ioc` `` assigns the namespace name "scala.ioc" as the default. (All worker definitions presented thus far have been in the "scala.ioc" namespace.)
* `` `namespace ioc|scala.ioc` `` assigns the namespace name "scala.ioc" to the prefix "ioc".

### Namespaced Workers

* `` `#ref` `` references the worker named "ref" in the default namespace.
* `` `#ioc|let` `` references the worker named "let" in the namespace whose name assigned to the prefix "ioc".
* `` `#scala.ioc#=` `` references the worker named "=" in the namespace whose name is "scala.ioc".
* `` `#|myWorker` `` references the worker named "myWorker" that's in the nameless namespace.

## Multiple Factory Configuration Files

TBD

## Extending the Framework

TBD

## Packaged Extensions

TBD
