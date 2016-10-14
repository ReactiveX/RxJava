package rx;
import rx.functions.Func1;
import rx.functions.Func2;

/**
 * Created by kgalligan on 10/13/16.
 */

public class CompletableTestFunc1b implements Func2<Completable, Completable.OnSubscribe, Completable.OnSubscribe>
{
    @Override
    public Completable.OnSubscribe call(Completable t1, Completable.OnSubscribe t2) {
        return t2;
    }
}
