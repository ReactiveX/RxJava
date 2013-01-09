package org.rx.reactive;

/**
 * Provides a mechanism for receiving push-based notifications.
 * <p>
 * After an Watcher calls a Watchable's <code>IObservable.subscribe</code> method, the Watchable calls the Watcher's <code>onNext</code> method to provide notifications. A well-behaved Watchable will call a Watcher's <code>onCompleted</code> closure exactly once or the Watcher's <code>onError</code> closure exactly once.
 * <p>
 * For more informationon, see: <a href="https://confluence.corp.netflix.com/display/API/Watchers%2C+Watchables%2C+and+the+Reactive+Pattern#Watchers%2CWatchables%2CandtheReactivePattern-SettingupwatchersinGroovy">API.Next Programmer's Guide: Watchers, Watchables, and the Reactive Pattern: Setting up Watchers in Groovy</a>
 * 
 * @param <T>
 */
public interface IObserver<T> {

    /**
     * Notifies the Watcher that the Watchable has finished sending push-based notifications.
     * <p>
     * The Watchable will not call this closure if it calls <code>onError</code>.
     */
    public void onCompleted();

    /**
     * Notifies the Watcher that the Watchable has experienced an error condition.
     * <p>
     * If the Watchable calls this closure, it will not thereafter call <code>onNext</code> or <code>onCompleted</code>.
     * 
     * @param e
     */
    public void onError(Exception e);

    /**
     * Provides the Watcher with new data.
     * <p>
     * The Watchable calls this closure 1 or more times, unless it calls <code>onError</code> in which case this closure may never be called.
     * <p>
     * The Watchable will not call this closure again after it calls either <code>onCompleted</code> or <code>onError</code>, though this does not guarantee that chronologically-speaking, this closure will not be called after one of those closures is called (because the Watchable may assign the calling of these closures to chronologically-independent threads). See <a href="https://confluence.corp.netflix.com/display/API/Watchers%2C+Watchables%2C+and+the+Reactive+Pattern#Watchers%2CWatchables%2CandtheReactivePattern-%7B%7Bwx.synchronize%28%26%238239%3B%29%7D%7D"><code>wx.synchronize()</code></a> for information on how to enforce chronologically-ordered behavior.
     * 
     * @param args
     */
    public void onNext(T args);
}
