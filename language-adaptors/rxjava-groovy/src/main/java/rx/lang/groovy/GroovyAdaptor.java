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
package rx.lang.groovy;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;

import java.util.Arrays;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;
import rx.util.functions.FunctionLanguageAdaptor;

public class GroovyAdaptor implements FunctionLanguageAdaptor {

    @Override
    public Object call(Object function, Object[] args) {
        return ((Closure<?>) function).call(args);
    }

    public Class<?>[] getFunctionClass() {
        return new Class<?>[] { Closure.class };
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
        public void testCreateViaGroovy() {
            runGroovyScript("o.create({it.onNext('hello');it.onCompleted();}).subscribe({ result -> a.received(result)});");
            verify(assertion, times(1)).received("hello");
        }

        @Test
        public void testFilterViaGroovy() {
            runGroovyScript("o.filter(o.toObservable(1, 2, 3), {it >= 2}).subscribe({ result -> a.received(result)});");
            verify(assertion, times(0)).received(1);
            verify(assertion, times(1)).received(2);
            verify(assertion, times(1)).received(3);
        }

        @Test
        public void testLast() {
            String script = "mockApiCall.getObservable().last().subscribe({ result -> a.received(result)});";
            runGroovyScript(script);
            verify(assertion, times(1)).received("hello_1");
        }

        @Test
        public void testMap() {
            String script = "mockApiCall.getObservable().map({v -> 'say' + v}).subscribe({ result -> a.received(result)});";
            runGroovyScript(script);
            verify(assertion, times(1)).received("sayhello_1");
        }

        @Test
        public void testMapViaGroovy() {
            runGroovyScript("o.map(o.toObservable(1, 2, 3), {'hello_' + it}).subscribe({ result -> a.received(result)});");
            verify(assertion, times(1)).received("hello_" + 1);
            verify(assertion, times(1)).received("hello_" + 2);
            verify(assertion, times(1)).received("hello_" + 3);
        }

        @Test
        public void testMaterializeViaGroovy() {
            runGroovyScript("o.materialize(o.toObservable(1, 2, 3)).subscribe({ result -> a.received(result)});");
            // we expect 4 onNext calls: 3 for 1, 2, 3 ObservableNotification.OnNext and 1 for ObservableNotification.OnCompleted
            verify(assertion, times(4)).received(any(Notification.class));
            verify(assertion, times(0)).error(any(Exception.class));
        }

        @Test
        public void testMergeDelayErrorViaGroovy() {
            runGroovyScript("o.mergeDelayError(o.toObservable(1, 2, 3), o.merge(o.toObservable(6), o.error(new NullPointerException()), o.toObservable(7)), o.toObservable(4, 5)).subscribe({ result -> a.received(result)}, { exception -> a.error(exception)});");
            verify(assertion, times(1)).received(1);
            verify(assertion, times(1)).received(2);
            verify(assertion, times(1)).received(3);
            verify(assertion, times(1)).received(4);
            verify(assertion, times(1)).received(5);
            verify(assertion, times(1)).received(6);
            verify(assertion, times(0)).received(7);
            verify(assertion, times(1)).error(any(NullPointerException.class));
        }

        @Test
        public void testMergeViaGroovy() {
            runGroovyScript("o.merge(o.toObservable(1, 2, 3), o.merge(o.toObservable(6), o.error(new NullPointerException()), o.toObservable(7)), o.toObservable(4, 5)).subscribe({ result -> a.received(result)}, { exception -> a.error(exception)});");
            // executing synchronously so we can deterministically know what order things will come
            verify(assertion, times(1)).received(1);
            verify(assertion, times(1)).received(2);
            verify(assertion, times(1)).received(3);
            verify(assertion, times(0)).received(4); // the NPE will cause this sequence to be skipped
            verify(assertion, times(0)).received(5); // the NPE will cause this sequence to be skipped
            verify(assertion, times(1)).received(6); // this comes before the NPE so should exist
            verify(assertion, times(0)).received(7);// this comes in the sequence after the NPE
            verify(assertion, times(1)).error(any(NullPointerException.class));
        }

        @Test
        public void testScriptWithMaterialize() {
            String script = "mockApiCall.getObservable().materialize().subscribe({ result -> a.received(result)});";
            runGroovyScript(script);
            // 2 times: once for hello_1 and once for onCompleted
            verify(assertion, times(2)).received(any(Notification.class));
        }

        @Test
        public void testScriptWithMerge() {
            String script = "o.merge(mockApiCall.getObservable(), mockApiCall.getObservable()).subscribe({ result -> a.received(result)});";
            runGroovyScript(script);
            verify(assertion, times(1)).received("hello_1");
            verify(assertion, times(1)).received("hello_2");
        }

        @Test
        public void testScriptWithOnNext() {
            String script = "mockApiCall.getObservable().subscribe({ result -> a.received(result)})";
            runGroovyScript(script);
            verify(assertion).received("hello_1");
        }

        @Test
        public void testSkipTakeViaGroovy() {
            runGroovyScript("o.skip(o.toObservable(1, 2, 3), 1).take(1).subscribe({ result -> a.received(result)});");
            verify(assertion, times(0)).received(1);
            verify(assertion, times(1)).received(2);
            verify(assertion, times(0)).received(3);
        }

        @Test
        public void testSkipViaGroovy() {
            runGroovyScript("o.skip(o.toObservable(1, 2, 3), 2).subscribe({ result -> a.received(result)});");
            verify(assertion, times(0)).received(1);
            verify(assertion, times(0)).received(2);
            verify(assertion, times(1)).received(3);
        }

        @Test
        public void testTakeViaGroovy() {
            runGroovyScript("o.take(o.toObservable(1, 2, 3), 2).subscribe({ result -> a.received(result)});");
            verify(assertion, times(1)).received(1);
            verify(assertion, times(1)).received(2);
            verify(assertion, times(0)).received(3);
        }

        @Test
        public void testToSortedList() {
            runGroovyScript("mockApiCall.getNumbers().toSortedList().subscribe({ result -> a.received(result)});");
            verify(assertion, times(1)).received(Arrays.asList(1, 2, 3, 4, 5));
        }

        @Test
        public void testToSortedListStatic() {
            runGroovyScript("o.toSortedList(o.toObservable(1, 3, 2, 5, 4)).subscribe({ result -> a.received(result)});");
            verify(assertion, times(1)).received(Arrays.asList(1, 2, 3, 4, 5));
        }

        @Test
        public void testToSortedListWithFunction() {
            runGroovyScript("mockApiCall.getNumbers().toSortedList({a, b -> a - b}).subscribe({ result -> a.received(result)});");
            verify(assertion, times(1)).received(Arrays.asList(1, 2, 3, 4, 5));
        }

        @Test
        public void testToSortedListWithFunctionStatic() {
            runGroovyScript("o.toSortedList(o.toObservable(1, 3, 2, 5, 4), {a, b -> a - b}).subscribe({ result -> a.received(result)});");
            verify(assertion, times(1)).received(Arrays.asList(1, 2, 3, 4, 5));
        }

        private void runGroovyScript(String script) {
            ClassLoader parent = getClass().getClassLoader();
            @SuppressWarnings("resource")
            GroovyClassLoader loader = new GroovyClassLoader(parent);

            Binding binding = new Binding();
            binding.setVariable("mockApiCall", new TestFactory());
            binding.setVariable("a", assertion);
            binding.setVariable("o", rx.Observable.class);

            /* parse the script and execute it */
            InvokerHelper.createScript(loader.parseClass(script), binding).run();
        }

        private static interface ScriptAssertion {
            public void error(Exception o);

            public void received(Object o);
        }

        private static class TestFactory {
            int counter = 1;

            @SuppressWarnings("unused")
            public Observable<Integer> getNumbers() {
                return Observable.toObservable(1, 3, 2, 5, 4);
            }

            @SuppressWarnings("unused")
            public TestObservable getObservable() {
                return new TestObservable(counter++);
            }
        }

        private static class TestObservable extends Observable<String> {
            private final int count;

            public TestObservable(int count) {
                super(new Func1<Observer<String>, Subscription>() {

                    @Override
                    public Subscription call(Observer<String> t1) {
                        // do nothing, override subscribe for test
                        return null;
                    }
                });
                this.count = count;
            }

            public Subscription subscribe(Observer<String> observer) {

                observer.onNext("hello_" + count);
                observer.onCompleted();

                return new Subscription() {

                    public void unsubscribe() {
                        // unregister ... will never be called here since we are executing synchronously
                    }

                };
            }
        }
    }

}
