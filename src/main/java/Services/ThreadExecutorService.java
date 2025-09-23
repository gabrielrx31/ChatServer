package services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Handles Thread pool.
public class ThreadExecutorService {
  private ExecutorService pool;

  // Bruger poolSize-parameteren til at oprette puljen
  public ThreadExecutorService(int poolSize) {
    this.pool = Executors.newFixedThreadPool(poolSize);
    System.out.println("Tråd-pulje oprettet med " + poolSize + " tråde.");
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
    System.out.println("Tråd-pulje lukket.");
  }
}