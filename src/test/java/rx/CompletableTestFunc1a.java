package rx;
import rx.functions.Func1;

/**
 * Created by kgalligan on 10/13/16.
 */

public class CompletableTestFunc1a implements Func1<Completable.OnSubscribe, Completable.OnSubscribe>
{
    @Override
    public Completable.OnSubscribe call(Completable.OnSubscribe t) {
        return t;
    }
}
