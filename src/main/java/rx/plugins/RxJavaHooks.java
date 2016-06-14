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
package rx.plugins;

import rx.*;
import rx.Completable.CompletableOnSubscribe;
import rx.Observable.*;
import rx.annotations.Experimental;
import rx.functions.*;
import rx.internal.operators.*;

/**
 * Utility class that holds hooks for various Observable, Single and Completable lifecycle-related
 * points as well as Scheduler hooks.
 */
@Experimental
public final class RxJavaHooks {
    /** Utility class. */
    private RxJavaHooks() {
        throw new IllegalStateException("No instances!");
    }
    
    /**
     * Prevents changing the hook callbacks when set to true.
     */
    /* test */ static volatile boolean lockdown;
    
    static volatile Action1<Throwable> onError;
    
    @SuppressWarnings("rawtypes")
    static volatile Func1<Observable.OnSubscribe, Observable.OnSubscribe> onObservableCreate;

    @SuppressWarnings("rawtypes")
    static volatile Func1<Single.OnSubscribe, Single.OnSubscribe> onSingleCreate;

    static volatile Func1<Completable.CompletableOnSubscribe, Completable.CompletableOnSubscribe> onCompletableCreate;

    @SuppressWarnings("rawtypes")
    static volatile Func2<Observable, Observable.OnSubscribe, Observable.OnSubscribe> onObservableStart;

    @SuppressWarnings("rawtypes")
    static volatile Func2<Single, Observable.OnSubscribe, Observable.OnSubscribe> onSingleStart;

    static volatile Func2<Completable, Completable.CompletableOnSubscribe, Completable.CompletableOnSubscribe> onCompletableStart;

    
    static volatile Func1<Scheduler, Scheduler> onComputationScheduler;

    static volatile Func1<Scheduler, Scheduler> onIOScheduler;

    static volatile Func1<Scheduler, Scheduler> onNewThreadScheduler;
    
    static volatile Func1<Action0, Action0> onScheduleAction;

    static volatile Func1<Subscription, Subscription> onObservableReturn;

    static volatile Func1<Subscription, Subscription> onSingleReturn;

    /** Initialize with the default delegation to the original RxJavaPlugins. */
    static {
        init();
    }
    
    /**
     * Initialize the hooks via delegating to RxJavaPlugins.
     */
    @SuppressWarnings({ "rawtypes", "unchecked", "deprecation"})
    static void init() {
        onError = new Action1<Throwable>() {
            @Override
            public void call(Throwable e) {
                RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
            }
        };
        
        RxJavaPlugins.getInstance().getObservableExecutionHook();
        
        onObservableCreate = new Func1<OnSubscribe, OnSubscribe>() {
            @Override
            public OnSubscribe call(OnSubscribe f) {
                return RxJavaPlugins.getInstance().getObservableExecutionHook().onCreate(f);
            }
        };
        
        onObservableStart = new Func2<Observable, OnSubscribe, OnSubscribe>() {
            @Override
            public OnSubscribe call(Observable t1, OnSubscribe t2) {
                return RxJavaPlugins.getInstance().getObservableExecutionHook().onSubscribeStart(t1, t2);
            }
        };
        
        onObservableReturn = new Func1<Subscription, Subscription>() {
            @Override
            public Subscription call(Subscription f) {
                return RxJavaPlugins.getInstance().getObservableExecutionHook().onSubscribeReturn(f);
            }
        };
        
        RxJavaPlugins.getInstance().getSingleExecutionHook();

        onSingleCreate = new Func1<rx.Single.OnSubscribe, rx.Single.OnSubscribe>() {
            @Override
            public rx.Single.OnSubscribe call(rx.Single.OnSubscribe f) {
                return RxJavaPlugins.getInstance().getSingleExecutionHook().onCreate(f);
            }
        };
        
        onSingleStart = new Func2<Single, Observable.OnSubscribe, Observable.OnSubscribe>() {
            @Override
            public Observable.OnSubscribe call(Single t1, Observable.OnSubscribe t2) {
                return RxJavaPlugins.getInstance().getSingleExecutionHook().onSubscribeStart(t1, t2);
            }
        };
        
        onSingleReturn = new Func1<Subscription, Subscription>() {
            @Override
            public Subscription call(Subscription f) {
                return RxJavaPlugins.getInstance().getSingleExecutionHook().onSubscribeReturn(f);
            }
        };
        
        RxJavaPlugins.getInstance().getCompletableExecutionHook();

        onCompletableCreate = new Func1<CompletableOnSubscribe, CompletableOnSubscribe>() {
            @Override
            public CompletableOnSubscribe call(CompletableOnSubscribe f) {
                return RxJavaPlugins.getInstance().getCompletableExecutionHook().onCreate(f);
            }
        };
        
        onCompletableStart = new Func2<Completable, Completable.CompletableOnSubscribe, Completable.CompletableOnSubscribe>() {
            @Override
            public Completable.CompletableOnSubscribe call(Completable t1, Completable.CompletableOnSubscribe t2) {
                return RxJavaPlugins.getInstance().getCompletableExecutionHook().onSubscribeStart(t1, t2);
            }
        };

        onScheduleAction = new Func1<Action0, Action0>() {
            @Override
            public Action0 call(Action0 a) {
                return RxJavaPlugins.getInstance().getSchedulersHook().onSchedule(a);
            }
        };
    }
    
