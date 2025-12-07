import java.awt.Color;

/**
 * Визуальное представление повара.
 */
public class CookVisual {
    
    public final int id;
    public final int x;
    public final int y;
    public final Color color;

    public CookVisual(int id, int x, int y, Color color) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.color = color;
    }
}
