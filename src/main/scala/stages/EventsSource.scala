package stages

import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.stage._

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{Failure, Success}
import Event._

import scala.concurrent.duration._
import scala.language.postfixOps
import EventsSource._
import dispatcher.ExecutionContexts

/**
  * Created on 2017-02-12.
  */

class EventsSource[E <: Event](reader: EventReader[E], 
                               count: Int, 
                               backoffOptions: BackoffOptions = DefaultBackoffOptions) extends GraphStage[SourceShape[E]] {

  val out: Outlet[E] = Outlet("EventSource")
  override val shape: SourceShape[E] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new TimerGraphStageLogic(shape) with BackOff with OutHandler with StageLogging {

      val backoff: BackoffOptions = backoffOptions
      
      val buffer = mutable.Queue[E]()
      def bufferRoom = math.max(0, count - buffer.size)
      def hasRoom = bufferRoom > (count / 2)
      
      var registerCallbacks: Future[Seq[E]] => Unit = _
      var isReading = false
      var offset = 0L
      
      override def preStart(): Unit = {
        val successCallback = getAsyncCallback[Seq[E]](receivedEvents)
        val failureCallback = getAsyncCallback[Throwable](receiveFailed)
          
        registerCallbacks = registerAsyncCallbacks(successCallback, failureCallback)
      }
      
      def receivedEvents(events: Seq[E]) = {
        log.debug(s"Received Events ${events.size}")
        
        resetBackoff()
        
        isReading = false
        
        offset = if (events.nonEmpty) events.max.id + 1 else offset
        
        buffer.enqueue(events:_*)
        log.debug(s"Buffer: ${buffer.size}")
        tryPush()
      }
      
      def receiveFailed(ex: Throwable) = {
        log.error(ex, "Receive Failed")
        isReading = false

        scheduleWithBackOff(None)
      }

      override def onPull(): Unit = {
        tryPush()
        tryRead()
      }

      override protected def onTimer(timerKey: Any): Unit = tryRead()  
      
      def tryPush() = if (buffer.nonEmpty && !isClosed(out) && isAvailable(out)) {
        log.debug("Pushing element")
        push(out, buffer.dequeue())
      } 
      
      def tryRead() = if (hasRoom && !isReading) {
        log.debug(s"Pulling from offset $offset")
        isReading = true
        registerCallbacks(reader.read(offset, bufferRoom))
      }
      
      def registerAsyncCallbacks[T](success: AsyncCallback[T], failure: AsyncCallback[Throwable])(future: Future[T]) = 
        future.onComplete {
          case Success(t) => success.invoke(t)
          case Failure(ex) => failure.invoke(ex)
      }(ExecutionContexts.sameThreadExecutionContext)
      
      
      setHandler(out, this)
    }
}

object EventsSource {
  val DefaultFetchCount = 100
  val DefaultBackoffOptions = BackoffOptions(10 seconds, 60 seconds, randomFactor = 0.2)
  
  def apply[E <: Event](reader: EventReader[E], 
                        count: Int = DefaultFetchCount, 
                        backoffOptions: BackoffOptions = DefaultBackoffOptions) = {

    
    new EventsSource(reader, count, backoffOptions)
  }
}