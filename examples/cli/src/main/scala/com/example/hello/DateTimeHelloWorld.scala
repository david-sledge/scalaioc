package com.example.hello

import java.time.ZonedDateTime

class DateTimeHelloWorld(message: String, val dateTime: ZonedDateTime)
{
  def printMessage() = {
    println(s"Your Message : $message\nI was given the specific time of $dateTime.")
  }
}

object DateTimeHelloWorld {
  def apply(message: String, dateTime: ZonedDateTime) = new DateTimeHelloWorld(message, dateTime)
}
