This test folder only contains performance and functional/integration style tests.

The unit tests themselves are embedded as inner classes of the Java code (such as here https://github.com/Netflix/RxJava/tree/master/rxjava-core/src/main/java/rx/operators).

Also, each of the language adaptors has a /src/test/ folder which further testing. See Groovy for an example: https://github.com/Netflix/RxJava/tree/master/language-adaptors/rxjava-groovy/src/test