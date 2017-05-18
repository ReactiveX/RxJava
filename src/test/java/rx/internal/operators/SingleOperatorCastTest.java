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

import org.junit.Test;
import rx.Observer;
import rx.Single;

import static org.mockito.Mockito.*;

public class SingleOperatorCastTest {

    @Test
    public void testSingleCast() {
        Single<?> source = Single.just(1);
        Single<Integer> single = source.cast(Integer.class);

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        single.subscribe(observer);
        verify(observer, times(1)).onNext(1);
        verify(observer, never()).onError(
            org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSingleCastWithWrongType() {
        Single<?> source = Single.just(1);
        Single<Boolean> single = source.cast(Boolean.class);

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        single.subscribe(observer);
        verify(observer, times(1)).onError(
            org.mockito.Matchers.any(ClassCastException.class));
    }
}
