import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.ThreadLocalRandom;

public class RestaurantGUI extends JFrame implements Waiter.WaiterCallback {

    private static final int W = 1000, H = 700;
    private static final int KITCHEN_Y = 480;
    private static final int COUNTER_Y = KITCHEN_Y - 15;
    private static final double SPEED = 5.0;

    private JSpinner cooksSp, waitersSp, timeSp, clientsSp;
    private JButton startBtn, stopBtn;
    private JLabel statusLbl, statsLbl;
    private RestaurantPanel panel;

    private final List<TableVisual> tables = Collections.synchronizedList(new ArrayList<>());
    private final List<WaiterVisual> waiters = new CopyOnWriteArrayList<>();
    private final List<CookVisual> cooks = new CopyOnWriteArrayList<>();
    private final List<FoodVisual> counter = new CopyOnWriteArrayList<>();

    private final Map<Integer, Integer> tableMap = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicInteger served = new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicInteger total = new java.util.concurrent.atomic.AtomicInteger(0);

    private Restaurant restaurant;
    private ScheduledExecutorService animator;
    private volatile boolean running = false;

    public RestaurantGUI() {
        super("Ресторан");
        initTables();
        setupUI();
    }
    private void initTables() {
        tables.clear();
        int x0 = 80, y0 = 70, dx = 140, dy = 95, id = 1;
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 5; c++)
                tables.add(new TableVisual(id++, x0 + c * dx, y0 + r * dy));
    }

    private void setupUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(W, H);
        setLocationRelativeTo(null);

        JPanel main = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.setBackground(new Color(45, 45, 50));

        top.add(lbl("Повара:")); cooksSp = spin(1, 6, 2); top.add(cooksSp);
        top.add(lbl("Официанты:")); waitersSp = spin(1, 5, 3); top.add(waitersSp);
        top.add(lbl("Время(с):")); timeSp = spin(10, 300, 60); top.add(timeSp);
        top.add(lbl("Клиенты:")); clientsSp = spin(5, 100, 20); top.add(clientsSp);

        startBtn = btn("Старт", new Color(76, 175, 80));
        startBtn.addActionListener(e -> start());
        top.add(startBtn);

        stopBtn = btn("Стоп", new Color(244, 67, 54));
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(e -> stop());
        top.add(stopBtn);

        statusLbl = lbl("Готов"); top.add(statusLbl);
        statsLbl = lbl(""); top.add(statsLbl);

        main.add(top, BorderLayout.NORTH);
        panel = new RestaurantPanel(KITCHEN_Y, tables, waiters, cooks, counter);
        main.add(panel, BorderLayout.CENTER);
        setContentPane(main);
    }

    private JLabel lbl(String s) {
        JLabel l = new JLabel(s);
        l.setForeground(Color.WHITE);
        return l;
    }

    private JSpinner spin(int min, int max, int val) {
        return new JSpinner(new SpinnerNumberModel(val, min, max, 1));
    }

    private JButton btn(String s, Color bg) {
        JButton b = new JButton(s);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        return b;
    }

    private void start() {
        reset();
        int nc = (int) cooksSp.getValue();
        int nw = (int) waitersSp.getValue();
        int time = (int) timeSp.getValue();
        int clients = (int) clientsSp.getValue();

        initCooks(nc);
        initWaiters(nw);

        running = true;
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        statusLbl.setText("Работает...");

        restaurant = new Restaurant(nc, nw, 20, 15, clients);
        restaurant.setCallback(this);

        animator = Executors.newSingleThreadScheduledExecutor();
        animator.scheduleAtFixedRate(() -> {
            if (running) {
                moveWaiters();
                SwingUtilities.invokeLater(() -> {
                    statsLbl.setText("Готово: " + served.get() + "/" + total.get());
                    panel.repaint();
                });
            }
        }, 0, 30, TimeUnit.MILLISECONDS);

        new Thread(() -> {
            try { restaurant.runFor(time * 1000L); }
            catch (InterruptedException ignored) {}
            SwingUtilities.invokeLater(this::onEnd);
        }).start();
    }

    private void reset() {
        synchronized (tables) {
            for (TableVisual t : tables) {
                t.hasClient = false;
                t.hasFood = false;
                t.clientId = 0;
                t.waitingForFood = false;
            }
        }
        waiters.clear();
        cooks.clear();
        counter.clear();
        tableMap.clear();
        served.set(0);
        total.set(0);
    }

    private void initCooks(int n) {
        for (int i = 0; i < n; i++)
            cooks.add(new CookVisual(i + 1, 150 + i * 120, KITCHEN_Y + 60, Color.WHITE));
    }

    private void initWaiters(int n) {
        Color[] cols = {Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, Color.MAGENTA};
        for (int i = 0; i < n; i++) {
            int x = 180 + i * 90, y = COUNTER_Y - 30;
            WaiterVisual w = new WaiterVisual(i + 1, x, y, cols[i % 5]);
            w.homeX = x;
            w.homeY = y;
            waiters.add(w);
        }
    }

    private void stop() {
        running = false;
        if (restaurant != null) new Thread(() -> restaurant.close()).start();
    }

    private void onEnd() {
        running = false;
        if (animator != null) animator.shutdown();

        // Очищаем всё
        for (WaiterVisual w : waiters) {
            w.state = WaiterState.IDLE;
            w.x = w.homeX;
            w.y = w.homeY;
            w.hasFood = false;
            w.notification = false;
        }
        synchronized (tables) {
            for (TableVisual t : tables) {
                t.hasClient = false;
                t.hasFood = false;
                t.waitingForFood = false;
            }
        }
        counter.clear();
        tableMap.clear();

        panel.repaint();
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        statusLbl.setText("Завершено");
    }

    @Override
    public void onState(int id, String state) {
        WaiterVisual w = waiters.stream().filter(x -> x.id == id).findFirst().orElse(null);
        if (w == null) return;

        String[] parts = state.split(":");
        String cmd = parts[0];
        int clientId = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        switch (cmd) {
            case "IDLE":
                w.state = WaiterState.IDLE;
                w.targetX = w.homeX;
                w.targetY = w.homeY;
                w.hasFood = false;
                w.notification = false;
                break;

            case "TO_TABLE":
                total.incrementAndGet();
                seat(clientId); // теперь случайный выбор свободного стола
                Integer ti = tableMap.get(clientId);
                if (ti != null) {
                    TableVisual t = tables.get(ti);
                    w.state = WaiterState.GOING_TO_TABLE;
                    w.targetX = t.x + 25;
                    w.targetY = t.y + 40;
                    w.currentClientId = clientId;
                    w.notification = true;
                }
                break;

            case "TO_KITCHEN":
                ti = tableMap.get(clientId);
                if (ti != null) tables.get(ti).waitingForFood = true;
                w.state = WaiterState.GOING_TO_KITCHEN;
                w.targetX = w.homeX;
                // Остановка перед линией выдачи
                w.targetY = COUNTER_Y - 8;
                w.notification = true;
                break;

            case "WAIT":
                w.state = WaiterState.WAITING_FOR_FOOD;
                w.notification = false;
                break;

            case "DELIVER":
                ti = tableMap.get(clientId);
                if (ti != null) {
                    TableVisual t = tables.get(ti);
                    w.state = WaiterState.DELIVERING;
                    w.targetX = t.x + 25;
                    w.targetY = t.y + 40;
                    w.hasFood = true;
                    w.notification = true;
                }
                break;

            case "DONE":
                ti = tableMap.get(clientId);
                if (ti != null) {
                    TableVisual t = tables.get(ti);
                    t.hasFood = true;
                    t.waitingForFood = false;
                    int idx = ti;
                    CompletableFuture.delayedExecutor(1500, TimeUnit.MILLISECONDS).execute(() -> {
                        synchronized (tables) {
                            if (idx < tables.size()) {
                                tables.get(idx).hasClient = false;
                                tables.get(idx).hasFood = false;
                            }
                        }
                        tableMap.remove(clientId);
                    });
                }
                served.incrementAndGet();
                w.hasFood = false;
                w.notification = false;
                break;

            case "RETURN":
                w.state = WaiterState.RETURNING;
                w.targetX = w.homeX;
                w.targetY = w.homeY;
                w.notification = true;
                break;
        }
    }

    private void seat(int clientId) {
        List<Integer> free = new ArrayList<>();
        synchronized (tables) {
            for (int i = 0; i < tables.size(); i++) {
                if (!tables.get(i).hasClient) free.add(i);
            }
            if (free.isEmpty()) return;
            int idx = free.get(ThreadLocalRandom.current().nextInt(free.size()));
            TableVisual t = tables.get(idx);
            t.hasClient = true;
            t.clientId = clientId;
            tableMap.put(clientId, idx);
        }
    }

    private void moveWaiters() {
        for (WaiterVisual w : waiters) {
            double targetX = w.targetX;
            double targetY = Math.min(w.targetY, COUNTER_Y - 8);

            double dx = targetX - w.x;
            double dy = targetY - w.y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist > 3) {
                w.x += (dx / dist) * SPEED;
                w.y += (dy / dist) * SPEED;
            } else if (w.notification) {
                w.notification = false;
                if (restaurant != null) {
                    Waiter real = restaurant.getWaiter(w.id);
                    if (real != null) real.notifyArrived();
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RestaurantGUI().setVisible(true));
    }
}