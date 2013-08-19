/**
 * Copyright 2013 Netflix, Inc.
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
package rx.lang.clojure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observer;
import rx.util.functions.FunctionLanguageAdaptor;

import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.Var;

/**
 * Defines the single Clojure class {@code IFn} that should map to Rx functions
 * For now, a unit test to prove the Clojure integration works
 */
public class ClojureAdaptor implements FunctionLanguageAdaptor {

    @Override
    public Map<Class<?>, Class<?>> getFunctionClassRewritingMap() {
        Map<Class<?>, Class<?>> m = new HashMap<Class<?>, Class<?>>();
        m.put(IFn.class, ClojureFunctionWrapper.class);
        return m;
    }

    @Override
    public Map<Class<?>, Class<?>> getActionClassRewritingMap() {
        Map<Class<?>, Class<?>> m = new HashMap<Class<?>, Class<?>>();
        m.put(IFn.class, ClojureActionWrapper.class);
        return m;
    }

    @Override
    public Set<Class<?>> getAllClassesToRewrite() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(IFn.class);
        return classes;
    }

    public static class UnitTest {

        @Mock
        ScriptAssertion assertion;

        @Mock
        Observer<Integer> w;

        @Before
        public void before() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        public void testTake() {
            runClojureScript("(-> (rx.Observable/toObservable [\"one\" \"two\" \"three\"]) (.take 2) (.subscribe (fn [arg] (println arg))))");
        }

        // commented out for now as I can't figure out how to set the var 'a' with the 'assertion' instance when running the code from java 
        //        @Test
        //        public void testFilter() {
        //            runClojureScript("(-> (org.rx.reactive.Observable/toObservable [1 2 3])  (.filter (fn [v] (>= v 2))) (.subscribe (fn [result] (a.received(result)))))");
        //            verify(assertion, times(0)).received(1);
        //            verify(assertion, times(1)).received(2);
        //            verify(assertion, times(1)).received(3);
        //        }

        private static interface ScriptAssertion {
            public void error(Exception o);

            public void received(Object o);
        }

        private void runClojureScript(String script) {
            Object code = RT.var("clojure.core", "read-string").invoke(script);
            Var eval = RT.var("clojure.core", "eval");
            Object result = eval.invoke(code);
            System.out.println("Result: " + result);
        }
    }
}
