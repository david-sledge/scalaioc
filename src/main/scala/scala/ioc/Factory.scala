package scala.ioc

import scala.collection.concurrent.TrieMap
import scala.collection._

final class Factory(val id: Any)
{
  abstract class Manager

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

  // TODO:  create unit test
  def getCachedResult(id: Any, c: Map[Any, Any]) = getResult(true, id, c)

  // TODO:  create unit test
  def putToWork(id: Any, c: Map[Any, Any]) = getResult(false, id, c)

  // TODO:  create unit test
  def hasManager(id: Any) = managers contains id

  // TODO:  create unit test
  def getWorker(id: Any): Map[Any, Any] => Any = {
    managers(id) match {
      case EvaledThunk(value, worker) => worker
      case Thunk(worker) => worker
      case Strict(worker) => worker
    }
  }

  // TODO:  create unit test
  def setManager(id: Any, worker: Map[Any, Any] => Any) = {
    managers += id -> Strict(worker)
    ()
  }

  // TODO:  create unit test
  def setCacheManager(id: Any, worker: Map[Any, Any] => Any) = {
    managers += id -> Thunk(worker)
    ()
  }

  // TODO:  create unit test
  def getManagerIds() = managers.keys
}
