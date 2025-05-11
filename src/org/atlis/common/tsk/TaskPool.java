package org.atlis.common.tsk;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * 
 * @author smokey
 */
public class TaskPool {

    private static final int MAX_THREADS = 32;
    private static final int BASE_INTERVAL = 10;
    private static final int TIME_THRESHOLD = 5;

    private static final Queue<Task> tasks = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger activeThreads = new AtomicInteger(0);
    private static volatile boolean running = true;

    public static void start() {
        spawnWorker();
    }

    public static void stop() {
        running = false;
    }

    public static void add(Task task) {
        tasks.add(task);
    }

    public static void remove(Task task) {
        tasks.remove(task);
    }

    private static void spawnWorker() {
        if (activeThreads.get() >= MAX_THREADS) return;

        Thread t = new Thread(() -> {
            activeThreads.incrementAndGet();
            try {
                while (running) {
                    long loopStart = System.nanoTime();

                    long now = System.currentTimeMillis();
                    for (Task task : tasks) {
                        if (now - task.lastTick >= task.interval) {
                            try {
                                task.execute();
                                task.lastTick = now;
                            } catch (Exception e) {
                                System.err.println("Error in task: " + task.getClass().getSimpleName());
                                e.printStackTrace();
                            }
                        }
                    }

                    long elapsed = (System.nanoTime() - loopStart) / 1_000_000; // in ms
 
                    if (elapsed > TIME_THRESHOLD && activeThreads.get() < MAX_THREADS) {
                        spawnWorker();
                    }

                    LockSupport.parkNanos(BASE_INTERVAL * 1_000_000L);
                }
            } finally {
                activeThreads.decrementAndGet();
            }
        }, "TaskPool-Worker-" + activeThreads.get());

        t.setDaemon(true);
        t.start();
    }
}
