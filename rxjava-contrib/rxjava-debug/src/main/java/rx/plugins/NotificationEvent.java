package rx.plugins;

import rx.Notification;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.observers.SafeSubscriber;
import rx.operators.DebugSubscriber;
import rx.operators.Operator;

public class NotificationEvent<T> {
    public static enum Kind {
        OnNext, OnError, OnCompleted, Subscribe, Unsubscribe
    }

    private final OnSubscribe<T> source;
    private final Operator<T, ?> from;
    private final Kind kind;
    private final Notification<T> notification;
    private final Operator<?, T> to;
    private final long nanoTime;
    private final long threadId;
    private Observer o;

    public static <T> NotificationEvent<T> createSubscribe(Observer<? super T> o, OnSubscribe<T> source) {
        Operator<?, T> to = null;
        Operator<T, ?> from = null;
        if (o instanceof DebugSubscriber) {
            to = ((DebugSubscriber<T>) o).getTo();
            from = ((DebugSubscriber<T>) o).getFrom();
            o = ((DebugSubscriber) o).getActual();
        }
        return new NotificationEvent<T>(o, from, Kind.Subscribe, null, to, source);
    }

    public static <T> NotificationEvent<T> createOnNext(Observer<? super T> o, Operator<T, ?> from, T t, Operator<?, T> to) {
        return new NotificationEvent<T>(o, from, Kind.OnNext, Notification.createOnNext(t), to, null);
    }

    public static <T> NotificationEvent<T> createOnError(Observer<? super T> o, Operator<T, ?> from, Throwable e, Operator<?, T> to) {
        return new NotificationEvent<T>(o, from, Kind.OnError, Notification.<T> createOnError(e), to, null);
    }

    public static <T> NotificationEvent<T> createOnCompleted(Observer<? super T> o, Operator<T, ?> from, Operator<?, T> to) {
        return new NotificationEvent<T>(o, from, Kind.OnCompleted, Notification.<T> createOnCompleted(), to, null);
    }

    public static <T> NotificationEvent<T> createUnsubscribe(Observer<? super T> o, Operator<T, ?> from, Operator<?, T> to) {
        return new NotificationEvent<T>(o, from, Kind.Unsubscribe, null, to, null);
    }

    private NotificationEvent(Observer o, Operator<T, ?> from, Kind kind, Notification<T> notification, Operator<?, T> to, OnSubscribe<T> source) {
        this.o = (o instanceof SafeSubscriber) ? ((SafeSubscriber) o).getActual() : o;
        this.from = from;
        this.kind = kind;
        this.notification = notification;
        this.to = to;
        this.source = source;
        this.nanoTime = System.nanoTime();
        this.threadId = Thread.currentThread().getId();
    }

    public Operator<T, ?> getFrom() {
        return from;
    }

    public Notification<T> getNotification() {
        return notification;
    }

    public Operator<?, T> getTo() {
        return to;
    }

    public long getNanoTime() {
        return nanoTime;
    }

    public long getThreadId() {
        return threadId;
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder("{");
        s.append(" \"nano\": ").append(nanoTime);
        s.append(", \"thread\": ").append(threadId);
        s.append(", \"observer\": \"").append(o.getClass().getName()).append("@").append(Integer.toHexString(o.hashCode())).append("\"");
        s.append(", \"type\": \"").append(kind).append("\"");
        if (notification != null) {
            if (notification.hasValue())
                s.append(", \"value\": \"").append(notification.getValue()).append("\"");
            if (notification.hasThrowable())
                s.append(", \"exception\": \"").append(notification.getThrowable().getMessage().replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
        }
        if (source != null)
            s.append(", \"source\": \"").append(source.getClass().getName()).append("@").append(Integer.toHexString(source.hashCode())).append("\"");
        if (from != null)
            s.append(", \"from\": \"").append(from.getClass().getName()).append("@").append(Integer.toHexString(from.hashCode())).append("\"");
        if (to != null)
            s.append(", \"to\": \"").append(to.getClass().getName()).append("@").append(Integer.toHexString(to.hashCode())).append("\"");
        s.append("}");
        return s.toString();
    }
}
