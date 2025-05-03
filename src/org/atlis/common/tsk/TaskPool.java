package org.atlis.common.tsk;

import java.util.ArrayList;

public class TaskPool extends ArrayList<Task> implements Runnable {

    public boolean running = true; 
     
    @Override
    public void run() {
        while(running) {
            long cur = System.currentTimeMillis();
            for(Task task : this) {
                if(task.interval <= cur - task.lastTick) {
                    task.execute();
                    task.lastTick = cur;
                }
            }
            //System.gc();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    } 

    public void stop() {
        running = false;
    }
}
