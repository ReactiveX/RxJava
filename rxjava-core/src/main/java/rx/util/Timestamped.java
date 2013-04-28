package rx.util;

/**
 * Composite class that takes a value and a timestamp and wraps them.   
 */
public final class Timestamped<T> {
    private final long timestampMillis;
    private final T value;

    public Timestamped(long timestampMillis, T value) {
        this.value = value;
        this.timestampMillis = timestampMillis;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public T getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Timestamped)) {
            return false;
        }
        Timestamped<?> other = (Timestamped<?>) obj;
        if (timestampMillis != other.timestampMillis) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (timestampMillis ^ (timestampMillis));
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }
}
