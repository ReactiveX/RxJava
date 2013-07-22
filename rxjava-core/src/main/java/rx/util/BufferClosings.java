package rx.util;

public class BufferClosings {

    public static BufferClosing create() {
        return new BufferClosing() {};
    }

    private BufferClosings() {
        // Prevent instantation.
    }
}