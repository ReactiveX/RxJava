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
package rx.plugins;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

/**
 * Abstract ExecutionHook with invocations at different lifecycle points of {@link Observable} execution with a default no-op implementation.
 * <p>
 * See {@link RxJavaPlugins} or the RxJava GitHub Wiki for information on configuring plugins: <a
 * href="https://github.com/Netflix/RxJava/wiki/Plugins">https://github.com/Netflix/RxJava/wiki/Plugins</a>.
 * <p>
 * <b>Note on thread-safety and performance</b>
 * <p>
 * A single implementation of this class will be used globally so methods on this class will be invoked concurrently from multiple threads so all functionality must be thread-safe.
 * <p>
 * Methods are also invoked synchronously and will add to execution time of the observable so all behavior should be fast. If anything time-consuming is to be done it should be spawned asynchronously
 * onto separate worker threads.
 * 
 * */
public abstract class RxJavaObservableExecutionHook {

    /**
     * Invoked before {@link Observable#subscribe(rx.Observer)} is about to be executed.
     * <p>
     * This can be used to decorate or replace the <code>onSubscribe</code> function or just perform extra logging, metrics and other such things and pass-thru the function.
     * 
     * @param observableInstance
     *            The executing {@link Observable} instance.
     * @param onSubscribe
     *            original {@link Func1}<{@link Observer}{@code<T>}, {@link Subscription}> to be executed
     * @return {@link Func1}<{@link Observer}{@code<T>}, {@link Subscription}> function that can be modified, decorated, replaced or just returned as a pass-thru.
     */
    public <T> Func1<Observer<T>, Subscription> onSubscribeStart(Observable<T> observableInstance, Func1<Observer<T>, Subscription> onSubscribe) {
        // pass-thru by default
        return onSubscribe;
    }

    /**
     * Invoked after successful execution of {@link Observable#subscribe(rx.Observer)} with returned {@link Subscription}.
     * <p>
     * This can be used to decorate or replace the {@link Subscription} instance or just perform extra logging, metrics and other such things and pass-thru the subscription.
     * 
     * @param observableInstance
     *            The executing {@link Observable} instance.
     * @param subscription
     *            original {@link Subscription}
     * @return {@link Subscription} subscription that can be modified, decorated, replaced or just returned as a pass-thru.
     */
    public <T> Subscription onSubscribeReturn(Observable<T> observableInstance, Subscription subscription) {
        // pass-thru by default
        return subscription;
    }

    /**
     * Invoked after failed execution of {@link Observable#subscribe(Observer)} with thrown Exception.
     * <p>
     * This is NOT errors emitted via {@link Observer#onError(Exception)} but exceptions thrown when attempting
     * to subscribe to a {@link Func1}<{@link Observer}{@code<T>}, {@link Subscription}>.
     * 
     * @param observableInstance
     *            The executing {@link Observable} instance.
     * @param e
     *            Exception thrown by {@link Observable#subscribe(Observer)}
     * @return Exception that can be decorated, replaced or just returned as a pass-thru.
     */
    public <T> Exception onSubscribeError(Observable<T> observableInstance, Exception e) {
        // pass-thru by default
        return e;
    }

}
