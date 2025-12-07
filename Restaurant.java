import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Ресторан — координирует работу всех компонентов.
 */
public class Restaurant {
    
    private final int cookCount;
    private final int waiterCount;
    private final int kitchenQueueSize;
    private final int clientQueueSize;
    private final int maxClients;

    private Kitchen kitchen;
    private ClientGenerator clientGenerator;
    private BlockingDeque<Waiter.ClientRequest> clientQueue;
    
    private final List<Waiter> waiters = new ArrayList<>();
    private final List<Thread> waiterThreads = new ArrayList<>();
    
    private volatile boolean running = false;
    private Waiter.WaiterCallback callback;

    public Restaurant(int cooks, int waiters, int kitchenQueueSize, int clientQueueSize, int maxClients) {
        this.cookCount = cooks;
        this.waiterCount = waiters;
        this.kitchenQueueSize = kitchenQueueSize;
        this.clientQueueSize = clientQueueSize;
        this.maxClients = maxClients;
    }

    public void setCallback(Waiter.WaiterCallback callback) {
        this.callback = callback;
    }

    public Waiter getWaiter(int id) {
        for (Waiter w : waiters) {
            if (w.getId() == id) {
                return w;
            }
        }
        return null;
    }

    public void open() {
        if (running) {
            return;
        }

        Order.reset();
        ClientGenerator.reset();

        System.out.println("========== РЕСТОРАН ОТКРЫТ ==========");
        System.out.println("Поваров: " + cookCount + ", Официантов: " + waiterCount + ", Клиентов: " + maxClients);

        // Создаём очередь клиентов
        clientQueue = new LinkedBlockingDeque<>(clientQueueSize);

        // Запускаем кухню
        kitchen = new Kitchen(cookCount, kitchenQueueSize);
        kitchen.start();

        // Запускаем официантов
        for (int i = 1; i <= waiterCount; i++) {
            Waiter waiter = new Waiter(i, clientQueue, kitchen);
            if (callback != null) {
                waiter.setCallback(callback);
            }
            waiters.add(waiter);
            
            Thread thread = new Thread(waiter);
            waiterThreads.add(thread);
            thread.start();
        }

        // Запускаем генератор клиентов
        clientGenerator = new ClientGenerator(clientQueue, maxClients);
        clientGenerator.start();
        
        running = true;
    }

    public void close() {
        if (!running) {
            return;
        }

        System.out.println("========== ЗАКРЫВАЕТСЯ ==========");
        System.out.println("В очереди клиентов: " + clientQueue.size());
        System.out.println("В очереди кухни: " + kitchen.getQueueSize());

        // Останавливаем генератор
        clientGenerator.shutdown();

        // Останавливаем официантов
        for (Waiter waiter : waiters) {
            waiter.stop();
        }

        // Ждём завершения потоков официантов
        for (Thread thread : waiterThreads) {
            try {
                thread.join(30000);
                if (thread.isAlive()) {
                    System.out.println("Поток " + thread.getName() + " не завершился");
                    thread.interrupt();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Закрываем кухню
        kitchen.shutdown();

        // Очищаем списки
        waiters.clear();
        waiterThreads.clear();
        running = false;

        System.out.println("========== ЗАКРЫТ ==========");
    }

    public void runFor(long milliseconds) throws InterruptedException {
        open();
        Thread.sleep(milliseconds);
        close();
    }

    public boolean isRunning() {
        return running;
    }
}
