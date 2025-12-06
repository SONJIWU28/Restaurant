import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Waiter implements Runnable {
    private final int id;
    private final BlockingQueue<Request> queue;
    private final Kitchen kitchen;
    private final AtomicBoolean work = new AtomicBoolean(true);

    private WaiterCallback callback;
    private volatile CompletableFuture<Void> arrived;

    public Waiter(int id, BlockingQueue<Request> queue, Kitchen kitchen) {
        this.id = id;
        this.queue = queue;
        this.kitchen = kitchen;
    }

    public void setCallback(WaiterCallback cb) { this.callback = cb; }

    private void state(String s) {
        if (callback != null) callback.onState(id, s);
    }

    public void notifyArrived() {
        if (arrived != null) arrived.complete(null);
    }

    private void waitArrival() {
        arrived = new CompletableFuture<>();
        try {
            arrived.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {}
    }

    public void run() {
        Thread.currentThread().setName("Официант-" + id);
        System.out.println("[Официант-" + id + "] Начал смену");

        while (work.get() || !queue.isEmpty()) {
            try {
                state("IDLE");
                Request req = queue.poll(300, TimeUnit.MILLISECONDS);
                if (req == null) continue;

                System.out.println("[Официант-" + id + "] Принял: " + req);
                state("TO_TABLE:" + req.clientId);
                waitArrival();

                Order order = new Order(req.clientId, req.dish, id);
                state("TO_KITCHEN:" + req.clientId);
                waitArrival();

                if (!kitchen.add(order)) {
                    System.out.println("[Официант-" + id + "] Кухня отклонила");
                    continue;
                }

                state("WAIT:" + req.clientId);
                Order ready = order.getReady().orTimeout(60, TimeUnit.SECONDS).join();

                state("DELIVER:" + req.clientId);
                waitArrival();

                System.out.println("[Официант-" + id + "] Доставил: " + ready.getDish() +
                        " клиенту " + ready.getClientId() + " (" + ready.getWait() + " мс)");

                state("DONE:" + req.clientId);

                state("RETURN:" + req.clientId);
                waitArrival();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.out.println("[Официант-" + id + "] Ошибка: " + e.getMessage());
            }
        }
        System.out.println("[Официант-" + id + "] Закончил смену");
    }

    public void stop() {
        work.set(false);
        if (arrived != null) arrived.complete(null);
    }

    public int getId() { return id; }

    public record Request(int clientId, Dish dish) {
        public String toString() { return "Клиент " + clientId + " -> " + dish; }
    }

    public interface WaiterCallback {
        void onState(int waiterId, String state);
    }
}