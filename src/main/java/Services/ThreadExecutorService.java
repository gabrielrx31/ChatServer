package Services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Handles Thread pool. 
public class ThreadExecutorService {
  private ExecutorService pool;

  // Creates the PoolSize based on Avaliable Cores on the system. 
  public ThreadExecutorService(int poolSize) {
    this.pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
  }

  // Adds Runnable to the pool,
  public void addJob(Runnable job) {
    try {
      pool.submit(job);
    } catch (Exception e) {
      System.err.println("An error occurred while submitting a job: " + e.getMessage());
      e.printStackTrace();
    }
  }
  // Shutdown the pool
  public void shutdown() {
    pool.shutdown();
  }
}