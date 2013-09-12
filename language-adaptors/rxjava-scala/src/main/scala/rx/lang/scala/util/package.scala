package rx.lang.scala

package object util {
  type Closing = rx.util.Closing

  // TODO rx.util.Closings

  type CompositeException = rx.util.CompositeException

  // TODO rx.util.Exceptions

  // rx.util.OnErrorNotImplementedException TODO what's this?

  type Opening = rx.util.Opening

  // rx.util.Openings // TODO

  // rx.util.Range // TODO do we need this? Or the Scala Range?

  type Timestamped[+T] = rx.util.Timestamped[_ <: T]
  object Timestamped {
    def apply[T](timestampMillis: Long, value: T): Timestamped[T] = {
      new rx.util.Timestamped(timestampMillis, value)
    }
  }
}