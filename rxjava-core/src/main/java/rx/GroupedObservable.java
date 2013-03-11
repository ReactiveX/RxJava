package rx;

import rx.util.functions.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class GroupedObservable<K, T> extends Observable<T> {
    private final K key;
    private final Observable<T> delegate;

    public GroupedObservable(K key, Observable<T> delegate) {
        this.key = key;
        this.delegate = delegate;
    }

    public K getKey() {
        return key;
    }

    public Subscription subscribe(Observer<T> observer) {
        return delegate.subscribe(observer);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Subscription subscribe(Map<String, Object> callbacks) {
        return delegate.subscribe(callbacks);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Subscription subscribe(Object o) {
        return delegate.subscribe(o);
    }

    public Subscription subscribe(Action1<T> onNext) {
        return delegate.subscribe(onNext);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Subscription subscribe(Object onNext, Object onError) {
        return delegate.subscribe(onNext, onError);
    }

    public Subscription subscribe(Action1<T> onNext, Action1<Exception> onError) {
        return delegate.subscribe(onNext, onError);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Subscription subscribe(Object onNext, Object onError, Object onComplete) {
        return delegate.subscribe(onNext, onError, onComplete);
    }

    public Subscription subscribe(Action1<T> onNext, Action1<Exception> onError, Action0 onComplete) {
        return delegate.subscribe(onNext, onError, onComplete);
    }

    public void forEach(Action1<T> onNext) {
        delegate.forEach(onNext);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void forEach(Object o) {
        delegate.forEach(o);
    }

    @Override
    public T single() {
        return delegate.single();
    }

    public T single(Func1<T, Boolean> predicate) {
        return delegate.single(predicate);
    }

    @Override
    public T single(Object predicate) {
        return delegate.single(predicate);
    }

    public T singleOrDefault(T defaultValue) {
        return delegate.singleOrDefault(defaultValue);
    }

    public T singleOrDefault(T defaultValue, Func1<T, Boolean> predicate) {
        return delegate.singleOrDefault(defaultValue, predicate);
    }

    public T singleOrDefault(T defaultValue, Object predicate) {
        return delegate.singleOrDefault(defaultValue, predicate);
    }

    public static <T1 extends Object> Observable<T1> create(Func1<Observer<T1>, Subscription> func) {
        return Observable.create(func);
    }

    public static <T1 extends Object> Observable<T1> create(Object func) {
        return Observable.create(func);
    }

    public static <T1 extends Object> Observable<T1> empty() {
        return Observable.empty();
    }

    public static <T1 extends Object> Observable<T1> error(Exception exception) {
        return Observable.error(exception);
    }

    public static <T1 extends Object> Observable<T1> filter(Observable<T1> that, Func1<T1, Boolean> predicate) {
        return Observable.filter(that, predicate);
    }

    public static <T1 extends Object> Observable<T1> filter(Observable<T1> that, Object function) {
        return Observable.filter(that, function);
    }

    public static <T1 extends Object> Observable<T1> from(Iterable<T1> iterable) {
        return Observable.from(iterable);
    }

    public static <T1 extends Object> Observable<T1> from(T1... items) {
        return Observable.from(items);
    }

    public static Observable<Integer> range(int start, int count) {
        return Observable.range(start, count);
    }

    public static <T1 extends Object> Observable<T1> just(T1 value) {
        return Observable.just(value);
    }

    public static <T1 extends Object> Observable<T1> last(Observable<T1> that) {
        return Observable.last(that);
    }

    public static <T1 extends Object> T1 lastOrDefault(Observable<T1> source, T1 defaultValue) {
        return Observable.lastOrDefault(source, defaultValue);
    }

    public static <T1 extends Object> T1 lastOrDefault(Observable<T1> source, T1 defaultValue, Func1<T1, Boolean> predicate) {
        return Observable.lastOrDefault(source, defaultValue, predicate);
    }

    public static <T1 extends Object> T1 lastOrDefault(Observable<T1> source, T1 defaultValue, Object predicate) {
        return Observable.lastOrDefault(source, defaultValue, predicate);
    }

    public static <T1 extends Object, R> Observable<R> map(Observable<T1> sequence, Func1<T1, R> func) {
        return Observable.map(sequence, func);
    }

    public static <T1 extends Object, R> Observable<R> map(Observable<T1> sequence, Object func) {
        return Observable.map(sequence, func);
    }

    public static <T1 extends Object, R> Observable<R> mapMany(Observable<T1> sequence, Func1<T1, Observable<R>> func) {
        return Observable.mapMany(sequence, func);
    }

    public static <T1 extends Object, R> Observable<R> mapMany(Observable<T1> sequence, Object func) {
        return Observable.mapMany(sequence, func);
    }

    public static <T1 extends Object> Observable<Notification<T1>> materialize(Observable<T1> sequence) {
        return Observable.materialize(sequence);
    }

    public static <T1 extends Object> Observable<T1> merge(List<Observable<T1>> source) {
        return Observable.merge(source);
    }

    public static <T1 extends Object> Observable<T1> merge(Observable<Observable<T1>> source) {
        return Observable.merge(source);
    }

    public static <T1 extends Object> Observable<T1> merge(Observable<T1>... source) {
        return Observable.merge(source);
    }

    public static <T1 extends Object> Observable<T1> concat(Observable<T1>... source) {
        return Observable.concat(source);
    }

    public static <T1 extends Object> Observable<T1> mergeDelayError(List<Observable<T1>> source) {
        return Observable.mergeDelayError(source);
    }

    public static <T1 extends Object> Observable<T1> mergeDelayError(Observable<Observable<T1>> source) {
        return Observable.mergeDelayError(source);
    }

    public static <T1 extends Object> Observable<T1> mergeDelayError(Observable<T1>... source) {
        return Observable.mergeDelayError(source);
    }

    public static <T1 extends Object> Observable<T1> never() {
        return Observable.never();
    }

    public static Subscription noOpSubscription() {
        return Observable.noOpSubscription();
    }

    public static Subscription createSubscription(Action0 unsubscribe) {
        return Observable.createSubscription(unsubscribe);
    }

    public static Subscription createSubscription(Object unsubscribe) {
        return Observable.createSubscription(unsubscribe);
    }

    public static <T1 extends Object> Observable<T1> onErrorResumeNext(Observable<T1> that, Func1<Exception, Observable<T1>> resumeFunction) {
        return Observable.onErrorResumeNext(that, resumeFunction);
    }

    public static <T1 extends Object> Observable<T1> onErrorResumeNext(Observable<T1> that, Object resumeFunction) {
        return Observable.onErrorResumeNext(that, resumeFunction);
    }

    public static <T1 extends Object> Observable<T1> onErrorResumeNext(Observable<T1> that, Observable<T1> resumeSequence) {
        return Observable.onErrorResumeNext(that, resumeSequence);
    }

    public static <T1 extends Object> Observable<T1> onErrorReturn(Observable<T1> that, Func1<Exception, T1> resumeFunction) {
        return Observable.onErrorReturn(that, resumeFunction);
    }

    public static <T1 extends Object> Observable<T1> reduce(Observable<T1> sequence, Func2<T1, T1, T1> accumulator) {
        return Observable.reduce(sequence, accumulator);
    }

    public static <T1 extends Object> Observable<T1> reduce(Observable<T1> sequence, Object accumulator) {
        return Observable.reduce(sequence, accumulator);
    }

    public static <T1 extends Object> Observable<T1> reduce(Observable<T1> sequence, T1 initialValue, Func2<T1, T1, T1> accumulator) {
        return Observable.reduce(sequence, initialValue, accumulator);
    }

    public static <T1 extends Object> Observable<T1> reduce(Observable<T1> sequence, T1 initialValue, Object accumulator) {
        return Observable.reduce(sequence, initialValue, accumulator);
    }

    public static <T1 extends Object> Observable<T1> scan(Observable<T1> sequence, Func2<T1, T1, T1> accumulator) {
        return Observable.scan(sequence, accumulator);
    }

    public static <T1 extends Object> Observable<T1> scan(Observable<T1> sequence, Object accumulator) {
        return Observable.scan(sequence, accumulator);
    }

    public static <T1 extends Object> Observable<T1> scan(Observable<T1> sequence, T1 initialValue, Func2<T1, T1, T1> accumulator) {
        return Observable.scan(sequence, initialValue, accumulator);
    }

    public static <T1 extends Object> Observable<T1> scan(Observable<T1> sequence, T1 initialValue, Object accumulator) {
        return Observable.scan(sequence, initialValue, accumulator);
    }

    public static <T1 extends Object> Observable<T1> skip(Observable<T1> items, int num) {
        return Observable.skip(items, num);
    }

    public static <T1 extends Object> Observable<T1> synchronize(Observable<T1> observable) {
        return Observable.synchronize(observable);
    }

    public static <T1 extends Object> Observable<T1> take(Observable<T1> items, int num) {
        return Observable.take(items, num);
    }

    public static <T1 extends Object> Observable<T1> takeLast(Observable<T1> items, int count) {
        return Observable.takeLast(items, count);
    }

    public static <T1 extends Object> Observable<T1> takeWhile(Observable<T1> items, Func1<T1, Boolean> predicate) {
        return Observable.takeWhile(items, predicate);
    }

    public static <T1 extends Object> Observable<T1> takeWhile(Observable<T1> items, Object predicate) {
        return Observable.takeWhile(items, predicate);
    }

    public static <T1 extends Object> Observable<T1> takeWhileWithIndex(Observable<T1> items, Func2<T1, Integer, Boolean> predicate) {
        return Observable.takeWhileWithIndex(items, predicate);
    }

    public static <T1 extends Object> Observable<T1> takeWhileWithIndex(Observable<T1> items, Object predicate) {
        return Observable.takeWhileWithIndex(items, predicate);
    }

    public static <T1 extends Object> Observable<List<T1>> toList(Observable<T1> that) {
        return Observable.toList(that);
    }

    public static <T1 extends Object> Iterable<T1> toIterable(Observable<T1> that) {
        return Observable.toIterable(that);
    }

    public static <T1 extends Object> Iterable<T1> next(Observable<T1> items) {
        return Observable.next(items);
    }

    public static <T1 extends Object> T1 single(Observable<T1> that) {
        return Observable.single(that);
    }

    public static <T1 extends Object> T1 single(Observable<T1> that, Func1<T1, Boolean> predicate) {
        return Observable.single(that, predicate);
    }

    public static <T1 extends Object> T1 single(Observable<T1> that, Object predicate) {
        return Observable.single(that, predicate);
    }

    public static <T1 extends Object> T1 singleOrDefault(Observable<T1> that, T1 defaultValue) {
        return Observable.singleOrDefault(that, defaultValue);
    }

    public static <T1 extends Object> T1 singleOrDefault(Observable<T1> that, T1 defaultValue, Func1<T1, Boolean> predicate) {
        return Observable.singleOrDefault(that, defaultValue, predicate);
    }

    public static <T1 extends Object> T1 singleOrDefault(Observable<T1> that, T1 defaultValue, Object predicate) {
        return Observable.singleOrDefault(that, defaultValue, predicate);
    }

    public static <T1 extends Object> Observable<T1> toObservable(Iterable<T1> iterable) {
        return Observable.toObservable(iterable);
    }

    public static <T1 extends Object> Observable<T1> toObservable(Future<T1> future) {
        return Observable.toObservable(future);
    }

    public static <T1 extends Object> Observable<T1> toObservable(Future<T1> future, long time, TimeUnit unit) {
        return Observable.toObservable(future, time, unit);
    }

    public static <T1 extends Object> Observable<T1> toObservable(T1... items) {
        return Observable.toObservable(items);
    }

    public static <T1 extends Object> Observable<List<T1>> toSortedList(Observable<T1> sequence) {
        return Observable.toSortedList(sequence);
    }

    public static <T1 extends Object> Observable<List<T1>> toSortedList(Observable<T1> sequence, Func2<T1, T1, Integer> sortFunction) {
        return Observable.toSortedList(sequence, sortFunction);
    }

    public static <T1 extends Object> Observable<List<T1>> toSortedList(Observable<T1> sequence, Object sortFunction) {
        return Observable.toSortedList(sequence, sortFunction);
    }

    public static <R, T0, T1> Observable<R> zip(Observable<T0> w0, Observable<T1> w1, Func2<T0, T1, R> reduceFunction) {
        return Observable.zip(w0, w1, reduceFunction);
    }

    public static <T1 extends Object> Observable<Boolean> sequenceEqual(Observable<T1> first, Observable<T1> second) {
        return Observable.sequenceEqual(first, second);
    }

    public static <R, T0, T1> Observable<R> zip(Observable<T0> w0, Observable<T1> w1, Object function) {
        return Observable.zip(w0, w1, function);
    }

    public static <R, T0, T1, T2> Observable<R> zip(Observable<T0> w0, Observable<T1> w1, Observable<T2> w2, Func3<T0, T1, T2, R> function) {
        return Observable.zip(w0, w1, w2, function);
    }

    public static <R, T0, T1, T2> Observable<R> zip(Observable<T0> w0, Observable<T1> w1, Observable<T2> w2, Object function) {
        return Observable.zip(w0, w1, w2, function);
    }

    public static <R, T0, T1, T2, T3> Observable<R> zip(Observable<T0> w0, Observable<T1> w1, Observable<T2> w2, Observable<T3> w3, Func4<T0, T1, T2, T3, R> reduceFunction) {
        return Observable.zip(w0, w1, w2, w3, reduceFunction);
    }

    public static <R, T0, T1, T2, T3> Observable<R> zip(Observable<T0> w0, Observable<T1> w1, Observable<T2> w2, Observable<T3> w3, Object function) {
        return Observable.zip(w0, w1, w2, w3, function);
    }

    public Observable<T> filter(Func1<T, Boolean> predicate) {
        return delegate.filter(predicate);
    }

    @Override
    public Observable<T> filter(Object callback) {
        return delegate.filter(callback);
    }

    @Override
    public Observable<T> last() {
        return delegate.last();
    }

    public T lastOrDefault(T defaultValue) {
        return delegate.lastOrDefault(defaultValue);
    }

    public T lastOrDefault(T defaultValue, Func1<T, Boolean> predicate) {
        return delegate.lastOrDefault(defaultValue, predicate);
    }

    public T lastOrDefault(T defaultValue, Object predicate) {
        return delegate.lastOrDefault(defaultValue, predicate);
    }

    public <R> Observable<R> map(Func1<T, R> func) {
        return delegate.map(func);
    }

    @Override
    public <R> Observable<R> map(Object callback) {
        return delegate.map(callback);
    }

    public <R> Observable<R> mapMany(Func1<T, Observable<R>> func) {
        return delegate.mapMany(func);
    }

    @Override
    public <R> Observable<R> mapMany(Object callback) {
        return delegate.mapMany(callback);
    }

    @Override
    public Observable<Notification<T>> materialize() {
        return delegate.materialize();
    }

    public Observable<T> onErrorResumeNext(Func1<Exception, Observable<T>> resumeFunction) {
        return delegate.onErrorResumeNext(resumeFunction);
    }

    @Override
    public Observable<T> onErrorResumeNext(Object resumeFunction) {
        return delegate.onErrorResumeNext(resumeFunction);
    }

    public Observable<T> onErrorResumeNext(Observable<T> resumeSequence) {
        return delegate.onErrorResumeNext(resumeSequence);
    }

    public Observable<T> onErrorReturn(Func1<Exception, T> resumeFunction) {
        return delegate.onErrorReturn(resumeFunction);
    }

    @Override
    public Observable<T> onErrorReturn(Object resumeFunction) {
        return delegate.onErrorReturn(resumeFunction);
    }

    public Observable<T> reduce(Func2<T, T, T> accumulator) {
        return delegate.reduce(accumulator);
    }

    @Override
    public Observable<T> reduce(Object accumulator) {
        return delegate.reduce(accumulator);
    }

    public Observable<T> reduce(T initialValue, Func2<T, T, T> accumulator) {
        return delegate.reduce(initialValue, accumulator);
    }

    public Observable<T> reduce(T initialValue, Object accumulator) {
        return delegate.reduce(initialValue, accumulator);
    }

    public Observable<T> scan(Func2<T, T, T> accumulator) {
        return delegate.scan(accumulator);
    }

    @Override
    public Observable<T> scan(Object accumulator) {
        return delegate.scan(accumulator);
    }

    public Observable<T> scan(T initialValue, Func2<T, T, T> accumulator) {
        return delegate.scan(initialValue, accumulator);
    }

    public Observable<T> scan(T initialValue, Object accumulator) {
        return delegate.scan(initialValue, accumulator);
    }

    @Override
    public Observable<T> skip(int num) {
        return delegate.skip(num);
    }

    @Override
    public Observable<T> take(int num) {
        return delegate.take(num);
    }

    public Observable<T> takeWhile(Func1<T, Boolean> predicate) {
        return delegate.takeWhile(predicate);
    }

    @Override
    public Observable<T> takeWhile(Object predicate) {
        return delegate.takeWhile(predicate);
    }

    public Observable<T> takeWhileWithIndex(Func2<T, Integer, Boolean> predicate) {
        return delegate.takeWhileWithIndex(predicate);
    }

    @Override
    public Observable<T> takeWhileWithIndex(Object predicate) {
        return delegate.takeWhileWithIndex(predicate);
    }

    @Override
    public Observable<T> takeLast(int count) {
        return delegate.takeLast(count);
    }

    @Override
    public Observable<List<T>> toList() {
        return delegate.toList();
    }

    @Override
    public Observable<List<T>> toSortedList() {
        return delegate.toSortedList();
    }

    public Observable<List<T>> toSortedList(Func2<T, T, Integer> sortFunction) {
        return delegate.toSortedList(sortFunction);
    }

    @Override
    public Observable<List<T>> toSortedList(Object sortFunction) {
        return delegate.toSortedList(sortFunction);
    }

    @Override
    public Iterable<T> toIterable() {
        return delegate.toIterable();
    }

    @Override
    public Iterable<T> next() {
        return delegate.next();
    }
}
