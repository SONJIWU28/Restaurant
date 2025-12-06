
public class WaiterTask {
    public enum Type {
        TAKE_ORDER,
        DELIVER_FOOD
    }

    public Type type;
    public int waiterId;
    public int clientId;
    public int tableIdx;
    public String dish;

    public WaiterTask(Type type, int waiterId, int clientId, int tableIdx) {
        this.type = type;
        this.waiterId = waiterId;
        this.clientId = clientId;
        this.tableIdx = tableIdx;
    }
}