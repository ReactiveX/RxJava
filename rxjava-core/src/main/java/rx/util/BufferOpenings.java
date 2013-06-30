package rx.util;

public class BufferOpenings {

    public static BufferOpening create() {
        return new BufferOpening() {};
    }

    private BufferOpenings() {
        // Prevent instantation.
    }
}