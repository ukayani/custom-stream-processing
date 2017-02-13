package stages

import java.util.concurrent.ThreadLocalRandom

import akka.stream.stage.TimerGraphStageLogic

import scala.concurrent.duration.FiniteDuration

/**
  * A simple exponential backoff scheduler for TimerGraphStageLogic based on Akka's BackoffSupervisor logic
  */
trait BackOff {
  this: TimerGraphStageLogic =>

  val backoff: BackoffOptions
  
  var failureCount = 0

  /**
    * Calculates a duration between minBackoff and maxBackoff factoring in failureCount for exponential increase
    * Borrowed from Akka's BackoffSupervisor
    * @param failureCount
    * @param minBackoff
    * @param maxBackoff
    * @param randomFactor
    * @return
    */
  def calculateBackoff(
                        failureCount: Int,
                        minBackoff: FiniteDuration,
                        maxBackoff: FiniteDuration,
                        randomFactor: Double): FiniteDuration = {
    val rnd = 1.0 + ThreadLocalRandom.current().nextDouble() * randomFactor
    if (failureCount >= 30) // Duration overflow protection (> 100 years)
      maxBackoff
    else
      maxBackoff.min(minBackoff * math.pow(2, failureCount)) * rnd match {
        case f: FiniteDuration ⇒ f
        case _ ⇒ maxBackoff
      }
  }

  def scheduleWithBackOff(timerKey: Any) = {
    import backoff._
    scheduleOnce(timerKey, calculateBackoff(failureCount, minBackoff, maxBackoff, randomFactor))
    failureCount = failureCount + 1
  }
  
  def resetBackoff() = failureCount = 0

}

case class BackoffOptions(minBackoff: FiniteDuration, maxBackoff: FiniteDuration, randomFactor: Double)