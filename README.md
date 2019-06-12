# scalaioc
IoC/DI Framework Written in Scala

[![Build Status](https://travis-ci.org/david-sledge/scalaioc.svg?branch=master)](https://travis-ci.org/david-sledge/scalaioc)

## Simple Tutorial

Let's start with a plain-ol'-scala-object.  (The following examples can be found [here](https://github.com/david-sledge/scalaioc/tree/master/examples/simple/src/main "simple example").)

HelloWorld.scala:

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

staff.fsp:

```scala
`namespace|scala.ioc`

// this is a factory manager
"helloWorld" `#=` com.example.hello.HelloWorld("Hello World!")
```

Main entry point to the application (MainApp.scala).

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

## Factory managers

factory managers are specified using `#=` (lazy) and `#=>` (eager).

staff.fsp:

```scala
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

MainApp.scala:

```scala
package com.example.hello

import scala.ioc.ppm._
import scala.ioc.Factory

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
  println("Will the lazy manager produce the same as before? " +
    factory.putToWork("lazyManager").equals(dateTime))
  println("Will the eager manager produce the same as before? " +
    factory.putToWork("eagerManager").equals(anotherDateTime))
}
```

## Managers referencing other managers

Managers can put other managers to work with `#ref` and `#ref!`.  The latter
will force even the lazy managers to produce something new.

DateTimeHelloWorld.scala:

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

staff.fsp:

```scala
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
  	`#ref!`("lazyManager").asInstanceOf[java.time.ZonedDateTime])
```

MainApp.scala:

```scala
package com.example.hello

import scala.ioc.ppm._
import scala.ioc.Factory

import java.time.ZonedDateTime

object MainApp extends App {

  // build the factory and staff it
  val (factory, _) = staffFactoryFromResource("staff.fsp")
  ...

  // The productive senior manager puts the lazy one to work.
  val dateTimeHelloWorld =
    factory.putToWork("productiveSeniorManager").asInstanceOf[DateTimeHelloWorld]
  dateTimeHelloWorld.printMessage
  // Will the lazy manager give the productive one the same ol'?
  println("Will the lazy manager give senior manager the same as before? " +
    dateTimeHelloWorld.dateTime.equals(dateTime))
}
```

## Context variables

TBD

## Multiple Factory Configuration Files

TBD

## Extending the Framework

TBD
