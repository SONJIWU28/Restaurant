import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

public class Restaurant {
    private final int cookNum, waiterNum, kitchenSize, queueSize, maxClients;

    private Kitchen kitchen;
    private ClientGenerator generator;
    private final List<Waiter> waiters = new ArrayList<>();
    private final List<Thread> threads = new ArrayList<>();
    private BlockingQueue<Waiter.Request> clientQueue;

    private volatile boolean run = false;
    private Waiter.WaiterCallback callback;

    public Restaurant(int cooks, int waiters, int kitchenSize, int queueSize, int maxClients) {
        this.cookNum = cooks;
        this.waiterNum = waiters;
        this.kitchenSize = kitchenSize;
        this.queueSize = queueSize;
        this.maxClients = maxClients;
    }

    public void setCallback(Waiter.WaiterCallback cb) { this.callback = cb; }

    public Waiter getWaiter(int id) {
        return waiters.stream().filter(w -> w.getId() == id).findFirst().orElse(null);
    }

    public void open() {
        if (run) return;

        Order.reset();
        ClientGenerator.reset();

        System.out.println("========== РЕСТОРАН ОТКРЫТ ==========");

        clientQueue = new LinkedBlockingQueue<>(queueSize);

        kitchen = new Kitchen(cookNum, kitchenSize);
        kitchen.start();

        for (int i = 1; i <= waiterNum; i++) {
            Waiter w = new Waiter(i, clientQueue, kitchen);
            if (callback != null) w.setCallback(callback);
            waiters.add(w);
            Thread t = new Thread(w);
            threads.add(t);
            t.start();
        }

        generator = new ClientGenerator(clientQueue, maxClients);
        generator.start();
        run = true;
    }

    public void close() {
        if (!run) return;

        System.out.println("========== ЗАКРЫВАЕТСЯ ==========");
        generator.shutdown();
        waiters.forEach(Waiter::stop);
        for (Thread t : threads) {
            try {
                t.join(30000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        kitchen.shutdown();

        waiters.clear();
        threads.clear();
        run = false;

        System.out.println("========== ЗАКРЫТ ==========");
    }
    public void runFor(long ms) throws InterruptedException {
        open();
        Thread.sleep(ms);
        close();
    }
    public boolean isRunning() { return run; }
}