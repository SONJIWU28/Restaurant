import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Генератор клиентов.
 * 
 * Периодически создаёт новых клиентов и добавляет их в очередь.
 * VIP клиенты добавляются в начало очереди (приоритет).
 */
public class ClientGenerator {
    
    private static final AtomicInteger CLIENT_ID = new AtomicInteger(0);
    private static final AtomicInteger SERVED_COUNT = new AtomicInteger(0);
    private static final AtomicInteger VIP_PARTIES = new AtomicInteger(0);

    private final BlockingDeque<Waiter.ClientRequest> queue;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final int maxClients;

    public ClientGenerator(BlockingDeque<Waiter.ClientRequest> queue, int maxClients) {
        this.queue = queue;
        this.maxClients = maxClients;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Генератор"));
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            System.out.println("[КЛИЕНТЫ] Открыто (макс: " + maxClients + ")");
            scheduleNext();
        }
    }

    private void scheduleNext() {
        if (!running.get() || CLIENT_ID.get() >= maxClients) {
            return;
        }
        
        int delay = ThreadLocalRandom.current().nextInt(
            Constants.CLIENT_GEN_MIN_DELAY_MS, 
            Constants.CLIENT_GEN_MAX_DELAY_MS
        );
        scheduler.schedule(this::generateClients, delay, TimeUnit.MILLISECONDS);
    }

    private void generateClients() {
        if (!running.get() || CLIENT_ID.get() >= maxClients) {
            return;
        }

        int current = CLIENT_ID.get();
        int remaining = maxClients - current;
        double progress = (double) current / maxClients;

        if (shouldGenerateVip(progress, remaining)) {
            generateVipParty(remaining);
        } else {
            generateRegularClients(remaining);
        }

        scheduleNext();
    }

    private boolean shouldGenerateVip(double progress, int remaining) {
        if (VIP_PARTIES.get() > 0 || remaining < Constants.VIP_BATCH_MIN) {
            return false;
        }
        
        // В окне 40-60% — шанс 30%
        if (progress >= Constants.VIP_START_PROGRESS && progress <= Constants.VIP_END_PROGRESS) {
            return ThreadLocalRandom.current().nextDouble() < Constants.VIP_CHANCE;
        }
        
        // После 70% — гарантированный VIP
        return progress >= Constants.VIP_GUARANTEED_PROGRESS;
    }

    private void generateVipParty(int remaining) {
        VIP_PARTIES.incrementAndGet();
        
        int batchSize = ThreadLocalRandom.current().nextInt(Constants.VIP_BATCH_MIN, Constants.VIP_BATCH_MAX);
        batchSize = Math.min(batchSize, remaining);
        
        System.out.println("[КЛИЕНТЫ] === VIP ПАРТИЯ (" + batchSize + " чел) ===");
        
        for (int i = 0; i < batchSize && CLIENT_ID.get() < maxClients; i++) {
            addClient(true);
        }
    }

    private void generateRegularClients(int remaining) {
        int batchSize = ThreadLocalRandom.current().nextInt(1, 3);
        batchSize = Math.min(batchSize, remaining);
        
        for (int i = 0; i < batchSize && CLIENT_ID.get() < maxClients; i++) {
            addClient(false);
        }
    }

    private void addClient(boolean vip) {
        int id = CLIENT_ID.incrementAndGet();
        Dish dish;
        if (vip) {
            dish = Dish.randomVip();
        } else {
            dish = Dish.randomRegular();
        }
        Waiter.ClientRequest request = new Waiter.ClientRequest(id, dish, vip);

        // VIP в начало очереди, обычные в конец
        boolean added;
        if (vip) {
            added = queue.offerFirst(request);
        } else {
            added = queue.offerLast(request);
        }

        if (added) {
            System.out.println("[КЛИЕНТЫ] " + request);
        } else {
            System.out.println("[КЛИЕНТЫ] #" + id + " ушёл — очередь полная");
        }
    }

    public void stop() {
        running.set(false);
    }

    public void shutdown() {
        stop();
        scheduler.shutdown();
        
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("[КЛИЕНТЫ] Закрыто");
    }

    public static void served() {
        SERVED_COUNT.incrementAndGet();
    }

    public static void reset() {
        CLIENT_ID.set(0);
        SERVED_COUNT.set(0);
        VIP_PARTIES.set(0);
    }
}
