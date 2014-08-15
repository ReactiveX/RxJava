object Transforming extends App {
  /**
   * Asynchronously calls 'customObservableNonBlocking' and defines
   * a chain of operators to apply to the callback sequence.
   */
  def simpleComposition() {
    AsyncObservable.customObservableNonBlocking().drop(10).take(5)
      .map(stringValue => stringValue + "_xform")
      .subscribe(s => println("onNext => " + s))
  }

  simpleComposition()
}
