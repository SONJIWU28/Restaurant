import java.util.ArrayList;
import java.util.List;

/**
 * Визуальное представление стола в ресторане.
 */
public class TableVisual {
    
    public final int id;
    public final int x;
    public final int y;
    public int width = 50;
    public int height = 32;
    public boolean vipTable = false;
    public final List<Seat> seats = new ArrayList<>();

    public TableVisual(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
        setCapacity(1);
    }

    public void setCapacity(int capacity) {
        seats.clear();
        for (int i = 0; i < capacity; i++) {
            seats.add(new Seat());
        }
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Место за столом.
     */
    public static class Seat {
        public int clientId = 0;
        public boolean vip = false;
        public boolean waitingForFood = false;
        public boolean hasFood = false;

        public boolean isFree() {
            return clientId == 0;
        }

        public void reset() {
            clientId = 0;
            vip = false;
            waitingForFood = false;
            hasFood = false;
        }
    }
}
