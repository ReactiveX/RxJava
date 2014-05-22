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
package rx.usecases;

import org.openjdk.jmh.annotations.GenerateMicroBenchmark;

import rx.Observable;
import rx.functions.Func1;

public class PerfTransforms {

    @GenerateMicroBenchmark
    public void mapTransformation(UseCaseInput input) throws InterruptedException {
        input.observable.map(new Func1<Integer, String>() {

            @Override
            public String call(Integer i) {
                return String.valueOf(i);
            }

        }).map(new Func1<String, Integer>() {

            @Override
            public Integer call(String i) {
                return Integer.parseInt(i);
            }

        }).subscribe(input.observer);
        input.awaitCompletion();
    }

    @GenerateMicroBenchmark
    public void flatMapTransformsUsingFrom(UseCaseInput input) throws InterruptedException {
        input.observable.flatMap(new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer i) {
                return Observable.from(i);
            }

        }).subscribe(input.observer);
        input.awaitCompletion();
    }

    @GenerateMicroBenchmark
    public void flatMapTransformsUsingJust(UseCaseInput input) throws InterruptedException {
        input.observable.flatMap(new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer i) {
                return Observable.just(i);
            }

        }).subscribe(input.observer);
        input.awaitCompletion();
    }

}
