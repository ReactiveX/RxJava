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
package rx.subscriptions;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.concurrent.Future;

import rx.*;
import rx.annotations.Experimental;
import rx.functions.Action0;
import rx.internal.schedulers.ScheduledAction;
import rx.internal.util.SubscriptionList;

/**
 * Helper methods and utilities for creating and working with {@link Subscription} objects
 */
public final class Subscriptions {
    private Subscriptions() {
        throw new IllegalStateException("No instances!");
    }
    /**
     * Returns a {@link Subscription} to which {@code unsubscribe} does nothing except to change
     * {@code isUnsubscribed} to {@code true}. It's stateful and {@code isUnsubscribed} indicates if
     * {@code unsubscribe} is called, which is different from {@link #unsubscribed()}.
     *
     * <pre><code>
     * Subscription empty = Subscriptions.empty();
     * System.out.println(empty.isUnsubscribed()); // false
     * empty.unsubscribe();
     * System.out.println(empty.isUnsubscribed()); // true
     * </code></pre>
     *
     * @return a {@link Subscription} to which {@code unsubscribe} does nothing except to change
     *         {@code isUnsubscribed} to {@code true}
     */
    public static Subscription empty() {
        return BooleanSubscription.create();
    }

    /**
     * Returns a {@link Subscription} to which {@code unsubscribe} does nothing, as it is already unsubscribed.
     * Its {@code isUnsubscribed} always returns {@code true}, which is different from {@link #empty()}.
     *
     * <pre><code>
     * Subscription unsubscribed = Subscriptions.unsubscribed();
     * System.out.println(unsubscribed.isUnsubscribed()); // true
     * </code></pre>
     *
     * @return a {@link Subscription} to which {@code unsubscribe} does nothing, as it is already unsubscribed
     * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
     */
    @Experimental
    public static Subscription unsubscribed() {
        return UNSUBSCRIBED;
    }

    /**
     * Creates and returns a {@link Subscription} that invokes the given {@link Action0} when unsubscribed.
     * 
     * @param unsubscribe
     *            Action to invoke on unsubscribe.
     * @return {@link Subscription}
     */
    public static Subscription create(final Action0 unsubscribe) {
        return BooleanSubscription.create(unsubscribe);
    }

    /**
     * Converts a {@link Future} into a {@link Subscription} and cancels it when unsubscribed.
     * 
     * @param f
     *            the {@link Future} to convert
     * @return a {@link Subscription} that wraps {@code f}
     */
    public static Subscription from(final Future<?> f) {
        return new FutureSubscription(f);
    }

        /** Naming classes helps with debugging. */
    private static final class FutureSubscription implements Subscription {
        final Future<?> f;

        public FutureSubscription(Future<?> f) {
            this.f = f;
        }
        @Override
        public void unsubscribe() {
            f.cancel(true);
        }

        @Override
        public boolean isUnsubscribed() {
            return f.isCancelled();
        }
        @Override
        public String toString() {
            return Subscriptions.dump(this);
        }
    }

    /**
     * Converts a set of {@link Subscription}s into a {@link CompositeSubscription} that groups the multiple
     * Subscriptions together and unsubscribes from all of them together.
     * 
     * @param subscriptions
     *            the Subscriptions to group together
     * @return a {@link CompositeSubscription} representing the {@code subscriptions} set
     */

    public static CompositeSubscription from(Subscription... subscriptions) {
        return new CompositeSubscription(subscriptions);
    }

    /**
     * A {@link Subscription} that does nothing when its unsubscribe method is called.
     */
    private static final Unsubscribed UNSUBSCRIBED = new Unsubscribed();
        /** Naming classes helps with debugging. */
    private static final class Unsubscribed implements Subscription {
        @Override
        public void unsubscribe() {
        }

