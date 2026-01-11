package camera;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.glfw.GLFW.*;

public class Camera {
    private final long window;
    private final Vector3f position = new Vector3f(0, 1.5f, 3);
    private float pitch = 0f;
    private float yaw = -90f;
    private double lastX, lastY;
    private boolean firstMouse = true;

    // Store window dimensions for aspect ratio
    private int windowWidth = 1280;
    private int windowHeight = 720;
    private float aspectRatio = 1280f / 720f;

    // Camera settings
    private final float FOV = (float) Math.toRadians(70f);
    private final float NEAR = 0.1f;
    private final float FAR = 1000f;

    public Camera(long window) {
        this.window = window;

        // Get initial window size
        updateWindowSize();

        // Center mouse position for first mouse movement calculation
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetWindowSize(window, width, height);
        lastX = width[0] / 2.0;
        lastY = height[0] / 2.0;
    }

    public void updateAspectRatio() {
        // Get current window size
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetWindowSize(window, width, height);
        aspectRatio = (float) width[0] / height[0];
    }

    // Call this when window is resized
    public void onWindowResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        this.aspectRatio = (float) width / height;

        // Update OpenGL viewport
        org.lwjgl.opengl.GL11.glViewport(0, 0, width, height);
    }

    private void updateWindowSize() {
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetWindowSize(window, width, height);
        onWindowResize(width[0], height[0]);
    }

    public void updateRotation() {
        double[] mx = new double[1];
        double[] my = new double[1];
        glfwGetCursorPos(window, mx, my);

        if (firstMouse) {
            lastX = mx[0];
            lastY = my[0];
            firstMouse = false;
        }

        float sensitivity = 0.1f;
        yaw += (mx[0] - lastX) * sensitivity;
        pitch -= (my[0] - lastY) * sensitivity;

        pitch = Math.max(-89f, Math.min(89f, pitch));

        lastX = mx[0];
        lastY = my[0];
    }

    public Matrix4f getView() {
        return new Matrix4f().lookAt(
                position,
                new Vector3f(position).add(getForward()),
                new Vector3f(0, 1, 0)
        );
    }

    public Matrix4f getProjection() {
        // Use current aspect ratio, not hardcoded values
        return new Matrix4f().perspective(FOV, aspectRatio, NEAR, FAR);
    }

    private Vector3f getForward() {
        return new Vector3f(
                (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch)),
                (float) Math.sin(Math.toRadians(pitch)),
                (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch))
        ).normalize();
    }

    public Vector3f getRight() {
        return getForward().cross(0, 1, 0, new Vector3f()).normalize();
    }

    public Vector3f getFront() {
        return getForward();
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPosition(Vector3f position) {
        this.position.set(position);
    }

    public void move(Vector3f delta) {
        this.position.add(delta);
    }
}