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
package rx;

/**
 * Provides a mechanism for receiving push-based notifications.
 * <p>
 * After an Observer calls {@link IObservable#subscribe(Observer)}, the
 * {@link IObservable} calls the Observer's <code>onNext</code> method to
 * provide notifications. A well-behaved {@link IObservable} will
 * call {@link Observer#onCompleted()} exactly once or
 * {@link Observer#onError(Throwable)} exactly once.
 * <p>
 * For more information see the <a href="https://github.com/Netflix/RxJava/wiki/Observable">RxJava Wiki</a>
 * 
 * @param <T>
 */
public interface Observer<T> {

    /**
     * Notifies the Observer that the {@link IObservable} has finished sending
     * push-based notifications.
     * <p>
     * The {@link IObservable} will not call this method if it calls
     * {@link #onError(Throwable)}.
     */
    public void onCompleted();

    /**
     * Notifies the Observer that the {@link IObservable} has experienced an
     * error condition.
     * <p>
     * If the {@link IObservable} calls this method, it will not thereafter
     * call {@link #onNext(Object)} or {@link #onCompleted()}.
     * 
     * @param e
     */
    public void onError(Throwable e);

    /**
     * Provides the Observer with new data.
     * <p>
     * The {@link IObservable} calls this method 1 or more times, unless it
     * calls {@link #onError(Throwable)}, in which case this method may never
     * be called.
     * <p>
     * The {@link IObservable} will not call this method again after it calls
     * either {@link #onCompleted()} or {@link #onError(Throwable)}.
     * 
     * @param args
     */
    public void onNext(T args);
}
