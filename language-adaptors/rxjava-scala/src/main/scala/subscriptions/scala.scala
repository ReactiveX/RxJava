package rx.lang

package object scala {

  /**
   * Allows to construct observables in a similar way as futures.
   *
   * Example:
   *
   * {{{
   * implicit val scheduler = Schedulers.threadPoolForIO
   * val o: Observable[List[Friend]] = observable {
   *    session.getFriends
   * }
   * o.subscribe(
   *   friendList => println(friendList),
   *   err => println(err.getMessage)
   * )
   * }}}
   */
  def observable[T](body: => T)(implicit scheduler: Scheduler): Observable[T] = {
    Observable(1).observeOn(scheduler).map(_ => body)
  }
}
