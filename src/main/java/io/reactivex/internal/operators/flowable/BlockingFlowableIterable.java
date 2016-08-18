/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.flowable;

import java.util.Iterator;

import org.reactivestreams.Publisher;

public final class BlockingFlowableIterable<T> implements Iterable<T> {
    final Publisher<? extends T> source;
    
    final int bufferSize;
    
    public BlockingFlowableIterable(Publisher<? extends T> source, int bufferSize) {
        this.source = source;
        this.bufferSize = bufferSize;
    }

    @Override
    public Iterator<T> iterator() {
        BlockingFlowableIterator<T> it = new BlockingFlowableIterator<T>(bufferSize);
        source.subscribe(it);
        return it;
    }
}