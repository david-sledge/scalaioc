package org.iocframework.example.hello

import org.iocframework.staffFactoryFromResource
import scala.ioc.Factory

object MainApp extends App {
  val (factory, _) = staffFactoryFromResource("FactoryStaff.sfs")
  val obj = factory.putToWork("helloWorld").asInstanceOf[HelloWorld]
  obj.getMessage()
}
