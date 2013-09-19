package rx.lang.groovy

import org.junit.Test

import rx.Observable
import rx.Scheduler
import rx.concurrency.Schedulers
import rx.util.functions.Func1

class TestParallel {

    @Test
    public void testParallelOperator() {
        Observable.range(0, 100)
                .parallel({
                    it.map({ return it; })
                })
                .toBlockingObservable()
                .forEach({ println("T: " + it + " Thread: " + Thread.currentThread());  });
    }
}
