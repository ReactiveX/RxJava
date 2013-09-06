Not all unit tests are here, many are also embedded as inner classes of the main code (such as here: [rxjava-core/src/main/java/rx/operators](https://github.com/Netflix/RxJava/tree/master/rxjava-core/src/main/java/rx/operators)).

* For an explanation of this design choice see 
Ben J. Christensen's [JUnit Tests as Inner Classes](http://benjchristensen.com/2011/10/23/junit-tests-as-inner-classes/).

Also, each of the language adaptors has a /src/test/ folder which further testing (see Groovy for an example: [language-adaptors/rxjava-groovy/src/test](https://github.com/Netflix/RxJava/tree/master/language-adaptors/rxjava-groovy/src/test)).
