package Services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// A wrapper for Java's ExecutorService to manage a pool of threads for background tasks.
public class ThreadExecutorService {
    private ExecutorService pool;

    public ThreadExecutorService(int poolSize) {
        // A fixed thread pool is used to cap resource consumption,
        // preventing the server from being overwhelmed by too many concurrent tasks.
        this.pool = Executors.newFixedThreadPool(poolSize);
        System.out.println("Thread pool created with " + poolSize + " threads.");
    }

    // Submits a new task to the thread pool's queue.
    public void addJob(Runnable job) {
        try {
            pool.submit(job);
        } catch (Exception e) {
            // This might happen if the pool is shutting down or has been terminated.
            System.err.println("Error adding job to the pool: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Initiates a graceful shutdown of the thread pool.
    // Previously submitted tasks will be executed, but no new tasks will be accepted.
    public void shutdown() {
        pool.shutdown();
        System.out.println("Thread pool is shutting down.");
    }
}