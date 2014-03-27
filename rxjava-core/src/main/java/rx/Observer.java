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
package rx;

/**
 * Provides a mechanism for receiving push-based notifications.
 * <p>
 * After an Observer calls an {@link Observable}'s <code>Observable.subscribe</code> method, the
 * {@link Observable} calls the Observer's <code>onNext</code> method to provide notifications. A well-behaved
 * {@link Observable} will call an Observer's <code>onCompleted</code> closure exactly once or the Observer's
 * <code>onError</code> closure exactly once.
 * <p>
 * For more information see the <a href="https://github.com/Netflix/RxJava/wiki/Observable">RxJava Wiki</a>
 * 
 * @param <T>
 */
public interface Observer<T> {

    /**
     * Notifies the Observer that the {@link Observable} has finished sending push-based notifications.
     * <p>
     * The {@link Observable} will not call this closure if it calls <code>onError</code>.
     */
    public abstract void onCompleted();

    /**
     * Notifies the Observer that the {@link Observable} has experienced an error condition.
     * <p>
     * If the {@link Observable} calls this closure, it will not thereafter call <code>onNext</code> or
     * <code>onCompleted</code>.
     * 
     * @param e
     */
    public abstract void onError(Throwable e);

    /**
     * Provides the Observer with new data.
     * <p>
     * The {@link Observable} calls this closure 1 or more times, unless it calls <code>onError</code> in which
     * case this closure may never be called.
     * <p>
     * The {@link Observable} will not call this closure again after it calls either <code>onCompleted</code> or
     * <code>onError</code>.
     * 
     * @param t
     */
    public abstract void onNext(T t);

}
