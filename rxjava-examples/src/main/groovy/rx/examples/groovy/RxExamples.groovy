package rx.examples.groovy;
import rx.observables.Observable

def hello(String[] names) {
    Observable.toObservable(names)
        .subscribe({ println "Hello " + it + "!"})
}

hello("Ben", "George")