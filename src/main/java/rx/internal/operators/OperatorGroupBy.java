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
package rx.internal.operators;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Observer;
import rx.Producer;
import rx.Subscriber;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.observables.GroupedObservable;
import rx.subscriptions.Subscriptions;

/**
 * Groups the items emitted by an Observable according to a specified criterion, and emits these
 * grouped items as Observables, one Observable per group.
 * <p>
 * <img width="640" height="360" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/groupBy.png" alt="">
 *
 * @param <K> the key type
 * @param <T> the source and group value type
 */
public final class OperatorGroupBy<T, K, R> implements Operator<GroupedObservable<K, R>, T> {

	final Func1<? super T, ? extends K> keySelector;
	final Func1<? super T, ? extends R> elementSelector;

	@SuppressWarnings("unchecked")
	public OperatorGroupBy(final Func1<? super T, ? extends K> keySelector) {
		this(keySelector, (Func1<T, R>)IDENTITY);
	}

	public OperatorGroupBy(Func1<? super T, ? extends K> keySelector, Func1<? super T, ? extends R> elementSelector) {
		this.keySelector = keySelector;
		this.elementSelector = elementSelector;
	}

	@Override
	public Subscriber<? super T> call(final Subscriber<? super GroupedObservable<K, R>> child) {
		return new GroupBySubscriber<K, T, R>(keySelector, elementSelector, child);
	}
	static final class GroupBySubscriber<K, T, R> extends Subscriber<T> {
		private static final int MAX_QUEUE_SIZE = 1024;
		final Func1<? super T, ? extends K> keySelector;
		final Func1<? super T, ? extends R> elementSelector;
		final Subscriber<? super GroupedObservable<K, R>> child;

		public GroupBySubscriber(Func1<? super T, ? extends K> keySelector, Func1<? super T, ? extends R> elementSelector, Subscriber<? super GroupedObservable<K, R>> child) {
			// a new CompositeSubscription to decouple the subscription as the inner subscriptions need a separate lifecycle
			// and will unsubscribe on this parent if they are all unsubscribed
			super();
			this.keySelector = keySelector;
			this.elementSelector = elementSelector;
			this.child = child;
		}
		private final ConcurrentHashMap<K, BufferUntilSubscriber<T>> groups = new ConcurrentHashMap<K, BufferUntilSubscriber<T>>();
		private final ConcurrentHashMap<K, AtomicLong> requestedPerGroup = new ConcurrentHashMap<K, AtomicLong>();
		private final ConcurrentHashMap<K, AtomicLong> countPerGroup = new ConcurrentHashMap<K, AtomicLong>();
		private final ConcurrentHashMap<K, Queue<Object>> buffer = new ConcurrentHashMap<K, Queue<Object>>(MAX_QUEUE_SIZE);
		
		private static final NotificationLite<Object> nl = NotificationLite.instance();

		volatile int completionCounter;
		volatile int completionEmitted;
		volatile int terminated;

		@SuppressWarnings("rawtypes")
		static final AtomicIntegerFieldUpdater<GroupBySubscriber> COMPLETION_COUNTER_UPDATER
		= AtomicIntegerFieldUpdater.newUpdater(GroupBySubscriber.class, "completionCounter");
		@SuppressWarnings("rawtypes")
		static final AtomicIntegerFieldUpdater<GroupBySubscriber> COMPLETION_EMITTED_UPDATER
		= AtomicIntegerFieldUpdater.newUpdater(GroupBySubscriber.class, "completionEmitted");
		@SuppressWarnings("rawtypes")
		static final AtomicIntegerFieldUpdater<GroupBySubscriber> TERMINATED_UPDATER
		= AtomicIntegerFieldUpdater.newUpdater(GroupBySubscriber.class, "terminated");

		volatile long requested;
		@SuppressWarnings("rawtypes")
		static final AtomicLongFieldUpdater<GroupBySubscriber> REQUESTED = AtomicLongFieldUpdater.newUpdater(GroupBySubscriber.class, "requested");

		volatile long bufferedCount;
		@SuppressWarnings("rawtypes")
		static final AtomicLongFieldUpdater<GroupBySubscriber> BUFFERED_COUNT = AtomicLongFieldUpdater.newUpdater(GroupBySubscriber.class, "bufferedCount");


		@Override
		public void onStart() {
			REQUESTED.set(this, MAX_QUEUE_SIZE);
			request(MAX_QUEUE_SIZE);
		}

		@Override
		public void onCompleted() {
			if (TERMINATED_UPDATER.compareAndSet(this, 0, 1)) {
				// if we receive onCompleted from our parent we onComplete children
				// for each group check if it is ready to accept more events if so pass the oncomplete through else buffer it.
				for(K key : groups.keySet()) {
					emitItem(key, nl.completed());
				}
				// special case for empty (no groups emitted)
				if (completionCounter == 0) {
					// we must track 'completionEmitted' seperately from 'completed' since `completeInner` can result in childObserver.onCompleted() being emitted
					if (COMPLETION_EMITTED_UPDATER.compareAndSet(this, 0, 1)) {
						child.onCompleted();
					}
				}
			}
		}

		@Override
		public void onError(Throwable e) {
			if (TERMINATED_UPDATER.compareAndSet(this, 0, 1)) {
				// we immediately tear everything down if we receive an error
				child.onError(e);
			}
		}

