package com.example.hello

import scala.ioc.ppm._
import scala.ioc.Factory

object MainApp extends App {
  def printEq(obj1: Any, obj2: Any) =
    println(if (obj1 == obj2) "Same object" else "Not the same object")

  // instantiate the factory
  val (factory, _) = staffFactoryFromResource("staff.sfs")
  // put the manager to work and get the fruits of her labor
  val obj1 = factory.putToWork("helloWorld1").asInstanceOf[HelloWorld]
  obj1.getMessage()

  Thread sleep 2000
  // since this manager is lazy, she won't put any effort to produce a new deliverable.
  // She'll just give us what she provided the first time
  val obj1Again = factory.putToWork("helloWorld1").asInstanceOf[HelloWorld]
  obj1Again.getMessage()

  printEq(obj1, obj1Again)

  // This guy, however, is like an excitable puppy who's eager to please
  val obj2 = factory.putToWork("helloWorld2").asInstanceOf[HelloWorld]
  obj2.getMessage()
  Thread sleep 2000
  // Task him again, and he'll work to give something new
  val differentObj2 = factory.putToWork("helloWorld2").asInstanceOf[HelloWorld]
  differentObj2.getMessage()

  printEq(obj2, differentObj2)

  // Even though the first manager is lazy, you can still make her work hard
  val differentObj1 = factory.crackTheWhip("helloWorld1").asInstanceOf[HelloWorld]
  differentObj1.getMessage()

  printEq(obj1, differentObj1)
}
