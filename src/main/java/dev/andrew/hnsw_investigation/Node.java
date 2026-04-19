package dev.andrew.hnsw_investigation;

import java.util.*;

class Node {
    int id;
    float[] vector;
    List<List<Integer>> neighbors;

    public Node(int id, float[] vector, int maxLayer) {
        this.id = id;
        this.vector = vector;
        this.neighbors = new ArrayList<>();
        for (int i = 0; i <= maxLayer; i++) {
            neighbors.add(new ArrayList<>());
        }
    }
}
