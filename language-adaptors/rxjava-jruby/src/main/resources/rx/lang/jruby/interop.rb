module Rx
  module Lang
    module Jruby
      class Interop
        WRAPPERS = {
          Java::RxUtilFunctions::Action         => Java::RxLangJruby::JRubyActionWrapper
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

              klass_to_open.send(:alias_method, "#{method_name}_without_wrapping", method_name)
              klass_to_open.send(:define_method, method_name) do |*args, &block|
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

                send("#{method_name}_without_wrapping", *(args + [block].compact))
              end
            end
          end
        end
      end
    end
  end
end

Rx::Lang::Jruby::Interop.instance
