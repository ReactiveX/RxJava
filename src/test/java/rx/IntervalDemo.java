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
package rx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import rx.functions.Action0;
import rx.functions.Action1;

@Ignore
// since this doesn't do any automatic testing
public class IntervalDemo {

    @Test
    public void demoInterval() throws Exception {
        testLongObservable(Observable.interval(500, TimeUnit.MILLISECONDS).take(4), "demoInterval");
    }

    public void testLongObservable(Observable<Long> o, final String testname) throws Exception {
        final List<Long> l = new ArrayList<Long>();
        Action1<Long> onNext = new Action1<Long>() {
            @Override
            public void call(Long i) {
                l.add(i);
                System.out.println(testname + " got " + i);
            }
        };
        Action1<Throwable> onError = new Action1<Throwable>() {
            @Override
            public void call(Throwable t) {
                t.printStackTrace();
            }
        };
        Action0 onComplete = new Action0() {
            @Override
            public void call() {
                System.out.println(testname + " complete");
            }
        };
        o.subscribe(onNext, onError, onComplete);

        // need to wait, otherwise JUnit kills the thread of interval()
        Thread.sleep(2500);
    }

}
