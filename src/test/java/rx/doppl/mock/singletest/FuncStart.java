package rx.doppl.mock.singletest;
import rx.Single;
import rx.functions.Func2;

/**
 * Created by kgalligan on 10/15/16.
 */

public class FuncStart implements Func2<Single, Single.OnSubscribe, Single.OnSubscribe>
{
    @Override
    public Single.OnSubscribe call(Single t1, Single.OnSubscribe t2) {
        return t2;
    }
}
