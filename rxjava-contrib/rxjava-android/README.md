# rxjava-android

Android specific bindings for Rx.

This module adds a number of classes to RxJava that make writing reactive components in 
Android applications easy and hassle free. More specifically, it

- provides a `Scheduler` that schedules an `Observable` on a given Android `Handler` thread, particularly the main UI thread
- provides base `Observer` implementations that make guarantees w.r.t. to reliable and thread-safe use throughout 
      `Fragment` and `Activity` life-cycle callbacks (coming soon)
- provides reusable, self-contained reactive components for common Android use cases and UI concerns (coming soon)


# Binaries

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22rxjava-android%22).

Example for [Maven](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22rxjava-android%22):

```xml
<dependency>
    <groupId>com.netflix.rxjava</groupId>
    <artifactId>rxjava-android</artifactId>
    <version>0.10.1</version>
</dependency>
```

and for Ivy:

```xml
<dependency org="com.netflix.rxjava" name="rxjava-android" rev="0.10.1" />
```

# Sample usage

We are working on a samples project which provides runnable code samples that demonstrate common Rx patterns and
their use in Android applications.

## Observing on the UI thread

One of the most common operations when dealing with asynchronous tasks on Android is to observe the task's
result or outcome on the main UI thread. Using vanilla Android, this would
typically be accomplished with an `AsyncTask`. With RxJava instead you would declare your `Observable`
to be observed on the main thread:

    public class ReactiveFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Observable.from("one", "two", "three", "four", "five")
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(/* an Observer */);
    }
 
This will execute the Observable on a new thread, and emit results through `onNext` on the main UI thread.
   
## Observing on arbitrary threads
The previous sample is merely a specialization of a more general concept, namely binding asynchronous
communication to an Android message loop using the `Handler` class. In order to observe an `Observable`
on an arbitrary thread, create a `Handler` bound to that thread and use the `AndroidSchedulers.handlerThread`
scheduler:

    new Thread(new Runnable() {
        @Override
        public void run() {
            final Handler handler = new Handler(); // bound to this thread
            Observable.from("one", "two", "three", "four", "five")
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.handlerThread(handler))
                    .subscribe(/* an Observer */)
                    
            // perform work, ...
        }
    }, "custom-thread-1").start();

This will execute the Observable on a new thread and emit results through `onNext` on "custom-thread-1".
(This example is contrived since you could as well call `observeOn(Schedulers.currentThread())` but it
shall suffice to illustrate the idea.)
