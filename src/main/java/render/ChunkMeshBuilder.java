package render;

import mesh.ChunkMeshData;
import world.Chunk;
import texture.TextureAtlas;
import world.Block;
import face.Face;

import java.util.*;
import java.util.concurrent.*;

public class ChunkMeshBuilder {
    private static final ExecutorService executor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() - 1),
            new ThreadFactory() {
                private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();

                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = defaultFactory.newThread(r);
                    thread.setDaemon(true);
                    thread.setPriority(Thread.MIN_PRIORITY);
                    return thread;
                }
            }
    );

    private static final Map<String, Future<List<ChunkMeshData>>> futures = new ConcurrentHashMap<>();

    public static Future<List<ChunkMeshData>> buildAsync(int chunkX, int chunkZ, Chunk chunk, TextureAtlas atlas) {
        String key = chunkX + "_" + chunkZ;

        // Cancel existing task for this chunk
        Future<List<ChunkMeshData>> existing = futures.get(key);
        if (existing != null && !existing.isDone()) {
            existing.cancel(true);
            System.out.println("🔄 Cancelled previous async build for " + key);
        }

        // IMPORTANT: Create a snapshot of the chunk data for thread safety
        ChunkDataSnapshot snapshot = new ChunkDataSnapshot(chunk, atlas);

        // Submit new task with the snapshot
        Future<List<ChunkMeshData>> future = executor.submit(() -> {
            try {
                return generateMeshDataFromSnapshot(snapshot);
            } catch (Exception e) {
                System.err.println("Error generating mesh data for chunk " + key + ": " + e.getMessage());
                e.printStackTrace();
                return Collections.emptyList();
            }
        });

        futures.put(key, future);
        System.out.println("📦 Async mesh build started for " + key + " (using snapshot)");
        return future;
    }

    private static List<ChunkMeshData> generateMeshDataFromSnapshot(ChunkDataSnapshot snapshot) {
        // Group vertices by texture (CPU only, no OpenGL!)
        Map<String, List<Float>> verticesByTexture = new HashMap<>();

        int totalFaces = 0;

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    Block block = snapshot.blocks[x][y][z];
                    if (block != null && block.isVisible()) {
                        // Check each face
                        for (Face face : Face.values()) {
                            if (isFaceVisible(snapshot.blocks, x, y, z, face)) {
                                String textureName = block.getTexture(face);

                                List<Float> vertices = verticesByTexture.get(textureName);
                                if (vertices == null) {
                                    vertices = new ArrayList<>();
                                    verticesByTexture.put(textureName, vertices);
                                }

                                addFaceVertices(vertices, x, y, z, face, block, snapshot.atlas);
                                totalFaces++;
                            }
                        }
                    }
                }
            }
        }

        System.out.println("Generated mesh data for chunk " + snapshot.chunkX + "," + snapshot.chunkZ +
                ": " + totalFaces + " faces, " + verticesByTexture.size() + " texture groups");

        // Convert to ChunkMeshData objects
        List<ChunkMeshData> result = new ArrayList<>();
        for (Map.Entry<String, List<Float>> entry : verticesByTexture.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                float[] vertexArray = new float[entry.getValue().size()];
                for (int i = 0; i < vertexArray.length; i++) {
                    vertexArray[i] = entry.getValue().get(i);
                }
                result.add(new ChunkMeshData(entry.getKey(), vertexArray));
            }
        }

        return result;
    }

    // Updated to use snapshot
    private static boolean isFaceVisible(Block[][][] blocks, int x, int y, int z, Face face) {
        int nx = x + face.dx;
        int ny = y + face.dy;
        int nz = z + face.dz;

        // Check bounds
        if (nx < 0 || ny < 0 || nz < 0 || nx >= Chunk.SIZE || ny >= Chunk.SIZE || nz >= Chunk.SIZE) {
            return true; // Edge of chunk, always visible
        }

        Block neighbor = blocks[nx][ny][nz];
        return neighbor == null || !neighbor.isVisible();
    }

    // NEW: Thread-safe chunk data snapshot
    private static class ChunkDataSnapshot {
        private final int chunkX, chunkZ;
        private final Block[][][] blocks;
        private final TextureAtlas atlas;

        public ChunkDataSnapshot(Chunk chunk, TextureAtlas atlas) {
            this.chunkX = chunk.chunkX;
            this.chunkZ = chunk.chunkZ;
            this.atlas = atlas;

            // Create a copy of the block data for thread safety
            this.blocks = new Block[Chunk.SIZE][Chunk.SIZE][Chunk.SIZE];
            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int y = 0; y < Chunk.SIZE; y++) {
                    for (int z = 0; z < Chunk.SIZE; z++) {
                        this.blocks[x][y][z] = chunk.getBlock(x, y, z);
                    }
                }
            }
        }
    }

    // Add this method to RenderManager if not exists
    public void forceRebuildChunk(int chunkX, int chunkZ) {
        // Delegate to ChunkRenderer
        // You'll need to add this method to RenderManager too
    }

    public static List<ChunkMeshData> generateMeshData(Chunk chunk, TextureAtlas atlas) {
        // Group vertices by texture (CPU only, no OpenGL!)
        Map<String, List<Float>> verticesByTexture = new HashMap<>();

        int totalFaces = 0;
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block != null && block.isVisible()) {
                        // Check each face
                        for (Face face : Face.values()) {
                            if (isFaceVisible(chunk, x, y, z, face)) {
                                String textureName = block.getTexture(face);

                                List<Float> vertices = verticesByTexture.get(textureName);
                                if (vertices == null) {
                                    vertices = new ArrayList<>();
                                    verticesByTexture.put(textureName, vertices);
                                }

                                addFaceVertices(vertices, x, y, z, face, block, atlas);
                                totalFaces++;
                            }
                        }
                    }
                }
            }
        }


        // Convert to ChunkMeshData objects
        List<ChunkMeshData> result = new ArrayList<>();
        for (Map.Entry<String, List<Float>> entry : verticesByTexture.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                // Convert List<Float> to float[] for better performance
                float[] vertexArray = new float[entry.getValue().size()];
                for (int i = 0; i < vertexArray.length; i++) {
                    vertexArray[i] = entry.getValue().get(i);
                }
                result.add(new ChunkMeshData(entry.getKey(), vertexArray));
            }
        }

        return result;
    }

    private static void addFaceVertices(List<Float> vertices, int x, int y, int z, Face face,
                                        Block block, TextureAtlas atlas) {
        float[][] verts = face.getVertices();
        String textureName = block.getTexture(face);
        TextureAtlas.UVCoords uv = atlas.getUV(textureName);

        // Define UV coordinates for this face
        float[][] texCoords;

        switch (face) {
            case TOP:
                texCoords = new float[][] {
                        {uv.u1, uv.v2}, {uv.u2, uv.v2}, {uv.u2, uv.v1},
                        {uv.u2, uv.v1}, {uv.u1, uv.v1}, {uv.u1, uv.v2}
                };
                break;
            case BOTTOM:
                texCoords = new float[][] {
                        {uv.u1, uv.v1}, {uv.u2, uv.v1}, {uv.u2, uv.v2},
                        {uv.u2, uv.v2}, {uv.u1, uv.v2}, {uv.u1, uv.v1}
                };
                break;
            default: // Sides
                texCoords = new float[][] {
                        {uv.u1, uv.v2}, {uv.u2, uv.v2}, {uv.u2, uv.v1},
                        {uv.u2, uv.v1}, {uv.u1, uv.v1}, {uv.u1, uv.v2}
                };
        }

        // Add 6 vertices (2 triangles)
        for (int i = 0; i < 6; i++) {
            // Position
            vertices.add(verts[i][0] + x);
            vertices.add(verts[i][1] + y);
            vertices.add(verts[i][2] + z);
            // Texture coordinates
            vertices.add(texCoords[i][0]);
            vertices.add(texCoords[i][1]);
        }
    }

    private static boolean isFaceVisible(Chunk chunk, int x, int y, int z, Face face) {
        int nx = x + face.dx;
        int ny = y + face.dy;
        int nz = z + face.dz;

        // Check bounds
        if (nx < 0 || ny < 0 || nz < 0 || nx >= Chunk.SIZE || ny >= Chunk.SIZE || nz >= Chunk.SIZE) {
            return true; // Edge of chunk, always visible
        }

        Block neighbor = chunk.getBlock(nx, ny, nz);
        return neighbor == null || !neighbor.isVisible();
    }

    public static void cancelAll() {
        for (Future<List<ChunkMeshData>> future : futures.values()) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
        futures.clear();
    }

    public static void shutdown() {
        cancelAll();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}