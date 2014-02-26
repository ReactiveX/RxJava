package com.netflix.rxjava.android.samples;

import android.os.SystemClock;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class SampleObservables {

    /**
     * Emits numbers as strings, where these numbers a generated on a background thread.
     */
    public static Observable<String> numberStrings() {
        return Observable.range(1, 10).map(new Func1<Integer, String>() {
            @Override
            public String call(Integer integer) {
                return integer.toString();
            }
        }).doOnNext(new Action1<String>() {
            @Override
            public void call(String s) {
                SystemClock.sleep(1000);
            }
        }).subscribeOn(Schedulers.newThread());
    }

}
