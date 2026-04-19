package dev.andrew.hnsw_investigation;

class Utilities {

    public static float distance(float[] a, float[] b) {
        float sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return (float) Math.sqrt(sum);
    }

}