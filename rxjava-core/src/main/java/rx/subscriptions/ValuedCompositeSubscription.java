/**
 * Copyright 2013 Netflix, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import rx.Subscription;
import rx.subscriptions.AbstractAtomicSubscription.SubscriptionState;
import rx.util.functions.Action0;
import rx.util.functions.Actions;

/**
 * A composite subscription which contains other subscriptions with an associated
 * value.
 * <p>
 * This composite doesn't allow {@code null} {@link rx.Subscription}s as keys but
 * permits {@code null} as values.
 * <p>
 * The composite retains the order of the added/put key-value pairs.
 * <p>
 * The {@code unsubscribe()} method unsubscribes in FIFO order.
 * 
 * @param <T> the value type
 */
public class ValuedCompositeSubscription<T> extends AbstractAtomicSubscription {
    /** The map holding the subscriptions and their associated value. */
    private final Map<Subscription, T> map = new LinkedHashMap<Subscription, T>();
    /**
     * Constructs an empty ValuedCompositeSubscription.
     */
    public ValuedCompositeSubscription() {
        super();
    }
    /**
     * Constructs a ValuedCompositeSubscription with the given initial
     * key and value.
     * @param key the initial key
     * @param value the initial value
     */
    public ValuedCompositeSubscription(Subscription key, T value) {
        this();
        if (key == null) {
            throw new NullPointerException("key");
        }
        map.put(key, value);
    }
    /**
     * Add or replace a value associated with the given subscription key.
     * @param key the key to add or replace
     * @param value the value to add or replace with
     * @return this instance to allow method chaining.
     */
    public ValuedCompositeSubscription<T> add(Subscription key, T value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        AddEntry ae = new AddEntry(key, value);
        if (!call(ae)) {
            key.unsubscribe();
        }
        return this;
    }
    /**
     * Adds or replaces a value associated with the given subscription key
     * and returns the previous value if any.
     * @param key the key to add or replace
     * @param value the value to add or replace with
     * @return the previous value associated with the key or {@code null} if
     * <ul>
     * <li>there was no pervious key</li>
     * <li>this composite was unsubscribed during the operation.</li>
     * </ul>
     */
    public T put(Subscription key, T value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        AddEntry ae = new AddEntry(key, value);
        if (!call(ae)) {
            key.unsubscribe();
        }
        return ae.previous;
    }
    /**
     * Try adding/replacing a key-value pair to this composite.
     * @param key the key to add
     * @param value the value to add
     * @return true if the key-value pair was added, false if this
     *         composite was unsubscribed during the operation
     */
    public boolean tryAdd(Subscription key, T value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        AddEntry ae = new AddEntry(key, value);
        return call(ae);
    }
    /**
     * Removes and unsubscribes the given subscription key from this composite.
     * @param key the key to remove
     * @return the associated value of the key or {@code null} if
     * <ul>
     * <li>the {@code key} is {@code null},</li>
     * <li>there was no pervious key or</li>
     * <li>this composite was unsubscribed during the operation.</li>
     * </ul>
     */
    public T remove(Subscription key) {
        if (key == null) {
            return null;
        }
        RemoveEntry re = new RemoveEntry(key);
        if (call(re) && re.result) {
            key.unsubscribe();
        }
        return re.value;
    }
    /**
     * Deletes (removes) a subscription from this composite but does not
     * call {@code unsubscribe()} on it.
     * @param key the key to remove without unsubscribing
     * @return The associated value of the key or {@code null} if
     * <ul>
     * <li>the {@code key} is {@code null},</li>
     * <li>there was no pervious key or</li>
     * <li>this composite was unsubscribed during the operation.</li>
     * </ul>
     */
    public T delete(Subscription key) {
        if (key == null) {
            return null;
        }
        DeleteEntry de = new DeleteEntry(key);
        call(de);
        return de.value;
    }
    /**
     * Adds a new key-value pair to this composite only if the key is
     * not already present.
     * @param key the subscription key to add
     * @param value the value to add
     * @return true if the key-value pair was added, false if
     * <ul>
     * <li>the {@code key} was already present or</li>
     * <li>this composite was unsubscribed unsubscribed during the operation.</li>
     * </ul>
     */
    public boolean putIfAbsent(Subscription key, T value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        AddIfEntry aie = new AddIfEntry(key, value);
        if (!call(aie)) {
            key.unsubscribe();
        }
        return aie.result;
    }
    /**
     * Check if the composite is empty.
     * @return true if the composite is empty
     */
    public boolean isEmpty() {
        return size() == 0;
    }
    /**
     * Returns the number of key-value pairs in this composite.
     * @return the number of key-value pairs in this composite 
     */
    public int size() {
        Size s = new Size();
        call(s);
        return s.result;
    }
    /**
     * Atomically adds or replaces key-value pairs in this composite.
     * <p>
     * Copies the {@code map} contents before modifying the composite, therefore,
     * a {@code null} key in {@code map} will yield a {@link NullPointerException}
     * and the composite remains unchanged.
     * @param map the map of subscription key and value pairs
     */
    public void putAll(Map<? extends Subscription, ? extends T> map) {
        PutAll pa = new PutAll(map);
        if (!call(pa)) {
            Subscriptions.unsubscribeAll(pa.mapCopy.keySet());
        }
    }
    /**
     * Checks if the given subscription key is in this composite.
     * @param key the key to check
     * @return true if in this composite, false if
     * <ul>
     * <li>the {@code key} is {@code null}.</li>
     * <li>the {@code key} is not in the composite or</li>
     * <li>this composite was unsubscribed during the operation.</li>
     * </ul>
     */
    public boolean contains(Subscription key) {
        if (key == null) {
            return false;
        }
        Contains c = new Contains(key);
        call(c);
        return c.result;
    }
    /**
     * Clears and unsubscribes all subscriptions maintained by this composite.
     * <p>
     * It unsubscribes in FIFO order.
     * @see #deleteClear()
     */
    public void clear() {
        ListAndCleanup c = new ListAndCleanup();
        if (call(c)) {
            Subscriptions.unsubscribeAll(c.list);
        }
    }
    /**
     * Clears but does not unsubscribe all subscriptions maintained by this composite.
     * @see #clear() 
     */
    public void deleteClear() {
        ListAndCleanup c = new ListAndCleanup();
        call(c);
    }
    /**
     * Atomically removes and unsubscribes any subscription which are both present
     * in this composite and in source.
     * @param source the source sequence of subscriptions to remove 
     */
    public void removeAll(Iterable<? extends Subscription> source) {
        RemoveAll ra = new RemoveAll(source);
        if (call(ra)) {
            Subscriptions.unsubscribeAll(ra.copy);
        }
    }
     /**
     * Atomically removes any subscription which are both present
     * in this composite and in source but does not unsubscribe them.
     * @param source the source sequence of subscriptions to remove 
     */
    public void deleteAll(Iterable<? extends Subscription> source) {
        DeleteAll da = new DeleteAll(source);
        call(da);
    }
    /**
     * Returns an unmodifiable collection of values maintained by this composite.
     * @return an unmodifiable collection of values
     */
    public Collection<T> values() {
        Values v = new Values();
        if (call(v)) {
            return Collections.unmodifiableCollection(v.result);
        }
        return Collections.emptyList();
    }
    @Override
    public void unsubscribe() {
        ListAndCleanup c = new ListAndCleanup();
        if (callAndSet(SubscriptionState.UNSUBSCRIBED, c)) {
            Subscriptions.unsubscribeAll(c.list);
        }
    }
    /**
     * Marks this composite as unsubscribed, clears the maintained subscriptions
     * but does not unsubscribe them.
     */
    public void deleteUnsubscribe() {
        ListAndCleanup c = new ListAndCleanup();
        callAndSet(SubscriptionState.UNSUBSCRIBED, c);
    }
    /**
     * Atomically retrieve the maintained subscription keys and mark this composite
     * as unsubscribed.
     * <p>
     * The returned subscriptions are not unsubscribed.
     * @return the collection of keys
     */
    public Collection<Subscription> getKeysAndUnsubscribe() {
        ListAndCleanup c = new ListAndCleanup();
        if (callAndSet(SubscriptionState.UNSUBSCRIBED, c)) {
            return Collections.unmodifiableList(c.list);
        }
        return Collections.emptyList();
    }
    /**
     * Atomically retrieve the maintained subscription keys and values and mark this composite
     * as unsubscribed.
     * <p>
     * The returned subscription keys are not unsubscribed.
     * @return the collection of keys
     */
    public Map<Subscription, T> getEntriesAndUnsubscribe() {
        MapAndCleanup c = new MapAndCleanup();
        if (callAndSet(SubscriptionState.UNSUBSCRIBED, c)) {
            return Collections.unmodifiableMap(c.mapCopy);
        }
        return Collections.emptyMap();
    }
    /**
     * Atomically retrieve the maintained values and mark this composite
     * as unsubscribed.
     * <p>
     * The cleared subscription keys are not unsubscribed.
     * @return the collection of values
     */
    public Collection<T> getValuesAndUnsubscribe() {
        ValueAndCleanup c = new ValueAndCleanup();
        if (callAndSet(SubscriptionState.UNSUBSCRIBED, c)) {
            return Collections.unmodifiableList(c.values);
        }
        return Collections.emptyList();
    }
    /**
     * Adds the value to this composite and generates a subscription token for it.
     * @param value the value to add
     * @return the subscription token
     */
    public Subscription add(T value) {
        Token token = new Token();
        if (tryAdd(token.composite, value)) {
            return token.client;
        }
        return Subscriptions.empty();
    }
    /**
     * Return a value associated with the key.
     * @param key the key to look for
     * @return the associated value or null if
     * <ul>
     * <li>the key is not present or</li>
     * <li>the composite was unsubscribed during the operation.</li>
     * </ul>
     */
    public T get(Subscription key) {
        Get g = new Get(key);
        call(g);
        return g.result;
    }
    /**
     * Return a value associated with the key.
     * @param key the key to look for
     * @param defaultValue the default value if the key is missing or the composite
     *                     was unsubscribed during the operation
     * @return the associated value or the default value if
     * <ul>
     * <li>the key is not present or</li>
     * <li>the composite was unsubscribed during the operation.</li>
     * </ul>
     */
    public T getOrDefault(Subscription key, T defaultValue) {
        Get g = new Get(key);
        call(g);
        return g.present ? g.result : defaultValue;
                
    }
    /**
     * Atomically retrieve the maintained values, unsubscribe their keys and mark this composite
     * as unsubscribed.
     * <p>
     * The cleared subscription keys are not unsubscribed.
     * @return the collection of values
     */
    public Collection<T> unsubscribeAndGet() {
        Map<Subscription, T> m = getEntriesAndUnsubscribe();
        Subscriptions.unsubscribeAll(m.keySet());
        return Collections.unmodifiableList(new ArrayList<T>(m.values()));
    }
    /**
     * Action to check if a key is in the map.
     */
    private final class Contains implements Action0 {
        final Subscription key;
        boolean result;
        public Contains(Subscription key) {
            this.key = key;
        }
        @Override
        public void call() {
            result = map.containsKey(key);
        }
    }
    /**
     * Retrieves the size of the map.
     */
    private final class Size implements Action0 {
        int result;
        @Override
        public void call() {
            result = map.size();
        }
    }
    /**
     * Action that adds/replaces a value for the given subscription key.
     */
    private final class AddEntry implements Action0 {
        final Subscription key;
        final T value;
        T previous;
        public AddEntry(Subscription key, T value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public void call() {
            previous = map.put(key, value);
        }
    }
    /**
     * Action that adds a value for the given subscription key only if the key
     * is not already in the map.
     */
    private final class AddIfEntry implements Action0 {
        final Subscription key;
        final T value;
        boolean result;
        public AddIfEntry(Subscription key, T value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public void call() {
            if (!map.containsKey(key)) {
                map.put(key, value);
                result = true;
            } else {
                result = false;
            }
        }
    }
    /**
     * Action that removes an entry and unsubscribes the key.
     * <p>
     * If the given key is not in the map no unsubscription will happen.
     */
    private final class RemoveEntry implements Action0 {
        final Subscription key;
        boolean result;
        T value;
        public RemoveEntry(Subscription key) {
            this.key = key;
        }

