package rx.plugins;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class RxJavaPluginsTest {

  @Test
  public void testErrorHandlerDefaultImpl() {
    RxJavaErrorHandler impl = new RxJavaPlugins().getErrorHandler();
    assertTrue(impl instanceof RxJavaErrorHandlerDefault);
  }

  @Test
  public void testErrorHandlerViaRegisterMethod() {
    RxJavaPlugins p = new RxJavaPlugins();
    p.registerErrorHandler(new RxJavaErrorHandlerTestImpl());
    RxJavaErrorHandler impl = p.getErrorHandler();
    assertTrue(impl instanceof RxJavaErrorHandlerTestImpl);
  }

  @Test
  public void testErrorHandlerViaProperty() {
    try {
      RxJavaPlugins p = new RxJavaPlugins();
      String fullClass = getFullClassNameForTestClass(RxJavaErrorHandlerTestImpl.class);
      System.setProperty("rxjava.plugin.RxJavaErrorHandler.implementation", fullClass);
      RxJavaErrorHandler impl = p.getErrorHandler();
      assertTrue(impl instanceof RxJavaErrorHandlerTestImpl);
    } finally {
      System.clearProperty("rxjava.plugin.RxJavaErrorHandler.implementation");
    }
  }

  // inside test so it is stripped from Javadocs
  public static class RxJavaErrorHandlerTestImpl extends RxJavaErrorHandler {
    // just use defaults
  }

  @Test
  public void testObservableExecutionHookDefaultImpl() {
    RxJavaPlugins p = new RxJavaPlugins();
    RxJavaObservableExecutionHook impl = p.getObservableExecutionHook();
    assertTrue(impl instanceof RxJavaObservableExecutionHookDefault);
  }

  @Test
  public void testObservableExecutionHookViaRegisterMethod() {
    RxJavaPlugins p = new RxJavaPlugins();
    p.registerObservableExecutionHook(new RxJavaObservableExecutionHookTestImpl());
    RxJavaObservableExecutionHook impl = p.getObservableExecutionHook();
    assertTrue(impl instanceof RxJavaObservableExecutionHookTestImpl);
  }

  @Test
  public void testObservableExecutionHookViaProperty() {
    try {
      RxJavaPlugins p = new RxJavaPlugins();
      String fullClass = getFullClassNameForTestClass(RxJavaObservableExecutionHookTestImpl.class);
      System.setProperty("rxjava.plugin.RxJavaObservableExecutionHook.implementation", fullClass);
      RxJavaObservableExecutionHook impl = p.getObservableExecutionHook();
      assertTrue(impl instanceof RxJavaObservableExecutionHookTestImpl);
    } finally {
      System.clearProperty("rxjava.plugin.RxJavaObservableExecutionHook.implementation");
    }
  }

  // inside test so it is stripped from Javadocs
  public static class RxJavaObservableExecutionHookTestImpl extends RxJavaObservableExecutionHook {
    // just use defaults
  }

  private static String getFullClassNameForTestClass(Class<?> cls) {
    return RxJavaPlugins.class.getPackage().getName() + "." + RxJavaPluginsTest.class.getSimpleName() + "$" + cls.getSimpleName();
  }
}
