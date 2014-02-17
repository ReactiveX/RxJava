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

    public OnErrorThrowable(Throwable exception) {
        super(exception);
        hasValue = false;
        this.value = null;
    }

    public OnErrorThrowable(Throwable exception, Object value) {
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
        if (t instanceof OnErrorThrowable) {
            return (OnErrorThrowable) t;
        } else {
            return new OnErrorThrowable(t);
        }
    }
}
