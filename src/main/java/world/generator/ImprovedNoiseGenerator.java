package world.generator;

import java.util.Random;

public class ImprovedNoiseGenerator {
    private final long seed;
    private final Random random;

    // Noise layers for different features
    private final PerlinNoise baseNoise;
    private final PerlinNoise detailNoise;
    private final PerlinNoise mountainNoise;
    private final PerlinNoise biomeNoise;

    public ImprovedNoiseGenerator(long seed) {
        this.seed = seed;
        this.random = new Random(seed);

        // Different seeds for different noise layers
        this.baseNoise = new PerlinNoise(seed);
        this.detailNoise = new PerlinNoise(seed + 1000);
        this.mountainNoise = new PerlinNoise(seed + 2000);
        this.biomeNoise = new PerlinNoise(seed + 3000);
    }

    // Generate height with varied terrain
    public float getHeight(float x, float z) {
        // Base terrain
        float base = baseNoise.noise(x * 0.002f, z * 0.002f) * 32.0f;

        // Add mountains
        float mountain = mountainNoise.noise(x * 0.001f, z * 0.001f);
        mountain = mountain * mountain * 64.0f; // Square to make peaks sharper

        // Add small details
        float detail = detailNoise.noise(x * 0.01f, z * 0.01f) * 4.0f;

        // Combine
        float height = 64.0f + base + mountain + detail;

        return Math.max(0, height);
    }

    // Get biome type at position (0-1)
    public float getBiome(float x, float z) {
        return (biomeNoise.noise(x * 0.0005f, z * 0.0005f) + 1.0f) * 0.5f;
    }

    // Get temperature (0-1)
    public float getTemperature(float x, float z) {
        return (baseNoise.noise(x * 0.0003f, z * 0.0003f) + 1.0f) * 0.5f;
    }

    // Get humidity (0-1)
    public float getHumidity(float x, float z) {
        return (detailNoise.noise(x * 0.0004f, z * 0.0004f) + 1.0f) * 0.5f;
    }

    // Simple Perlin noise implementation
    private static class PerlinNoise {
        private final int[] p = new int[512];

        public PerlinNoise(long seed) {
            Random rand = new Random(seed);

            // Initialize permutation array
            int[] permutation = new int[256];
            for (int i = 0; i < 256; i++) {
                permutation[i] = i;
            }

            // Shuffle
            for (int i = 0; i < 256; i++) {
                int j = rand.nextInt(256);
                int temp = permutation[i];
                permutation[i] = permutation[j];
                permutation[j] = temp;
            }

            // Duplicate for overflow
            for (int i = 0; i < 256; i++) {
                p[256 + i] = p[i] = permutation[i];
            }
        }

        public float noise(float x, float y) {
            int X = (int)Math.floor(x) & 255;
            int Y = (int)Math.floor(y) & 255;

            x -= Math.floor(x);
            y -= Math.floor(y);

            float u = fade(x);
            float v = fade(y);

            int aa = p[p[X] + Y];
            int ab = p[p[X] + Y + 1];
            int ba = p[p[X + 1] + Y];
            int bb = p[p[X + 1] + Y + 1];

            return lerp(v, lerp(u, grad(aa, x, y),
                            grad(ba, x - 1, y)),
                    lerp(u, grad(ab, x, y - 1),
                            grad(bb, x - 1, y - 1)));
        }

        private float fade(float t) {
            return t * t * t * (t * (t * 6 - 15) + 10);
        }

        private float lerp(float t, float a, float b) {
            return a + t * (b - a);
        }

        private float grad(int hash, float x, float y) {
            int h = hash & 7;
            float u = h < 4 ? x : y;
            float v = h < 4 ? y : x;
            return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
        }
    }
}