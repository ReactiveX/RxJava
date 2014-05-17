package rx.plugins;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Observer;
import rx.observers.SafeSubscriber;
import rx.operators.DebugSubscriber;

public class DebugNotification<T> {
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
    @SuppressWarnings("rawtypes")
    private final Observer observer;

    @SuppressWarnings("unchecked")
    public static <T, C> DebugNotification<T> createSubscribe(Observer<? super T> o, Observable<? extends T> source, OnSubscribe<T> sourceFunc) {
        Operator<?, ? super T> to = null;
        Operator<? extends T, ?> from = null;
        if (o instanceof SafeSubscriber) {
            o = ((SafeSubscriber<T>) o).getActual();
        }
        if (o instanceof DebugSubscriber) {
            @SuppressWarnings("rawtypes")
            final DebugSubscriber ds = (DebugSubscriber) o;
            to = ds.getTo();
            from = ds.getFrom();
            o = ds.getActual();
        }
        if (sourceFunc instanceof DebugHook.DebugOnSubscribe) {
            sourceFunc = (OnSubscribe<T>) ((SafeSubscriber<T>) sourceFunc).getActual();
        }
        return new DebugNotification<T>(o, from, Kind.Subscribe, null, null, to, source, sourceFunc);
    }

    public static <T> DebugNotification<T> createOnNext(Observer<? super T> o, Operator<? extends T, ?> from, T t, Operator<?, ? super T> to) {
        return new DebugNotification<T>(o, from, Kind.OnNext, t, null, to, null, null);
    }

    public static <T> DebugNotification<T> createOnError(Observer<? super T> o, Operator<? extends T, ?> from, Throwable e, Operator<?, ? super T> to) {
        return new DebugNotification<T>(o, from, Kind.OnError, null, e, to, null, null);
    }

    public static <T> DebugNotification<T> createOnCompleted(Observer<? super T> o, Operator<? extends T, ?> from, Operator<?, ? super T> to) {
        return new DebugNotification<T>(o, from, Kind.OnCompleted, null, null, to, null, null);
    }

    public static <T> DebugNotification<T> createUnsubscribe(Observer<? super T> o, Operator<? extends T, ?> from, Operator<?, ? super T> to) {
        return new DebugNotification<T>(o, from, Kind.Unsubscribe, null, null, to, null, null);
    }

    @SuppressWarnings("rawtypes")
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

    @SuppressWarnings("rawtypes")
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
        s.append("\"observer\": ");
        if (observer != null)
            s.append("\"").append(observer.getClass().getName()).append("@").append(Integer.toHexString(observer.hashCode())).append("\"");
        else
            s.append("null");
        s.append(", \"type\": \"").append(kind).append("\"");
        if (kind == Kind.OnNext)
            s.append(", \"value\": ").append(quote(value)).append("");
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

    public static String quote(Object obj) {
        if (obj == null) {
            return "null";
        }

        String string;
        try {
            string = obj.toString();
        } catch (Throwable e) {
            return "\"\"";
        }
        if (string == null || string.length() == 0) {
            return "\"\"";
        }

        char c = 0;
        int i;
        int len = string.length();
        StringBuilder sb = new StringBuilder(len + 4);
        String t;

        sb.append('"');
        for (i = 0; i < len; i += 1) {
            c = string.charAt(i);
            switch (c) {
            case '\\':
            case '"':
                sb.append('\\');
                sb.append(c);
                break;
            case '/':
                sb.append('\\');
                sb.append(c);
                break;
            case '\b':
                sb.append("\\b");
                break;
            case '\t':
                sb.append("\\t");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\f':
                sb.append("\\f");
                break;
            case '\r':
                sb.append("\\r");
                break;
            default:
                if (c < ' ') {
                    t = "000" + Integer.toHexString(c);
                    sb.append("\\u" + t.substring(t.length() - 4));
                } else {
                    sb.append(c);
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
