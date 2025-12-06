import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Kitchen {
    private final BlockingQueue<Order> queue;
    private final ExecutorService cooks;
    private final Thread dispatcher;
    private final AtomicBoolean open = new AtomicBoolean(false);
    private final int size;

    public Kitchen(int cookNum, int queueSize) {
        this.size = queueSize;
        this.queue = new LinkedBlockingQueue<>(queueSize);
        this.cooks = Executors.newFixedThreadPool(cookNum, new ThreadFactory() {
            private final AtomicInteger n = new AtomicInteger(0);
            public Thread newThread(Runnable r) {
                return new Thread(r, "Повар-" + n.incrementAndGet());
            }
        });
        this.dispatcher = new Thread(this::dispatch, "Диспетчер");
    }

    public void start() {
        if (open.compareAndSet(false, true)) {
            dispatcher.start();
            System.out.println("[КУХНЯ] Открыта");
        }
    }

    private void dispatch() {
        while (open.get() || !queue.isEmpty()) {
            try {
                Order o = queue.poll(200, TimeUnit.MILLISECONDS);
                if (o != null) cooks.submit(() -> cook(o));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void cook(Order o) {
        String name = Thread.currentThread().getName();
        System.out.println("[" + name + "] Готовит: " + o);
        try {
            int t = o.getDish().getTime();
            Thread.sleep(t);
            System.out.println("[" + name + "] Готово: " + o + " (" + t + " мс)");
            o.done();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean add(Order o) {
        if (!open.get()) return false;
        boolean ok = queue.offer(o);
        if (ok) System.out.println("[КУХНЯ] Принят: " + o + " | Очередь: " + queue.size() + "/" + size);
        return ok;
    }

    public void shutdown() {
        System.out.println("[КУХНЯ] Закрывается...");
        open.set(false);
        try {
            dispatcher.join(5000);
            cooks.shutdown();
            cooks.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            cooks.shutdownNow();
        }
        System.out.println("[КУХНЯ] Закрыта");
    }

    public boolean isOpen() { return open.get(); }
}