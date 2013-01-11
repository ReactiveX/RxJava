# JRuby Adaptor for RxJava


This adaptor allows 'org.jruby.RubyProc' lambda functions to be used and RxJava will know how to invoke them.

This enables code such as:

```ruby
  Observable.toObservable("one", "two", "three")
    .take(2) 
    .subscribe(lambda{|arg| puts arg})
```