		// The grouped observable propagates the 'producer.request' call from it's subscriber to this method
		// Here we keep track of the requested count for each group
		// If we already have items queued when a request comes in we vend those and decrement the outstanding request count


		void requestFromGroupedObservable(long n, K key) {
			requestedPerGroup.get(key).getAndAdd(n);
			if (countPerGroup.get(key).getAndIncrement() == 0) {
				pollQueue(key);
			}
		}

		@Override
		public void onNext(T t) {
			try {
				final K key = keySelector.call(t);
				BufferUntilSubscriber<T> group = groups.get(key);
				if (group == null) {
					// this group doesn't exist
					if (child.isUnsubscribed()) {
						// we have been unsubscribed on the outer so won't send any  more groups
						return;
					}
					group = BufferUntilSubscriber.create();
					final BufferUntilSubscriber<T> _group = group;

					GroupedObservable<K, R> go = new GroupedObservable<K, R>(key, new OnSubscribe<R>() {

						@Override
						public void call(final Subscriber<? super R> o) {
							// number of children we have running
							COMPLETION_COUNTER_UPDATER.incrementAndGet(GroupBySubscriber.this);
							o.add(Subscriptions.create(new Action0() {

								@Override
								public void call() {
									completeInner();
								}

							}));

							o.setProducer(new Producer() {

								@Override
								public void request(long n) {

									requestFromGroupedObservable(n, key);
								}

							});
							_group.unsafeSubscribe(new Subscriber<T>(o) {

								@Override
								public void onCompleted() {

									o.onCompleted();
									completeInner();
								}

								@Override
								public void onError(Throwable e) {
									o.onError(e);
								}

								@Override
								public void onNext(T t) {
									o.onNext(elementSelector.call(t));

								}
							});
						}
					});

					groups.put(key, group);
					requestedPerGroup.put(key, new AtomicLong());
					countPerGroup.put(key, new AtomicLong());
					child.onNext(go);
				}

				emitItem(key, nl.next(t));
			} catch (Throwable e) {
				onError(OnErrorThrowable.addValueAsLastCause(e, t));
			}
		}

		private void emitItem(K key, Object item) {
			Queue<Object> q = buffer.get(key);
			// short circuit buffering
			if(requestedPerGroup.get(key).get() > 0 && (q == null || q.isEmpty()) ) {

				BufferUntilSubscriber<T> group = groups.get(key);
				nl.accept((Observer)group, item);

				requestedPerGroup.get(key).decrementAndGet();
				REQUESTED.decrementAndGet(this);
			} else { 
				if(q == null) {
					q = new ConcurrentLinkedQueue<Object>();
					buffer.putIfAbsent(key, q);

				} 

				q.add(item);
				BUFFERED_COUNT.incrementAndGet(this);
				REQUESTED.decrementAndGet(this);

				if (countPerGroup.get(key).getAndIncrement() == 0) {
					pollQueue(key);
				}
			}
			requestMoreIfNecessary();
		}


		private void pollQueue(K key) {
			do  {
				drainIfPossible(key);
				long c = countPerGroup.get(key).decrementAndGet();
				if (c > 1) {

					/*
					 * Set down to 1 and then iterate again.
					 * we lower it to 1 otherwise it could have grown very large while in the last poll loop
					 * and then we can end up looping all those times again here before existing even once we've drained
					 */
					countPerGroup.get(key).set( 1);
					// we now loop again, and if anything tries scheduling again after this it will increment and cause us to loop again after
				} 
			} while(countPerGroup.get(key).get() > 0);
		}

		private void requestMoreIfNecessary() {
			if(REQUESTED.get(this) == 0) {
				long toRequest = MAX_QUEUE_SIZE - BUFFERED_COUNT.get(this);
				REQUESTED.set(this,toRequest);
				request(toRequest);
			}
		}

		private void drainIfPossible(K key) {
			if(buffer.containsKey(key)) {
				Queue<Object> bufferedItems = buffer.get(key);
				BufferUntilSubscriber<T> group = groups.get(key);
				while(requestedPerGroup.get(key).get() > 0) {
					
					Object t = bufferedItems.poll();
					if(t != null) {

						nl.accept((Observer)group, t);
						requestedPerGroup.get(key).decrementAndGet();
						BUFFERED_COUNT.decrementAndGet(this);
						REQUESTED.decrementAndGet(this);

						// if we have used up all the events we requested from upstream then figure out what to ask for this time based on the empty space in the buffer
						requestMoreIfNecessary();
					} else {
						// queue is empty break
						break;
					}
				}
			} 
		}
	
		private void completeInner() {
			// count can be < 0 because unsubscribe also calls this
			if (COMPLETION_COUNTER_UPDATER.decrementAndGet(this) <= 0 && (terminated == 1 || child.isUnsubscribed())) {

				// completionEmitted ensures we only emit onCompleted once
				if (COMPLETION_EMITTED_UPDATER.compareAndSet(this, 0, 1)) {

					if (child.isUnsubscribed()) {
						// if the entire groupBy has been unsubscribed and children are completed we will propagate the unsubscribe up.
						unsubscribe();
					}
					child.onCompleted();
				}
			}
		}
	}

	private final static Func1<Object, Object> IDENTITY = new Func1<Object, Object>() {
		@Override
		public Object call(Object t) {
			return t;
		}
	};


}