    /**
     * Reset all hook callbacks to those of the current RxJavaPlugins handlers.
     * 
     * @see #clear()
     */
    public static void reset() {
        if (lockdown) {
            return;
        }
        init();
    }

    /**
     * Clears all hooks to be no-operations (and passthroughs)
     * and onError hook to signal errors to the caller thread's
     * UncaughtExceptionHandler.
     * 
     * @see #reset()
     */
    public static void clear() {
        if (lockdown) {
            return;
        }
        onError = null;
        
        onObservableCreate = null;
        onObservableStart = null;
        onObservableReturn = null;
        
        onSingleCreate = null;
        onSingleStart = null;
        onSingleReturn = null;
        
        onCompletableCreate = null;
        onCompletableStart = null;
        
        onComputationScheduler = null;
        onIOScheduler = null;
        onNewThreadScheduler = null;
    }

    /**
     * Prevents changing a hooks.
     */
    public static void lockdown() {
        lockdown = true;
    }
    
    /**
     * Returns true if the hooks can no longer be changed.
     * @return true if the hooks can no longer be changed
     */
    public static boolean isLockdown() {
        return lockdown;
    }
    /**
     * Consume undeliverable Throwables (acts as a global catch).
     * @param ex the exception to handle
     */
    public static void onError(Throwable ex) {
        Action1<Throwable> f = onError;
        if (f != null) {
            f.call(ex);
            return;
        }
        Thread current = Thread.currentThread();
        current.getUncaughtExceptionHandler().uncaughtException(current, ex);
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Observable.OnSubscribe<T> onCreate(Observable.OnSubscribe<T> onSubscribe) {
        Func1<OnSubscribe, OnSubscribe> f = onObservableCreate;
        if (f != null) {
            return f.call(onSubscribe);
        }
        return onSubscribe;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Single.OnSubscribe<T> onCreate(Single.OnSubscribe<T> onSubscribe) {
        Func1<Single.OnSubscribe, Single.OnSubscribe> f = onSingleCreate;
        if (f != null) {
            return f.call(onSubscribe);
        }
        return onSubscribe;
    }

    public static <T> Completable.CompletableOnSubscribe onCreate(Completable.CompletableOnSubscribe onSubscribe) {
        Func1<Completable.CompletableOnSubscribe, Completable.CompletableOnSubscribe> f = onCompletableCreate;
        if (f != null) {
            return f.call(onSubscribe);
        }
        return onSubscribe;
    }
    
    public static Scheduler onComputationScheduler(Scheduler scheduler) {
        Func1<Scheduler, Scheduler> f = onComputationScheduler;
        if (f != null) {
            return f.call(scheduler);
        }
        return scheduler;
    }

    public static Scheduler onIOScheduler(Scheduler scheduler) {
        Func1<Scheduler, Scheduler> f = onIOScheduler;
        if (f != null) {
            return f.call(scheduler);
        }
        return scheduler;
    }

    public static Scheduler onNewThreadScheduler(Scheduler scheduler) {
        Func1<Scheduler, Scheduler> f = onNewThreadScheduler;
        if (f != null) {
            return f.call(scheduler);
        }
        return scheduler;
    }

    public static Action0 onScheduledAction(Action0 action) {
        Func1<Action0, Action0> f = onScheduleAction;
        if (f != null) {
            return f.call(action);
        }
        return action;
    }
    
    public static void setOnCompletableCreate(
            Func1<Completable.CompletableOnSubscribe, Completable.CompletableOnSubscribe> onCompletableCreate) {
        if (lockdown) {
            return;
        }
        RxJavaHooks.onCompletableCreate = onCompletableCreate;
    }
    
    public static void setOnComputationScheduler(Func1<Scheduler, Scheduler> onComputationScheduler) {
        if (lockdown) {
            return;
        }
        RxJavaHooks.onComputationScheduler = onComputationScheduler;
    }
    
    public static void setOnError(Action1<Throwable> onError) {
        if (lockdown) {
            return;
        }
        RxJavaHooks.onError = onError;
    }
    
    public static void setOnIOScheduler(Func1<Scheduler, Scheduler> onIOScheduler) {
        if (lockdown) {
            return;
        }
        RxJavaHooks.onIOScheduler = onIOScheduler;
    }
    
    public static void setOnNewThreadScheduler(Func1<Scheduler, Scheduler> onNewThreadScheduler) {
        if (lockdown) {
            return;
        }
        RxJavaHooks.onNewThreadScheduler = onNewThreadScheduler;
    }
    
    @SuppressWarnings("rawtypes")
    public static void setOnObservableCreate(
            Func1<Observable.OnSubscribe, Observable.OnSubscribe> onObservableCreate) {
        if (lockdown) {
            return;
        }
        RxJavaHooks.onObservableCreate = onObservableCreate;
    }
    
    public static void setOnScheduleAction(Func1<Action0, Action0> onScheduleAction) {
        if (lockdown) {
            return;
        }
        RxJavaHooks.onScheduleAction = onScheduleAction;
    }
    
    @SuppressWarnings("rawtypes")
    public static void setOnSingleCreate(Func1<Single.OnSubscribe, Single.OnSubscribe> onSingleCreate) {
        if (lockdown) {
            return;
        }
        RxJavaHooks.onSingleCreate = onSingleCreate;
    }
    
    public static void setOnCompletableStart(
            Func2<Completable, Completable.CompletableOnSubscribe, Completable.CompletableOnSubscribe> onCompletableStart) {
        if (lockdown) {
            return;
        }
        RxJavaHooks.onCompletableStart = onCompletableStart;
    }
    
    @SuppressWarnings("rawtypes")
    public static void setOnObservableStart(
            Func2<Observable, Observable.OnSubscribe, Observable.OnSubscribe> onObservableStart) {
        if (lockdown) {
            return;
        }
        RxJavaHooks.onObservableStart = onObservableStart;
    }
    
    @SuppressWarnings("rawtypes")
    public static void setOnSingleStart(Func2<Single, Observable.OnSubscribe, Observable.OnSubscribe> onSingleStart) {
        if (lockdown) {
            return;
        }
        RxJavaHooks.onSingleStart = onSingleStart;
    }
    
    public static void setOnObservableReturn(Func1<Subscription, Subscription> onObservableReturn) {
        if (lockdown) {
            return;
        }
        RxJavaHooks.onObservableReturn = onObservableReturn;
    }
    
    public static void setOnSingleReturn(Func1<Subscription, Subscription> onSingleReturn) {
        if (lockdown) {
            return;
        }
        RxJavaHooks.onSingleReturn = onSingleReturn;
    }
    
    public static Func1<Scheduler, Scheduler> getOnComputationScheduler() {
        return onComputationScheduler;
    }
    
    public static Action1<Throwable> getOnError() {
        return onError;
    }
    
    public static Func1<Scheduler, Scheduler> getOnIOScheduler() {
        return onIOScheduler;
    }
    
    public static Func1<Scheduler, Scheduler> getOnNewThreadScheduler() {
        return onNewThreadScheduler;
    }
    
    @SuppressWarnings("rawtypes")
    public static Func1<Observable.OnSubscribe, Observable.OnSubscribe> getOnObservableCreate() {
        return onObservableCreate;
    }
    
    public static Func1<Action0, Action0> getOnScheduleAction() {
        return onScheduleAction;
    }
    
    @SuppressWarnings("rawtypes")
    public static Func1<Single.OnSubscribe, Single.OnSubscribe> getOnSingleCreate() {
        return onSingleCreate;
    }
    
    public static Func1<Completable.CompletableOnSubscribe, Completable.CompletableOnSubscribe> getOnCompletableCreate() {
        return onCompletableCreate;
    }
    
    public static Func2<Completable, Completable.CompletableOnSubscribe, Completable.CompletableOnSubscribe> getOnCompletableStart() {
        return onCompletableStart;
    }
    
    @SuppressWarnings("rawtypes")
    public static Func2<Observable, Observable.OnSubscribe, Observable.OnSubscribe> getOnObservableStart() {
        return onObservableStart;
    }
    
    @SuppressWarnings("rawtypes")
    public static Func2<Single, Observable.OnSubscribe, Observable.OnSubscribe> getOnSingleStart() {
        return onSingleStart;
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> OnSubscribe<T> onObservableStart(Observable<T> instance, OnSubscribe<T> onSubscribe) {
        Func2<Observable, OnSubscribe, OnSubscribe> f = onObservableStart;
        if (f != null) {
            return f.call(instance, onSubscribe);
        }
        return onSubscribe;
    }
    
    public static Func1<Subscription, Subscription> getOnObservableReturn() {
        return onObservableReturn;
    }
    
    public static Func1<Subscription, Subscription> getOnSingleReturn() {
        return onSingleReturn;
    }
    
    public static Subscription onObservableReturn(Subscription subscription) {
        Func1<Subscription, Subscription> f = onObservableReturn;
        if (f != null) {
            return f.call(subscription);
        }
        return subscription;
    }

    public static Throwable onObservableError(Throwable error) {
        // TODO add hook
        return error;
    }

    public static <T, R> Operator<R, T> onObservableLift(Operator<R, T> operator) {
        // TODO add hook
        return operator;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Observable.OnSubscribe<T> onSingleStart(Single<T> instance, Observable.OnSubscribe<T> onSubscribe) {
        Func2<Single, Observable.OnSubscribe, Observable.OnSubscribe> f = onSingleStart;
        if (f != null) {
            return f.call(instance, onSubscribe);
        }
        return onSubscribe;
    }

    public static Subscription onSingleReturn(Subscription subscription) {
        Func1<Subscription, Subscription> f = onSingleReturn;
        if (f != null) {
            return f.call(subscription);
        }
        return subscription;
    }

    public static Throwable onSingleError(Throwable error) {
        // TODO add hook
        return error;
    }

    public static <T, R> Operator<R, T> onSingleLift(Operator<R, T> operator) {
        // TODO add hook
        return operator;
    }

    public static <T> Completable.CompletableOnSubscribe onCompletableStart(Completable instance, Completable.CompletableOnSubscribe onSubscribe) {
        Func2<Completable, CompletableOnSubscribe, CompletableOnSubscribe> f = onCompletableStart;
        if (f != null) {
            return f.call(instance, onSubscribe);
        }
        return onSubscribe;
    }

    public static Throwable onCompletableError(Throwable error) {
        // TODO add hook
        return error;
    }

    public static <T, R> Completable.CompletableOperator onCompletableLift(Completable.CompletableOperator operator) {
        // TODO add hook
        return operator;
    }
    
    /**
     * Resets the assembly tracking hooks to their default delegates to
     * RxJavaPlugins.
     */
    @SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
    public static void resetAssemblyTracking() {
        if (lockdown) {
            return;
        }

        RxJavaPlugins plugin = RxJavaPlugins.getInstance();
        
        final RxJavaObservableExecutionHook observableExecutionHook = plugin.getObservableExecutionHook();
        
        onObservableCreate = new Func1<OnSubscribe, OnSubscribe>() {
            @Override
            public OnSubscribe call(OnSubscribe f) {
                return observableExecutionHook.onCreate(f);
            }
        };
        
        final RxJavaSingleExecutionHook singleExecutionHook = plugin.getSingleExecutionHook();

        onSingleCreate = new Func1<rx.Single.OnSubscribe, rx.Single.OnSubscribe>() {
            @Override
            public rx.Single.OnSubscribe call(rx.Single.OnSubscribe f) {
                return singleExecutionHook.onCreate(f);
            }
        };
        
        final RxJavaCompletableExecutionHook completableExecutionHook = plugin.getCompletableExecutionHook();

        onCompletableCreate = new Func1<CompletableOnSubscribe, CompletableOnSubscribe>() {
            @Override
            public CompletableOnSubscribe call(CompletableOnSubscribe f) {
                return completableExecutionHook.onCreate(f);
            }
        };

    }

    /**
     * Clears the assembly tracking hooks to their default pass-through behavior.
     */
    public static void crearAssemblyTracking() {
        if (lockdown) {
            return;
        }
        onObservableCreate = null;
        onSingleCreate = null;
        onCompletableCreate = null;
    }

    /**
     * Sets up hooks that capture the current stacktrace when a source or an
     * operator is instantiated, keeping it in a field for debugging purposes
     * and alters exceptions passign along to hold onto this stacktrace.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void enableAssemblyTracking() {
        if (lockdown) {
            return;
        }
        
        onObservableCreate = new Func1<OnSubscribe, OnSubscribe>() {
            @Override
            public OnSubscribe call(OnSubscribe f) {
                return new OnSubscribeOnAssembly(f);
            }
        };

        onSingleCreate = new Func1<rx.Single.OnSubscribe, rx.Single.OnSubscribe>() {
            @Override
            public rx.Single.OnSubscribe call(rx.Single.OnSubscribe f) {
                return new OnSubscribeOnAssemblySingle(f);
            }
        };

        onCompletableCreate = new Func1<CompletableOnSubscribe, CompletableOnSubscribe>() {
            @Override
            public CompletableOnSubscribe call(CompletableOnSubscribe f) {
                return new OnSubscribeOnAssemblyCompletable(f);
            }
        };

    }
}
