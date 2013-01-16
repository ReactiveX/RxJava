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
package org.rx.lang.clojure;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.observables.Observer;
import rx.util.FunctionLanguageAdaptor;

import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.Var;

public class ClojureAdaptor implements FunctionLanguageAdaptor {

    @Override
    public Object call(Object function, Object[] args) {
        if (args.length == 0) {
            return ((IFn) function).invoke();
        } else if (args.length == 1) {
            return ((IFn) function).invoke(args[0]);
        } else if (args.length == 2) {
            return ((IFn) function).invoke(args[0], args[1]);
        } else if (args.length == 3) {
            return ((IFn) function).invoke(args[0], args[1], args[2]);
        } else if (args.length == 4) {
            return ((IFn) function).invoke(args[0], args[1], args[2], args[3]);
        } else if (args.length == 5) {
            return ((IFn) function).invoke(args[0], args[1], args[2], args[3], args[4]);
        } else if (args.length == 6) {
            return ((IFn) function).invoke(args[0], args[1], args[2], args[3], args[4], args[5]);
        } else if (args.length == 7) {
            return ((IFn) function).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
        } else if (args.length == 8) {
            return ((IFn) function).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
        } else if (args.length == 9) {
            return ((IFn) function).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
        } else if (args.length == 10) {
            return ((IFn) function).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9]);
        } else if (args.length == 11) {
            return ((IFn) function).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10]);
        } else if (args.length == 12) {
            return ((IFn) function).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11]);
        } else if (args.length == 13) {
            return ((IFn) function).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12]);
        } else if (args.length == 14) {
            return ((IFn) function).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13]);
        } else if (args.length == 15) {
            return ((IFn) function).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14]);
        } else if (args.length == 16) {
            return ((IFn) function).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15]);
        } else if (args.length == 17) {
            return ((IFn) function).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16]);
        } else if (args.length == 18) {
            return ((IFn) function).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17]);
        } else if (args.length == 19) {
            return ((IFn) function).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18]);
        } else if (args.length == 20) {
            return ((IFn) function).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18], args[19]);
        } else {
            return ((IFn) function).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18], args[19], Arrays.copyOfRange(args, 20, args.length));
        }
    }

    @Override
    public Class<?>[] getFunctionClass() {
        return new Class<?>[] { IFn.class };
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
            runClojureScript("(-> (org.rx.reactive.Observable/toObservable [\"one\" \"two\" \"three\"]) (.take 2) (.subscribe (fn [arg] (println arg))))");
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