        @Override
        public boolean isUnsubscribed() {
            return true;
        }
        @Override
        public String toString() {
            return Subscriptions.dump(this);
        }
    }
    /**
     * Dumps the contents of a subscription recursively.
     * @param s the subscription to dump
     * @return the textual representation of the subscription
     */
    public static String dump(Subscription s) {
        final StringBuilder b = new StringBuilder();
        final IdentityHashMap<Object, Integer> ids = new IdentityHashMap<Object, Integer>();
        
        dump(s, b, "", ids);
        
        return b.toString().trim();
    }
    
    private static Field findField(Object o, String name) {
        Class<?> c = o.getClass();
        while (c != null) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getName().equals(name)) {
                    f.setAccessible(true);
                    return f;
                }
            }
            c = c.getSuperclass();
        }
        throw new NoSuchFieldError(o + ": " + name);
    }
    
    private static void dump(Subscription s, StringBuilder b, String indent, IdentityHashMap<Object, Integer> ids) {
        if (s == null) {
            b.append(indent).append("null\r\n");
            return;
        }
        b.append(indent).append(s.getClass().getName());
        b.append("[id=");
        Integer id = ids.get(s);
        if (id != null) {
            b.append(id).append("] { ... }\r\n");
            return;
        }
        id = ids.size();
        ids.put(s, id);
        b.append(id);
        
        if (s instanceof BooleanSubscription) {
            b.append(", unsubscribed=").append(s.isUnsubscribed());
            BooleanSubscription bs = (BooleanSubscription) s;
            Action0 a = bs.action;
            if (a != null) {
                Integer id2 = ids.get(a);
                if (id2 == null) {
                    id2 = ids.size();
                    ids.put(a, id2);
                }
                b.append(", action=").append(a).append("[id=").append(id2).append("]]\r\n");
            } else {
                b.append(", action=null");
            }
        } else
        if (s instanceof FutureSubscription) {
            b.append(", unsubscribed=").append(s.isUnsubscribed());

            FutureSubscription fs = (FutureSubscription) s;
            Future<?> a = fs.f;
            if (a != null) {
                Integer id2 = ids.get(a);
                if (id2 == null) {
                    id2 = ids.size();
                    ids.put(a, id2);
                }
                b.append(", future=").append(a).append("[id=").append(id2).append("]]\r\n");
            } else {
                b.append(", future=null]\r\n");
            }
        } else
        if (s instanceof SerialSubscription) {
            SerialSubscription ss = (SerialSubscription) s;
            b.append(", unsubscribed=").append(ss.isUnsubscribed()).append("] {\r\n");
            dump(ss.get(), b, indent + "    ", ids);
            b.append(indent).append("}\r\n");
        } else
        if (s instanceof MultipleAssignmentSubscription) {
            MultipleAssignmentSubscription mas = (MultipleAssignmentSubscription) s;
            b.append(", unsubscribed=").append(mas.isUnsubscribed()).append("] {\r\n");
            dump(mas.get(), b, indent + "    ", ids);
            b.append(indent).append("}\r\n");
        } else
        if (s instanceof RefCountSubscription) {
            RefCountSubscription ref = (RefCountSubscription) s;
            b.append(", size=").append(ref.size())
            .append(", unsubscribed=").append(ref.isUnsubscribed())
            .append("] {\r\n");
            dump(ref.actual, b, indent + "    ", ids);
            b.append(indent).append("}\r\n");
            ;
        } else
        if (s instanceof Subscriber) {
            b.append(", unsubscribed=").append(s.isUnsubscribed());
            try {
                Field opf = findField(s, "op");
                Field csf = findField(s, "cs");
                Field pf = findField(s, "p");
                Field requestedf = findField(s, "requested");
                
                Subscriber<?> op = (Subscriber<?>)opf.get(s);
                Subscription cs = (Subscription)csf.get(s);
                Producer p = (Producer)pf.get(s);
                long req = requestedf.getLong(s);
                
                b.append(", requested=").append(req).append("] {\r\n");
                String indent2 = indent + "    ";
                String indent3 = indent2 + "    ";
                
                if (op != null) {
                    b.append(indent2).append("op={\r\n");
                    dump(op, b, indent3, ids);
                    b.append(indent2).append("}\r\n");
                } else {
                    b.append(indent2).append("op=null\r\n");
                }
                if (cs != null) {
                    b.append(indent2).append("cs={\r\n");
                    dump(cs, b, indent3, ids);
                    b.append(indent2).append("}\r\n");
                } else {
                    b.append(indent2).append("cs=null\r\n");
                }
                
                if (p instanceof Subscription) {
                    b.append(indent2).append("p={\r\n");
                    dump((Subscription)p, b, indent3, ids);
                    b.append(indent2).append("}\r\n");
                } else 
                if (p != null) {
                    Integer id2 = ids.get(p);
                    if (id2 == null) {
                        id2 = ids.size();
                        ids.put(p, id2);
                    }
                    b.append(indent2).append("p=").append(p).append("[id=").append(id2).append("]]\r\n");
                } else {
                    b.append(indent2).append("p=null\r\n");
                }
                
            } catch (Throwable t) {
                b.append("] " + t + "\r\n");
                return;
            }
            b.append(indent).append("}\r\n");
        } else
        if (s instanceof ScheduledAction) {
            
            ScheduledAction sa = (ScheduledAction) s;
            b.append(", unsubscribed=").append(s.isUnsubscribed());
            try {
                Field actionf = findField(s, "action");
                Field cancelf = findField(s, "cancel");
                
                Subscription cs = (Subscription)cancelf.get(s);
                Action0 a = (Action0)actionf.get(s);
                
                b.append("] {\r\n");
                String indent2 = indent + "    ";
                String indent3 = indent2 + "    ";
                
                Thread t = sa.get();
                if (t == null) {
                    b.append(indent2).append("thread=null\r\n");
                } else {
                    b.append(indent2).append("thread=").append(t).append("[id=")
                    .append(t.getId()).append("]]\r\n");
                }
                if (a == null) {
                    b.append(indent2).append("action=null\r\n");
                } else
                if (a instanceof Subscription) {
                    b.append(indent2).append("action={\r\n");
                    dump((Subscription)a, b, indent3, ids);
                    b.append(indent2).append("}\r\n");
                } else {
                    Integer id2 = ids.get(a);
                    if (id2 == null) {
                        id2 = ids.size();
                        ids.put(a, id2);
                    }
                    b.append(indent2).append("action=").append(a).append("[id=").append(id2).append("]]\r\n");
                }
                b.append(indent2).append("cs={\r\n");
                dump(cs, b, indent3, ids);
                b.append(indent2).append("}\r\n");
            } catch (Throwable t) {
                b.append("] " + t + "\r\n");
                return;
            }
            b.append("}\r\n");
        } else
        if (s instanceof CompositeSubscription) {
            CompositeSubscription cs = (CompositeSubscription) s;
            int c = cs.size();
            b.append(", size=").append(c)
            .append(", unsubscribed=").append(cs.isUnsubscribed());
            
            if (c > 0) {
                b.append("] {\r\n");
                String indent2 = indent + "    ";
                for (Subscription si : cs.getSubscriptions()) {
                    dump(si, b, indent2, ids);
                }
                b.append(indent).append("}\r\n");
            } else {
                b.append("] { empty }\r\n");
            }
        } else
        if (s instanceof SubscriptionList) {
            SubscriptionList cs = (SubscriptionList) s;
            int c = cs.size();
            b.append(", size=").append(c)
            .append(", unsubscribed=").append(cs.isUnsubscribed());
            
            if (c > 0) {
                b.append("] {\r\n");
                String indent2 = indent + "    ";
                for (Subscription si : cs.getSubscriptions()) {
                    dump(si, b, indent2, ids);
                }
                b.append(indent).append("}\r\n");
            } else {
                b.append("] { empty }\r\n");
            }
        } else {
            b.append(", unsubscribed=").append(s.isUnsubscribed());
            b.append("]\r\n");
        }
    }
}
