/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * An AtomicInteger with extra fields to pad it out to fit a typical cache line.
 */
public final class PaddedAtomicInteger extends AtomicInteger {
    private static final long serialVersionUID = 1L;
    /** Padding, public to prevent optimizing it away. */
    public int p1;
    /** Padding, public to prevent optimizing it away. */
    public int p2;
    /** Padding, public to prevent optimizing it away. */
    public int p3;
    /** Padding, public to prevent optimizing it away. */
    public int p4;
    /** Padding, public to prevent optimizing it away. */
    public int p5;
    /** Padding, public to prevent optimizing it away. */
    public int p6;
    /** Padding, public to prevent optimizing it away. */
    public int p7;
    /** Padding, public to prevent optimizing it away. */
    public int p8;
    /** Padding, public to prevent optimizing it away. */
    public int p9;
    /** Padding, public to prevent optimizing it away. */
    public int p10;
    /** Padding, public to prevent optimizing it away. */
    public int p11;
    /** Padding, public to prevent optimizing it away. */
    public int p12;
    /** Padding, public to prevent optimizing it away. */
    public int p13;
    /** @return prevents optimizing away the fields, most likely. */
    public int noopt() {
        return p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9 + p10 + p11 + p12 + p13;
    }
    
}
