package rx.internal.operators;


import junit.framework.Assert;
import org.junit.Test;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OperatorMatchTest {

    @Test
    public void testOperatorMatch() {
        Func1<String, Func1<String, String>> findMappingFunc = mock(Func1.class);
        Func1<String, String> mapping = mock(Func1.class);

        when(findMappingFunc.call(anyString())).thenReturn(mapping);

        OperatorMatch match = new OperatorMatch(findMappingFunc);

        Subscriber<String> subscriber = mock(Subscriber.class);

        match.call(subscriber).onNext("on next");

        verify(mapping, times(1)).call(anyString());
    }


    @Test
    public void testMatch() {
        List<Integer> expected = new ArrayList<Integer>();
        expected.add(0);
        expected.add(2);
        expected.add(2);
        expected.add(6);
        expected.add(4);
        expected.add(10);
        expected.add(6);
        expected.add(14);
        expected.add(8);
        expected.add(18);

        List<Integer> result = Observable
            .range(0, 10)
            .match(new Func1<Integer, Boolean>() {
                @Override
                public Boolean call(Integer integer) {
                    return integer % 2 == 1;
                }
            }, new Func1<Integer, Integer>() {
                @Override
                public Integer call(Integer integer) {
                    return integer * 2;
                }
            })
            .matchDefault(new Func1<Integer, Integer>() {
                @Override
                public Integer call(Integer integer) {
                    return integer;
                }
            }).toList().toBlocking().single();

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(expected.size(), result.size());

        for (int i = 0; i < result.size(); i++) {
            Assert.assertEquals(expected.get(i), result.get(i));
        }
    }

    @Test
    public void testMatchFollowedByMatch() {
        List<Integer> expected = new ArrayList<Integer>();
        expected.add(0);
        expected.add(2);
        expected.add(2);
        expected.add(6);
        expected.add(4);
        expected.add(0);
        expected.add(6);
        expected.add(0);
        expected.add(8);
        expected.add(0);

        List<Integer> result = Observable
            .range(0, 10)
            .match(new Func1<Integer, Boolean>() {
                @Override
                public Boolean call(Integer integer) {
                    return integer % 2 == 1;
                }
            }, new Func1<Integer, Integer>() {
                @Override
                public Integer call(Integer integer) {
                    return integer * 2;
                }
            })
            .matchDefault(new Func1<Integer, Integer>() {
                @Override
                public Integer call(Integer integer) {
                    return integer;
                }
            })
            .match(new Func1<Integer, Boolean>() {
                @Override
                public Boolean call(Integer integer) {
                    return integer >= 10;
                }
            }, new Func1<Integer, Integer>() {
                @Override
                public Integer call(Integer integer) {
                    return 0;
                }
            })
            .matchDefault(new Func1<Integer, Integer>() {
                @Override
                public Integer call(Integer integer) {
                    return integer;
                }
            }).toList().toBlocking().single();

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(expected.size(), result.size());

        for (int i = 0; i < result.size(); i++) {
            Assert.assertEquals(expected.get(i), result.get(i));
        }

    }

    @Test
    public void testFlatMatch() {
        List<Integer> expected = new ArrayList<Integer>();
        expected.add(6);
        expected.add(7);
        expected.add(8);
        expected.add(9);

        List<Integer> result = Observable
            .just(true)
            .flatMatch(new Func1<Boolean, Boolean>() {
                @Override
                public Boolean call(Boolean aBoolean) {
                    return !aBoolean;
                }
            }, new Func1<Boolean, Observable<Integer>>() {
                @Override
                public Observable<Integer> call(Boolean aBoolean) {
                    return Observable.just(1, 2, 3, 4, 5);
                }
            }).matchDefault(new Func1<Boolean, Observable<Integer>>() {
                @Override
                public Observable<Integer> call(Boolean aBoolean) {
                    return Observable.just(6, 7, 8, 9);
                }
            }).toList().toBlocking().single();

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(expected.size(), result.size());

        for (int i = 0; i < result.size(); i++) {
            Assert.assertEquals(expected.get(i), result.get(i));
        }
    }

    @Test
    public void testFlatMatchWithinFlatMatch() {

        List<Integer> expected = new ArrayList<Integer>();
        expected.add(0);
        expected.add(1);
        expected.add(4);
        expected.add(3);
        expected.add(4);
        expected.add(5);
        expected.add(6);
        expected.add(7);
        expected.add(8);
        expected.add(9);
        expected.add(10);
        expected.add(11);
        expected.add(12);
        expected.add(13);
        expected.add(14);
        expected.add(15);
        expected.add(16);
        expected.add(17);
        expected.add(18);
        expected.add(19);

        List<Integer> result = Observable
            .range(0, 20)
            .flatMatch(new Func1<Integer, Boolean>() {
                @Override
                public Boolean call(Integer integer) {
                    return integer % 2 == 0;
                }
            }, new Func1<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> call(Integer integer) {
                    return Observable.just(integer).flatMatch(new Func1<Integer, Boolean>() {
                        @Override
                        public Boolean call(Integer integer) {
                            return integer == 2;
                        }
                    }, new Func1<Integer, Observable<Integer>>() {
                        @Override
                        public Observable<Integer> call(Integer integer) {
                            return Observable.just(integer * 2);
                        }
                    })
                        .matchDefault(new Func1<Integer, Observable<Integer>>() {
                            @Override
                            public Observable<Integer> call(Integer integer) {
                                return Observable.just(integer);
                            }
                        });

                }
            }).matchDefault(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer integer) {
                return Observable.just(integer);
            }
        }).toList().toBlocking().single();

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(expected.size(), result.size());

        for (int i = 0; i < result.size(); i++) {
            Assert.assertEquals(expected.get(i), result.get(i));
        }
    }
}