require_relative "spec_helper"

describe Rx::Lang::Jruby::Interop do
  subject { described_class.instance }

  let(:observable) { Java::Rx::Observable.from([1, 2, 3]) }

  context "with a normal, non-function method signature" do
    it "calls straight through to the original Java method" do
      observable.should_not_receive(:toBlockingObservable_without_wrapping)
      observable.toBlockingObservable.should be_a(Java::RxObservables::BlockingObservable)
    end
  end

  context "with a method with a function method signature" do
    it "wraps function arguments if they're in the right position" do
      observable.should_receive(:subscribe_without_wrapping).with(kind_of(Java::RxLangJruby::JRubyActionWrapper))
      observable.subscribe(lambda {})
    end

    it "doesn't wrap function arguments if they're in the wrong position" do
      proc = lambda {}
      observable.should_receive(:subscribe_without_wrapping).with(1, 1, 1, proc)
      observable.subscribe(1, 1, 1, proc)
    end

    it "doesn't wrap non-function arguments" do
      observable.should_receive(:subscribe_without_wrapping).with(1)
      observable.subscribe(1)
    end

    it "doesn't wrap OnSubscribeFunc arguments" do
      proc = lambda {|observer| observer.onNext("hi")}
      Java::Rx::Observable.should_not_receive(:create_without_wrapping)
      Java::Rx::Observable.create(proc).should be_a(Java::Rx::Observable)
    end

    it "works with underscoreized method names" do
      observable.
          should_receive(:finally_do_without_wrapping).
          with(kind_of(Java::RxLangJruby::JRubyActionWrapper)).
          and_call_original

      observable.finally_do(lambda {})
    end

    it "passes a block through as the last argument" do
      proc = lambda {}
      observable.should_receive(:subscribe_without_wrapping).with(1, 1, 1, 1, proc)
      observable.subscribe(1, 1, 1, 1, &proc) # intentionally bogus call so it doesn't wrap the proc
    end
  end
end
