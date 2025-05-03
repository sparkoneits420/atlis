package org.atlis.common.util.path; 

import java.util.*;
import org.atlis.common.model.Tile;

public class PathFinder {

    private static class Node {
        Tile tile;
        Node parent;
        int gCost, hCost;

        Node(Tile tile, Node parent, int gCost, int hCost) {
            this.tile = tile;
            this.parent = parent;
            this.gCost = gCost;
            this.hCost = hCost;
        }

        int fCost() {
            return gCost + hCost;
        }
    }

    public static ArrayList<Tile> findPath(Tile start, Tile goal, boolean[][] walkableMap) { 
        
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingInt(Node::fCost));
        HashMap<Tile, Node> allNodes = new HashMap<>();
        HashSet<Tile> closedSet = new HashSet<>();

        Node startNode = new Node(start, null, 0, heuristic(start, goal));
        openSet.add(startNode);
        allNodes.put(start, startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            if (current.tile.equals(goal)) {
                return reconstructPath(current);
            }

            closedSet.add(current.tile);

            for (Tile neighbor : getNeighbors(current.tile, walkableMap)) {
                if (!inBounds(neighbor, walkableMap) || !walkableMap[neighbor.x][neighbor.y] || closedSet.contains(neighbor)) {
                    continue;
                }

                int tentativeG = current.gCost + 1;
                Node neighborNode = allNodes.getOrDefault(neighbor, new Node(neighbor, current, tentativeG, heuristic(neighbor, goal)));

                if (tentativeG < neighborNode.gCost || !allNodes.containsKey(neighbor)) {
                    neighborNode.gCost = tentativeG;
                    neighborNode.parent = current;
                    if (!openSet.contains(neighborNode)) openSet.add(neighborNode);
                    allNodes.put(neighbor, neighborNode);
                }
            }
        }

        return new ArrayList<>(); // No path found
    }

    private static boolean inBounds(Tile tile, boolean[][] map) {
        return tile.x >= 0 && tile.y >= 0 && tile.x < map.length && tile.y < map[0].length;
    }

    private static ArrayList<Tile> reconstructPath(Node node) {
        ArrayList<Tile> path = new ArrayList<>();
        while (node != null) {
            path.add(0, node.tile);
            node = node.parent;
        }
        return path;
    }

    private static int heuristic(Tile a, Tile b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y); 
    }

    private static List<Tile> getNeighbors(Tile tile, boolean[][] map) {
        List<Tile> neighbors = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = tile.x + dx;
                int ny = tile.y + dy;
                Tile neighbor = new Tile(nx, ny, tile.regionId);
                if (inBounds(neighbor, map)) {
                    neighbors.add(neighbor);
                }
            }
        }
        return neighbors;
    }
} 
