package rx.lang.scala

/**
 * Contains special Observables.
 * 
 * In Scala, this package only contains [[rx.lang.scala.observables.BlockingObservable]].
 * In the corresponding Java package `rx.observables`, there is also a
 * `GroupedObservable` and a `ConnectableObservable`, but these are not needed
 * in Scala, because we use a pair `(key, observable)` instead of `GroupedObservable`
 * and a pair `(startFunction, observable)` instead of `ConnectableObservable`. 
 */
package object observables {}
