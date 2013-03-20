package rx;

public class ObservedException extends RuntimeException
{
    public ObservedException(Exception exception)
    {
        super(exception);
    }
}
