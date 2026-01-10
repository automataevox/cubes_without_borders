package face;

public enum Face {

    FRONT(0, 0, 1,
            new float[][]{
                    {0, 0, 1},
                    {1, 0, 1},
                    {1, 1, 1},
                    {1, 1, 1},
                    {0, 1, 1},
                    {0, 0, 1}
            },
            new float[][]{  // texture coordinates (u, v)
                    {0, 0}, {1, 0}, {1, 1},
                    {1, 1}, {0, 1}, {0, 0}
            },
            new float[]{0, 0, 1}
    ),

    BACK(0, 0, -1,
            new float[][]{
                    {1, 0, 0},
                    {0, 0, 0},
                    {0, 1, 0},
                    {0, 1, 0},
                    {1, 1, 0},
                    {1, 0, 0}
            },
            new float[][]{  // texture coordinates (u, v)
                    {0, 0}, {1, 0}, {1, 1},
                    {1, 1}, {0, 1}, {0, 0}
            },
            new float[]{0, 0, -1}
    ),

    LEFT(-1, 0, 0,
            new float[][]{
                    {0, 0, 0},
                    {0, 0, 1},
                    {0, 1, 1},
                    {0, 1, 1},
                    {0, 1, 0},
                    {0, 0, 0}
            },
            new float[][]{  // texture coordinates (u, v)
                    {0, 0}, {1, 0}, {1, 1},
                    {1, 1}, {0, 1}, {0, 0}
            },
            new float[]{-1, 0, 0}
    ),

    RIGHT(1, 0, 0,
            new float[][]{
                    {1, 0, 1},
                    {1, 0, 0},
                    {1, 1, 0},
                    {1, 1, 0},
                    {1, 1, 1},
                    {1, 0, 1}
            },
            new float[][]{  // texture coordinates (u, v)
                    {0, 0}, {1, 0}, {1, 1},
                    {1, 1}, {0, 1}, {0, 0}
            },
            new float[]{1, 0, 0}
    ),

    TOP(0, 1, 0,
            new float[][]{
                    {0, 1, 1},
                    {1, 1, 1},
                    {1, 1, 0},
                    {1, 1, 0},
                    {0, 1, 0},
                    {0, 1, 1}
            },
            new float[][]{  // texture coordinates (u, v)
                    {0, 0}, {1, 0}, {1, 1},
                    {1, 1}, {0, 1}, {0, 0}
            },
            new float[]{0, 1, 0}
    ),

    BOTTOM(0, -1, 0,
            new float[][]{
                    {0, 0, 0},
                    {1, 0, 0},
                    {1, 0, 1},
                    {1, 0, 1},
                    {0, 0, 1},
                    {0, 0, 0}
            },
            new float[][]{  // texture coordinates (u, v)
                    {0, 0}, {1, 0}, {1, 1},
                    {1, 1}, {0, 1}, {0, 0}
            },
            new float[]{0, -1, 0}
    );

    public final int dx, dy, dz;
    private final float[][] vertices;
    private final float[][] texCoords;
    private final float[] normal;

    Face(int dx, int dy, int dz, float[][] vertices, float[][] texCoords, float[] normal) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.vertices = vertices;
        this.texCoords = texCoords;
        this.normal = normal;
    }

    public float[][] getVertices() {
        return vertices;
    }

    public float[][] getTexCoords() {
        return texCoords;
    }

    public float[] getNormal() {
        return normal;
    }
}
