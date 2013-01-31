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
package rx.lang.jruby;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.jruby.Ruby;
import org.jruby.RubyProc;
import org.jruby.embed.ScriptingContainer;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;
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

public class JRubyAdaptor implements FunctionLanguageAdaptor {

    @Override
    public Object call(Object function, Object[] args) {
        RubyProc rubyProc = ((RubyProc) function);
        Ruby ruby = rubyProc.getRuntime();
        IRubyObject rubyArgs[] = new IRubyObject[args.length];
        for (int i = 0; i < args.length; i++) {
            rubyArgs[i] = JavaEmbedUtils.javaToRuby(ruby, args[i]);
        }
        return rubyProc.getBlock().call(ruby.getCurrentContext(), rubyArgs);
    }

    @Override
    public Class<?>[] getFunctionClass() {
        return new Class<?>[] { RubyProc.class };
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
            runGroovyScript("Observable.create(lambda{|it| it.onNext('hello');it.onCompleted();}).subscribe(lambda{|result| a.received(result)});");
            verify(assertion, times(1)).received("hello");
        }

        @Test
        public void testFilterViaGroovy() {
            runGroovyScript("Observable.filter(Observable.toObservable(1, 2, 3), lambda{|it| it >= 2}).subscribe(lambda{|result| a.received(result)});");
            verify(assertion, times(0)).received(1L);
            verify(assertion, times(1)).received(2L);
            verify(assertion, times(1)).received(3L);
        }

        @Test
        public void testLast() {
            String script = "mockApiCall.getObservable().last().subscribe(lambda{|result| a.received(result)})";
            runGroovyScript(script);
            verify(assertion, times(1)).received("hello_1");
        }

        @Test
        public void testMap() {
            String script = "mockApiCall.getObservable().map(lambda{|v| 'say' + v}).subscribe(lambda{|result| a.received(result)});";
            runGroovyScript(script);
            verify(assertion, times(1)).received("sayhello_1");
        }

        @Test
        public void testMaterializeViaGroovy() {
            runGroovyScript("Observable.materialize(Observable.toObservable(1, 2, 3)).subscribe(lambda{|result| a.received(result)});");
            // we expect 4 onNext calls: 3 for 1, 2, 3 ObservableNotification.OnNext and 1 for ObservableNotification.OnCompleted
            verify(assertion, times(4)).received(any(Notification.class));
            verify(assertion, times(0)).error(any(Exception.class));
        }

        @Test
        public void testScriptWithMaterialize() {
            String script = "mockApiCall.getObservable().materialize().subscribe(lambda{|result| a.received(result)});";
            runGroovyScript(script);
            // 2 times: once for hello_1 and once for onCompleted
            verify(assertion, times(2)).received(any(Notification.class));
        }

        @Test
        public void testScriptWithMerge() {
            String script = "Observable.merge(mockApiCall.getObservable(), mockApiCall.getObservable()).subscribe(lambda{|result| a.received(result)});";
            runGroovyScript(script);
            verify(assertion, times(1)).received("hello_1");
            verify(assertion, times(1)).received("hello_2");
        }

        @Test
        public void testScriptWithOnNext() {
            String script = "mockApiCall.getObservable().subscribe(lambda{|result| a.received(result)})";
            runGroovyScript(script);
            verify(assertion).received("hello_1");
        }

        @Test
        public void testSkipTakeViaGroovy() {
            runGroovyScript("Observable.skip(Observable.toObservable(1, 2, 3), 1).take(1).subscribe(lambda{|result| a.received(result)});");
            verify(assertion, times(0)).received(1);
            verify(assertion, times(1)).received(2L);
            verify(assertion, times(0)).received(3);
        }

        @Test
        public void testSkipViaGroovy() {
            runGroovyScript("Observable.skip(Observable.toObservable(1, 2, 3), 2).subscribe(lambda{|result| a.received(result)});");
            verify(assertion, times(0)).received(1);
            verify(assertion, times(0)).received(2);
            verify(assertion, times(1)).received(3L);
        }

        @Test
        public void testTakeViaGroovy() {
            runGroovyScript("Observable.take(Observable.toObservable(1, 2, 3), 2).subscribe(lambda{|result| a.received(result)});");
            verify(assertion, times(1)).received(1L);
            verify(assertion, times(1)).received(2L);
            verify(assertion, times(0)).received(3);
        }

        @Test
        public void testToSortedList() {
            runGroovyScript("mockApiCall.getNumbers().toSortedList().subscribe(lambda{|result| a.received(result)});");
            verify(assertion, times(1)).received(Arrays.asList(1, 2, 3, 4, 5));
        }

        private void runGroovyScript(String script) {
            ScriptingContainer container = new ScriptingContainer();
            container.put("mockApiCall", new TestFactory());
            container.put("a", assertion);

            StringBuilder b = new StringBuilder();
            // force JRuby to always use subscribe(Object)
            b.append("import \"rx.Observable\"").append("\n");
            b.append("class Observable").append("\n");
            b.append("  java_alias :subscribe, :subscribe, [java.lang.Object]").append("\n");
            b.append("end").append("\n");
            b.append(script);

            container.runScriptlet(b.toString());
        }

        private static interface ScriptAssertion {
            public void error(Exception o);

            public void received(Object o);
        }

        public static class TestFactory {
            int counter = 1;

            public Observable<Integer> getNumbers() {
                return Observable.toObservable(1, 3, 2, 5, 4);
            }

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
