package scala.ioc

import scala.collection.concurrent.TrieMap
import scala.collection._

/**
 * A factory, the kind with workers including managers.
 */
final class Factory(val id: Any)
{
  private abstract class Manager

  private case class EvaledThunk(value: Any, worker: Map[Any, Any] => Any) extends Manager

  private case class Thunk(worker: Map[Any, Any] => Any) extends Manager

  private case class Strict(worker: Map[Any, Any] => Any) extends Manager

  private val managers = TrieMap[Any, Manager]()

  private def getResult(useCachedValue: Boolean, id: Any, c: Map[Any, Any]) = {
    lazy val newC = c + (this.id -> this)

    def evalThunk(worker: Map[Any, Any] => Any) = {
      val newValue = worker(newC)
      managers += id -> EvaledThunk(newValue, worker)
      newValue
    }

    managers(id) match {
      case EvaledThunk(value, worker) => if (useCachedValue) value else evalThunk(worker)
      case Thunk(worker) => evalThunk(worker)
      case Strict(worker) => worker(newC)
    }
  }

  /**
   * The lazy ones just give you the same-old same-old.
   */
  def getCachedResult(id: Any, c: Map[Any, Any]) = getResult(true, id, c)

  /**
   * Makes even the lazy ones work hard.
   */
  def putToWork(id: Any, c: Map[Any, Any]) = getResult(false, id, c)

  /**
   * Didn't we already hire this gal?
   */
  def hasManager(id: Any) = managers contains id

  /**
   * Look up someone in the company directory
   */
  def getManager(id: Any): Map[Any, Any] => Any = {
    managers(id) match {
      case EvaledThunk(value, worker) => worker
      case Thunk(worker) => worker
      case Strict(worker) => worker
    }
  }

  /**
   * This guy?  Way too eager.  He'll work hard just to produce something you've
   * he's already given you.
   */
  def setManager(id: Any, worker: Map[Any, Any] => Any) = {
    managers += id -> Strict(worker)
    ()
  }

  /**
   * This guy?  Really lazy.  Does the minimal amount of work.  He'll work the
   * first time you ask him to, but after that he just hands you stuff he's
   * already produced unless you force him to do otherwise.
   */
  def setLazyManager(id: Any, worker: Map[Any, Any] => Any) = {
    managers += id -> Thunk(worker)
    ()
  }

  /**
   * Get the managers on roll.
   */
  def getManagerIds() = managers.keys
}

object Factory {
  def apply(id: Any) = new Factory(id)
}