package mesh;

public class ChunkMeshData {
    public final String textureName;
    public final float[] vertices;

    public ChunkMeshData(String textureName, float[] vertices) {
        this.textureName = textureName;
        this.vertices = vertices;
    }

    public int getVertexCount() {
        return vertices.length / 5; // 5 floats per vertex (position + UV)
    }
}