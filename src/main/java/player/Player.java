package player;

import camera.Camera;
import org.joml.Vector3f;
import world.WorldManager;

public class Player {

    private final Camera camera;
    private final WorldManager world;

    private final float eyeHeight = 1.62f; // height of eyes above feet
    private final float height = 1.8f;     // total player height
    private final float width = 0.6f;      // player width

    private long lastMovementTime = System.currentTimeMillis();
    private static final long MOVEMENT_THRESHOLD_MS = 100; // 100ms threshold

    private Vector3f velocity = new Vector3f();
    private boolean onGround = false;

    private final float gravity = -12f;
    private final float jumpStrength = 7f;

    public Player(Camera camera, WorldManager world) {
        this.camera = camera;
        this.world = world;

        // Ensure player is actually on ground at spawn
        Vector3f cameraPos = camera.getPosition();
        Vector3f feetPos = new Vector3f(cameraPos.x, cameraPos.y - eyeHeight, cameraPos.z);

        // Check if we need to adjust spawn height
        if (!isOnSolidGround(feetPos)) {
            System.out.println("Adjusting spawn height...");

            // Find the ground below
            int groundY = findGroundHeight((int)feetPos.x, (int)feetPos.z);
            if (groundY > 0) {
                feetPos.y = groundY + 0.1f; // Just above ground
                camera.setPosition(new Vector3f(feetPos.x, feetPos.y + eyeHeight, feetPos.z));
                System.out.println("Adjusted to Y=" + feetPos.y);
            }
        }

        onGround = true;
    }

    private boolean isOnSolidGround(Vector3f feetPos) {
        // Check blocks directly under feet
        int checkY = (int)Math.floor(feetPos.y);
        int checkX = (int)Math.floor(feetPos.x);
        int checkZ = (int)Math.floor(feetPos.z);

        return world.hasCube(checkX, checkY, checkZ) ||
                world.hasCube(checkX, checkY-1, checkZ) ||
                world.hasCube(checkX, checkY-2, checkZ);
    }

    private int findGroundHeight(int x, int z) {
        // Search downward from spawn height
        for (int y = 128; y >= 0; y--) {
            if (world.hasCube(x, y, z)) {
                return y + 1; // Return top of block
            }
        }
        return 64; // Default height if no ground found
    }

    public void update(float deltaTime, boolean forward, boolean backward, boolean left, boolean right, boolean jump) {
        Vector3f move = new Vector3f();

        float speed = 8f * deltaTime;

        if (forward)  move.add(new Vector3f(camera.getFront()).mul(speed, new Vector3f()));
        if (backward) move.sub(new Vector3f(camera.getFront()).mul(speed, new Vector3f()));
        if (left)     move.sub(new Vector3f(camera.getRight()).mul(speed, new Vector3f()));
        if (right)    move.add(new Vector3f(camera.getRight()).mul(speed, new Vector3f()));

        // Apply horizontal movement
        camera.move(move);

        // Apply gravity
        if (!onGround) velocity.y += gravity * deltaTime;

        // Jump
        if (jump && onGround) {
            velocity.y = jumpStrength;
            onGround = false;
        }

        // Apply vertical movement
        Vector3f feetPos = new Vector3f(camera.getPosition()).sub(0, eyeHeight, 0); // feet position
        feetPos.add(velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);

        // Collision detection
        onGround = false;
        if (checkCollision(feetPos)) {
            velocity.y = 0;
            onGround = true;
            // Snap to top of block
            feetPos.y = (float)Math.floor(feetPos.y) + 1.0f;
        }

        // Set camera position at eye height above feet
        camera.setPosition(new Vector3f(feetPos.x, feetPos.y + eyeHeight, feetPos.z));
    }

    public boolean hasMovedRecently() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastMovementTime) < MOVEMENT_THRESHOLD_MS;
    }

    private boolean checkCollision(Vector3f feetPos) {
        // Check the blocks inside the player's bounding box
        int minX = (int)(feetPos.x - width / 2);
        int maxX = (int)(feetPos.x + width / 2);
        int minZ = (int)(feetPos.z - width / 2);
        int maxZ = (int)(feetPos.z + width / 2);
        int minY = (int)(feetPos.y);
        int maxY = (int)(feetPos.y + height);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (world.hasCube(x, y, z)) return true;
                }
            }
        }
        return false;
    }

    public Camera getCamera() {
        return camera;
    }
}
