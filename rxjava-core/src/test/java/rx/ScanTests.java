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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import rx.EventStream.Event;
import rx.functions.Action1;
import rx.functions.Func2;

public class ScanTests {

    @Test
    public void testUnsubscribeScan() {

        EventStream.getEventStream("HTTP-ClusterB", 20)
                .scan(new HashMap<String, String>(), new Func2<Map<String, String>, Event, Map<String, String>>() {

                    @Override
                    public Map<String, String> call(Map<String, String> accum, Event perInstanceEvent) {
                        accum.put("instance", perInstanceEvent.instanceId);
                        return accum;
                    }

                })
                .take(10)
                .toBlocking().forEach(new Action1<Map<String, String>>() {

                    @Override
                    public void call(Map<String, String> v) {
                        System.out.println(v);
                    }

                });
    }
}
