package stages

import scala.concurrent.Future

/**
  * Created on 2017-02-12.
  */
trait Event {
  val id: Long
  def toString: String
}

object Event {
  /* Implicit parameter to be used by SortedSet */
  implicit def eventOrder[T <:Event] = Ordering.fromLessThan[T](_.id < _.id)
}

trait EventReader[E <: Event] {
  def read(offset: Long, count: Int): Future[Seq[E]]
}

trait EventDeleter[E <: Event] {
  def delete(offset: Long): Future[Seq[Int]]
}

trait EventRepository[E <: Event] extends EventReader[E] with EventDeleter[E]
