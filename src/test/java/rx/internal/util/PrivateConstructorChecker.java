package rx.internal.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public class PrivateConstructorChecker<T> {

    private final Class<T> clazz;

    private final Class<? extends Throwable> expectedTypeOfException;

    private final String expectedExceptionMessage;

    private PrivateConstructorChecker(Class<T> clazz,
                                      Class<? extends Throwable> expectedTypeOfException,
                                      String expectedExceptionMessage) {
        this.clazz = clazz;
        this.expectedTypeOfException = expectedTypeOfException;
        this.expectedExceptionMessage = expectedExceptionMessage;
    }

    public static class Builder<T> {

        private final Class<T> clazz;

        private Class<? extends Throwable> expectedTypeOfException;

        private String expectedExceptionMessage;

        Builder(Class<T> clazz) {
            this.clazz = clazz;
        }

        /**
         * Sets the expected type of exception that must be thrown by the constructor of required class.
         * <p/>
         * If you don't want to check exception message, you can set just type of the exception.
         *
         * @param expectedTypeOfException type of the exception that must be thrown by the constructor
         *                                of required class, should not be {@code null}.
         * @return builder.
         */
        public Builder<T> expectedTypeOfException(Class<? extends Throwable> expectedTypeOfException) {
            if (expectedTypeOfException == null) {
                throw new IllegalArgumentException("expectedTypeOfException can not be null");
            }

            this.expectedTypeOfException = expectedTypeOfException;
            return this;
        }

        /**
         * Sets the expected message of the exception that must be thrown by the constructor of required class.
         * <p/>
         * If you don't want to check the type of the exception, you can set just a message.
         *
         * @param expectedExceptionMessage message of the exception that must be thrown by the constructor
         *                                 of required class, should not be {@code null}.
         * @return builder.
         */
        public Builder<T> expectedExceptionMessage(String expectedExceptionMessage) {
            if (expectedExceptionMessage == null) {
                throw new IllegalArgumentException("expectedExceptionMessage can not be null");
            }

            this.expectedExceptionMessage = expectedExceptionMessage;
            return this;
        }

        /**
         * Runs the check which will assert that required class has one private constructor
         * which throws or not throws exception.
         */
        public void check() {
            new PrivateConstructorChecker<T>(
                    clazz,
                    expectedTypeOfException,
                    expectedExceptionMessage
            ).check();
        }
    }

    /**
     * Creates instance of {@link PrivateConstructorChecker.Builder}.
     *
     * @param clazz class that needs to be checked.
     * @param <T>   type of the class.
     * @return {@link PrivateConstructorChecker.Builder} which will prepare
     * check of the passed class.
     */
    public static <T> Builder<T> forClass(Class<T> clazz) {
        return new Builder<T>(clazz);
    }

    /**
     * Runs the check which will assert that required class has one private constructor
     * which throws or not throws exception.
     */
    public void check() {
        final Constructor<?>[] constructors = clazz.getDeclaredConstructors();

        if (constructors.length > 1) {
            throw new AssertionError("Class has more than one constructor");
        }

        final Constructor<?> constructor = constructors[0];

        if (constructor.getParameterTypes().length > 0) {
            throw new AssertionError("Class has non-default constructor with some parameters");
        }

        constructor.setAccessible(true);

        if (!Modifier.isPrivate(constructor.getModifiers())) {
            throw new AssertionError("Constructor must be private");
        }

        try {
            constructor.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException("Can not instantiate instance of " + clazz, e);
        } catch (IllegalAccessException e) {
            // Fixed by setAccessible(true)
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();

            // It's okay case if we expect some exception from this constructor
            if (expectedTypeOfException != null || expectedExceptionMessage != null) {
                if (expectedTypeOfException != null && !expectedTypeOfException.equals(cause.getClass())) {
                    throw new IllegalStateException("Expected exception of type = "
                            + expectedTypeOfException + ", but was exception of type = "
                            + e.getCause().getClass()
                    );
                }

                if (expectedExceptionMessage != null && !expectedExceptionMessage.equals(cause.getMessage())) {
                    throw new IllegalStateException("Expected exception message = '"
                            + expectedExceptionMessage + "', but was = '"
                            + cause.getMessage() + "'",
                            e.getCause()
                    );
                }

                // Everything is okay
            } else {
                throw new IllegalStateException("No exception was expected", e);
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Looks like constructor of " + clazz + " is not default", e);
        }
    }
}
