import java.awt.Color;

/**
 * Визуальное представление официанта.
 */
public class WaiterVisual {
    
    public final int id;
    public final Color color;
    
    public double x;
    public double y;
    public double targetX;
    public double targetY;
    public double homeX;
    public double homeY;
    
    public WaiterState state = WaiterState.IDLE;
    public int currentClientId = 0;
    public boolean hasFood = false;
    public boolean awaitingArrival = false;

    public WaiterVisual(int id, double x, double y, Color color) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
        this.homeX = x;
        this.homeY = y;
        this.color = color;
    }

    public void reset() {
        x = homeX;
        y = homeY;
        targetX = homeX;
        targetY = homeY;
        state = WaiterState.IDLE;
        currentClientId = 0;
        hasFood = false;
        awaitingArrival = false;
    }
}