        @Override
        public void call() {
            if (map.containsKey(key)) {
                value = map.remove(key);
                result = true;
            }
        }
    }
    /**
     * Action that removes an entry but does not unsubscribe it.
     */
    private final class DeleteEntry implements Action0 {
        final Subscription key;
        T value;
        public DeleteEntry(Subscription key) {
            this.key = key;
        }

        @Override
        public void call() {
            value = map.remove(key);
        }
    }
    /** 
     * Clear the map and return the subscription keys.
     */
    private final class ListAndCleanup implements Action0 {
        List<Subscription> list;
        @Override
        public void call() {
            list = new ArrayList<Subscription>(map.keySet());
            map.clear();
        }
    }
    /** 
     * Clear the map and return the key-value pairs.
     */
    private final class MapAndCleanup implements Action0 {
        Map<Subscription, T> mapCopy;
        @Override
        public void call() {
            mapCopy = new LinkedHashMap<Subscription, T>(map);
            map.clear();
        }
    }
    /** 
     * Clear the map and return the values.
     */
    private final class ValueAndCleanup implements Action0 {
        List<T> values;
        @Override
        public void call() {
            values = new ArrayList<T>(map.values());
            map.clear();
        }
    }
    /**
     * Adds/replaces subscription keys and values.
     */
    private final class PutAll implements Action0 {
        final Map<Subscription, T> mapCopy;
        public PutAll(Map<? extends Subscription, ? extends T> map) {
            this.mapCopy = new LinkedHashMap<Subscription, T>();
            for (Map.Entry<? extends Subscription, ? extends T> e : map.entrySet()) {
                Subscription k = e.getKey();
                if (k == null) {
                    throw new NullPointerException();
                }
                this.mapCopy.put(k, e.getValue());
            }
        }

