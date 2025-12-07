import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Официант ресторана.
 * 
 * Каждый официант работает в отдельном потоке.
 * Цикл работы: взять заказ → посадить клиента → отнести на кухню → 
 *              дождаться готовности → доставить клиенту.
 */
public class Waiter implements Runnable {
    
    private final int id;
    private final BlockingDeque<ClientRequest> clientQueue;
    private final Kitchen kitchen;
    private final AtomicBoolean working = new AtomicBoolean(true);
    
    private WaiterCallback callback;
    private volatile CompletableFuture<Void> arrivedSignal;

    public Waiter(int id, BlockingDeque<ClientRequest> clientQueue, Kitchen kitchen) {
        this.id = id;
        this.clientQueue = clientQueue;
        this.kitchen = kitchen;
    }

    public void setCallback(WaiterCallback callback) {
        this.callback = callback;
    }

    public int getId() {
        return id;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Официант-" + id);
        System.out.println("[Официант-" + id + "] Начал смену");

        while (working.get() || !clientQueue.isEmpty()) {
            try {
                processNextClient();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("[Официант-" + id + "] Закончил смену");
    }

    private void processNextClient() throws InterruptedException {
        sendState("IDLE");
        
        ClientRequest request = clientQueue.poll(Constants.WAITER_POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (request == null) {
            return;
        }

        System.out.println("[Официант-" + id + "] Принял: " + request);

        // Ведём клиента к столу
        String vipFlag;
        if (request.vip()) {
            vipFlag = "1";
        } else {
            vipFlag = "0";
        }
        sendState("TO_TABLE:" + request.clientId() + ":" + vipFlag);
        waitForArrival();

        // Создаём заказ и несём на кухню
        Order order = new Order(request.clientId(), request.dish(), id, request.vip());
        sendState("TO_KITCHEN:" + request.clientId());
        waitForArrival();

        // Отдаём заказ на кухню
        if (!kitchen.addOrder(order)) {
            System.out.println("[Официант-" + id + "] Кухня отклонила заказ");
            return;
        }
        // Ждём готовности
        sendState("WAIT:" + request.clientId());
        
        try {
            Order ready = order.getReady()
                .orTimeout(Constants.ORDER_TIMEOUT_SEC, TimeUnit.SECONDS)
                .join();
            // Несём еду клиенту
            sendState("DELIVER:" + request.clientId());
            waitForArrival();
            System.out.println("[Официант-" + id + "] Доставил: " + ready.getDish() + 
                " клиенту " + ready.getClientId() + " (" + ready.getWaitTime() + " мс)");

            sendState("DONE:" + request.clientId());

            // Возвращаемся на базу
            sendState("RETURN:" + request.clientId());
            waitForArrival();

        } catch (CompletionException e) {
            System.out.println("[Официант-" + id + "] Таймаут заказа для клиента " + request.clientId());
        }
    }

    private void sendState(String state) {
        if (callback != null) {
            callback.onStateChanged(id, state);
        }
    }

    public void notifyArrived() {
        if (arrivedSignal != null) {
            arrivedSignal.complete(null);
        }
    }

    private void waitForArrival() {
        arrivedSignal = new CompletableFuture<>();
        try {
            arrivedSignal.get(Constants.WAITER_ARRIVAL_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.out.println("[Официант-" + id + "] Таймаут движения");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            System.out.println("[Официант-" + id + "] Ошибка: " + e.getMessage());
        }
    }

    public void stop() {
        working.set(false);
        if (arrivedSignal != null) {
            arrivedSignal.complete(null);
        }
    }

    public record ClientRequest(int clientId, Dish dish, boolean vip) {
        @Override
        public String toString() {
            String prefix;
            if (vip) {
                prefix = "[VIP] ";
            } else {
                prefix = "";
            }
            return prefix + "Клиент " + clientId + " -> " + dish;
        }
    }

    /**
     * Callback для уведомления GUI о состоянии официанта.
     */
    public interface WaiterCallback {
        void onStateChanged(int waiterId, String state);
    }
}
