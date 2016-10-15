package rx.doppl.mock.singletest;
import rx.Subscription;
import rx.functions.Func1;

/**
 * Created by kgalligan on 10/15/16.
 */

public class FuncReturn implements Func1<Subscription, Subscription>
{
    @Override
    public Subscription call(Subscription t) {
        return t;
    }
}
