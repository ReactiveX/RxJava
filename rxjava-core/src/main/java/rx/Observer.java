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
 * After an Observer calls a Observable's <code>Observable.subscribe</code> method, the Observable calls the Observer's <code>onNext</code> method to provide notifications. A well-behaved Observable
 * will
 * call a Observer's <code>onCompleted</code> closure exactly once or the Observer's <code>onError</code> closure exactly once.
 * <p>
 * For more informationon, see: <a
 * href="https://confluence.corp.netflix.com/display/API/Observers%2C+Observables%2C+and+the+Reactive+Pattern#Observers%2CObservables%2CandtheReactivePattern-SettingupObserversinGroovy">API.Next
 * Programmer's Guide: Observers, Observables, and the Reactive Pattern: Setting up Observers in Groovy</a>
 * 
 * @param <T>
 */
public interface Observer<T> {

    /**
     * Notifies the Observer that the Observable has finished sending push-based notifications.
     * <p>
     * The Observable will not call this closure if it calls <code>onError</code>.
     */
    public void onCompleted();

    /**
     * Notifies the Observer that the Observable has experienced an error condition.
     * <p>
     * If the Observable calls this closure, it will not thereafter call <code>onNext</code> or <code>onCompleted</code>.
     * 
     * @param e
     */
    public void onError(Exception e);

    /**
     * Provides the Observer with new data.
     * <p>
     * The Observable calls this closure 1 or more times, unless it calls <code>onError</code> in which case this closure may never be called.
     * <p>
     * The Observable will not call this closure again after it calls either <code>onCompleted</code> or <code>onError</code>, though this does not guarantee that chronologically-speaking, this
     * closure
     * will not be called after one of those closures is called (because the Observable may assign the calling of these closures to chronologically-independent threads). See <a href=
     * "https://confluence.corp.netflix.com/display/API/Observers%2C+Observables%2C+and+the+Reactive+Pattern#Observers%2CObservables%2CandtheReactivePattern-%7B%7Bwx.synchronize%28%26%238239%3B%29%7D%7D"
     * ><code>wx.synchronize()</code></a> for information on how to enforce chronologically-ordered behavior.
     * 
     * @param args
     */
    public void onNext(T args);
}
