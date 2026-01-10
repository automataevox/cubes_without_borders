package world;

import org.joml.Vector2i;
import org.joml.Vector3f;
import world.blocks.GrassBlock;
import world.blocks.StoneBlock;
import world.generator.NoiseGenerator;

import java.util.*;

public class WorldManager {
    private final Map<Vector2i, Chunk> chunks = new HashMap<>();
    private final Map<Vector3f, Block> blocks = new HashMap<>();
    private final Map<Vector2i, Boolean> loadedChunks = new HashMap<>();
    private final int CHUNK_SIZE = 16;
    private final int RENDER_DISTANCE = 3;

    private final NoiseGenerator noise = new NoiseGenerator(System.currentTimeMillis());

    // SIMPLE VERSION - just use chunks map
    public List<Chunk> getLoadedChunks() {
        return new ArrayList<>(chunks.values());
    }

    public Chunk getChunkAt(int chunkX, int chunkZ) {
        return chunks.get(new Vector2i(chunkX, chunkZ));
    }

    public void generateChunk(int chunkX, int chunkZ) {
        Vector2i chunkKey = new Vector2i(chunkX, chunkZ);
        if (chunks.containsKey(chunkKey)) return;

        Chunk chunk = new Chunk(chunkX, 0, chunkZ);
        chunks.put(chunkKey, chunk);
        loadedChunks.put(chunkKey, true);

        int worldXOffset = chunkX * CHUNK_SIZE;
        int worldZOffset = chunkZ * CHUNK_SIZE;

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int worldX = x + worldXOffset;
                int worldZ = z + worldZOffset;

                float n = noise.interpolatedNoise(worldX * 0.1f, worldZ * 0.1f);
                int height = (int)((n + 2f) * 4) + 1;

                for (int y = 0; y < height; y++) {
                    Vector3f pos = new Vector3f(worldX, y, worldZ);
                    Block block = (y == height - 1) ? new GrassBlock() : new StoneBlock();

                    blocks.put(pos, block);
                    chunk.setBlock(x, y, z, block);
                }
            }
        }
    }

    public void generateChunksAround(Vector3f playerPos) {
        int playerChunkX = (int)Math.floor(playerPos.x / CHUNK_SIZE);
        int playerChunkZ = (int)Math.floor(playerPos.z / CHUNK_SIZE);

        Set<Vector2i> neededChunks = new HashSet<>();
        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                int cx = playerChunkX + dx;
                int cz = playerChunkZ + dz;
                neededChunks.add(new Vector2i(cx, cz));
                generateChunk(cx, cz);
            }
        }

        // Unload chunks that are no longer needed
        Iterator<Map.Entry<Vector2i, Chunk>> iterator = chunks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Vector2i, Chunk> entry = iterator.next();
            if (!neededChunks.contains(entry.getKey())) {
                entry.getValue().cleanup();
                iterator.remove();
                loadedChunks.remove(entry.getKey());

                // Also remove blocks from flat map
                int worldXOffset = entry.getKey().x * CHUNK_SIZE;
                int worldZOffset = entry.getKey().y * CHUNK_SIZE;
                for (int x = 0; x < CHUNK_SIZE; x++) {
                    for (int z = 0; z < CHUNK_SIZE; z++) {
                        for (int y = 0; y < 256; y++) {
                            blocks.remove(new Vector3f(worldXOffset + x, y, worldZOffset + z));
                        }
                    }
                }
            }
        }
    }

    public Block getBlock(Vector3f pos) {
        return blocks.get(pos);
    }

    public boolean hasCube(int x, int y, int z) {
        return blocks.containsKey(new Vector3f(x, y, z));
    }

    public void breakBlock(Vector3f pos) {
        blocks.remove(pos);

        int chunkX = (int)Math.floor(pos.x / CHUNK_SIZE);
        int chunkZ = (int)Math.floor(pos.z / CHUNK_SIZE);
        Vector2i chunkKey = new Vector2i(chunkX, chunkZ);
        Chunk chunk = chunks.get(chunkKey);
        if (chunk != null) {
            int localX = (int)pos.x - chunkX * CHUNK_SIZE;
            int localZ = (int)pos.z - chunkZ * CHUNK_SIZE;
            chunk.setBlock(localX, (int)pos.y, localZ, null);
        }
    }

    public Set<Vector3f> getRenderList() {
        return blocks.keySet();
    }

    public Vector3f getSpawnPoint() {
        int spawnX = CHUNK_SIZE / 2;
        int spawnZ = CHUNK_SIZE / 2;

        int highestY = -1;
        for (Vector3f pos : blocks.keySet()) {
            if ((int)pos.x == spawnX && (int)pos.z == spawnZ) {
                if (pos.y > highestY) highestY = (int)pos.y;
            }
        }

        return new Vector3f(spawnX + 0.5f, highestY + 1.5f, spawnZ + 0.5f);
    }

    public void cleanup() {
        for (Chunk chunk : chunks.values()) {
            chunk.cleanup();
        }
        chunks.clear();
        loadedChunks.clear();
        blocks.clear();
    }
}