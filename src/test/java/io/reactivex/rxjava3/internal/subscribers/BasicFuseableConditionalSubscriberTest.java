/*
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava3.internal.subscribers;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.operators.ConditionalSubscriber;
import io.reactivex.rxjava3.operators.QueueFuseable;
import io.reactivex.rxjava3.testsupport.*;

public class BasicFuseableConditionalSubscriberTest extends RxJavaTest {

    @Test
    public void offerThrows() {
        ConditionalSubscriber<Integer> cs = new ConditionalSubscriber<Integer>() {

            @Override
            public void onSubscribe(Subscription s) {
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }

            @Override
            public boolean tryOnNext(Integer t) {
                return false;
            }
        };

        BasicFuseableConditionalSubscriber<Integer, Integer> fcs = new BasicFuseableConditionalSubscriber<Integer, Integer>(cs) {

            @Override
            public boolean tryOnNext(Integer t) {
                return false;
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public int requestFusion(int mode) {
                return 0;
            }

            @Nullable
            @Override
            public Integer poll() throws Exception {
                return null;
            }
        };

        fcs.onSubscribe(new ScalarSubscription<>(fcs, 1));

        TestHelper.assertNoOffer(fcs);

        assertFalse(fcs.isEmpty());
        fcs.clear();
        assertTrue(fcs.isEmpty());
    }

    @Test
    public void implementationStopsOnSubscribe() {
        @SuppressWarnings("unchecked")
        ConditionalSubscriber<Integer> ts = mock(ConditionalSubscriber.class);

        BasicFuseableConditionalSubscriber<Integer, Integer> bfs = new BasicFuseableConditionalSubscriber<Integer, Integer>(ts) {

            @Override
            protected boolean beforeDownstream() {
                return false;
            }

            @Override
            public void onNext(@NonNull Integer t) {
                ts.onNext(t);
            }

            @Override
            public int requestFusion(int mode) {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public boolean tryOnNext(@NonNull Integer t) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public @Nullable Integer poll() throws Throwable {
                return null;
            }
        };

        bfs.onSubscribe(new BooleanSubscription());

        verify(ts, never()).onSubscribe(any());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(f -> f
                .map(v -> v)
                .filter(v -> true)
            );
    }

    @Test
    public void transitiveBoundaryFusionNone() {
        @SuppressWarnings("unchecked")
        ConditionalSubscriber<Integer> ts = mock(ConditionalSubscriber.class);

        BasicFuseableConditionalSubscriber<Integer, Integer> bfs = new BasicFuseableConditionalSubscriber<Integer, Integer>(ts) {

            @Override
            protected boolean beforeDownstream() {
                return false;
            }

            @Override
            public void onNext(@NonNull Integer t) {
                ts.onNext(t);
            }

            @Override
            public int requestFusion(int mode) {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public boolean tryOnNext(@NonNull Integer t) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public @Nullable Integer poll() throws Throwable {
                return null;
            }
        };

        bfs.onSubscribe(new BooleanSubscription());

        assertEquals(QueueFuseable.NONE, bfs.transitiveBoundaryFusion(QueueFuseable.ANY));
    }

    @Test
    public void transitiveBoundaryFusionAsync() {
        @SuppressWarnings("unchecked")
        ConditionalSubscriber<Integer> ts = mock(ConditionalSubscriber.class);

        BasicFuseableConditionalSubscriber<Integer, Integer> bfs = new BasicFuseableConditionalSubscriber<Integer, Integer>(ts) {

            @Override
            protected boolean beforeDownstream() {
                return false;
            }

            @Override
            public void onNext(@NonNull Integer t) {
                ts.onNext(t);
            }

            @Override
            public int requestFusion(int mode) {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public boolean tryOnNext(@NonNull Integer t) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public @Nullable Integer poll() throws Throwable {
                return null;
            }
        };

        bfs.onSubscribe(EmptySubscription.INSTANCE);

        assertEquals(QueueFuseable.ASYNC, bfs.transitiveBoundaryFusion(QueueFuseable.ANY));
    }

    @Test
    public void transitiveBoundaryFusionAsyncBoundary() {
        @SuppressWarnings("unchecked")
        ConditionalSubscriber<Integer> ts = mock(ConditionalSubscriber.class);

        BasicFuseableConditionalSubscriber<Integer, Integer> bfs = new BasicFuseableConditionalSubscriber<Integer, Integer>(ts) {

            @Override
            protected boolean beforeDownstream() {
                return false;
            }

            @Override
            public void onNext(@NonNull Integer t) {
                ts.onNext(t);
            }

            @Override
            public int requestFusion(int mode) {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public boolean tryOnNext(@NonNull Integer t) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public @Nullable Integer poll() throws Throwable {
                return null;
            }
        };

        bfs.onSubscribe(EmptySubscription.INSTANCE);

        assertEquals(QueueFuseable.NONE, bfs.transitiveBoundaryFusion(QueueFuseable.ANY | QueueFuseable.BOUNDARY));
    }
}
