package com.example

import scala.ioc.ppm._
import scala.ioc.Factory

import java.time.ZonedDateTime

package object hello {

  def run(
    obj0: => HelloWorld,
    dateTime0: => ZonedDateTime,
    anotherDateTime0: => ZonedDateTime,
    dateTimeHelloWorld0: => DateTimeHelloWorld,
  ) = {

    // put the "helloWorld" manager to work and get the fruits of her labor
    obj0.printMessage

    // gimme the time, ya lazy bum!
    val dateTime = dateTime0
    // you, people-pleaser, gimme the time, too.
    val anotherDateTime = anotherDateTime0

    // let's wait half a second
    Thread sleep 500

    // who will give the same ol', and who will give something new?
    println(s"Will the lazy manager produce the same as before?"
        + s" ${if (dateTime0.equals(dateTime)) "Yes" else "No"}")
    println(s"Will the eager manager produce the same as before?"
        + s" ${if (anotherDateTime0.equals(anotherDateTime)) "Yes" else "No"}")

    // The productive senior manager puts the lazy one to work.
    val dateTimeHelloWorld = dateTimeHelloWorld0
    dateTimeHelloWorld.printMessage
    // Will the lazy manager give the productive one the same ol'?
    println("The senior manager will make even the lazy manager perform some work.")
    println(s"Will the lazy manager give senior manager the same as before?"
        + s" ${if (dateTimeHelloWorld.dateTime.equals(dateTime)) "Yes" else "No"}")

  }
}
