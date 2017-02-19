package dispatcher
import scala.concurrent.ExecutionContext

/**
  * Created on 2017-02-19.
  */
object ExecutionContexts {
  object sameThreadExecutionContext extends ExecutionContext {

    override def execute(runnable: Runnable): Unit = runnable.run()
    override def reportFailure(t: Throwable): Unit =
      throw new IllegalStateException("exception in sameThreadExecutionContext", t)
  }
}
