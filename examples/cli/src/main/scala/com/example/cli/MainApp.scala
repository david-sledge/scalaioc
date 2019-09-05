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
