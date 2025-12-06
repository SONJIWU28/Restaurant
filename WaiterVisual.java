import java.awt.Color;

public class WaiterVisual {
    public int id;
    public double x, y;
    public double targetX, targetY;
    public double homeX, homeY;
    public Color color;
    public boolean hasFood;
    public WaiterState state = WaiterState.IDLE;
    public int currentClientId;
    public boolean notification = false;

    public WaiterVisual(int id, double x, double y, Color color) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
        this.color = color;
    }

    public void setTarget(double tx, double ty) {
        this.targetX = tx;
        this.targetY = ty;
    }
}