import rx.lang.scala.Observable

object AsyncObservable extends App {
  /**
   * This example shows a custom Observable that does not block
   * when subscribed to as it spawns a separate thread.
   */
  def customObservableNonBlocking(): Observable[String] = {
    Observable(
    /*
     * This 'call' method will be invoked when the Observable is subscribed to.
     *
     * It spawns a thread to do it asynchronously.
     */
      subscriber => {
        // For simplicity this example uses a Thread instead of an ExecutorService/ThreadPool
        new Thread(new Runnable() {
          def run() {
            for (i <- 0 to 75) {
              if (subscriber.isUnsubscribed) {
                return
              }
              subscriber.onNext("value_" + i)
            }
            // after sending all values we complete the sequence
            if (!subscriber.isUnsubscribed) {
              subscriber.onCompleted()
            }
          }
        }).start()
      }
    )
  }

  // To see output:
  customObservableNonBlocking().subscribe(println(_))
}
