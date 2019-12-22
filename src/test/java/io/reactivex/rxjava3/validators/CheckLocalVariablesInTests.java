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

package io.reactivex.rxjava3.validators;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import org.junit.Test;

import io.reactivex.rxjava3.testsupport.TestHelper;

/**
 * Checks for commonly copy-pasted but not-renamed local variables in unit tests.
 * <ul>
 * <li>{@code TestSubscriber} named as {@code to*}</li>
 * <li>{@code TestObserver} named as {@code ts*}</li>
 * <li>{@code PublishProcessor} named as {@code ps*}</li>
 * <li>{@code PublishSubject} named as {@code pp*}</li>
 * <li>{@code Subscription} with single letter name such as "s" or "d"</li>
 * <li>{@code Disposable} with single letter name such as "s" or "d"</li>
 * <li>{@code Flowable} named as {@code o|observable} + number</li>
 * <li>{@code Observable} named as {@code f|flowable} + number</li>
 * <li>{@code Subscriber} named as "o" or "observer"</li>
 * <li>{@code Observer} named as "s" or "subscriber"</li>
 * </ul>
 */
public class CheckLocalVariablesInTests {

    static void findPattern(String pattern) throws Exception {
        findPattern(pattern, false);
    }

    static void findPattern(String pattern, boolean checkMain) throws Exception {
        File f = TestHelper.findSource("Flowable");
        if (f == null) {
            System.out.println("Unable to find sources of RxJava");
            return;
        }

        Queue<File> dirs = new ArrayDeque<>();

        StringBuilder fail = new StringBuilder();
        fail.append("The following code pattern was found: ").append(pattern).append("\n");

        File parent = f.getParentFile().getParentFile();

        if (checkMain) {
            dirs.offer(new File(parent.getAbsolutePath().replace('\\', '/')));
        }
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
    public void subscriberAsTo() throws Exception {
        findPattern("TestSubscriber(Ex)?<.*>\\s+to");
    }

    @Test
    public void observerAsTs() throws Exception {
        findPattern("TestObserver(Ex)?<.*>\\s+ts");
    }

    @Test
    public void subscriberNoArgAsTo() throws Exception {
        findPattern("TestSubscriber(Ex)?\\s+to");
    }

    @Test
    public void observerNoArgAsTs() throws Exception {
        findPattern("TestObserver(Ex)?\\s+ts");
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

    @Test
    public void subscriberAsObserver() throws Exception {
        findPattern("Subscriber<.*>\\s+observer[0-9]?\\b");
    }

    @Test
    public void subscriberAsO() throws Exception {
        findPattern("Subscriber<.*>\\s+o[0-9]?\\b");
    }

    @Test
    public void singleAsObservable() throws Exception {
        findPattern("Single<.*>\\s+observable\\b");
    }

    @Test
    public void singleAsFlowable() throws Exception {
        findPattern("Single<.*>\\s+flowable\\b");
    }

    @Test
    public void observerAsSubscriber() throws Exception {
        findPattern("Observer<.*>\\s+subscriber[0-9]?\\b");
    }

    @Test
    public void observerAsS() throws Exception {
        findPattern("Observer<.*>\\s+s[0-9]?\\b");
    }

    @Test
    public void observerNoArgAsSubscriber() throws Exception {
        findPattern("Observer\\s+subscriber[0-9]?\\b");
    }

    @Test
    public void observerNoArgAsS() throws Exception {
        findPattern("Observer\\s+s[0-9]?\\b");
    }

    @Test
    public void flowableAsObservable() throws Exception {
        findPattern("Flowable<.*>\\s+observable[0-9]?\\b");
    }

    @Test
    public void flowableAsO() throws Exception {
        findPattern("Flowable<.*>\\s+o[0-9]?\\b");
    }

    @Test
    public void flowableNoArgAsO() throws Exception {
        findPattern("Flowable\\s+o[0-9]?\\b");
    }

    @Test
    public void flowableNoArgAsObservable() throws Exception {
        findPattern("Flowable\\s+observable[0-9]?\\b");
    }

    @Test
    public void processorAsSubject() throws Exception {
        findPattern("Processor<.*>\\s+subject(0-9)?\\b");
    }

    @Test
    public void maybeAsObservable() throws Exception {
        findPattern("Maybe<.*>\\s+observable\\b");
    }

    @Test
    public void maybeAsFlowable() throws Exception {
        findPattern("Maybe<.*>\\s+flowable\\b");
    }

    @Test
    public void completableAsObservable() throws Exception {
        findPattern("Completable\\s+observable\\b");
    }

    @Test
    public void completableAsFlowable() throws Exception {
        findPattern("Completable\\s+flowable\\b");
    }

    @Test
    public void subscriptionAsFieldS() throws Exception {
        findPattern("Subscription\\s+s[0-9]?;", true);
    }

    @Test
    public void subscriptionAsD() throws Exception {
        findPattern("Subscription\\s+d[0-9]?", true);
    }

    @Test
    public void subscriptionAsSubscription() throws Exception {
        findPattern("Subscription\\s+subscription[0-9]?;", true);
    }

    @Test
    public void subscriptionAsDParenthesis() throws Exception {
        findPattern("Subscription\\s+d[0-9]?\\)", true);
    }

    @Test
    public void queueSubscriptionAsD() throws Exception {
        findPattern("Subscription<.*>\\s+q?d[0-9]?\\b", true);
    }

    @Test
    public void booleanSubscriptionAsbd() throws Exception {
        findPattern("BooleanSubscription\\s+bd[0-9]?;", true);
    }

    @Test
    public void atomicSubscriptionAsS() throws Exception {
        findPattern("AtomicReference<Subscription>\\s+s[0-9]?;", true);
    }

    @Test
    public void atomicSubscriptionAsSInit() throws Exception {
        findPattern("AtomicReference<Subscription>\\s+s[0-9]?\\s", true);
    }

    @Test
    public void atomicSubscriptionAsSubscription() throws Exception {
        findPattern("AtomicReference<Subscription>\\s+subscription[0-9]?", true);
    }

    @Test
    public void atomicSubscriptionAsD() throws Exception {
        findPattern("AtomicReference<Subscription>\\s+d[0-9]?", true);
    }

    @Test
    public void disposableAsS() throws Exception {
        // the space before makes sure it doesn't match onSubscribe(Subscription) unnecessarily
        findPattern("Disposable\\s+s[0-9]?\\b", true);
    }

    @Test
    public void disposableAsFieldD() throws Exception {
        findPattern("Disposable\\s+d[0-9]?;", true);
    }

    @Test
    public void atomicDisposableAsS() throws Exception {
        findPattern("AtomicReference<Disposable>\\s+s[0-9]?", true);
    }

    @Test
    public void atomicDisposableAsD() throws Exception {
        findPattern("AtomicReference<Disposable>\\s+d[0-9]?;", true);
    }

    @Test
    public void subscriberAsFieldActual() throws Exception {
        findPattern("Subscriber<.*>\\s+actual[;\\)]", true);
    }

    @Test
    public void subscriberNoArgAsFieldActual() throws Exception {
        findPattern("Subscriber\\s+actual[;\\)]", true);
    }

    @Test
    public void subscriberAsFieldS() throws Exception {
        findPattern("Subscriber<.*>\\s+s[0-9]?;", true);
    }

    @Test
    public void observerAsFieldActual() throws Exception {
        findPattern("Observer<.*>\\s+actual[;\\)]", true);
    }

    @Test
    public void observerAsFieldSO() throws Exception {
        findPattern("Observer<.*>\\s+[so][0-9]?;", true);
    }

    @Test
    public void observerNoArgAsFieldActual() throws Exception {
        findPattern("Observer\\s+actual[;\\)]", true);
    }

    @Test
    public void observerNoArgAsFieldCs() throws Exception {
        findPattern("Observer\\s+cs[;\\)]", true);
    }

    @Test
    public void observerNoArgAsFieldSO() throws Exception {
        findPattern("Observer\\s+[so][0-9]?;", true);
    }

    @Test
    public void queueDisposableAsD() throws Exception {
        findPattern("Disposable<.*>\\s+q?s[0-9]?\\b", true);
    }

    @Test
    public void disposableAsDParenthesis() throws Exception {
        findPattern("Disposable\\s+s[0-9]?\\)", true);
    }

    @Test
    public void compositeDisposableAsCs() throws Exception {
        findPattern("CompositeDisposable\\s+cs[0-9]?", true);
    }

}