        @Override
        public void call() {
            map.putAll(mapCopy);
        }
    }
    /**
     * Remove and unsubscribe the common subscriptions.
     */
    private final class RemoveAll implements Action0 {
        final List<Subscription> copy;
        public RemoveAll(Iterable<? extends Subscription> source) {
            this.copy = new LinkedList<Subscription>();
            for (Subscription s : source) {
                if (s != null) {
                    copy.add(s);
                }
            }
        }

        @Override
        public void call() {
            Iterator<Subscription> it = copy.iterator();
            while (it.hasNext()) {
                Subscription s = it.next();
                if (map.containsKey(s)) {
                    map.remove(s);
                } else {
                    it.remove();
                }
            }
        }
    }
    /**
     * Remove and unsubscribe the common subscriptions.
     */
    private final class DeleteAll implements Action0 {
        final List<Subscription> copy;
        public DeleteAll(Iterable<? extends Subscription> source) {
            this.copy = new LinkedList<Subscription>();
            for (Subscription s : source) {
                if (s != null) {
                    copy.add(s);
                }
            }
        }

        @Override
        public void call() {
            Iterator<Subscription> it = copy.iterator();
            while (it.hasNext()) {
                Subscription s = it.next();
                if (map.containsKey(s)) {
                    map.remove(s);
                }
            }
        }
    }
    /**
     * Returns the values of the map.
     */
    private final class Values implements Action0 {
        Collection<T> result;

