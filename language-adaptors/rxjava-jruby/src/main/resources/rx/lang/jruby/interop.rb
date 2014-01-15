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

module Rx
  module Lang
    module Jruby
      class Interop
        WRAPPERS = {
          Java::RxUtilFunctions::Action         => Java::RxLangJruby::JRubyActionWrapper,
          Java::Rx::IObservable => false
        }

        WRAPPERS.default = Java::RxLangJruby::JRubyFunctionWrapper

        KLASSES  = [Java::Rx::Observable, Java::RxObservables::BlockingObservable]
        FUNCTION = Java::RxUtilFunctions::Function.java_class
        RUNTIME  = JRuby.runtime

        def self.instance
          @instance ||= new
        end

        def initialize
          KLASSES.each do |klass|
            function_methods = (klass.java_class.declared_instance_methods + klass.java_class.declared_class_methods).select do |method|
              method.public? && method.parameter_types.any? {|type| FUNCTION.assignable_from?(type)}
            end

            parameter_types = {}
            function_methods.each do |method|
              parameter_types[[method.name, method.static?]] ||= []

              method.parameter_types.each_with_index do |type, idx|
                next unless FUNCTION.assignable_from?(type)

                constructor = WRAPPERS.find do |java_class, wrapper|
                  type.ruby_class.ancestors.include?(java_class)
                end

                # Skip OnSubscribeFuncs
                next if constructor && constructor.last == false

                constructor = (constructor && constructor.last) || WRAPPERS.default

                parameter_types[[method.name, method.static?]][idx] ||= []
                parameter_types[[method.name, method.static?]][idx] << constructor
              end
            end

            parameter_types.each_pair do |_, types|
              types.map! do |type|
                next type.first if type && type.uniq.length == 1
                nil
              end
            end

            parameter_types.each_pair do |(method_name, static), types|
              next if types.all?(&:nil?)

              klass_to_open = static ? klass.singleton_class : klass

              [method_name, underscore(method_name)].uniq.each do |name|
                klass_to_open.send(:alias_method, "#{name}_without_wrapping", name)
                klass_to_open.send(:define_method, name) do |*args, &block|
                  context = RUNTIME.get_current_context

                  args = args.each_with_index.map do |arg, idx|
                    if arg.is_a?(Proc) && types[idx]
                      types[idx].new(context, arg)
                    else
                      arg
                    end
                  end

                  if block && types[args.length]
                    block = types[args.length].new(context, block)
                  end

                  send("#{name}_without_wrapping", *(args + [block].compact))
                end
              end
            end
          end
        end

        private

        # File activesupport/lib/active_support/inflector/methods.rb, line 89
        def underscore(camel_cased_word)
          word = camel_cased_word.to_s.dup
          word.gsub!('::', '/')
          word.gsub!(/(?:([A-Za-z\d])|^)((?=a)b)(?=\b|[^a-z])/) { "#{$1}#{$1 && '_'}#{$2.downcase}" }
          word.gsub!(/([A-Z\d]+)([A-Z][a-z])/,'\1_\2')
          word.gsub!(/([a-z\d])([A-Z])/,'\1_\2')
          word.tr!("-", "_")
          word.downcase!
          word
        end
      end
    end
  end
end

Rx::Lang::Jruby::Interop.instance
