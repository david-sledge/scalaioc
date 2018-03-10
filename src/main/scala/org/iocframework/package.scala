package org

import scala.tools.reflect.ToolBox
import scala.io.Source._
import scala.meta._
import scala.ioc._
import scala.reflect.runtime.universe.runtimeMirror

package object iocframework {
  // TODO:  scaladoc
  def staffFactory(conf: String, factory: Factory = Factory(),
      staffing: Staffing = Staffing()): (Factory, Staffing) = {
    val code = staffing.transformIoC(s"""
(factory: scala.ioc.Factory) =>
  (staffing: scala.ioc.Staffing) => {$conf}
""".parse[Stat].get).syntax
    // obtain toolbox
    val tb = runtimeMirror(this.getClass.getClassLoader).mkToolBox(options = "")
    // generate the AST
    val tree = tb.parse(code)
    // compile
    val f = tb.compile(tree)
    // ... execute
    f().asInstanceOf[Factory => Staffing => Any](factory)(staffing)
    // return the staffed factory and the staffing agency
    (factory, staffing)
  }

  // TODO:  scaladoc
  def staffFactoryFromFile(fileName: String, factory: Factory = Factory()
      , staffing: Staffing = Staffing()) =
    staffFactory(fromFile(fileName).mkString, factory, staffing)
}
