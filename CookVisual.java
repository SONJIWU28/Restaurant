import java.awt.Color;

public class CookVisual {
    public int id, x, y;
    public Color color;
    public boolean isCooking;
    public String currentDish;
    public int animFrame;

    public CookVisual(int id, int x, int y, Color color) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.color = color;
    }
}