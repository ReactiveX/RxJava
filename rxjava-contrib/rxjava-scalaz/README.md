# rxjava-scalaz
This provides some useful type class instances for `Observable`.  Therefore you can apply scalaz's fancy operators to `Observable`.

Provided type class instances are `Monoid`, `Monad`, `MonadPlus`, `Traverse`, `Foldable`, etc.

For QuickStart, please refer to [RxScalazDemo](./src/test/scala/rx/lang/scala/scalaz/examples/RxScalazDemo.scala).

## How to use

```scala
import scalaz._, Scalaz._
import rx.lang.scala.Observable
import rx.lang.scala.scalaz._

Observable.items(1, 2) |+| Observable.items(3, 4)             // == Observable.items(1 2 3 4)
Observable.items(1, 2) ∘ {_ + 1}                              // == Observable.items(2, 3)
(Observable.items(1, 2) |@| Observable.items(3, 4)) {_ + _}   // == Observable.items(4, 5, 5, 6)
1.η[Observable]                                              // == Observable.items(1)
(Observable.items(3) >>= {(i: Int) => Observable.items(i + 1)}) // Observable.items(4)
```

Some other useful operators are available.  Please see below for details.

## Provided Typeclass Instances
### Monoid
`Observable` obviously forms a monoid interms of  [`concat`](https://github.com/Netflix/RxJava/wiki/Mathematical-and-Aggregate-Operators#concat).

```scala
(Observable.items(1, 2) |+| Observable.items(3, 4)) === Observable.items(1, 2, 3, 4)
(Observable.items(1, 2) ⊹ Observable.items(3, 4)) === Observable.items(1, 2, 3, 4)
mzero[Observable[Int]] === Observable.empty
```

### Monad, MonadPlus
Essentially, `Observable` is similar to `Stream`. So, `Observable` can be a Stream-like `Monad` and can be a `MonadPlus` as well as `Monoid`.  Of course, `Observable` can be also `Functor` and `Applicative`.

```scala
// Functor operators
(Observable.items(1, 2) ∘ {_ + 1}) === Observable.items(2, 3)
(Observable.items(1, 2) >| 5) === Observable.items(5, 5)
Observable.items(1, 2).fpair === Observable.items((1, 1), (2, 2))
Observable.items(1, 2).fproduct {_ + 1} === Observable.items((1, 2), (2, 3))
Observable.items(1, 2).strengthL("x") === Observable.items(("x", 1), ("x", 2))
Observable.items(1, 2).strengthR("x") === Observable.items((1, "x"), (2, "x"))
Functor[Observable].lift {(_: Int) + 1}(Observable.items(1, 2)) === Observable.items(2, 3)

// Applicative operators
1.point[Observable] === Observable.items(1)
1.η[Observable] === Observable.items(1)
(Observable.items(1, 2) |@| Observable.items(3, 4)) {_ + _} === Observable.items(4, 5, 5, 6)
(Observable.items(1) <*> {(_: Int) + 1}.η[Observable]) === Observable.items(2)
Observable.items(1) <* Observable.items(2) === Observable.items(1)
Observable.items(1) *> Observable.items(2) === Observable.items(2)

// Monad and MonadPlus operators
(Observable.items(3) >>= {(i: Int) => Observable.items(i + 1)}) === Observable.items(4)
Observable.items(3) >> Observable.items(2) === Observable.items(2)
Observable.items(Observable.items(1, 2), Observable.items(3, 4)).μ === Observable.items(1, 2, 3, 4)
Observable.items(1, 2) <+> Observable.items(3, 4) === Observable.items(1, 2, 3, 4)
```

### Traverse and Foldable
`Observable` can be `Traverse` and `Foldable` as well as `Stream`.  This means you can fold `Observable` instance to single value.

```scala
Observable.items(1, 2, 3).foldMap {_.toString} === "123"
Observable.items(1, 2, 3).foldLeftM(0)((acc, v) => (acc * v).some) === 6.some
Observable.items(1, 2, 3).suml === 6
Observable.items(1, 2, 3).∀(_ > 3) === true
Observable.items(1, 2, 3).∃(_ > 3) === false
Observable.items(1, 2, 3).traverse(x => (x + 1).some) === Observable.items(2, 3, 4).some
Observable.items(1.some, 2.some).sequence === Observable.items(1, 2).some
```
