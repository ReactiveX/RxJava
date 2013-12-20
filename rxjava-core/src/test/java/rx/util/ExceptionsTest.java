package rx.util;

import java.io.IOException;

import org.junit.Test;

public class ExceptionsTest {

    @Test(expected=IOException.class)
    public void test() {
        Exceptions.propagate(new IOException());
    }
}
