package rx.doppl;
import java.lang.ref.WeakReference;

/**
 * Created by kgalligan on 11/13/16.
 */

public class J2objcWeakReference<T>
{
    private final WeakReference<T> weakReference;

    public J2objcWeakReference(T val)
    {
        weakReference = new WeakReference<T>(val);
    }

    public T get()
    {
        return weakReference.get();
    }
}
