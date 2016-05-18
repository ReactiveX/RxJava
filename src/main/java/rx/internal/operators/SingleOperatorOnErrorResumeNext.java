/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.internal.operators;

import rx.Single;
import rx.SingleSubscriber;
import rx.exceptions.Exceptions;
import rx.functions.Func1;

public class SingleOperatorOnErrorResumeNext<T> implements Single.OnSubscribe<T> {

    private final Single<? extends T> originalSingle;
    private final Func1<Throwable, ? extends Single<? extends T>> resumeFunctionInCaseOfError;

    private SingleOperatorOnErrorResumeNext(Single<? extends T> originalSingle, Func1<Throwable, ? extends Single<? extends T>> resumeFunctionInCaseOfError) {
        if (originalSingle == null) {
            throw new NullPointerException("originalSingle must not be null");
        }

        if (resumeFunctionInCaseOfError == null) {
            throw new NullPointerException("resumeFunctionInCaseOfError must not be null");
        }

        this.originalSingle = originalSingle;
        this.resumeFunctionInCaseOfError = resumeFunctionInCaseOfError;
    }

    public static <T> SingleOperatorOnErrorResumeNext<T> withFunction(Single<? extends T> originalSingle, Func1<Throwable, ? extends Single<? extends T>> resumeFunctionInCaseOfError) {
        return new SingleOperatorOnErrorResumeNext<T>(originalSingle, resumeFunctionInCaseOfError);
    }

    public static <T> SingleOperatorOnErrorResumeNext<T> withOther(Single<? extends T> originalSingle, final Single<? extends T> resumeSingleInCaseOfError) {
        if (resumeSingleInCaseOfError == null) {
            throw new NullPointerException("resumeSingleInCaseOfError must not be null");
        }

        return new SingleOperatorOnErrorResumeNext<T>(originalSingle, new Func1<Throwable, Single<? extends T>>() {
            @Override
            public Single<? extends T> call(Throwable throwable) {
                return resumeSingleInCaseOfError;
            }
        });
    }

    @Override
    public void call(final SingleSubscriber<? super T> child) {
        final SingleSubscriber<? super T> parent = new SingleSubscriber<T>() {
            @Override
            public void onSuccess(T value) {
                child.onSuccess(value);
            }

            @Override
            public void onError(Throwable error) {
                try {
                    resumeFunctionInCaseOfError.call(error).subscribe(child);
                } catch (Throwable innerError) {
                    Exceptions.throwOrReport(innerError, child);
                }
            }
        };

        child.add(parent);
        originalSingle.subscribe(parent);
    }
}
