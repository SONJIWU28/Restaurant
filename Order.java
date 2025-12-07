import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Заказ клиента.
 * Использует CompletableFuture для уведомления официанта о готовности.
 */
public class Order {
    
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private final int id;
    private final int clientId;
    private final int waiterId;
    private final Dish dish;
    private final boolean vip;
    private final long createdAt;
    private final CompletableFuture<Order> ready = new CompletableFuture<>();

    public Order(int clientId, Dish dish, int waiterId, boolean vip) {
        this.id = COUNTER.incrementAndGet();
        this.clientId = clientId;
        this.dish = dish;
        this.waiterId = waiterId;
        this.vip = vip;
        this.createdAt = System.currentTimeMillis();
    }

    public int getClientId() {
        return clientId;
    }

    public Dish getDish() {
        return dish;
    }

    public boolean isVip() {
        return vip;
    }

    public CompletableFuture<Order> getReady() {
        return ready;
    }

    /**
     * Помечает заказ как готовый.
     * Вызывается поваром после приготовления.
     */
    public void done() {
        ready.complete(this);
    }

    /**
     * Время ожидания заказа в миллисекундах.
     */
    public long getWaitTime() {
        return System.currentTimeMillis() - createdAt;
    }

    @Override
    public String toString() {
        String vipMark;
        if (vip) {
            vipMark = " [VIP]";
        } else {
            vipMark = "";
        }
        return "Заказ #" + id + vipMark + " (" + dish + ", клиент " + clientId + ")";
    }

    public static void reset() {
        COUNTER.set(0);
    }
}
