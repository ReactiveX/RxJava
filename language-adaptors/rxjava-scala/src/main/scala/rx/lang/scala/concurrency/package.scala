package rx.lang.scala

import rx.concurrency.CurrentThreadScheduler

package object concurrency {
  /*
  TODO
  rx.concurrency.CurrentThreadScheduler
  rx.concurrency.ExecutorScheduler
  rx.concurrency.ImmediateScheduler
  rx.concurrency.NewThreadScheduler
  rx.concurrency.Schedulers
  rx.concurrency.TestScheduler
 */
  
  lazy val CurrentThreadScheduler = rx.concurrency.CurrentThreadScheduler.getInstance()
  lazy val NewThreadScheduler = rx.concurrency.NewThreadScheduler.getInstance()

}