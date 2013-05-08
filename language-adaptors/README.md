# Language Adaptors

RxJava supports JVM languages via implementations of <a href="https://github.com/Netflix/RxJava/blob/master/rxjava-core/src/main/java/rx/util/functions/FunctionLanguageAdaptor.java">FunctionLanguageAdaptor</a>

If there is a language you'd like supported please look at the existing adaptors (such as <a href="https://github.com/Netflix/RxJava/blob/master/language-adaptors/rxjava-groovy/src/main/java/rx/lang/groovy/GroovyAdaptor.java">Groovy</a>) and implement the adaptor for your language of choice.

If you feel it would be valuable for the community submit a pull request and we'll accept it into the main project.

Please comply with the conventions established by the existing language adaptors if you intend to submit a pull request.

NOTE: Changes are coming in regards to static and dynamic typing and how language adaptors are used.

See https://github.com/Netflix/RxJava/issues/208 and https://github.com/Netflix/RxJava/issues/204
