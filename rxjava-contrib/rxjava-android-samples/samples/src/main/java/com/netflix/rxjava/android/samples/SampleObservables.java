package com.netflix.rxjava.android.samples;

import android.os.SystemClock;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class SampleObservables {

    /**
     * Emits numbers as strings, where these numbers a generated on a background thread.
     */
    public static Observable<String> numberStrings(int from, int to, final long delay) {
        return Observable.range(from, to).map(new Func1<Integer, String>() {
            @Override
            public String call(Integer integer) {
                return integer.toString();
            }
        }).doOnNext(new Action1<String>() {
            @Override
            public void call(String s) {
                SystemClock.sleep(delay);
            }
        }).subscribeOn(Schedulers.newThread());
    }

    public static Observable<String> fakeApiCall(final long delay) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                // simulate I/O latency
                SystemClock.sleep(delay);
                final String fakeJson = "{\"result\": 42}";
                subscriber.onNext(fakeJson);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io());
    }
}
