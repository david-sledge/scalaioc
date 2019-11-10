# scalaioc
IoC/DI Framework Written in Scala

[![Build Status](https://travis-ci.org/david-sledge/scalaioc.svg?branch=master)](https://travis-ci.org/david-sledge/scalaioc)

## Goals

* First and foremost support IoC and DI for Scala and Java applications.
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
libraryDependencies += "io.github.david-sledge" % "scalaioc_2.12" % "1.0.0-alpha.2"
```

## Simple Tutorial

Let's start with a plain-ol'-scala-object. (The following example can be found [here](https://github.com/david-sledge/scalaioc/tree/master/examples/simple/ "simple example").)

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
`namespace|scalaioc`

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

Factory managers are specified using `` `#=` `` (lazy) and `` `#=>` `` (productive). Factories contain workers that provide a service and/or produce a product. Managers are workers that can be referenced by name.

* `` `#=` `` executes the first time and caches the result for later calls.
* `` `#=>` `` executes without caching every time.

`staff.fsp`:

```
`namespace|scalaioc`

...

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
`namespace|scalaioc`

...

// This guy's such a workhorse, even other managers are more productive.
"productiveSeniorManager" `#=>`
  com.example.hello.DateTimeHelloWorld(
    "Hello World!",
    `#ref!`("lazyManager").asInstanceOf[java.time.ZonedDateTime],
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
```

## Context parameters/variables

Objects and values can be passed to managers when they're put to work.

### Command-Line Arguments

If the application utilizes command line arguments they can be past to a worker to be processed. The framework is packaged with a main class [scala.ioc.cli.Main](https://github.com/david-sledge/scalaioc/blob/master/src/main/scala/scala/ioc/cli/Main.scala "main class") that will pass on the command-line arguments to an initialization manager.

By default the main class reads the file name `staff.fsp` from the working directory, and instantiates a factory from the file. Then it puts the manager named `init` to work passing the command-line arguments. (The following example can be found [here](https://github.com/david-sledge/scalaioc/tree/master/examples/cli/ "CLI example"). There is also a [Java version of this example](https://github.com/david-sledge/scalaioc/tree/master/examples/java/ "Java CLI example").)

`staff.fsp`:

```
`namespace|scalaioc`

"init" `#=>`
  com.example.cli.parseOptions(
    `#$`("args"),
  )
```

In the `init` manager, the arguments are retrieved using `` `#$` `` with the string `"args"` and passed to `parseOptions`. This example uses [jopt-simple](http://jopt-simple.github.io/jopt-simple/ "JOpt Simple") to parse command line arguments. However, any other arg-parser library could be used, or the arguments could be processed without the aid of a third-party library.

`package.scala`:

```scala
package com.example

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
```

Other .fsp files can be specified using the flag `--If` followed by the file name. The main class will not pass these arguments to the initializing manager, nor any other argument starting with `--I` and any following associated arguments. The name of the initialization manager can be specified with `--Ii` followed by the manager's name.

The example has an alternative staff.fsp and an initialization manager that can be invoked using:

```sh
$ sbt "run --If src/main/resources/staff.fsp --Ii startup"
```

Multiple .fsp files can be specified by passing in multiple `--If <filename>` arguments.

### Passing Context Variables between Workers

*TBD*

`` `#let`(<key0> -> <value0>, <key1> -> <value1>, ... <keyN> -> <valueN>, <worker>)``

## Namespaces

### Namespace Declarations

* `` `namespace|scalaioc` `` assigns the namespace name "scalaioc" as the default. All the example .fsp files thus far have started with this declaration, so all worker definitions presented have been in the "scalaioc" namespace.
* `` `namespace i|scalaioc` `` assigns the namespace name "scalaioc" to the prefix "i".

### Namespaced Workers

* `` `#ref` `` references the worker named "ref" in the default namespace.
* `` `#i|let` `` references the worker named "let" in the namespace whose name assigned to the prefix "i".
* `` `#scalaioc#=` `` references the worker named "=" in the namespace whose name is "scalaioc".
* `` `#|myWorker` `` references the worker named "myWorker" that's in the nameless namespace.

### Workers in "scalaioc" Namespace

*work-in-progress*

* `` `#=` ``
* `` `#=>` ``
* `` `#ref` ``
* `` `#ref!` ``
* `` `#let` ``
* `` `#$` ``
* `` `#def` ``
* `` `#resource` ``
* `` `#embed` ``

## Multiple Factory Configuration Files

*TBD*

## Extending the Framework (unstable)

*work-in-progress*

```#def`([[<namespace name>, ]<local name>, ]<worker definition>)``

Worker definition must be functions with the follow signature:

```scala
(
  Option[String], // namespace name of invoking worker
  String,         // local name of invoking worker
) => (
  scala.ppm.MacroArgs(
    Option[scala.reflect.runtime.universe.Tree],  // LHS value
    List[scala.reflect.runtime.universe.Tree],    // Type arguments
    List[scala.reflect.runtime.universe.Tree],    // arguments
    ToolBox[scala.reflect.runtime.universe.type], // Toolbox instance
    Option[String],                               // description of the source of the trees
  )
) => scala.reflect.runtime.universe.Tree // resulting tree
```

## Packaged Extensions

*work-in-progress*

* Servlet extension: [servlet example](https://github.com/david-sledge/scalaioc/tree/master/examples/servlet/ "Servlet example") (enable with `` `#embed`("scala/ioc/servlet/http/macroDefs.fsp")``)
* XML writer (enable with `` `#embed`("scala/ioc/xml/macroDefs.fsp")``)
