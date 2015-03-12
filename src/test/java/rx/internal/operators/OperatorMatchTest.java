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
    @SuppressWarnings("unchecked")
    public void testMatchOperator() {
        List<OperatorMatch.MatchPair<String, String>> mappings = new ArrayList<OperatorMatch.MatchPair<String, String>>();

        Func1<String, Boolean> truePredicate = mock(Func1.class);
        Func1<String, Boolean> falsePredicate = mock(Func1.class);
        Func1<String, Boolean> falsePredicate2 = mock(Func1.class);
        Func1<String, String> defaultMapping = mock(Func1.class);
        Func1<String, String> trueMapping = mock(Func1.class);
        Func1<String, String> falseMapping = mock(Func1.class);
        Func1<String, String> falseMapping2 = mock(Func1.class);

        mappings.add(new OperatorMatch.MatchPair<String, String>(falsePredicate, falseMapping));
        mappings.add(new OperatorMatch.MatchPair<String, String>(truePredicate, trueMapping));
        mappings.add(new OperatorMatch.MatchPair<String, String>(falsePredicate2, falseMapping2));

        OperatorMatch<String, String> match = new OperatorMatch<String, String>(mappings, defaultMapping);

        Subscriber<String> subscriber = mock(Subscriber.class);

        when(truePredicate.call(anyString())).thenReturn(true);
        when(falsePredicate.call(anyString())).thenReturn(false);
        when(trueMapping.call(anyString())).thenReturn("true mapping");

        match.call(subscriber).onNext("on next");

        verify(falsePredicate, times(1)).call(anyString());
        verify(truePredicate, times(1)).call(anyString());
        verify(defaultMapping, times(0)).call(anyString());
        verify(trueMapping, times(1)).call(anyString());
        verify(falseMapping, times(0)).call(anyString());
        verify(falsePredicate2, times(0)).call(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMatchOperatorDefault() {
        List<OperatorMatch.MatchPair<String, String>> mappings = new ArrayList<OperatorMatch.MatchPair<String, String>>();

        Func1<String, Boolean> falsePredicate = mock(Func1.class);
        Func1<String, String> defaultMapping = mock(Func1.class);
        Func1<String, String> falseMapping = mock(Func1.class);

        mappings.add(new OperatorMatch.MatchPair<String, String>(falsePredicate, falseMapping));
        mappings.add(new OperatorMatch.MatchPair<String, String>(falsePredicate, falseMapping));
        mappings.add(new OperatorMatch.MatchPair<String, String>(falsePredicate, falseMapping));

        OperatorMatch<String, String> match = new OperatorMatch<String, String>(mappings, defaultMapping);

        Subscriber<String> subscriber = mock(Subscriber.class);

        when(falsePredicate.call(anyString())).thenReturn(false);

        match.call(subscriber).onNext("on next");

        verify(falsePredicate, times(3)).call(anyString());
        verify(defaultMapping, times(1)).call(anyString());
        verify(falseMapping, times(0)).call(anyString());
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
}