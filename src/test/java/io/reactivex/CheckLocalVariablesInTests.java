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

package io.reactivex;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * Checks for commonly copy-pasted but not-renamed local variables in unit tests.
 * <ul>
 * <li>{@code TestSubscriber} named as {@code to*}</li>
 * <li>{@code TestObserver} named as {@code ts*}</li>
 * <li>{@code PublishProcessor} named as {@code ps*}</li>
 * <li>{@code PublishSubject} named as {@code pp*}</li>
 * </ul>
 */
public class CheckLocalVariablesInTests {

    static void findPattern(String pattern) throws Exception {
        File f = MaybeNo2Dot0Since.findSource("Flowable");
        if (f == null) {
            System.out.println("Unable to find sources of RxJava");
            return;
        }

        Queue<File> dirs = new ArrayDeque<File>();

        StringBuilder fail = new StringBuilder();
        fail.append("The following code pattern was found: ").append(pattern).append("\n");

        File parent = f.getParentFile();

        dirs.offer(new File(parent.getAbsolutePath().replace('\\', '/').replace("src/main/java", "src/test/java")));

        Pattern p = Pattern.compile(pattern);

        int total = 0;

        while (!dirs.isEmpty()) {
            f = dirs.poll();

            File[] list = f.listFiles();
            if (list != null && list.length != 0) {

                for (File u : list) {
                    if (u.isDirectory()) {
                        dirs.offer(u);
                    } else {
                        String fname = u.getName();
                        if (fname.endsWith(".java")) {

                            int lineNum = 0;
                            BufferedReader in = new BufferedReader(new FileReader(u));
                            try {
                                for (;;) {
                                    String line = in.readLine();
                                    if (line != null) {
                                        lineNum++;

                                        line = line.trim();

                                        if (!line.startsWith("//") && !line.startsWith("*")) {
                                            if (p.matcher(line).find()) {
                                                fail
                                                .append(fname)
                                                .append("#L").append(lineNum)
                                                .append("    ").append(line)
                                                .append("\n");
                                                total++;
                                            }
                                        }
                                    } else {
                                        break;
                                    }
                                }
                            } finally {
                                in.close();
                            }
                        }
                    }
                }
            }
        }
        if (total != 0) {
            fail.append("Found ")
            .append(total)
            .append(" instances");
            System.out.println(fail);
            throw new AssertionError(fail.toString());
        }
    }

    @Test
    public void testSubscriberAsTo() throws Exception {
        findPattern("TestSubscriber<.*>\\s+to");
    }

    @Test
    public void testObserverAsTs() throws Exception {
        findPattern("TestObserver<.*>\\s+ts");
    }

    @Test
    public void publishSubjectAsPp() throws Exception {
        findPattern("PublishSubject<.*>\\s+pp");
    }

    @Test
    public void publishProcessorAsPs() throws Exception {
        findPattern("PublishProcessor<.*>\\s+ps");
    }

    @Test
    public void behaviorProcessorAsBs() throws Exception {
        findPattern("BehaviorProcessor<.*>\\s+bs");
    }

    @Test
    public void behaviorSubjectAsBp() throws Exception {
        findPattern("BehaviorSubject<.*>\\s+bp");
    }

    @Test
    public void connectableFlowableAsCo() throws Exception {
        findPattern("ConnectableFlowable<.*>\\s+co(0-9|\\b)");
    }

    @Test
    public void connectableObservableAsCf() throws Exception {
        findPattern("ConnectableObservable<.*>\\s+cf(0-9|\\b)");
    }

    @Test
    public void queueDisposableInsteadOfQueueFuseable() throws Exception {
        findPattern("QueueDisposable\\.(NONE|SYNC|ASYNC|ANY|BOUNDARY)");
    }

    @Test
    public void queueSubscriptionInsteadOfQueueFuseable() throws Exception {
        findPattern("QueueSubscription\\.(NONE|SYNC|ASYNC|ANY|BOUNDARY)");
    }

    @Test
    public void singleSourceAsMs() throws Exception {
        findPattern("SingleSource<.*>\\s+ms");
    }

    @Test
    public void singleSourceAsCs() throws Exception {
        findPattern("SingleSource<.*>\\s+cs");
    }

    @Test
    public void maybeSourceAsSs() throws Exception {
        findPattern("MaybeSource<.*>\\s+ss");
    }

    @Test
    public void maybeSourceAsCs() throws Exception {
        findPattern("MaybeSource<.*>\\s+cs");
    }

    @Test
    public void completableSourceAsSs() throws Exception {
        findPattern("CompletableSource<.*>\\s+ss");
    }

    @Test
    public void completableSourceAsMs() throws Exception {
        findPattern("CompletableSource<.*>\\s+ms");
    }

    @Test
    public void observableAsC() throws Exception {
        findPattern("Observable<.*>\\s+c\\b");
    }
}
