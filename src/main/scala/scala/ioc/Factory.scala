package scala.ioc

import scala.collection.concurrent.TrieMap

/**
 * A factory, the kind with workers including managers.
 */
final class Factory()
{
  import Factory._

  val FactoryId = "factory"

  private val managers = TrieMap[Any, Manager]()

  private def getResult(useCachedValue: Boolean, id: Any, c: Map[Any, Any]) = {
    lazy val newC = c + (FactoryId -> this)

    def evalThunk(worker: Map[Any, Any] => Any) = {
      val newValue = worker(newC)
      managers += id -> EvaledThunk(newValue, worker)
      newValue
    }

    managers.get(id) match {
      case Some(EvaledThunk(value, worker)) => if (useCachedValue) value else evalThunk(worker)
      case Some(Thunk(worker)) => evalThunk(worker)
      case Some(Strict(worker)) => worker(newC)
      case None => throw new Exception("manager with ID " + id + " does not exists.")
    }
  }

  /**
   * The lazy ones just give you the same-old same-old.
   */
  def putToWork(id: Any, c: Map[Any, Any] = Map()) = getResult(true, id, c)

  /**
   * Makes even the lazy ones work hard.
   */
  def crackTheWhip(id: Any, c: Map[Any, Any] = Map()) = getResult(false, id, c)

  /**
   * Didn't we already hire this gal?
   */
  def hasManager(id: Any) = managers contains id

  /**
   * Look up someone in the company directory
   */
  def getManager(id: Any): Option[Map[Any, Any] => Any] = {
    managers.get(id) match {
      case Some(EvaledThunk(value, worker)) => Some(worker)
      case Some(Thunk(worker)) => Some(worker)
      case Some(Strict(worker)) => Some(worker)
      case _ => None
    }
  }

  /**
   * Get the managers on roll.
   */
  def getManagerIds = managers.keys

  def clearCache =
    managers.foreach {
      case (id, EvaledThunk(_, worker)) => managers += id -> Thunk(worker)
      case _ => ()
    }

  def fireManager(id: Any) = {
    managers -= id
    ()
  }

  def fireEveryone() = {
    managers.clear()
  }
}

object Factory {

  private sealed abstract class Manager

  private case class EvaledThunk(value: Any, worker: Map[Any, Any] => Any) extends Manager

  private case class Thunk(worker: Map[Any, Any] => Any) extends Manager

  private case class Strict(worker: Map[Any, Any] => Any) extends Manager

  def apply() = new Factory()

  /**
   * This guy?  Way too eager.  He'll work hard just to produce something you've
   * he's already given you.
   */
  def setManager(factory: Factory, id: Any, worker: Map[Any, Any] => Any) = {
    factory.managers += id -> Strict(worker)
    ()
  }

  /**
   * This guy?  Really lazy.  Does the minimal amount of work.  He'll work the
   * first time you ask him to, but after that he just hands you stuff he's
   * already produced unless you force him to do otherwise.
   */
  def setLazyManager(factory: Factory, id: Any, worker: Map[Any, Any] => Any) = {
    factory.managers += id -> Thunk(worker)
    ()
  }
}
