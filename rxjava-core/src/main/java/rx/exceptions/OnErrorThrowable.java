/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.exceptions;

public class OnErrorThrowable extends RuntimeException {

    private static final long serialVersionUID = -569558213262703934L;

    private final boolean hasValue;
    private final Object value;

    private OnErrorThrowable(Throwable exception) {
        super(exception);
        hasValue = false;
        this.value = null;
    }

    private OnErrorThrowable(Throwable exception, Object value) {
        super(exception);
        hasValue = true;
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public boolean isValueNull() {
        return hasValue;
    }

    public static OnErrorThrowable from(Throwable t) {
        Throwable cause = Exceptions.getFinalCause(t);
        if (cause instanceof OnErrorThrowable.OnNextValue) {
            return new OnErrorThrowable(t, ((OnNextValue) cause).getValue());
        } else {
            return new OnErrorThrowable(t);
        }
    }

    /**
     * Adds the given value as the final cause of the given Throwable wrapped in OnNextValue RuntimeException..
     * 
     * @param e
     * @param value
     * @return Throwable e passed in
     */
    public static Throwable addValueAsLastCause(Throwable e, Object value) {
        Throwable lastCause = Exceptions.getFinalCause(e);
        if (lastCause != null && lastCause instanceof OnNextValue) {
            // purposefully using == for object reference check
            if (((OnNextValue) lastCause).getValue() == value) {
                // don't add another
                return e;
            }
        }
        Exceptions.addCause(e, new OnNextValue(value));
        return e;
    }

    public static class OnNextValue extends RuntimeException {

        private static final long serialVersionUID = -3454462756050397899L;
        private final Object value;

        public OnNextValue(Object value) {
            super("OnError while emitting onNext value: " + value);
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

    }
}