        @Override
        public void call() {
            result = new ArrayList<T>(map.values());
        }
    }
    /** Just deletes a key of a token. */
    private final class DeleteToken implements Action0 {
        final Subscription key;
        public DeleteToken(Subscription key) {
            this.key = key;
        }
        @Override
        public void call() {
            delete(key);
        }
    }
    /** The two-sided token to unsubscribe. */
    private final class Token {
        /** What to do on unsubscribe? */
        final AtomicReference<Action0> onUnsubscribe = new AtomicReference<Action0>();
        /** 
         * If the client calls and not already unsubscribed, the subscription key
         * is  deleted from the map.
         */
        final Subscription client = new Subscription() {
            @Override
            public void unsubscribe() {
                onUnsubscribe.getAndSet(Actions.empty0()).call();
            }
        };
        /**
         * Simply set the onUnsubscribe to none since the removal was
         * triggered by the composite and the map no longer contains
         * the item anyway.
         */
        final Subscription composite = new Subscription() {
            @Override
            public void unsubscribe() {
                onUnsubscribe.set(Actions.empty0());
            }
        };
        public Token() {
            onUnsubscribe.set(new DeleteToken(composite));
        }
    }
    /**
     * Get the value of the key.
     */
    private final class Get implements Action0 {
        final Subscription key;
        T result;
        boolean present;
        public Get(Subscription key) {
            this.key = key;
        }
        @Override
        public void call() {
            present = map.containsKey(key);
            if (present) {
                result = map.get(key);
            }
        }
    }
}
