import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class Order {
    private static final AtomicInteger CNT = new AtomicInteger(0);

    private final int id;
    private final int clientId;
    private final Dish dish;
    private final int waiterId;
    private final long time;
    private final CompletableFuture<Order> ready = new CompletableFuture<>();

    public Order(int clientId, Dish dish, int waiterId) {
        this.id = CNT.incrementAndGet();
        this.clientId = clientId;
        this.dish = dish;
        this.waiterId = waiterId;
        this.time = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public int getClientId() { return clientId; }
    public Dish getDish() { return dish; }
    public int getWaiterId() { return waiterId; }
    public CompletableFuture<Order> getReady() { return ready; }
    public void done() { ready.complete(this); }
    public long getWait() { return System.currentTimeMillis() - time; }

    public String toString() {
        return "Заказ #" + id + " (" + dish + ", клиент " + clientId + ")";
    }

    public static void reset() { CNT.set(0); }
}