package rx.lang.scala

trait Subscriber[-T] extends Observer[T] with Subscription {

  self =>

  private [scala] val asJavaSubscriber: rx.Subscriber[_ >: T] = new rx.Subscriber[T] with SubscriberAdapter[T] {
    override def onStart(): Unit = self.onStart()
    override def onNext(value: T): Unit = self.onNext(value)
    override def onError(error: Throwable): Unit = self.onError(error)
    override def onCompleted(): Unit = self.onCompleted()
    override def requestMore(n: Long): Unit = request(n)
  }

  private [scala] override val asJavaObserver: rx.Observer[_ >: T] = asJavaSubscriber
  private [scala] override val asJavaSubscription: rx.Subscription = asJavaSubscriber

  /**
   * Used to register an unsubscribe callback.
   */
  final def add(s: Subscription): Unit = {
    asJavaSubscriber.add(s.asJavaSubscription)
  }

  /**
   * Register a callback to be run when Subscriber is unsubscribed
   *
   * @param u callback to run when unsubscribed
   */
  final def add(u: => Unit): Unit = {
    asJavaSubscriber.add(Subscription(u).asJavaSubscription)
  }

  override final def unsubscribe(): Unit = {
    asJavaSubscriber.unsubscribe()
  }

  override final def isUnsubscribed: Boolean = {
    asJavaSubscriber.isUnsubscribed
  }

  def onStart(): Unit = {
    // do nothing by default
  }

  protected final def request(n: Long): Unit = {
    asJavaSubscriber match {
      case s: SubscriberAdapter[T] => s.requestMore(n)
      case _ => throw new rx.exceptions.MissingBackpressureException()
    }
  }

  def setProducer(producer: Producer): Unit = {
    asJavaSubscriber.setProducer(producer.asJavaProducer)
  }

  def setProducer(producer: Long => Unit): Unit = {
    asJavaSubscriber.setProducer(Producer(producer).asJavaProducer)
  }
}

object Subscriber extends ObserverFactoryMethods[Subscriber] {

  private[scala] def apply[T](subscriber: rx.Subscriber[T]): Subscriber[T] = new Subscriber[T] {
    override val asJavaSubscriber = subscriber
    override val asJavaObserver: rx.Observer[_ >: T] = asJavaSubscriber
    override val asJavaSubscription: rx.Subscription = asJavaSubscriber

    override def onStart(): Unit = asJavaSubscriber.onStart()
    override def onNext(value: T): Unit = asJavaSubscriber.onNext(value)
    override def onError(error: Throwable): Unit = asJavaSubscriber.onError(error)
    override def onCompleted(): Unit = asJavaSubscriber.onCompleted()
  }

  def apply[T](onNext: T => Unit, onError: Throwable => Unit, onCompleted: () => Unit): Subscriber[T] = {
    val n = onNext; val e = onError; val c = onCompleted
    // Java calls XXX; Scala receives XXX.
    Subscriber(new rx.Subscriber[T] {
      override def onNext(value: T): Unit = n(value)
      override def onError(error: Throwable): Unit = e(error)
      override def onCompleted(): Unit = c()
    })
  }

  def apply[T](subscriber: Subscriber[_], onNext: T => Unit, onError: Throwable => Unit, onCompleted: () => Unit): Subscriber[T] = {
    val n = onNext; val e = onError; val c = onCompleted
    // Java calls XXX; Scala receives XXX.
    Subscriber(new rx.Subscriber[T](subscriber.asJavaSubscriber) {
      override def onNext(value: T): Unit = n(value)
      override def onError(error: Throwable): Unit = e(error)
      override def onCompleted(): Unit = c()
    })
  }
}

sealed trait SubscriberAdapter[T] extends rx.Subscriber[T] {
  // Add a method to expose the protected `request` method
  def requestMore(n: Long): Unit
}
