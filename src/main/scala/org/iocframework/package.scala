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
    //println(code)
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
  def staffFactoryFromFile(fileName: String, encoding: String = "utf-8", factory: Factory = Factory()
      , staffing: Staffing = Staffing()) =
    staffFactory(fromFile(fileName, encoding).mkString, factory, staffing)

  // TODO:  scaladoc
  def staffFactoryFromResource(path: String, encoding: String = "utf-8", factory: Factory = Factory()
      , staffing: Staffing = Staffing()) =
    staffFactoryFromStream(getClass.getClassLoader.getResourceAsStream(path), encoding, factory, staffing)

  // TODO:  scaladoc
  def staffFactoryFromStream(stream: java.io.InputStream, encoding: String = "utf-8", factory: Factory = Factory()
      , staffing: Staffing = Staffing()) =
    staffFactory(scala.io.Source.fromInputStream(stream, encoding).mkString, factory, staffing)
}
