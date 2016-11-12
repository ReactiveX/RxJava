package rx.doppl.memory;
import org.junit.Test;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;

/**
 * Created by kgalligan on 11/11/16.
 */

public class BasicOperatorTest
{
    @Test
    public void testTake()
    {
        Observable<Integer> observable = Observable.create(new Observable.OnSubscribe<Integer>()
        {
            @Override
            public void call(Subscriber<? super Integer> subscriber)
            {
                subscriber.onNext(1);
                subscriber.onNext(2);
                subscriber.onNext(3);
                subscriber.onNext(4);
            }
        });

        observable.take(2).subscribe(new Action1<Integer>()
        {
            @Override
            public void call(Integer integer)
            {
                System.out.println("called "+ integer);
            }
        });
    }
}
