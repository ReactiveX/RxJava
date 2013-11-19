package rx.lang.scala

/**
 * Provides a mechanism for receiving push-based notifications.
 *
 * After an Observer calls an [[Observable]]'s `subscribe` method, the Observable
 * calls the Observer's `onNext` method to provide notifications. A well-behaved Observable will
 * call an Observer's `onCompleted` method exactly once or the Observer's `onError` method exactly once.
 */
trait Observer[-T] extends JavaWrapper[rx.Observer[_ >: T]] {

  /**
   * Notifies the Observer that the [[Observable]] has finished sending push-based notifications.
   *
   * The [[Observable]] will not call this method if it calls `onError`.
   */
  def onCompleted(): Unit = {
    asJava.onCompleted()
  }

  /**
   * Notifies the Observer that the [[Observable]] has experienced an error condition.
   *
   * If the [[Observable]] calls this method, it will not thereafter call `onNext` or `onCompleted`.
   */
  def onError(e: Throwable): Unit = {
    asJava.onError(e)
  }

  /**
   * Provides the Observer with new data.
   *
   * The [[Observable]] calls this closure 0 or more times.
   *
   * The [[Observable]] will not call this method again after it calls either `onCompleted` or `onError`.
   */
  def onNext(arg: T): Unit = {
    asJava.onNext(arg)
  }
}

object Observer {
  private[Observer] class ObserverWrapper[-T](val asJava: rx.Observer[_ >: T]) extends Observer[T]
  
  def apply[T](asJava: rx.Observer[_ >: T]): Observer[T] = {
    new ObserverWrapper(asJava)
  }
}
