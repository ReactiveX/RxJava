/**
 * Copyright 2013 Netflix, Inc.
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

package rx.operators;

import rx.Observable;
import rx.Scheduler;
import rx.subjects.AsyncSubject;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Action2;
import rx.util.functions.Action3;
import rx.util.functions.Action4;
import rx.util.functions.Action5;
import rx.util.functions.Action6;
import rx.util.functions.Action7;
import rx.util.functions.Action8;
import rx.util.functions.Action9;
import rx.util.functions.ActionN;
import rx.util.functions.Actions;
import rx.util.functions.Func0;
import rx.util.functions.Func1;
import rx.util.functions.Func2;
import rx.util.functions.Func3;
import rx.util.functions.Func4;
import rx.util.functions.Func5;
import rx.util.functions.Func6;
import rx.util.functions.Func7;
import rx.util.functions.Func8;
import rx.util.functions.Func9;
import rx.util.functions.FuncN;

/**
 * Convert an action or function call into an asynchronous operation
 * through an Observable.
 */
public final class OperationToAsync {
    private OperationToAsync() { throw new IllegalStateException("No instances!"); }
    /**
     * Action0 with Scheduler.
     */
    public static Func0<Observable<Void>> toAsync(final Action0 action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    /**
     * Func0 with Scheduler.
     */
    public static <R> Func0<Observable<R>> toAsync(final Func0<R> func, final Scheduler scheduler) {
        return new Func0<Observable<R>>() {
            @Override
            public Observable<R> call() {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call();
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result);
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }
    /**
     * Action1 with Scheduler.
     */
    public static <T1> Func1<T1, Observable<Void>> toAsync(final Action1<T1> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    /**
     * Func1 with Scheduler.
     */
    public static <T1, R> Func1<T1, Observable<R>> toAsync(final Func1<T1, R> func, final Scheduler scheduler) {
        return new Func1<T1, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result);
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }
    /**
     * Action2 with Scheduler.
     */
    public static <T1, T2> Func2<T1, T2, Observable<Void>> toAsync(final Action2<T1, T2> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    /**
     * Func2 with Scheduler.
     */
    public static <T1, T2, R> Func2<T1, T2, Observable<R>> toAsync(final Func2<T1, T2, R> func, final Scheduler scheduler) {
        return new Func2<T1, T2, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result);
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }
    /**
     * Action3 with Scheduler.
     */
    public static <T1, T2, T3> Func3<T1, T2, T3, Observable<Void>> toAsync(final Action3<T1, T2, T3> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    /**
     * Func3 with Scheduler.
     */
    public static <T1, T2, T3, R> Func3<T1, T2, T3, Observable<R>> toAsync(final Func3<T1, T2, T3, R> func, final Scheduler scheduler) {
        return new Func3<T1, T2, T3, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result); // Rx.NET: null value ?
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }
    /**
     * Action4 with Scheduler.
     */
    public static <T1, T2, T3, T4> Func4<T1, T2, T3, T4, Observable<Void>> toAsync(final Action4<T1, T2, T3, T4> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    /**
     * Func4 with Scheduler.
     */
    public static <T1, T2, T3, T4, R> Func4<T1, T2, T3, T4, Observable<R>> toAsync(final Func4<T1, T2, T3, T4, R> func, final Scheduler scheduler) {
        return new Func4<T1, T2, T3, T4, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3, final T4 t4) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3, t4);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result); // Rx.NET: null value ?
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }
    /**
     * Action5 with Scheduler.
     */
    public static <T1, T2, T3, T4, T5> Func5<T1, T2, T3, T4, T5, Observable<Void>> toAsync(final Action5<T1, T2, T3, T4, T5> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    /**
     * Func5 with Scheduler.
     */
    public static <T1, T2, T3, T4, T5, R> Func5<T1, T2, T3, T4, T5, Observable<R>> toAsync(final Func5<T1, T2, T3, T4, T5, R> func, final Scheduler scheduler) {
        return new Func5<T1, T2, T3, T4, T5, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3, t4, t5);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result); // Rx.NET: null value ?
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }
    /**
     * Action6 with Scheduler.
     */
    public static <T1, T2, T3, T4, T5, T6> Func6<T1, T2, T3, T4, T5, T6, Observable<Void>> toAsync(final Action6<T1, T2, T3, T4, T5, T6> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    /**
     * Func6 with Scheduler.
     */
    public static <T1, T2, T3, T4, T5, T6, R> Func6<T1, T2, T3, T4, T5, T6, Observable<R>> toAsync(final Func6<T1, T2, T3, T4, T5, T6, R> func, final Scheduler scheduler) {
        return new Func6<T1, T2, T3, T4, T5, T6, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5, final T6 t6) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3, t4, t5, t6);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result); // Rx.NET: null value ?
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }
    /**
     * Action7 with Scheduler.
     */
    public static <T1, T2, T3, T4, T5, T6, T7> Func7<T1, T2, T3, T4, T5, T6, T7, Observable<Void>> toAsync(final Action7<T1, T2, T3, T4, T5, T6, T7> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    /**
     * Func7 with Scheduler.
     */
    public static <T1, T2, T3, T4, T5, T6, T7, R> Func7<T1, T2, T3, T4, T5, T6, T7, Observable<R>> toAsync(final Func7<T1, T2, T3, T4, T5, T6, T7, R> func, final Scheduler scheduler) {
        return new Func7<T1, T2, T3, T4, T5, T6, T7, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5, final T6 t6, final T7 t7) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3, t4, t5, t6, t7);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result); // Rx.NET: null value ?
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }
    /**
     * Action8 with Scheduler.
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> Func8<T1, T2, T3, T4, T5, T6, T7, T8, Observable<Void>> toAsync(final Action8<T1, T2, T3, T4, T5, T6, T7, T8> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    /**
     * Func8 with Scheduler.
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Func8<T1, T2, T3, T4, T5, T6, T7, T8, Observable<R>> toAsync(final Func8<T1, T2, T3, T4, T5, T6, T7, T8, R> func, final Scheduler scheduler) {
        return new Func8<T1, T2, T3, T4, T5, T6, T7, T8, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5, final T6 t6, final T7 t7, final T8 t8) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3, t4, t5, t6, t7, t8);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result); // Rx.NET: null value ?
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }
    /**
     * Action9 with Scheduler.
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Func9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Observable<Void>> toAsync(final Action9<T1, T2, T3, T4, T5, T6, T7, T8, T9> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    /**
     * Func9 with Scheduler.
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Func9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Observable<R>> toAsync(final Func9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> func, final Scheduler scheduler) {
        return new Func9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5, final T6 t6, final T7 t7, final T8 t8, final T9 t9) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3, t4, t5, t6, t7, t8, t9);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result); // Rx.NET: null value ?
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }
    /**
     * ActionN with Scheduler.
     */
    public static FuncN<Observable<Void>> toAsync(final ActionN action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    /**
     * FuncN with Scheduler.
     */
    public static <R> FuncN<Observable<R>> toAsync(final FuncN<R> func, final Scheduler scheduler) {
        return new FuncN<Observable<R>>() {
            @Override
            public Observable<R> call(final Object... args) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(args);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result);
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }
}
