public enum Face {
    FRONT(0, 0, 1),
    BACK(0, 0, -1),
    LEFT(-1, 0, 0),
    RIGHT(1, 0, 0),
    TOP(0, 1, 0),
    BOTTOM(0, -1, 0);

    public final int dx, dy, dz;

    Face(int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }
}
