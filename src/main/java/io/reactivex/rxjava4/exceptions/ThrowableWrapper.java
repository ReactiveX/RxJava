/*
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava4.exceptions;

import java.io.Serial;

/**
 * A runtime exception to sneak around checked exceptions.
 * <p>
 * If you encounter me, it means some operator forgot to unwrap the inner throwable
 * at the right place.
 * @since 4.0.0
 */
public final class ThrowableWrapper extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -5280780582536857320L;

    /**
     * Constructs an instance with the given non-null original Throwable.
     * @param original the original Throwable
     */
    public ThrowableWrapper(Throwable original) {
        super(original != null ? original : new NullPointerException("original is null"));
    }
}
