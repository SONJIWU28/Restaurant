import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Кухня ресторана.
 * 
 * Реализует пул поваров через ExecutorService.
 * Заказы поступают через BlockingQueue и обрабатываются параллельно.
 */
public class Kitchen {
    
    private final int maxQueueSize;
    private final BlockingQueue<Order> orderQueue;
    private final ExecutorService cookPool;
    private final Thread dispatcher;
    private final AtomicBoolean open = new AtomicBoolean(false);

    public Kitchen(int cookCount, int queueSize) {
        this.maxQueueSize = queueSize;
        this.orderQueue = new LinkedBlockingQueue<>(queueSize);
        this.cookPool = Executors.newFixedThreadPool(cookCount, new CookThreadFactory());
        this.dispatcher = new Thread(this::dispatchOrders, "Диспетчер");
    }

    public void start() {
        if (open.compareAndSet(false, true)) {
            dispatcher.start();
            System.out.println("[КУХНЯ] Открыта");
        }
    }

    /**
     * Добавить заказ в очередь кухни.
     */
    public boolean addOrder(Order order) {
        if (!open.get()) {
            return false;
        }
        
        boolean accepted = orderQueue.offer(order);
        int queueSize = orderQueue.size();
        
        if (accepted) {
            String loadStatus = getLoadStatus(queueSize);
            System.out.println("[КУХНЯ] Принят: " + order + " | Очередь: " + queueSize + "/" + maxQueueSize + loadStatus);
        } else {
            System.out.println("[КУХНЯ] Очередь полная, отклонён: " + order);
        }
        
        return accepted;
    }

    private String getLoadStatus(int queueSize) {
        double load = (double) queueSize / maxQueueSize;
        if (load > 0.8) {
            return " [КРИТИЧЕСКАЯ ЗАГРУЗКА]";
        }
        if (load > 0.5) {
            return " [Высокая загрузка]";
        }
        return "";
    }

    /**
     * Диспетчер — раздаёт заказы поварам.
     */
    private void dispatchOrders() {
        while (open.get() || !orderQueue.isEmpty()) {
            try {
                Order order = orderQueue.poll(200, TimeUnit.MILLISECONDS);
                if (order != null) {
                    cookPool.submit(() -> cook(order));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (RejectedExecutionException e) {
                System.out.println("[КУХНЯ] Повара перегружены");
                break;
            }
        }
    }

    /**
     * Приготовление блюда.
     */
    private void cook(Order order) {
        String cookName = Thread.currentThread().getName();
        System.out.println("[" + cookName + "] Готовит: " + order);
        
        try {
            int cookTime = order.getDish().getTime();
            if (order.isVip()) {
                cookTime = (int) (cookTime * Constants.VIP_COOK_MULTIPLIER);
            }
            
            Thread.sleep(cookTime);
            
            System.out.println("[" + cookName + "] Готово: " + order + " (" + cookTime + " мс)");
            order.done();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[" + cookName + "] Прерван: " + order);
        }
    }

    public void shutdown() {
        System.out.println("[КУХНЯ] Закрывается...");
        open.set(false);
        
        try {
            dispatcher.join(5000);
            cookPool.shutdown();
            
            if (!cookPool.awaitTermination(10, TimeUnit.SECONDS)) {
                cookPool.shutdownNow();
                System.out.println("[КУХНЯ] Принудительное завершение");
            }
        } catch (InterruptedException e) {
            cookPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("[КУХНЯ] Закрыта");
    }

    public boolean isOpen() {
        return open.get();
    }

    public int getQueueSize() {
        return orderQueue.size();
    }

    private static class CookThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            int idx = counter.getAndIncrement();
            String name = Constants.COOK_NAMES[idx % Constants.COOK_NAMES.length];
            return new Thread(r, name);
        }
    }
}
