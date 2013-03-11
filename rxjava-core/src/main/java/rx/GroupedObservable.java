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

import rx.util.functions.*;

import java.util.List;
import java.util.Map;

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
