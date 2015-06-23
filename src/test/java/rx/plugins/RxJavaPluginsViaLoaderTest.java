/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.plugins;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class RxJavaPluginsViaLoaderTest {
    private MockLoader mockLoader;

    @Before
    public void resetBefore() {
        RxJavaPlugins.getInstance().reset();
        mockLoader = new MockLoader();
        Thread.currentThread().setContextClassLoader(mockLoader);
    }

    @After
    public void resetAfter() {
        Thread.currentThread().setContextClassLoader(null);
        RxJavaPlugins.getInstance().reset();
    }

    @Test
    public void testErrorHandlerViaServiceLoader() {
        mockLoader.register(
            "META-INF/services/" + RxJavaErrorHandler.class.getName(), 
            RxJavaPluginsViaLoaderTest.class.getResource("RxJavaErrorHandlerTestImpl.txt")
        );
        RxJavaPlugins p = new RxJavaPlugins();
        RxJavaErrorHandler impl = p.getErrorHandler();
        assertTrue(impl instanceof RxJavaErrorHandlerTestImpl);
    }

    @Test(expected = IllegalStateException.class)
    public void testTwoErrorHandlersYieldException() {
        mockLoader.register(
            "META-INF/services/" + RxJavaErrorHandler.class.getName(),
            RxJavaPluginsViaLoaderTest.class.getResource("RxJavaErrorHandlerTestBroken.txt")
        );
        RxJavaPlugins p = new RxJavaPlugins();
        RxJavaErrorHandler impl = p.getErrorHandler();
    }

    // inside test so it is stripped from Javadocs
    public static class RxJavaErrorHandlerTestImpl extends RxJavaErrorHandler {

        private volatile Throwable e;
        private volatile int count = 0;

        @Override
        public void handleError(Throwable e) {
            e.printStackTrace();
            this.e = e;
            count++;
        }

    }

    public static class RxJavaErrorHandlerTestImpl2 extends RxJavaErrorHandlerTestImpl {
    }

    @Test
    public void testObservableExecutionHookViaLoader() {
        mockLoader.register(
            "META-INF/services/" + RxJavaObservableExecutionHook.class.getName(), 
            RxJavaPluginsViaLoaderTest.class.getResource("RxJavaObservableExecutionHookTestImpl.txt")
        );
        RxJavaPlugins p = new RxJavaPlugins();
        RxJavaObservableExecutionHook impl = p.getObservableExecutionHook();
        assertTrue(impl instanceof RxJavaObservableExecutionHookTestImpl);
    }

    // inside test so it is stripped from Javadocs
    public static class RxJavaObservableExecutionHookTestImpl extends RxJavaObservableExecutionHook {
        // just use defaults
    }

    private static final class MockLoader extends ClassLoader {
        private final Map<String,URL[]> resources = new HashMap<String, URL[]>();
        
        public void register(String resource, URL... urls) {
            for (URL url : urls) {
                assertNotNull("Null URL when registering " + resource, url);
            }
            resources.put(resource, urls);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            URL[] urls = resources.get(name);
            if (urls != null) {
                return Collections.enumeration(Arrays.asList(urls));
            }
            return super.getResources(name);
        }
        
    }
}
