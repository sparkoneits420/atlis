package org.atlis.common.tsk;

public abstract class Task {

    public long interval, lastTick;
    public boolean active;

    public Task(long interval) {
        this.interval = interval;
    }

    public abstract void execute();

}
