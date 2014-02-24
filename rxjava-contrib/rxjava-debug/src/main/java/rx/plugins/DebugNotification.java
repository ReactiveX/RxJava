package rx.plugins;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Observer;
import rx.observers.SafeSubscriber;
import rx.operators.DebugSubscriber;

public class DebugNotification<T, C> {
    public static enum Kind {
        OnNext, OnError, OnCompleted, Subscribe, Unsubscribe
    }

    private final Observable<? extends T> source;
    private final OnSubscribe<T> sourceFunc;
    private final Operator<? extends T, ?> from;
    private final Kind kind;
    private final Operator<?, ? super T> to;
    private final Throwable throwable;
    private final T value;
    private final Observer observer;

    public static <T, C> DebugNotification<T, C> createSubscribe(Observer<? super T> o, Observable<? extends T> source, OnSubscribe<T> sourceFunc) {
        Operator<?, ? super T> to = null;
        Operator<? extends T, ?> from = null;
        if (o instanceof SafeSubscriber) {
            o = ((SafeSubscriber) o).getActual();
        }
        if (o instanceof DebugSubscriber) {
            to = ((DebugSubscriber<T, C>) o).getTo();
            from = ((DebugSubscriber<T, C>) o).getFrom();
            o = ((DebugSubscriber) o).getActual();
        }
        if (sourceFunc instanceof DebugHook.OnCreateWrapper) {
            sourceFunc = ((DebugHook.OnCreateWrapper) sourceFunc).getActual();
        }
        return new DebugNotification<T, C>(o, from, Kind.Subscribe, null, null, to, source, sourceFunc);
    }

    public static <T, C> DebugNotification<T, C> createOnNext(Observer<? super T> o, Operator<? extends T, ?> from, T t, Operator<?, ? super T> to) {
        return new DebugNotification<T, C>(o, from, Kind.OnNext, t, null, to, null, null);
    }

    public static <T, C> DebugNotification<T, C> createOnError(Observer<? super T> o, Operator<? extends T, ?> from, Throwable e, Operator<?, ? super T> to) {
        return new DebugNotification<T, C>(o, from, Kind.OnError, null, e, to, null, null);
    }

    public static <T, C> DebugNotification<T, C> createOnCompleted(Observer<? super T> o, Operator<? extends T, ?> from, Operator<?, ? super T> to) {
        return new DebugNotification<T, C>(o, from, Kind.OnCompleted, null, null, to, null, null);
    }

    public static <T, C> DebugNotification<T, C> createUnsubscribe(Observer<? super T> o, Operator<? extends T, ?> from, Operator<?, ? super T> to) {
        return new DebugNotification<T, C>(o, from, Kind.Unsubscribe, null, null, to, null, null);
    }

    private DebugNotification(Observer o, Operator<? extends T, ?> from, Kind kind, T value, Throwable throwable, Operator<?, ? super T> to, Observable<? extends T> source, OnSubscribe<T> sourceFunc) {
        this.observer = (o instanceof SafeSubscriber) ? ((SafeSubscriber) o).getActual() : o;
        this.from = from;
        this.kind = kind;
        this.value = value;
        this.throwable = throwable;
        this.to = to;
        this.source = source;
        this.sourceFunc = sourceFunc;
    }

    public Observer getObserver() {
        return observer;
    }

    public Operator<? extends T, ?> getFrom() {
        return from;
    }

    public T getValue() {
        return value;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public Operator<?, ? super T> getTo() {
        return to;
    }

    public Kind getKind() {
        return kind;
    }
    
    public Observable<? extends T> getSource() {
        return source;
    }
    
    public OnSubscribe<T> getSourceFunc() {
        return sourceFunc;
    }

    @Override
    /**
     * Does a very bad job of making JSON like string.
     */
    public String toString() {
        final StringBuilder s = new StringBuilder("{");
        s.append("\"observer\": \"").append(observer.getClass().getName()).append("@").append(Integer.toHexString(observer.hashCode())).append("\"");
        s.append(", \"type\": \"").append(kind).append("\"");
        if (kind == Kind.OnNext)
            // not json safe
            s.append(", \"value\": \"").append(value).append("\"");
        if (kind == Kind.OnError)
            s.append(", \"exception\": \"").append(throwable.getMessage().replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
        if (source != null)
            s.append(", \"source\": \"").append(source.getClass().getName()).append("@").append(Integer.toHexString(source.hashCode())).append("\"");
        if (sourceFunc != null)
            s.append(", \"sourceFunc\": \"").append(sourceFunc.getClass().getName()).append("@").append(Integer.toHexString(sourceFunc.hashCode())).append("\"");
        if (from != null)
            s.append(", \"from\": \"").append(from.getClass().getName()).append("@").append(Integer.toHexString(from.hashCode())).append("\"");
        if (to != null)
            s.append(", \"to\": \"").append(to.getClass().getName()).append("@").append(Integer.toHexString(to.hashCode())).append("\"");
        s.append("}");
        return s.toString();
    }
}
