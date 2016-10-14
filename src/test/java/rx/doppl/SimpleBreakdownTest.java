package rx.doppl;
import org.junit.Test;

import rx.Observable;
import rx.Subscriber;
import rx.observers.TestSubscriber;

/**
 * Created by kgalligan on 10/13/16.
 */

public class SimpleBreakdownTest
{
    @Test
    public void longRunning() {
        Subscriber<Integer> ts = new Subscriber<Integer>()
        {
            @Override
            public void onCompleted()
            {

            }

            @Override
            public void onError(Throwable e)
            {

            }

            @Override
            public void onNext(Integer integer)
            {
                System.out.println("Mine: "+ integer);
            }
        };

        int n = 1000 * 5;

        Observable.range(1, n)
                .subscribe(ts);

//        ts.assertValueCount(n * 2);
//        ts.assertNoErrors();
//        ts.assertCompleted();
    }
}
