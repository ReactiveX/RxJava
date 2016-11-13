package rx.doppl;
import java.lang.ref.WeakReference;

/**
 * Created by kgalligan on 11/13/16.
 */

public class J2objcWeakReference<T>
{
    private static final boolean USE_WEAK = false;
    private final WeakReference<T> weakReference;
    private final T hardRef;
    public J2objcWeakReference(T val)
    {
        weakReference = USE_WEAK ? new WeakReference<T>(val) : null;
        hardRef = USE_WEAK ? null : val;
    }

    public T get()
    {
        return USE_WEAK ? weakReference.get() : hardRef;
    }
}
