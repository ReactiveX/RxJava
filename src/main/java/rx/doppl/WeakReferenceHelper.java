package rx.doppl;
import com.google.j2objc.WeakProxy;

/**
 * Created by kgalligan on 11/14/16.
 */

public class WeakReferenceHelper
{
    public static <T> T wrapWeakProxyIfSame(T target, Object compare)
    {
        if(target == compare)
            return WeakProxy.forObject(target);
        else
            return target;
    }
}
