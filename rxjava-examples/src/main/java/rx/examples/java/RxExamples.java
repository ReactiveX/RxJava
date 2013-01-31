package rx.examples.java;

import rx.Observable;
import rx.util.functions.Action1;

public class RxExamples {

    public static void main(String args[]) {
        hello("Ben", "George");
    }

    public static void hello(String... names) {
        Observable.toObservable(names).subscribe(new Action1<String>() {

            @Override
            public void call(String s) {
                System.out.println("Hello " + s + "!");
            }

        });
    }
}
