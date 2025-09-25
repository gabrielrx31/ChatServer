package Services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import server.utils.Logger;
import server.utils.Logger.LogEvent;

// A wrapper for Java's ExecutorService to manage a pool of threads for background tasks.
public class ThreadExecutorService {
    private ExecutorService pool;

    public ThreadExecutorService(int poolSize) {
        this.pool = Executors.newFixedThreadPool(poolSize);
        Logger.info(LogEvent.SYSTEM_ERROR, "Thread pool created with " + poolSize + " threads");
    }

    // Submits a new task to the thread pool's queue.
    public void addJob(Runnable job) {
        try {
            pool.submit(job);
            Logger.info(LogEvent.SYSTEM_ERROR, "New job submitted to thread pool");
        } catch (Exception e) {
            Logger.error(LogEvent.SYSTEM_ERROR, "Error adding job to the pool", e);
        }
    }

    // Initiates a graceful shutdown of the thread pool.
    public void shutdown() {
        pool.shutdown();
        Logger.info(LogEvent.SYSTEM_ERROR, "Thread pool is shutting down");
    }
}
