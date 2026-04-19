package dev.andrew.hnsw_investigation;

import java.util.*;

public class SimpleHNSW {
    private static final int M = 16; // Max neighbors per node
    private static final int M_MAX = M * 2; // Max number of neighbors for an existing node
    private static final int EF_CONSTRUCTION = 200; // Search budget during build
    private static final double M_L = 1.0 / Math.log(M); // Layer probability factor

    private final Map<Integer, Node> nodes = new HashMap<>();
    private int entryPointId = -1;
    private int maxLayer = -1;

    public void insert(int id, float[] vector) {
        int nodeLayer = (int) Math.floor(-Math.log(Math.random()) * M_L);
        Node newNode = new Node(id, vector, nodeLayer);
        nodes.put(id, newNode);

        if (entryPointId == -1) { // short-circuit for first node
            entryPointId = id;
            maxLayer = nodeLayer;
            return;
        }

        int currObj = entryPointId;
        // 1. Fast search through upper layers
        for (int l = maxLayer; l > nodeLayer; l--) { // for each layer above the layer that this node will start to be inserted into, do a purely greedy search
            currObj = searchLayer(vector, currObj, 1, l).peek().id();
        }

        // 2. Multilayer insertion with efConstruction
        for (int l = Math.min(nodeLayer, maxLayer); l >= 0; l--) { // for each layer, starting at the max layer or the current layer (whichever is lower)
            PriorityQueue<Neighbor> candidates = searchLayer(vector, currObj, EF_CONSTRUCTION, l);
            // Select M neighbors and connect them (Simplified: just take top M)
            for (int i = 0; i < M && !candidates.isEmpty(); i++) {
                int neighborId = candidates.poll().id();
                newNode.neighbors.get(l).add(neighborId);
                nodes.get(neighborId).neighbors.get(l).add(id); // Bi-directional
                List<Integer> existingNeighbors = nodes.get(neighborId).neighbors.get(l);
                if (existingNeighbors.size() > M_MAX) {
                    // Simple Pruning: Keep only the closest M_MAX. Node could be optimized to use a PriorityQueue internally.
                    existingNeighbors.sort(Comparator.comparingDouble(
                        nodeId -> Utilities.distance(
                                nodes.get(nodeId).vector,
                                nodes.get(neighborId).vector)));
                    List<Integer> newNeighbors = new ArrayList<>(existingNeighbors.subList(0, M_MAX));
                    nodes.get(neighborId).neighbors.set(l, newNeighbors);
                }
            }
            currObj = newNode.neighbors.get(l).get(0); // Start next layer search from a known neighbor
        }

        if (nodeLayer > maxLayer) {
            maxLayer = nodeLayer;
            entryPointId = id;
        }
    }

    
    // This is the "Beam Search" engine of HNSW
    private PriorityQueue<Neighbor> searchLayer(float[] query, int entryPoint, int ef, int layer) {
        PriorityQueue<Neighbor> topCandidates = new PriorityQueue<>(Collections.reverseOrder());
        PriorityQueue<Neighbor> candidates = new PriorityQueue<>();
        Set<Integer> visited = new HashSet<>();

        float dist = Utilities.distance(query, nodes.get(entryPoint).vector);
        Neighbor first = new Neighbor(entryPoint, dist);
        topCandidates.add(first);
        candidates.add(first);
        visited.add(entryPoint);

        while (!candidates.isEmpty()) {
            Neighbor c = candidates.poll();
            Neighbor f = topCandidates.peek();

            if (c.dist() > f.dist())
                break;

            for (int neighborId : nodes.get(c.id()).neighbors.get(layer)) {
                if (!visited.contains(neighborId)) {
                    visited.add(neighborId);
                    float d = Utilities.distance(query, nodes.get(neighborId).vector);
                    if (topCandidates.size() < ef || d < topCandidates.peek().dist()) {
                        Neighbor res = new Neighbor(neighborId, d);
                        candidates.add(res);
                        topCandidates.add(res);
                        if (topCandidates.size() > ef) {
                            topCandidates.poll();
                        }
                    }
                }
            }
        }
        return topCandidates;
    }
}
