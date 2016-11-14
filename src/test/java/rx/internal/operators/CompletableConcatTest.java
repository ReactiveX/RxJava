package rx.internal.operators;

import java.util.concurrent.TimeUnit;

import org.junit.*;

import rx.*;
import rx.functions.*;
import rx.schedulers.Schedulers;

public class CompletableConcatTest {

    @Test
    public void asyncObservables() {

        final int[] calls = { 0 };

        Completable.concat(Observable.range(1, 5).map(new Func1<Integer, Completable>() {
            @Override
            public Completable call(final Integer v) {
                System.out.println("Mapping " + v);
                return Completable.fromAction(new Action0() {
                    @Override
                    public void call() {
                        System.out.println("Processing " + (calls[0] + 1));
                        calls[0]++;
                    }
                })
                .subscribeOn(Schedulers.io())
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        System.out.println("Inner complete " + v);
                    }
                })
                .observeOn(Schedulers.computation());
            }
        })
        ).test()
        .awaitTerminalEventAndUnsubscribeOnTimeout(5, TimeUnit.SECONDS)
        .assertResult();

        Assert.assertEquals(5, calls[0]);
    }
}
