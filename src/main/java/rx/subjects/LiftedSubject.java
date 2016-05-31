/**
 * Copyright 2016 Netflix, Inc.
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

package rx.subjects;

import rx.*;

/**
 * Wraps another Subject and applies Operators to the front and back side of it. 
 * <p>
 * This class allows composing Subjects with front and back-behavior and remain in the
 * Subject world.
 * 
 * @param <T> the new front value type
 * @param <U> the input value type of the wrapped subject
 * @param <V> the output value type of the wrapped subject
 * @param <R> the new output type
 */
final class LiftedSubject<T, U, V, R> extends Subject<T, R> {
    
    /**
     * Constructs a new subject wrapper around the given Subject and
     * applies the specified front and back operators to it while still acting
     * as a self-contained Subject.
     * <p>
     * Note that the same sequential requirement applies to the returned Subject instance
     * as any other Subject. In addition, one should avoid calling onXXX methods on the
     * wrapped Subject and the returned Subject instance concurrently as this may lead
     * to undefined behavior.
     * 
     * @param <T> the new front value type
     * @param <U> the input value type of the wrapped subject
     * @param <V> the output value type of the wrapped subject
     * @param <R> the new output type
     * @param actual the actual subject
     * @param front the operator to apply to the input side
     * @param back the operator to apply to the output side
     * @return the new Subject wrapper
     */
    public static <T, U, V, R> Subject<T, R> create(
            final Subject<U, V> actual, 
            Operator<U, T> front, 
            Operator<R, V> back) {
        if (actual == null) {
            throw new NullPointerException("actual is null");
        }
        if (front == null) {
            throw new NullPointerException("front is null");
        }
        if (back == null) {
            throw new NullPointerException("back is null");
        }
        Subscriber<? super T> frontSubscriber = front.call(new Subscriber<U>() {
            @Override
            public void onNext(U t) {
                actual.onNext(t);
            }
            
            @Override
            public void onError(Throwable e) {
                actual.onError(e);
            }
            
            @Override
            public void onCompleted() {
                actual.onCompleted();
            }
        });
        
        if (frontSubscriber == null) {
            throw new NullPointerException("The operator " + front + " returned a null Subscriber");
        }
        
        State<T, U, V, R> state = new State<T, U, V, R>(actual, frontSubscriber, back);
        return new LiftedSubject<T, U, V, R>(state);
    }
    
    final State<T, U, V, R> state;
    
    private LiftedSubject(State<T, U, V, R> state) {
        super(state);
        this.state = state;
    }
    
    @Override
    public boolean hasObservers() {
        return state.subject.hasObservers();
    }
    
    @Override
    public void onNext(T t) {
        state.onNext(t);
    }
    
    @Override
    public void onError(Throwable e) {
        state.onError(e);
    }
    
    @Override
    public void onCompleted() {
        state.onCompleted();
    }
    
    /**
     * Holds onto the wrapped subject, the input operator's Subscriber and the output
     * operator (which has to be lifted for every incoming Subscriber).
     *
     * @param <T> the new front value type
     * @param <U> the input value type of the wrapped subject
     * @param <V> the output value type of the wrapped subject
     * @param <R> the new output type
     */
    static final class State<T, U, V, R> extends Subscriber<T> implements OnSubscribe<R> {
        
        final Subject<U, V> subject;
        
        final Subscriber<? super T> frontSubscriber;
        
        final Operator<R, V> backOperator;
        
        public State(Subject<U, V> subject, Subscriber<? super T> frontSubscriber, 
                Operator<R, V> backOperator) {
            this.subject = subject;
            this.frontSubscriber = frontSubscriber;
            this.backOperator = backOperator;
        }

        @Override
        public void call(Subscriber<? super R> t) {
            this.subject.lift(backOperator).unsafeSubscribe(t);
        }
        
        @Override
        public void onNext(T t) {
            this.frontSubscriber.onNext(t);
        }
        
        @Override
        public void onError(Throwable e) {
            this.frontSubscriber.onError(e);
        }
        
        @Override
        public void onCompleted() {
            this.frontSubscriber.onCompleted();
        }
    }
    
    /** The single instance of the identity Operator. */
    enum IdentityOperator implements Operator<Object, Object> {
        INSTANCE
        ;
        
        @Override
        public Subscriber<? super Object> call(Subscriber<? super Object> t) {
            return t;
        }
    }
    /**
     * Returns the identity operator that returns the passed Subscriber (i.e., the downstream
     * Subscriber passes through directly).
     * @param <T> the value type
     * @return the identity operator
     */
    @SuppressWarnings("unchecked")
    public static <T> Operator<T, T> identity() {
        return (Operator<T, T>)IdentityOperator.INSTANCE;
    }
}
