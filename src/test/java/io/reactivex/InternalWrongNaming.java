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

import org.junit.Test;

/**
 * Adds license header to java files.
 */
public class InternalWrongNaming {

    static void checkInternalOperatorNaming(String baseClassName, String consumerClassName, String... ignore) throws Exception {
        File f = MaybeNo2Dot0Since.findSource(baseClassName);
        if (f == null) {
            return;
        }

        String rxdir = f.getParentFile().getAbsolutePath().replace('\\', '/');

        if (!rxdir.endsWith("/")) {
            rxdir += "/";
        }

        rxdir += "internal/operators/" + baseClassName.toLowerCase() + "/";

        File[] list = new File(rxdir).listFiles();
        if (list != null && list.length != 0) {

            StringBuilder fail = new StringBuilder();

            int count = 0;

            outer:
            for (File g : list) {
                for (String s : ignore) {
                    if (g.getName().equals(s + ".java")) {
                        continue outer;
                    }
                }
                List<String> lines = readFile(g);

                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.contains(consumerClassName)) {

                        fail.append("java.lang.RuntimeException: " + g.getName() + " mentions " + consumerClassName)
                        .append("\r\n at io.reactivex.internal.operators.")
                        .append(baseClassName.toLowerCase()).append(".").append(g.getName().replace(".java", ""))
                        .append(" (").append(g.getName()).append(":").append(i + 1).append(")\r\n\r\n");

                        count++;
                    }
                }
            }

            if (fail.length() != 0) {
                System.out.println(fail);

                System.out.println();
                System.out.println("Total: " + count);
                throw new AssertionError(fail.toString());
            }

        }
    }

    static List<String> readFile(File u) throws Exception {
        List<String> lines = new ArrayList<String>();

        BufferedReader in = new BufferedReader(new FileReader(u));
        try {
            for (;;) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }

                lines.add(line);
            }
        } finally {
            in.close();
        }
        return lines;
    }

    @Test
    public void observableNoSubscriber() throws Exception {
        checkInternalOperatorNaming("Observable", "Subscriber",
                "ObservableFromPublisher"
        );
    }

    @Test
    public void observableNoSubscribers() throws Exception {
        checkInternalOperatorNaming("Observable", "subscribers");
    }

    @Test
    public void observableNoSubscription() throws Exception {
        checkInternalOperatorNaming("Observable", "Subscription",
                "ObservableFromPublisher", "ObservableDelaySubscriptionOther");
    }

    @Test
    public void observableNoPublisher() throws Exception {
        checkInternalOperatorNaming("Observable", "Publisher",
                "ObservableFromPublisher");
    }

    @Test
    public void observableNoFlowable() throws Exception {
        checkInternalOperatorNaming("Observable", "Flowable", "ObservableFromPublisher");
    }

    @Test
    public void observableProducer() throws Exception {
        checkInternalOperatorNaming("Observable", "Producer");
    }

    @Test
    public void observableProducers() throws Exception {
        checkInternalOperatorNaming("Observable", "producers");
    }

    @Test
    public void flowableNoProducer() throws Exception {
        checkInternalOperatorNaming("Flowable", "Producer");
    }

    @Test
    public void flowableNoProducers() throws Exception {
        checkInternalOperatorNaming("Flowable", "producers");
    }

    @Test
    public void flowableNoUnsubscrib() throws Exception {
        checkInternalOperatorNaming("Flowable", "unsubscrib");
    }

    @Test
    public void observableNoUnsubscrib() throws Exception {
        checkInternalOperatorNaming("Observable", "unsubscrib");
    }

    @Test
    public void flowableNoObserver() throws Exception {
        checkInternalOperatorNaming("Flowable", "Observer",
                "FlowableFromObservable",
                "FlowableLastSingle",
                "FlowableAnySingle",
                "FlowableAllSingle",
                "FlowableToListSingle",
                "FlowableCollectSingle",
                "FlowableCountSingle",
                "FlowableElementAtMaybe",
                "FlowableElementAtSingle",
                "FlowableSingleSingle",
                "FlowableSingleMaybe",
                "FlowableLastMaybe",
                "FlowableIgnoreElementsCompletable",
                "FlowableReduceMaybe",
                "FlowableReduceWithSingle",
                "FlowableReduceSeedSingle",
                "FlowableFlatMapCompletable",
                "FlowableFlatMapCompletableCompletable",
                "FlowableFlatMapSingle",
                "FlowableFlatMapMaybe",
                "FlowableSequenceEqualSingle"
        );
    }
}
