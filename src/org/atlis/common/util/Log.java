package org.atlis.common.util;
 
import java.text.SimpleDateFormat;
import java.util.ArrayList; 
import org.atlis.common.tsk.Task; 

/**
 *
 * @author spark
 */
public class Log extends Task {
     
    private final SimpleDateFormat form;
    private long lastTime = 0; 
    private final StringBuilder bdr;
    private final ArrayList<String> logs; 
    
    public Log() {
        super(Constants.LOG_DUMP_INTERVAL);
        form = new SimpleDateFormat(Constants.DATE_FORMAT);
        bdr = new StringBuilder();
        logs = new ArrayList<>();
    }
    
    public void put(String s) {
        long cur = System.currentTimeMillis(); 
        if(cur - lastTime >= Constants.LOG_DUMP_INTERVAL) { 
            lastTime = cur;
            bdr.append("[").append(form.format(cur)).append("]: \n");
        }
        bdr.append("{").append(s).append("}");
        String message = bdr.toString();
        System.out.println(message);
        logs.add(message);
        bdr.setLength(0);
    } 

    @Override
    public void execute() { 
        if(logs.isEmpty())
            return;  
        XMLPersistence.saveTextFile(logs, Constants.CACHE_DIR 
                + "/logs/" + form.format(System.currentTimeMillis())
                        .replaceAll(":", " ") + ".txt");
        logs.clear(); 
    }
    
    public void dump() { 
        if(logs.isEmpty()) {
            put("Couldn't dump logs, they are empty!");
            return;
        }
        execute(); 
        logs.clear();
    }
} 