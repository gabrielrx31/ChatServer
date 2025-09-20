# ChatServer

ThreadExecutorService:
- This class handles multi-threading using a pool size that automatically matches the number of CPU cores on the system.
- The server submits jobs (Runnable tasks) to the pool, which creates a thread for each task and executes it.