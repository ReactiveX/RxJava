import rx.lang.scala.Observable

object CreateFromSource {

  def create() {
    val o1 = Observable.just("a", "b", "c")

    def list = List(5, 6, 7, 8)
    val o2 = Observable.from(list)

    val o3 = Observable.just("one object")
  }
}
