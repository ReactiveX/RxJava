# Copyright 2013 Mike Ragalie
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

require_relative "spec_helper"

describe Java::RxLangJruby::JRubyFunctionWrapper do
  let(:spy) { double(:spy, :call => nil) }
  subject { described_class.new(JRuby.runtime.get_current_context, lambda {|*args| spy.call(args); args}) }

  let(:interfaces) do
    [Java::RxUtilFunctions::Func0,
     Java::RxUtilFunctions::Func1,
     Java::RxUtilFunctions::Func2,
     Java::RxUtilFunctions::Func3,
     Java::RxUtilFunctions::Func4,
     Java::RxUtilFunctions::Func5,
     Java::RxUtilFunctions::Func6,
     Java::RxUtilFunctions::Func7,
     Java::RxUtilFunctions::Func8,
     Java::RxUtilFunctions::Func9,
     Java::RxUtilFunctions::FuncN]
  end

  it "implements the interfaces" do
    interfaces.each do |interface|
      subject.is_a?(interface)
    end
  end

  it "successfully uses the interfaces" do
    spy.should_receive(:call).with([])
    spy.should_receive(:call).with([1])
    spy.should_receive(:call).with([1, 2])
    spy.should_receive(:call).with([1, 2, 3])
    spy.should_receive(:call).with([1, 2, 3, 4])
    spy.should_receive(:call).with([1, 2, 3, 4, 5])
    spy.should_receive(:call).with([1, 2, 3, 4, 5, 6])
    spy.should_receive(:call).with([1, 2, 3, 4, 5, 6, 7])
    spy.should_receive(:call).with([1, 2, 3, 4, 5, 6, 7, 8])
    spy.should_receive(:call).with([1, 2, 3, 4, 5, 6, 7, 8, 9])
    spy.should_receive(:call).with([1, 2, 3, 4, 5, 6, 7, 8, 9, 10])

    subject.call.should == []
    subject.call(1).should == [1]
    subject.call(1, 2).should == [1, 2]
    subject.call(1, 2, 3).should == [1, 2, 3]
    subject.call(1, 2, 3, 4).should == [1, 2, 3, 4]
    subject.call(1, 2, 3, 4, 5).should == [1, 2, 3, 4, 5]
    subject.call(1, 2, 3, 4, 5, 6).should == [1, 2, 3, 4, 5, 6]
    subject.call(1, 2, 3, 4, 5, 6, 7).should == [1, 2, 3, 4, 5, 6, 7]
    subject.call(1, 2, 3, 4, 5, 6, 7, 8).should == [1, 2, 3, 4, 5, 6, 7, 8]
    subject.call(1, 2, 3, 4, 5, 6, 7, 8, 9).should == [1, 2, 3, 4, 5, 6, 7, 8, 9]
    subject.call(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).should == [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
  end
end
