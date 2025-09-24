package Services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Denne klasse håndterer en tråd-pulje til at køre jobs asynkront.
public class ThreadExecutorService {
    private ExecutorService pool;

    public ThreadExecutorService(int poolSize) {
        // Opretter en pulje med et fast antal tråde for at undgå at overbelaste systemet.
        this.pool = Executors.newFixedThreadPool(poolSize);
        System.out.println("Tråd-pulje oprettet med " + poolSize + " tråde.");
    }

    // Tilføjer et nyt job (en Runnable) til puljens kø.
    public void addJob(Runnable job) {
        try {
            pool.submit(job);
        } catch (Exception e) {
            System.err.println("Der opstod en fejl under tilføjelse af job til puljen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Lukker puljen ned. Nye jobs vil blive afvist.
    public void shutdown() {
        pool.shutdown();
        System.out.println("Tråd-pulje lukket ned.");
    }
}