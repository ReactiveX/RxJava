package rx.internal.schedulers;

/**
 * Represents the capability of a Scheduler to be start or shut down its maintained
 * threads.
 */
public interface SchedulerLifecycle {
    /** 
     * Allows the Scheduler instance to start threads 
     * and accept tasks on them.
     * <p>Implementations should make sure the call is idempotent and threadsafe. 
     */
    void start();
    /** 
     * Instructs the Scheduler instance to stop threads 
     * and stop accepting tasks on any outstanding Workers. 
     * <p>Implementations should make sure the call is idempotent and threadsafe. 
     */
    void shutdown();
}