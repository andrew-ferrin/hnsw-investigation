package dev.andrew.hnsw_investigation;

public record Neighbor(int id, float dist) implements Comparable<Neighbor> {

    @Override
    public int compareTo(Neighbor other) {
        return Float.compare(this.dist, other.dist);
    }

}
