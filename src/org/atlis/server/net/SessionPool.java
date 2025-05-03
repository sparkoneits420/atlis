package org.atlis.server.net;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.atlis.common.model.Player;

public class SessionPool {

    public static final Map<SocketChannel, Session> sessions = new ConcurrentHashMap<>();

    public static void register(SocketChannel channel, Selector selector) throws IOException {
        channel.configureBlocking(false);
        SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
        Session session = new Session(channel, key);
        key.attach(session);
        sessions.put(channel, session);
    }

    public static void unregister(SocketChannel channel) throws IOException {
        sessions.remove(channel);
        channel.close();
    }

    public static Session getSession(SocketChannel channel) {
        return sessions.get(channel);
    }

    public static boolean contains(SocketChannel channel) {
        return sessions.containsKey(channel);
    }

    public class PlayerRegistry {

        private static final Map<Long, Player> players = new ConcurrentHashMap<>();

        public static void register(Player player) {
            players.put(player.getId(), player);
        }

        public static Player get(long id) {
            return players.get(id);
        }

        public static Map<Long, Player> all() {
            return players;
        }

        public static Session getSessionByPlayer(Player player) {
            for (Session session : sessions.values()) {
                if (session.getPlayer() == player) {
                    return session;
                }
            }
            return null;
        }
    }
}
