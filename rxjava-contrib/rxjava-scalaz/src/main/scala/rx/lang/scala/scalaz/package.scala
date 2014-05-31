/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.lang.scala

import scala.language.higherKinds
import _root_.scalaz._
import _root_.scalaz.Tags.{Zip => TZip}

/**
 * This package object provides some type class instances for Observable.
 */
package object scalaz {

  // Monoid
  implicit def observableMonoid[A] = new Monoid[Observable[A]] {
    override def zero: Observable[A] = Observable.empty
    override def append(f1: Observable[A], f2: => Observable[A]): Observable[A] = f1 ++ f2
  }

  implicit val observableInstances = new MonadPlus[Observable] with Zip[Observable]
    with IsEmpty[Observable] with Traverse[Observable] {

    // Monad
    override def point[A](a: => A) = Observable.items(a)
    override def bind[A, B](oa: Observable[A])(f: (A) => Observable[B]) = oa.flatMap(f)

    // MonadPlus
    override def empty[A]: Observable[A] = observableMonoid[A].zero
    override def plus[A](a: Observable[A], b: => Observable[A]): Observable[A] = observableMonoid[A].append(a, b)

    // Zip
    override def zip[A, B](a: => Observable[A], b: => Observable[B]): Observable[(A, B)] = a zip b

    // IsEmpty (NOTE: This method is blocking call)
    override def isEmpty[A](fa: Observable[A]): Boolean = fa.isEmpty.toBlocking.first

    // Traverse (NOTE: This method is blocking call)
    override def traverseImpl[G[_], A, B](fa: Observable[A])(f: (A) => G[B])(implicit G: Applicative[G]): G[Observable[B]] = {
      val seed: G[Observable[B]] = G.point(Observable.empty)
      fa.foldLeft(seed) {
        (ys, x) => G.apply2(ys, f(x))((bs, b) => bs :+ b)
      }.toBlocking.first
    }
  }

  // Observable can be ZipList like applicative functor.
  // However, due to https://github.com/scalaz/scalaz/issues/338,
  // This instance doesn't have 'implicit' modifier.
  val observableZipApplicative: Applicative[({ type λ[α] = Observable[α] @@ TZip })#λ] = new Applicative[({ type λ[α] = Observable[α] @@ TZip })#λ] {
    def point[A](a: => A) = TZip(Observable.items(a).repeat)
    def ap[A, B](oa: => Observable[A] @@ TZip)(of: => Observable[A => B] @@ TZip) = TZip(of.zip(oa) map {fa => fa._1(fa._2)})
  }

}
