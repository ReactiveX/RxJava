package rx.operators;

import org.junit.Test;
import rx.GroupedObservable;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;
import rx.util.functions.Functions;

import java.util.*;

import static org.junit.Assert.assertEquals;

public final class OperatorGroupBy {

    public static <K, T, R> Func1<Observer<GroupedObservable<K, R>>, Subscription> groupBy(Observable<T> source, final Func1<T, K> keySelector, final Func1<T, R> elementSelector) {

        final Observable<KeyValue<K, R>> keyval = source.map(new Func1<T, KeyValue<K, R>>() {
            @Override
            public KeyValue<K, R> call(T t) {
                K key = keySelector.call(t);
                R value = elementSelector.call(t);

                return new KeyValue<K, R>(key, value);
            }
        });

        return new GroupBy<K, T, R>(keyval);
    }

    private static class GroupBy<K, T, R> implements Func1<Observer<GroupedObservable<K, R>>, Subscription> {
        private final Observable<KeyValue<K, R>> source;
        private final Set<K> keys = new HashSet<K>();

        private GroupBy(Observable<KeyValue<K, R>> source) {
            this.source = source;
        }


        @Override
        public Subscription call(final Observer<GroupedObservable<K, R>> observer) {

            return source.subscribe(new Observer<KeyValue<K, R>>() {
                private final Object lock = new Object();

                @Override
                public void onCompleted() {
                    observer.onCompleted();
                }

                @Override
                public void onError(Exception e) {
                    observer.onError(e);
                }

                @Override
                public void onNext(final KeyValue<K, R> args) {

                    if (!keys.contains(args.key)) {
                        synchronized (lock) {
                            if (!keys.contains(args.key)) {
                                observer.onNext(buildObservableFor(source, args));
                                keys.add(args.key);
                            }
                        }
                    }

                }

            });
        }
    }

    private static <K, R> GroupedObservable<K, R> buildObservableFor(Observable<KeyValue<K, R>> source, final KeyValue<K, R> element) {
        final Observable<R> observable = source.filter(new Func1<KeyValue<K, R>, Boolean>() {
            @Override
            public Boolean call(KeyValue<K, R> pair) {
                return element.key.equals(pair.key);
            }
        }).map(new Func1<KeyValue<K, R>, R>() {
            @Override
            public R call(KeyValue<K, R> pair) {
                return pair.value;
            }
        });
        return new GroupedObservable<K, R>(element.key, observable);
    }


    private static class KeyValue<K, V> {
        private final K key;
        private final V value;

        private KeyValue(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    public static class UnitTest {
        final Func1<String, Integer> length = new Func1<String, Integer>() {
            @Override
            public Integer call(String s) {
                return s.length();
            }
        };

        @Test
        public void testGroupBy() {
            Observable<String> source = Observable.from("one", "two", "three", "four", "five", "six");
            Observable<GroupedObservable<Integer, String>> grouped = Observable.create(groupBy(source, length, Functions.<String>identity()));

            Map<Integer, List<String>> map = toMap(grouped);

            assertEquals(3, map.size());
            assertEquals(Arrays.asList("one", "two", "six"), map.get(3));
            assertEquals(Arrays.asList("four", "five"), map.get(4));
            assertEquals(Arrays.asList("three"), map.get(5));

        }

        private static <K, V> Map<K, List<V>> toMap(Observable<GroupedObservable<K, V>> observable) {
            Map<K, List<V>> result = new HashMap<K, List<V>>();
            for (GroupedObservable<K, V> g : observable.toIterable()) {
                K key = g.getKey();

                for (V value : g.toIterable()) {
                    List<V> values = result.get(key);
                    if (values == null) {
                        values = new ArrayList<V>();
                        result.put(key, values);
                    }

                    values.add(value);
                }

            }

            return result;
        }

    }

}
