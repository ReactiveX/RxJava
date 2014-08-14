# rxjava-scalaz
This provides some useful type class instances for `Observable`.  Therefore you can apply scalaz's fancy operators to `Observable`.

Provided type class instances are `Monoid`, `Monad`, `MonadPlus`, `Traverse`, `Foldable`, etc.

For QuickStart, please refer to [RxScalazDemo](./src/test/scala/rx/lang/scala/scalaz/examples/RxScalazDemo.scala).

## How to use

```scala
import scalaz._, Scalaz._
import rx.lang.scala.Observable
import rx.lang.scala.scalaz._

Observable.just(1, 2) |+| Observable.just(3, 4)             // == Observable.just(1 2 3 4)
Observable.just(1, 2) ∘ {_ + 1}                              // == Observable.just(2, 3)
(Observable.just(1, 2) |@| Observable.just(3, 4)) {_ + _}   // == Observable.just(4, 5, 5, 6)
1.η[Observable]                                              // == Observable.just(1)
(Observable.just(3) >>= {(i: Int) => Observable.just(i + 1)}) // Observable.just(4)
```

Some other useful operators are available.  Please see below for details.

## Provided Typeclass Instances
### Monoid
`Observable` obviously forms a monoid interms of  [`concat`](https://github.com/Netflix/RxJava/wiki/Mathematical-and-Aggregate-Operators#concat).

```scala
(Observable.just(1, 2) |+| Observable.just(3, 4)) === Observable.just(1, 2, 3, 4)
(Observable.just(1, 2) ⊹ Observable.just(3, 4)) === Observable.just(1, 2, 3, 4)
mzero[Observable[Int]] === Observable.empty
```

### Monad, MonadPlus
Essentially, `Observable` is similar to `Stream`. So, `Observable` can be a Stream-like `Monad` and can be a `MonadPlus` as well as `Monoid`.  Of course, `Observable` can be also `Functor` and `Applicative`.

```scala
// Functor operators
(Observable.just(1, 2) ∘ {_ + 1}) === Observable.just(2, 3)
(Observable.just(1, 2) >| 5) === Observable.just(5, 5)
Observable.just(1, 2).fpair === Observable.just((1, 1), (2, 2))
Observable.just(1, 2).fproduct {_ + 1} === Observable.just((1, 2), (2, 3))
Observable.just(1, 2).strengthL("x") === Observable.just(("x", 1), ("x", 2))
Observable.just(1, 2).strengthR("x") === Observable.just((1, "x"), (2, "x"))
Functor[Observable].lift {(_: Int) + 1}(Observable.just(1, 2)) === Observable.just(2, 3)

// Applicative operators
1.point[Observable] === Observable.just(1)
1.η[Observable] === Observable.just(1)
(Observable.just(1, 2) |@| Observable.just(3, 4)) {_ + _} === Observable.just(4, 5, 5, 6)
(Observable.just(1) <*> {(_: Int) + 1}.η[Observable]) === Observable.just(2)
Observable.just(1) <* Observable.just(2) === Observable.just(1)
Observable.just(1) *> Observable.just(2) === Observable.just(2)

// Monad and MonadPlus operators
(Observable.just(3) >>= {(i: Int) => Observable.just(i + 1)}) === Observable.just(4)
Observable.just(3) >> Observable.just(2) === Observable.just(2)
Observable.just(Observable.just(1, 2), Observable.just(3, 4)).μ === Observable.just(1, 2, 3, 4)
Observable.just(1, 2) <+> Observable.just(3, 4) === Observable.just(1, 2, 3, 4)
```

### Traverse and Foldable
`Observable` can be `Traverse` and `Foldable` as well as `Stream`.  This means you can fold `Observable` instance to single value.

```scala
Observable.just(1, 2, 3).foldMap {_.toString} === "123"
Observable.just(1, 2, 3).foldLeftM(0)((acc, v) => (acc * v).some) === 6.some
Observable.just(1, 2, 3).suml === 6
Observable.just(1, 2, 3).∀(_ > 3) === true
Observable.just(1, 2, 3).∃(_ > 3) === false
Observable.just(1, 2, 3).traverse(x => (x + 1).some) === Observable.just(2, 3, 4).some
Observable.just(1.some, 2.some).sequence === Observable.just(1, 2).some
```
