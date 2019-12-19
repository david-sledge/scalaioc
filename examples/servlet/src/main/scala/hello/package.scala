

package object hello {

  def testAndConsumeName[T](nameOpt: Option[String], some: String => T, none: => T): T = {

    nameOpt match {
      case Some(name) => some(s"Awesome $name")
      case _ => none
    }

  }

}
