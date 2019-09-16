package com.example.hello

import scala.ioc.ppm._
import scala.ioc.Factory

import java.time.ZonedDateTime

object MainApp extends App {

  // build the factory and staff it
  val (factory, _) = staffFactoryFromResource("staff.fsp")
  // put the "helloWorld" manager to work and get the fruits of her labor
  val obj = factory.putToWork("helloWorld").asInstanceOf[HelloWorld]
  obj.printMessage

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

  // The productive senior manager puts the lazy one to work.
  val dateTimeHelloWorld =
    factory.putToWork("productiveSeniorManager").asInstanceOf[DateTimeHelloWorld]
  dateTimeHelloWorld.printMessage
  // Will the lazy manager give the productive one the same ol'?
  println("The senior manager will make even the lazy manager perform some work.")
  println(s"Will the lazy manager give senior manager the same as before?"
      + s" ${if (dateTimeHelloWorld.dateTime.equals(dateTime)) "Yes" else "No"}")
}
