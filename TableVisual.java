public class TableVisual {
    public int id, x, y;
    public boolean hasClient, hasFood, waitingForFood;
    public int clientId;

    public TableVisual(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }
}