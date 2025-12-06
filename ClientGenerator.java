import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientGenerator {
    private static final AtomicInteger ID = new AtomicInteger(0);

    private final BlockingQueue<Waiter.Request> queue;
    private final ScheduledExecutorService timer;
    private final AtomicBoolean run = new AtomicBoolean(false);
    private final int minDelay, maxDelay, maxClients;

    public ClientGenerator(BlockingQueue<Waiter.Request> queue, int maxClients) {
        this(queue, 800, 2000, maxClients);
    }

    public ClientGenerator(BlockingQueue<Waiter.Request> queue, int minDelay, int maxDelay, int maxClients) {
        this.queue = queue;
        this.minDelay = minDelay;
        this.maxDelay = maxDelay;
        this.maxClients = maxClients;
        this.timer = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Генератор"));
    }

    public void start() {
        if (run.compareAndSet(false, true)) {
            System.out.println("[КЛИЕНТЫ] Открыто (макс: " + maxClients + ")");
            next();
        }
    }

    private void next() {
        if (!run.get() || ID.get() >= maxClients) return;
        int delay = ThreadLocalRandom.current().nextInt(minDelay, maxDelay + 1);
        timer.schedule(this::gen, delay, TimeUnit.MILLISECONDS);
    }

    private void gen() {
        if (!run.get() || ID.get() >= maxClients) return;

        int id = ID.incrementAndGet();
        Waiter.Request req = new Waiter.Request(id, Dish.random());

        if (queue.offer(req)) {
            System.out.println("[КЛИЕНТЫ] Новый: " + req);
        } else {
            System.out.println("[КЛИЕНТЫ] Клиент " + id + " ушёл - нет мест");
        }
        next();
    }

    public void stop() { run.set(false); }

    public void shutdown() {
        stop();
        timer.shutdown();
        try {
            timer.awaitTermination(2, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) { timer.shutdownNow(); }
        System.out.println("[КЛИЕНТЫ] Закрыто");
    }

    public static void reset() { ID.set(0); }
}