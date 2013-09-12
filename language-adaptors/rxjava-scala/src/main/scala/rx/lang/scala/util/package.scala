package rx.lang.scala

package object util {
  type Closing = rx.util.Closing

  object Closings {
    def create(): Closing = rx.util.Closings.create()
  }

  type CompositeException = rx.util.CompositeException

  // TODO not sure if we need this in Scala
  object Exceptions {
    def propageate(ex: Throwable) = rx.util.Exceptions.propagate(ex)
  }

  // rx.util.OnErrorNotImplementedException TODO what's this?

  type Opening = rx.util.Opening

  object Openings {
    def create(): Opening = rx.util.Openings.create()
  }

  // rx.util.Range not needed because there's a standard Scala Range

  type Timestamped[+T] = rx.util.Timestamped[_ <: T]
  object Timestamped {
    def apply[T](timestampMillis: Long, value: T): Timestamped[T] = {
      new rx.util.Timestamped(timestampMillis, value)
    }
  }
}