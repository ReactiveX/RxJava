package rx.android.testsupport;

import com.xtremelabs.robolectric.RobolectricConfig;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.runners.model.InitializationError;

import java.io.File;

public class AndroidTestRunner extends RobolectricTestRunner {

    public AndroidTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass, new RobolectricConfig(new File("src/test/resources")));
    }
}
