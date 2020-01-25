/**
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

package io.reactivex.rxjava3.internal.operators.maybe;

import static org.mockito.Mockito.*;
import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.subjects.MaybeSubject;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class MaybeDematerializeTest extends RxJavaTest {

    @Test
    public void success() {
        Maybe.just(Notification.createOnNext(1))
        .dematerialize(Functions.<Notification<Integer>>identity())
        .test()
        .assertResult(1);
    }

    @Test
    public void empty() {
        Maybe.just(Notification.<Integer>createOnComplete())
        .dematerialize(Functions.<Notification<Integer>>identity())
        .test()
        .assertResult();
    }

    @Test
    public void emptySource() throws Throwable {
        @SuppressWarnings("unchecked")
        Function<Notification<Integer>, Notification<Integer>> function = mock(Function.class);

        Maybe.<Notification<Integer>>empty()
        .dematerialize(function)
        .test()
        .assertResult();

        verify(function, never()).apply(any());
    }

    @Test
    public void error() {
        Maybe.<Notification<Integer>>error(new TestException())
        .dematerialize(Functions.<Notification<Integer>>identity())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorNotification() {
        Maybe.just(Notification.<Integer>createOnError(new TestException()))
        .dematerialize(Functions.<Notification<Integer>>identity())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybe(new Function<Maybe<Object>, MaybeSource<Object>>() {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public MaybeSource<Object> apply(Maybe<Object> v) throws Exception {
                return v.dematerialize((Function)Functions.identity());
            }
        });
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(MaybeSubject.<Notification<Integer>>create().dematerialize(Functions.<Notification<Integer>>identity()));
    }

    @Test
    public void selectorCrash() {
        Maybe.just(Notification.createOnNext(1))
        .dematerialize(new Function<Notification<Integer>, Notification<Integer>>() {
            @Override
            public Notification<Integer> apply(Notification<Integer> v) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void selectorNull() {
        Maybe.just(Notification.createOnNext(1))
        .dematerialize(Functions.justFunction((Notification<Integer>)null))
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void selectorDifferentType() {
        Maybe.just(Notification.createOnNext(1))
        .dematerialize(new Function<Notification<Integer>, Notification<String>>() {
            @Override
            public Notification<String> apply(Notification<Integer> v) throws Exception {
                return Notification.createOnNext("Value-" + 1);
            }
        })
        .test()
        .assertResult("Value-1");
    }
}
