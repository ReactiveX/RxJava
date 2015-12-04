package rx.internal.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PrivateConstructionCheckerTest {

    @Test
    public void builderShouldThrowExceptionIfNullWasPassedAsExpectedTypeOfException() {
        try {
            PrivateConstructorChecker
                    .forClass(Object.class)
                    .expectedTypeOfException(null);

            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("expectedTypeOfException can not be null", expected.getMessage());
        }
    }

    @Test
    public void builderShouldThrowExceptionIfNullWasPassedAsExpectedExceptionMessage() {
        try {
            PrivateConstructorChecker
                    .forClass(Object.class)
                    .expectedExceptionMessage(null);

            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("expectedExceptionMessage can not be null", expected.getMessage());
        }
    }

    static class ClassWithoutDefaultConstructor {
        private ClassWithoutDefaultConstructor(String someParam) {
        }
    }

    @Test
    public void shouldThrowExceptionIfClassHasNonDefaultConstructor() {
        try {
            PrivateConstructorChecker
                    .forClass(ClassWithoutDefaultConstructor.class)
                    .check();

            fail();
        } catch (AssertionError expected) {
            assertEquals(
                    "Class has non-default constructor with some parameters",
                    expected.getMessage()
            );
        }
    }

    static class ClassWithPrivateConstructor {
        private ClassWithPrivateConstructor() {
        }
    }

    @Test
    public void shouldAssertThatConstructorIsPrivateAndDoesNotThrowExceptions() {
        PrivateConstructorChecker
                .forClass(ClassWithPrivateConstructor.class)
                .check();
    }

    static class ClassWithDefaultProtectedConstructor {
        ClassWithDefaultProtectedConstructor() {
        }
    }

    @Test
    public void shouldThrowExceptionBecauseConstructorHasDefaultModifier() {
        try {
            PrivateConstructorChecker
                    .forClass(ClassWithDefaultProtectedConstructor.class)
                    .check();

            fail();
        } catch (AssertionError expected) {
            assertEquals("Constructor must be private", expected.getMessage());
        }
    }

    static class ClassWithProtectedConstructor {
        protected ClassWithProtectedConstructor() {
        }
    }

    @Test
    public void shouldThrowExceptionBecauseConstructorHasProtectedModifier() {
        try {
            PrivateConstructorChecker
                    .forClass(ClassWithProtectedConstructor.class)
                    .check();

            fail();
        } catch (AssertionError expected) {
            assertEquals("Constructor must be private", expected.getMessage());
        }
    }

    static class ClassWithPublicConstructor {
        public ClassWithPublicConstructor() {
        }
    }

    @Test
    public void shouldThrowExceptionBecauseConstructorHasPublicModifier() {
        try {
            PrivateConstructorChecker
                    .forClass(ClassWithPublicConstructor.class)
                    .check();

            fail();
        } catch (AssertionError expected) {
            assertEquals("Constructor must be private", expected.getMessage());
        }
    }

    static class ClassWithConstructorThatThrowsException {
        private ClassWithConstructorThatThrowsException() {
            throw new IllegalStateException("test exception");
        }
    }

    @Test
    public void shouldCheckThatConstructorThrowsExceptionWithoutCheckingMessage() {
        PrivateConstructorChecker
                .forClass(ClassWithConstructorThatThrowsException.class)
                .expectedTypeOfException(IllegalStateException.class)
                .check();
    }

    @Test
    public void shouldCheckThatConstructorThrowsExceptionWithExpectedMessage() {
        PrivateConstructorChecker
                .forClass(ClassWithConstructorThatThrowsException.class)
                .expectedTypeOfException(IllegalStateException.class)
                .expectedExceptionMessage("test exception")
                .check();
    }

    @Test
    public void shouldCheckThatConstructorThrowsExceptionWithExpectedMessageButWithoutExpectedExceptionType() {
        PrivateConstructorChecker
                .forClass(ClassWithConstructorThatThrowsException.class)
                .expectedExceptionMessage("test exception")
                .check(); // without checking exception's type
    }

    @Test
    public void shouldThrowExceptionBecauseTypeOfExpectedExceptionDoesNotMatch() {
        try {
            PrivateConstructorChecker
                    .forClass(ClassWithConstructorThatThrowsException.class)
                    .expectedTypeOfException(IllegalArgumentException.class) // Incorrect type
                    .check();

            fail();
        } catch (IllegalStateException expected) {
            assertEquals("Expected exception of type = class java.lang.IllegalArgumentException, " +
                            "but was exception of type = class java.lang.IllegalStateException",
                    expected.getMessage()
            );
        }
    }

    @Test
    public void shouldThrowExceptionBecauseExpectedMessageDoesNotMatch() {
        try {
            PrivateConstructorChecker
                    .forClass(ClassWithConstructorThatThrowsException.class)
                    .expectedTypeOfException(IllegalStateException.class) // Correct type
                    .expectedExceptionMessage("lol, not something that you've expected?") // Incorrect message
                    .check();

            fail();
        } catch (IllegalStateException expected) {
            assertEquals("Expected exception message = 'lol, not something that you've expected?', " +
                            "but was = 'test exception'",
                    expected.getMessage()
            );
        }
    }

    @Test
    public void shouldThrowExceptionBecauseConstructorThrownUnexpectedException() {
        try {
            PrivateConstructorChecker
                    .forClass(ClassWithConstructorThatThrowsException.class)
                    .check(); // We don't expect exception, but it will be thrown

            fail();
        } catch (IllegalStateException expected) {
            assertEquals("No exception was expected", expected.getMessage());
        }
    }

    static class ClassWith2Constructors {
        // This is good constructor for the checker
        private ClassWith2Constructors() {

        }

        // This is bad constructor
        private ClassWith2Constructors(String str) {

        }
    }

    @Test
    public void shouldThrowExceptionBecauseClassHasMoreThanOneConstructor() {
        try {
            PrivateConstructorChecker
                    .forClass(ClassWith2Constructors.class)
                    .check();

            fail();
        } catch (AssertionError expected) {
            assertEquals("Class has more than one constructor", expected.getMessage());
        }
    }

    static class ClassWithoutDeclaredConstructor {

    }

    @Test
    public void shouldThrowExceptionBecauseClassDoesNotHaveDeclaredConstructors() {
        try {
            PrivateConstructorChecker
                    .forClass(ClassWithoutDeclaredConstructor.class)
                    .check();

            fail();
        } catch (AssertionError expected) {
            assertEquals("Constructor must be private", expected.getMessage());
        }
    }
}
