package org.atlis.common.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import org.atlis.common.tsk.Task;
import org.atlis.common.tsk.TaskPool;

/**
 *
 * @author spark
 */
public class Log {

    private static final SimpleDateFormat form = new SimpleDateFormat(Constants.DATE_FORMAT);
    private static final ArrayList<String> logs = new ArrayList<>(); 
    private static long lastTime = 0;

    static {
        TaskPool.add(new Task(Constants.LOG_DUMP_INTERVAL) {
            @Override
            public void execute() {
                if (logs.isEmpty()) return;
                XMLPersistence.saveTextFile(logs, Constants.CACHE_DIR
                        + "/logs/" + form.format(System.currentTimeMillis()).replaceAll(":", " ") + ".txt");
                logs.clear();
            }
        });
    }

    public static void print(String s) {
        print(s, false);
    }
    
    public static void print(String s, boolean error) {
        long cur = System.currentTimeMillis();
        StringBuilder bdr = new StringBuilder();
        if (cur - lastTime >= Constants.LOG_DUMP_INTERVAL) {
            lastTime = cur;
            bdr.append("[").append(form.format(cur)).append("]: \n");
        }
        bdr.append("{").append(s).append("}\n"); 
        String message = bdr.toString();
        if(!error) {
            System.out.print(message);
            System.out.flush();
        } else {
            System.err.print(message);
            System.err.flush();
        }
        logs.add(message);
        bdr.setLength(0);
    }

    public static void print(String s, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        print(s + "\n" + sw.toString(), true);
    }

    public static void dump() {
        if (logs.isEmpty()) {
            print("Couldn't dump logs, they are empty!");
            return;
        }
        XMLPersistence.saveTextFile(logs, Constants.CACHE_DIR
                + "/logs/" + form.format(System.currentTimeMillis()).replaceAll(":", " ") + ".txt");
        logs.clear();
    }
}
