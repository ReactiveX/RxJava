package io.reactivex.observable;

import io.reactivex.Observable;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ObservableDistinctTest {

    @Test(description = "test `Observable.distinctOfGroup(n)`",
            dataProvider = "DataProvider.Observable.distinctOfGroup",
            dataProviderClass = ParamsDataProvider.class)
    public void testDistinctOfGroup(int n, Integer[] base, Integer[] result) {
        Observable
                .fromArray(base)
                .distinctOfGroup(n)
                .test()
                .assertValues(result)
                .assertComplete();
    }

    private static class ParamsDataProvider {

        @DataProvider(name = "DataProvider.Observable.distinctOfGroup")
        public static Object[][] params() {
            return new Object [][] {
                    { 1,
                            new Integer[] { 1, 2, 3, 4, 3, 5, 6, 7 },
                            new Integer[] { 1, 2, 3, 4, 3, 5, 6, 7 } },
                    { 1,
                            new Integer[] { 1, 2, 3, 3, 4, 5, 6, 7 },
                            new Integer[] { 1, 2, 3, 4, 5, 6, 7 } },
                    { 1,
                            new Integer[] { 1, 2, 3, 3, 3, 4, 5, 6, 7 },
                            new Integer[] { 1, 2, 3, 4, 5, 6, 7 } },

                    { 2,
                            new Integer[] { 1, 2, 3, 4, 3, 5, 6, 7 },
                            new Integer[] { 1, 2, 3, 4, 5, 6, 7 } },
                    { 2,
                            new Integer[] { 1, 2, 3, 3, 4, 5, 6, 7 },
                            new Integer[] { 1, 2, 3, 4, 5, 6, 7 } },
                    { 2,
                            new Integer[] { 1, 2, 3, 3, 3, 4, 5, 6, 7 },
                            new Integer[] { 1, 2, 3, 4, 5, 6, 7 } },

                    { 3,
                            new Integer[] { 1, 2, 3, 4, 3, 5, 6, 7 },
                            new Integer[] { 1, 2, 3, 4, 5, 6, 7 } },
                    { 3,
                            new Integer[] { 1, 2, 3, 3, 4, 5, 6, 7 },
                            new Integer[] { 1, 2, 3, 4, 5, 6, 7 } },
                    { 3,
                            new Integer[] { 1, 2, 3, 3, 3, 4, 5, 6, 7 },
                            new Integer[] { 1, 2, 3, 4, 5, 6, 7 } },
                    { 3,
                            new Integer[] { 1, 2, 3, 3, 3, 3, 3, 3, 4, 5, 6, 7 },
                            new Integer[] { 1, 2, 3, 4, 5, 6, 7 } },
                    { 3,
                            new Integer[] { 1, 2, 3, 3, 3, 4, 3, 3, 4, 5, 6, 7 },
                            new Integer[] { 1, 2, 3, 4, 5, 6, 7 } },
            };
        }

    }

}
