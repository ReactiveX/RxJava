package rx.lang.scala.util

/**
 * Wraps a value and a timestamp.
 */
class Timestamped[+T](val asJava: rx.util.Timestamped[_ <: T]) extends AnyVal {
  /**
   * Returns the timestamp, in milliseconds.
   */
  def millis: Long = asJava.getTimestampMillis
  
  /**
   * Returns the value.
   */
  def value: T = asJava.getValue : T
}

/**
 * Provides constructor and pattern matching functionality for `Timestamped`.
 */
object Timestamped {
  def apply[T](timestampMillis: Long, value: T): Timestamped[T] = {
    new Timestamped(new rx.util.Timestamped(timestampMillis, value))
  }

  def apply[T](asJava: rx.util.Timestamped[_ <: T]): Timestamped[T] = {
    new Timestamped(asJava)
  }

  def unapply[T](v: Timestamped[T]): Option[(Long, T)] = {
    Some((v.millis, v.value))
  }
}
