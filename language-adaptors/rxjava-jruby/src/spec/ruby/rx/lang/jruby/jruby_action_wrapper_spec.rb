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

describe Java::RxLangJruby::JRubyActionWrapper do
  let(:spy) { double(:spy, :call => nil) }
  subject { described_class.new(JRuby.runtime.get_current_context, lambda {|*args| spy.call(args)}) }

  let(:interfaces) do
    [Java::RxUtilFunctions::Action,
     Java::RxUtilFunctions::Action0,
     Java::RxUtilFunctions::Action1,
     Java::RxUtilFunctions::Action2,
     Java::RxUtilFunctions::Action3]
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

    subject.call
    subject.call(1)
    subject.call(1, 2)
    subject.call(1, 2, 3)
  end
end
