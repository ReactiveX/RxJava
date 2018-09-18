There are a variety of operators that you can use to react to or recover from `onError` notifications from Observables. For example, you might:

1. swallow the error and switch over to a backup Observable to continue the sequence
1. swallow the error and emit a default item
1. swallow the error and immediately try to restart the failed Observable
1. swallow the error and try to restart the failed Observable after some back-off interval

The following pages explain these operators.

* [**`onErrorResumeNext( )`**](http://reactivex.io/documentation/operators/catch.html) — instructs an Observable to emit a sequence of items if it encounters an error
* [**`onErrorReturn( )`**](http://reactivex.io/documentation/operators/catch.html) — instructs an Observable to emit a particular item when it encounters an error
* [**`onExceptionResumeNext( )`**](http://reactivex.io/documentation/operators/catch.html) — instructs an Observable to continue emitting items after it encounters an exception (but not another variety of throwable)
* [**`retry( )`**](http://reactivex.io/documentation/operators/retry.html) — if a source Observable emits an error, resubscribe to it in the hopes that it will complete without error
* [**`retryWhen( )`**](http://reactivex.io/documentation/operators/retry.html) — if a source Observable emits an error, pass that error to another Observable to determine whether to resubscribe to the source