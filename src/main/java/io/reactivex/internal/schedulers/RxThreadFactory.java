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

package io.reactivex.internal.schedulers;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A ThreadFactory that counts how many threads have been created and given a prefix,
 * sets the created Thread's name to {@code prefix-count}.
 */
public final class RxThreadFactory extends AtomicLong implements ThreadFactory {

    private static final long serialVersionUID = -7789753024099756196L;

    final String prefix;

    final int priority;

    final boolean nonBlocking;

//    static volatile boolean CREATE_TRACE;

    public RxThreadFactory(String prefix) {
        this(prefix, Thread.NORM_PRIORITY, false);
    }

    public RxThreadFactory(String prefix, int priority) {
        this(prefix, priority, false);
    }

    public RxThreadFactory(String prefix, int priority, boolean nonBlocking) {
        this.prefix = prefix;
        this.priority = priority;
        this.nonBlocking = nonBlocking;
    }

    @Override
    public Thread newThread(Runnable r) {
        StringBuilder nameBuilder = new StringBuilder(prefix).append('-').append(incrementAndGet());

//        if (CREATE_TRACE) {
//            nameBuilder.append("\r\n");
//            for (StackTraceElement se :Thread.currentThread().getStackTrace()) {
//                String s = se.toString();
//                if (s.contains("sun.reflect.")) {
//                    continue;
//                }
//                if (s.contains("junit.runners.")) {
//                    continue;
//                }
//                if (s.contains("org.gradle.internal.")) {
//                    continue;
//                }
//                if (s.contains("java.util.concurrent.ThreadPoolExecutor")) {
//                    continue;
//                }
//                nameBuilder.append(s).append("\r\n");
//            }
//        }

        String name = nameBuilder.toString();
        Thread t = nonBlocking ? new RxCustomThread(r, name) : new Thread(r, name);
        t.setPriority(priority);
        t.setDaemon(true);
        return t;
    }

    @Override
    public String toString() {
        return "RxThreadFactory[" + prefix + "]";
    }

    static final class RxCustomThread extends Thread implements NonBlockingThread {
        RxCustomThread(Runnable run, String name) {
            super(run, name);
        }
    }
}
