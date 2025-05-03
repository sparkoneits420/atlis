package org.atlis.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.atlis.common.model.Region;
import org.atlis.common.tsk.TaskPool;

import org.atlis.server.net.Session;
import org.atlis.server.net.SessionPool;
import org.atlis.common.util.Constants;
import org.atlis.common.util.Log;
import org.atlis.server.net.sql.Database;

public class Server {

    public static Log log;
    public static TaskPool taskPool;
    public static boolean running = true;
    public static ExecutorService executor = Executors.newCachedThreadPool();
    public static HashMap<Long, Region> cachedRegions;
    public static boolean webServerDown = true;
    public static final int MAX_REGIONS = 512;

    public static void main(String[] args) {
        log = new Log();
        taskPool = new TaskPool();
        executor.submit(taskPool);
        cachedRegions = new HashMap<>();
        loadProperties(".properties");
        try {
            start(); 
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }

    public static void start() throws Exception {
        try (Selector selector = Selector.open(); ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            Database.connect();
            serverChannel.bind(new InetSocketAddress(Constants.PORT));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server started on port " + Constants.PORT);

            ByteBuffer buffer = ByteBuffer.allocate(Constants.BUFFER_SIZE);

            while (isRunning()) {
                selector.select();
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                if (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isAcceptable()) {
                        SocketChannel channel = serverChannel.accept();
                        SessionPool.register(channel, selector);
                    } else if (key.isReadable() && key.attachment() instanceof Session) {
                        Session session = (Session) key.attachment();
                        session.read(key, buffer);

                    }
                }
            }
        }
    }

    public static void loadProperties(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            Properties props = new Properties();
            props.load(fis);
            for (String name : props.stringPropertyNames()) {
                System.setProperty(name, props.getProperty(name));
            }
        } catch (IOException e) {
            System.err.println("Failed to load properties: " + e.getMessage());
        }
    }

    public static HashMap<Long, Region> getCachedRegions() {
        return cachedRegions;
    }

    public static TaskPool getTaskPool() {
        return taskPool;
    }

    public static Log getLog() {
        return log;
    }

    public static boolean isRunning() {
        return running;
    }
}
