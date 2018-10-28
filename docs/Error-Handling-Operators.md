There are a variety of operators that you can use to react to or recover from `onError` notifications from reactive sources, such as `Observable`s. For example, you might:

1. swallow the error and switch over to a backup Observable to continue the sequence
1. swallow the error and emit a default item
1. swallow the error and immediately try to restart the failed Observable
1. swallow the error and try to restart the failed Observable after some back-off interval

# Outline

- [`doOnError`](#doonerror)
- [`onErrorComplete`](#onerrorcomplete)
- [`onErrorResumeNext`](#onerrorresumenext)
- [`onErrorReturn`](#onerrorreturn)
- [`onErrorReturnItem`](#onerrorreturnitem)
- [`onExceptionResumeNext`](#onexceptionresumenext)
- [`retry`](#retry)
- [`retryUntil`](#retryuntil)
- [`retryWhen`](#retrywhen)

## doOnError

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Completable`

**ReactiveX documentation:** [http://reactivex.io/documentation/operators/do.html](http://reactivex.io/documentation/operators/do.html)

When the source reactive type signals an error event, the given `io.reactivex.functions.Consumer` is invoked.

## onErrorComplete

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Completable`

**ReactiveX documentation:** [http://reactivex.io/documentation/operators/catch.html](http://reactivex.io/documentation/operators/catch.html)

When the reactive type signals an error event, the error will be swallowed and replaced by a complete event.

Optionally, a `io.reactivex.functions.Predicate` can be specified that gives more control over when an error event should be replaced by a complete event, and when not.

## onErrorResumeNext

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Completable`

**ReactiveX documentation:** [http://reactivex.io/documentation/operators/catch.html](http://reactivex.io/documentation/operators/catch.html)

Instructs a reactive type to emit a sequence of items if it encounters an error.

## onErrorReturn

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX documentation:** [http://reactivex.io/documentation/operators/catch.html](http://reactivex.io/documentation/operators/catch.html)

Instructs a reactive type to emit the item returned by the specified `io.reactivex.functions.Function` when it encounters an error.

## onErrorReturnItem

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX documentation:** [http://reactivex.io/documentation/operators/catch.html](http://reactivex.io/documentation/operators/catch.html)

Instructs a reactive type to emit a particular item when it encounters an error.

## onExceptionResumeNext

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX documentation:** [http://reactivex.io/documentation/operators/catch.html](http://reactivex.io/documentation/operators/catch.html)

Instructs a reactive type to continue emitting items after it encounters an `java.lang.Exception`. Unlike [`onErrorResumeNext`](#onerrorresumenext), this one lets other types of `Throwable` continue through.

## retry

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Completable`

**ReactiveX documentation:** [http://reactivex.io/documentation/operators/retry.html](http://reactivex.io/documentation/operators/retry.html)

If a source reactive type emits an error, resubscribe to it in the hopes that it will complete without error.

## retryUntil

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX documentation:** [http://reactivex.io/documentation/operators/retry.html](http://reactivex.io/documentation/operators/retry.html)

If a source reactive type emits an error, resubscribe to it until the given `io.reactivex.functions.BooleanSupplier` returns `true`.

## retryWhen

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Completable`

**ReactiveX documentation:** [http://reactivex.io/documentation/operators/retry.html](http://reactivex.io/documentation/operators/retry.html)

If a source reactive type emits an error, pass that error to another `Observable` or `Flowable` to determine whether to resubscribe to the source.
