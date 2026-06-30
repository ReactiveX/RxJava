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

package io.reactivex.rxjava4.validators;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.functions.Predicate;
import io.reactivex.rxjava4.testsupport.TestHelper;

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
public class CheckLocalVariablesInTest extends RxJavaTest {

    static void findPattern(String pattern) throws Throwable {
        findPattern(pattern, false, _ -> true);
    }

    static void findPattern(String pattern, boolean checkMain) throws Throwable {
        findPattern(pattern, checkMain, _ -> true);
    }

    static void findPattern(String pattern, Predicate<? super String> fileFilter) throws Throwable {
        findPattern(pattern, false, fileFilter);
    }

    static void findPattern(String pattern, boolean checkMain, Predicate<? super String> fileFilter) throws Throwable {
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
            if (list != null) {

                for (File u : list) {
                    if (u.isDirectory()) {
                        dirs.offer(u);
                    } else {
                        String fname = u.getName();
                        if (fname.endsWith(".java") && fileFilter.test(u.getAbsolutePath())) {

                            int lineNum = 0;
                            try (BufferedReader in = new BufferedReader(new FileReader(u))) {
                                for (; ; ) {
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
                                                        .append("\n")
                                                        .append(" at ")
                                                        .append(fname.replace(".java", ""))
                                                        .append(".method(")
                                                        .append(fname)
                                                        .append(":")
                                                        .append(lineNum)
                                                        .append(")\n");

                                                total++;
                                            }
                                        }
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (total != 0) {
            fail.insert(0, "Found " + total + " instances");
            System.out.println(fail);
            throw new AssertionError(fail.toString());
        }
    }

    @Test
    public void subscriberAsTo() throws Throwable {
        findPattern("TestSubscriber(Ex)?<.*>\\s+to");
    }

    @Test
    public void observerAsTs() throws Throwable {
        findPattern("TestObserver(Ex)?<.*>\\s+ts");
    }

    @Test
    public void subscriberNoArgAsTo() throws Throwable {
        findPattern("TestSubscriber(Ex)?\\s+to");
    }

    @Test
    public void observerNoArgAsTs() throws Throwable {
        findPattern("TestObserver(Ex)?\\s+ts");
    }

    @Test
    public void publishSubjectAsPp() throws Throwable {
        findPattern("PublishSubject<.*>\\s+pp");
    }

    @Test
    public void publishProcessorAsPs() throws Throwable {
        findPattern("PublishProcessor<.*>\\s+ps");
    }

    @Test
    public void unicastSubjectAsUp() throws Throwable {
        findPattern("UnicastSubject<.*>\\s+up");
    }

    @Test
    public void unicastProcessorAsUs() throws Throwable {
        findPattern("UnicastProcessor<.*>\\s+us");
    }

    @Test
    public void behaviorProcessorAsBs() throws Throwable {
        findPattern("BehaviorProcessor<.*>\\s+bs");
    }

    @Test
    public void behaviorSubjectAsBp() throws Throwable {
        findPattern("BehaviorSubject<.*>\\s+bp");
    }

    @Test
    public void connectableFlowableAsCo() throws Throwable {
        findPattern("ConnectableFlowable<.*>\\s+co(0-9|\\b)");
    }

    @Test
    public void connectableObservableAsCf() throws Throwable {
        findPattern("ConnectableObservable<.*>\\s+cf(0-9|\\b)");
    }

    @Test
    public void queueDisposableInsteadOfQueueFuseable() throws Throwable {
        findPattern("QueueDisposable\\.(NONE|SYNC|ASYNC|ANY|BOUNDARY)");
    }

    @Test
    public void queueSubscriptionInsteadOfQueueFuseable() throws Throwable {
        findPattern("QueueSubscription\\.(NONE|SYNC|ASYNC|ANY|BOUNDARY)");
    }

    @Test
    public void singleSourceAsMs() throws Throwable {
        findPattern("SingleSource<.*>\\s+ms");
    }

    @Test
    public void singleSubjectAsMs() throws Throwable {
        findPattern("SingleSubject<.*>\\s+ms");
    }

    @Test
    public void singleSourceAsCs() throws Throwable {
        findPattern("SingleSource<.*>\\s+cs");
    }

    @Test
    public void maybeSourceAsSs() throws Throwable {
        findPattern("MaybeSource<.*>\\s+ss");
    }

    @Test
    public void maybeSubjectAsSs() throws Throwable {
        findPattern("MaybeSubject<.*>\\s+ss");
    }

    @Test
    public void maybeSourceAsCs() throws Throwable {
        findPattern("MaybeSource<.*>\\s+cs");
    }

    @Test
    public void completableSourceAsSs() throws Throwable {
        findPattern("CompletableSource<.*>\\s+ss");
    }

    @Test
    public void completableSourceAsMs() throws Throwable {
        findPattern("CompletableSource<.*>\\s+ms");
    }

    @Test
    public void completableSubjectAsSs() throws Throwable {
        findPattern("CompletableSubject<.*>\\s+ss");
    }

    @Test
    public void completableSubjectAsMs() throws Throwable {
        findPattern("CompletableSubject<.*>\\s+ms");
    }

    @Test
    public void observableAsC() throws Throwable {
        findPattern("Observable<.*>\\s+c\\b");
    }

    @Test
    public void subscriberAsObserver() throws Throwable {
        findPattern("Subscriber<.*>\\s+observer[0-9]?\\b");
    }

    @Test
    public void subscriberAsO() throws Throwable {
        findPattern("Subscriber<.*>\\s+o[0-9]?\\b");
    }

    @Test
    public void singleAsObservable() throws Throwable {
        findPattern("Single<.*>\\s+observable\\b");
    }

    @Test
    public void singleAsFlowable() throws Throwable {
        findPattern("Single<.*>\\s+flowable\\b");
    }

    @Test
    public void observerAsSubscriber() throws Throwable {
        findPattern("Observer<.*>\\s+subscriber[0-9]?\\b");
    }

    @Test
    public void observerAsS() throws Throwable {
        findPattern("Observer<.*>\\s+s[0-9]?\\b");
    }

    @Test
    public void observerNoArgAsSubscriber() throws Throwable {
        findPattern("Observer\\s+subscriber[0-9]?\\b");
    }

    @Test
    public void observerNoArgAsS() throws Throwable {
        findPattern("Observer\\s+s[0-9]?\\b");
    }

    @Test
    public void flowableAsObservable() throws Throwable {
        findPattern("Flowable<.*>\\s+observable[0-9]?\\b");
    }

    @Test
    public void flowableAsO() throws Throwable {
        findPattern("Flowable<.*>\\s+o[0-9]?\\b");
    }

    @Test
    public void flowableNoArgAsO() throws Throwable {
        findPattern("Flowable\\s+o[0-9]?\\b");
    }

    @Test
    public void flowableNoArgAsObservable() throws Throwable {
        findPattern("Flowable\\s+observable[0-9]?\\b");
    }

    @Test
    public void processorAsSubject() throws Throwable {
        findPattern("Processor<.*>\\s+subject(0-9)?\\b");
    }

    @Test
    public void maybeAsObservable() throws Throwable {
        findPattern("Maybe<.*>\\s+observable\\b");
    }

    @Test
    public void maybeAsFlowable() throws Throwable {
        findPattern("Maybe<.*>\\s+flowable\\b");
    }

    @Test
    public void completableAsObservable() throws Throwable {
        findPattern("Completable\\s+observable\\b");
    }

    @Test
    public void completableAsFlowable() throws Throwable {
        findPattern("Completable\\s+flowable\\b");
    }

    @Test
    public void subscriptionAsFieldS() throws Throwable {
        findPattern("Subscription\\s+s[0-9]?;", true);
    }

    @Test
    public void subscriptionAsD() throws Throwable {
        findPattern("Subscription\\s+d[0-9]?", true);
    }

    @Test
    public void subscriptionAsSubscription() throws Throwable {
        findPattern("Subscription\\s+subscription[0-9]?;", true);
    }

    @Test
    public void subscriptionAsDParenthesis() throws Throwable {
        findPattern("Subscription\\s+d[0-9]?\\)", true);
    }

    @Test
    public void queueSubscriptionAsD() throws Throwable {
        findPattern("Subscription<.*>\\s+q?d[0-9]?\\b", true);
    }

    @Test
    public void booleanSubscriptionAsbd() throws Throwable {
        findPattern("BooleanSubscription\\s+bd[0-9]?;", true);
    }

    @Test
    public void atomicSubscriptionAsS() throws Throwable {
        findPattern("AtomicReference<Subscription>\\s+s[0-9]?;", true);
    }

    @Test
    public void atomicSubscriptionAsSInit() throws Throwable {
        findPattern("AtomicReference<Subscription>\\s+s[0-9]?\\s", true);
    }

    @Test
    public void atomicSubscriptionAsSubscription() throws Throwable {
        findPattern("AtomicReference<Subscription>\\s+subscription[0-9]?", true);
    }

    @Test
    public void atomicSubscriptionAsD() throws Throwable {
        findPattern("AtomicReference<Subscription>\\s+d[0-9]?", true);
    }

    @Test
    public void disposableAsS() throws Throwable {
        // the space before makes sure it doesn't match onSubscribe(Subscription) unnecessarily
        findPattern("Disposable\\s+s[0-9]?\\b", true);
    }

    @Test
    public void disposableAsFieldD() throws Throwable {
        findPattern("Disposable\\s+d[0-9]?;", true);
    }

    @Test
    public void atomicDisposableAsS() throws Throwable {
        findPattern("AtomicReference<Disposable>\\s+s[0-9]?", true);
    }

    @Test
    public void atomicDisposableAsD() throws Throwable {
        findPattern("AtomicReference<Disposable>\\s+d[0-9]?;", true);
    }

    @Test
    public void subscriberAsFieldActual() throws Throwable {
        findPattern("Subscriber<.*>\\s+actual[;\\)]", true);
    }

    @Test
    public void subscriberNoArgAsFieldActual() throws Throwable {
        findPattern("Subscriber\\s+actual[;\\)]", true);
    }

    @Test
    public void subscriberAsFieldS() throws Throwable {
        findPattern("Subscriber<.*>\\s+s[0-9]?;", true);
    }

    @Test
    public void observerAsFieldActual() throws Throwable {
        findPattern("Observer<.*>\\s+actual[;\\)]", true);
    }

    @Test
    public void observerAsFieldSO() throws Throwable {
        findPattern("Observer<.*>\\s+[so][0-9]?;", true);
    }

    @Test
    public void observerNoArgAsFieldActual() throws Throwable {
        findPattern("Observer\\s+actual[;\\)]", true);
    }

    @Test
    public void observerNoArgAsFieldCs() throws Throwable {
        findPattern("Observer\\s+cs[;\\)]", true);
    }

    @Test
    public void observerNoArgAsFieldSO() throws Throwable {
        findPattern("Observer\\s+[so][0-9]?;", true);
    }

    @Test
    public void queueDisposableAsD() throws Throwable {
        findPattern("Disposable<.*>\\s+q?s[0-9]?\\b", true);
    }

    @Test
    public void disposableAsDParenthesis() throws Throwable {
        findPattern("Disposable\\s+s[0-9]?\\)", true);
    }

    @Test
    public void compositeDisposableAsCs() throws Throwable {
        findPattern("CompositeDisposable\\s+cs[0-9]?", true);
    }

    @Test
    public void checkFortestNGInJUnitFiles() throws Throwable {
        findPattern("\\sorg\\.testng\\.", false, f -> !f.toLowerCase().contains("tck"));
    }

}
