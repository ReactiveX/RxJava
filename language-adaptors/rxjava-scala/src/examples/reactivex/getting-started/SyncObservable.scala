import rx.lang.scala.Observable

object SyncObservable extends App {
  /**
   * This example shows a custom Observable that blocks
   * when subscribed to (does not spawn an extra thread).
   */
  def customObservableBlocking(): Observable[String] = {
    Observable(aSubscriber => {
      for (i <- 0 to 50) {
        if (!aSubscriber.isUnsubscribed) {
          aSubscriber.onNext("value_" + i)
        }
      }
      // after sending all values we complete the sequence
      if (!aSubscriber.isUnsubscribed) {
        aSubscriber.onCompleted()
      }
    })
  }

  // To see output:
  customObservableBlocking().subscribe(println(_))
}
