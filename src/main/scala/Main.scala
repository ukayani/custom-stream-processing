import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import stages.{Event, EventReader, EventsSource}

import scala.concurrent.Future

/**
  * Created on 2017-02-12.
  */
object Main extends App {
  
  implicit val actorSystem = ActorSystem()
  
  implicit val ec = actorSystem.dispatcher
  
  implicit val materializer = ActorMaterializer()
  
  case class Sample(id: Long, body: String) extends Event
  
  class SampleReader extends EventReader[Sample] {
    
    val events = for {
      i <- 1 until 30
    } yield Sample(i, s"Event: ${i.toString}")
    
    override def read(offset: Long, count: Int): Future[Seq[Sample]] = Future.successful(events.filter(_.id >= offset).take(count))
  }
  
  Source.fromGraph(new EventsSource(new SampleReader(), 5)).runForeach(println)
  
}
