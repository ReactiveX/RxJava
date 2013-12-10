package rx.lang.scala

/**
 * These functions convert between RxScala types RxJava types.
 * Pure Scala projects won't need them, but they will be useful for polyglot projects.
 * This object only contains conversions between types. For conversions between functions,
 * use [[rx.lang.scala.ImplicitFunctionConversions]].
 */
object JavaConversions {
  import language.implicitConversions
  
  implicit def toJavaNotification[T](s: Notification[T]): rx.Notification[_ <: T] = s.asJavaNotification
  
  implicit def toScalaNotification[T](s: rx.Notification[_ <: T]): Notification[T] = Notification(s)

  implicit def toJavaSubscription(s: Subscription): rx.Subscription = s.asJavaSubscription
  
  implicit def toScalaSubscription(s: rx.Subscription): Subscription = Subscription(s)

  implicit def scalaSchedulerToJavaScheduler(s: Scheduler): rx.Scheduler = s.asJavaScheduler
  
  implicit def javaSchedulerToScalaScheduler(s: rx.Scheduler): Scheduler = Scheduler(s)

  implicit def toJavaObserver[T](s: Observer[T]): rx.Observer[_ >: T] = s.asJavaObserver
  
  implicit def toScalaObserver[T](s: rx.Observer[_ >: T]): Observer[T] = Observer(s)

  implicit def toJavaObservable[T](s: Observable[T]): rx.Observable[_ <: T] = s.asJavaObservable
  
  implicit def toScalaObservable[T](observable: rx.Observable[_ <: T]): Observable[T] = {
    new Observable[T]{
      val asJavaObservable = observable
    }
  }
}