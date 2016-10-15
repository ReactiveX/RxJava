package rx.doppl.mock.singletest;
import rx.Single;
import rx.functions.Func1;

/**
 * Created by kgalligan on 10/15/16.
 */

public class FuncCreate implements Func1<Single.OnSubscribe, Single.OnSubscribe>
{
    @Override
    public Single.OnSubscribe call(Single.OnSubscribe t) {
        return t;
    }
}
