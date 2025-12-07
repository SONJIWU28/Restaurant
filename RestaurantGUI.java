import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * Главное окно симулятора ресторана.
 */
public class RestaurantGUI extends JFrame implements Waiter.WaiterCallback {

    // UI компоненты
    private JSpinner cooksSpinner;
    private JSpinner waitersSpinner;
    private JSpinner timeSpinner;
    private JSpinner clientsSpinner;
    private JButton startButton;
    private JButton stopButton;
    private JLabel statusLabel;
    private JLabel statsLabel;
    private RestaurantPanel panel;

    // Данные визуализации
    private final List<TableVisual> tables = Collections.synchronizedList(new ArrayList<>());
    private final List<WaiterVisual> waiters = new CopyOnWriteArrayList<>();
    private final List<CookVisual> cooks = new CopyOnWriteArrayList<>();

    // Счётчики
    private final java.util.concurrent.atomic.AtomicInteger servedCount = new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicInteger totalCount = new java.util.concurrent.atomic.AtomicInteger(0);

    // Симуляция
    private Restaurant restaurant;
    private ScheduledExecutorService animator;
    private volatile boolean running = false;
    private int targetClients = 0;

    public RestaurantGUI() {
        super("Ресторан");
        initTables();
        initUI();
    }

    private void initTables() {
        tables.clear();
        int tableId = 1;

        // Обычные столы
        for (int row = 0; row < Constants.TABLE_ROWS; row++) {
            for (int col = 0; col < Constants.TABLE_COLUMNS; col++) {
                // Пропускаем VIP-зону в центре
                boolean isVipArea = (row == 1 || row == 2) && (col == 2 || col == 3);
                if (isVipArea) {
                    continue;
                }

                int x = Constants.TABLE_START_X + col * Constants.TABLE_SPACING_X;
                int y = Constants.TABLE_START_Y + row * Constants.TABLE_SPACING_Y;
                
                TableVisual table = new TableVisual(tableId++, x, y);
                table.setCapacity(Constants.REGULAR_TABLE_CAPACITY);
                table.setSize(Constants.REGULAR_TABLE_WIDTH, Constants.REGULAR_TABLE_HEIGHT);
                tables.add(table);
            }
        }

        // VIP стол
        int vipX = Constants.TABLE_START_X + 2 * Constants.TABLE_SPACING_X - 30;
        int vipY = Constants.TABLE_START_Y + Constants.TABLE_SPACING_Y + 15;
        
        TableVisual vipTable = new TableVisual(tableId, vipX, vipY);
        vipTable.vipTable = true;
        vipTable.setCapacity(Constants.VIP_TABLE_CAPACITY);
        vipTable.setSize(Constants.VIP_TABLE_WIDTH, Constants.VIP_TABLE_HEIGHT);
        tables.add(vipTable);
    }

    private void initUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        setMinimumSize(new Dimension(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT));
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(createControlPanel(), BorderLayout.NORTH);
        
        panel = new RestaurantPanel(tables, waiters, cooks);
        mainPanel.add(panel, BorderLayout.CENTER);
        
        setContentPane(mainPanel);
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        controlPanel.setBackground(Constants.COLOR_CONTROL_PANEL);

        controlPanel.add(createLabel("Повара:"));
        cooksSpinner = createSpinner(1, 6, 2);
        controlPanel.add(cooksSpinner);

        controlPanel.add(createLabel("Официанты:"));
        waitersSpinner = createSpinner(1, 5, 3);
        controlPanel.add(waitersSpinner);

        controlPanel.add(createLabel("Время(с):"));
        timeSpinner = createSpinner(10, 300, 60);
        controlPanel.add(timeSpinner);

        controlPanel.add(createLabel("Клиенты:"));
        clientsSpinner = createSpinner(5, 100, 30);
        controlPanel.add(clientsSpinner);

        startButton = createButton("Старт", Constants.COLOR_BTN_START);
        startButton.addActionListener(e -> startSimulation());
        controlPanel.add(startButton);

        stopButton = createButton("Стоп", Constants.COLOR_BTN_STOP);
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopSimulation());
        controlPanel.add(stopButton);

        statusLabel = createLabel("Готов");
        controlPanel.add(statusLabel);

        statsLabel = createLabel("");
        controlPanel.add(statsLabel);

        return controlPanel;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font(Constants.FONT_NAME, Font.PLAIN, 12));
        return label;
    }

    private JSpinner createSpinner(int min, int max, int value) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
        spinner.setFont(new Font(Constants.FONT_NAME, Font.PLAIN, 12));
        return spinner;
    }

    private JButton createButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font(Constants.FONT_NAME, Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void startSimulation() {
        reset();

        int numCooks = (int) cooksSpinner.getValue();
        int numWaiters = (int) waitersSpinner.getValue();
        int duration = (int) timeSpinner.getValue();
        int numClients = (int) clientsSpinner.getValue();

        targetClients = numClients;
        initCooks(numCooks);
        initWaiters(numWaiters);

        running = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        statusLabel.setText("Работает...");

        restaurant = new Restaurant(numCooks, numWaiters, Constants.KITCHEN_SIZE, Constants.CLIENT_QUEUE, numClients);
        restaurant.setCallback(this);

        // Аниматор
        animator = Executors.newSingleThreadScheduledExecutor();
        animator.scheduleAtFixedRate(() -> {
            if (running) {
                updateWaiterPositions();
                SwingUtilities.invokeLater(() -> {
                    statsLabel.setText("Готово: " + servedCount.get() + "/" + totalCount.get());
                    panel.repaint();
                });
            }
        }, 0, Constants.ANIMATION_INTERVAL_MS, TimeUnit.MILLISECONDS);

        // Симуляция в отдельном потоке
        new Thread(() -> {
            try {
                restaurant.runFor(duration * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            SwingUtilities.invokeLater(this::onSimulationEnd);
        }).start();
    }

    private void reset() {
        waiters.clear();
        cooks.clear();
        servedCount.set(0);
        totalCount.set(0);
        TableManager.resetAll(tables);
    }

    private void initCooks(int count) {
        for (int i = 0; i < count; i++) {
            int x = 150 + i * 120;
            int y = Constants.KITCHEN_Y + 55;
            cooks.add(new CookVisual(i + 1, x, y, Color.WHITE));
        }
    }

    private void initWaiters(int count) {
        Color[] colors = {Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, Color.MAGENTA};
        
        for (int i = 0; i < count; i++) {
            int x = 180 + i * 90;
            int y = Constants.COUNTER_Y - 35;
            waiters.add(new WaiterVisual(i + 1, x, y, colors[i % colors.length]));
        }
    }

    private void stopSimulation() {
        running = false;
        if (restaurant != null) {
            new Thread(() -> restaurant.close()).start();
        }
    }

    private void onSimulationEnd() {
        running = false;
        
        if (animator != null) {
            animator.shutdown();
        }

        for (WaiterVisual w : waiters) {
            w.reset();
        }
        TableManager.resetAll(tables);

        panel.repaint();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        statusLabel.setText("Готово: " + servedCount.get() + "/" + targetClients);
    }

    // ==================== CALLBACK ОТ ОФИЦИАНТОВ ====================

    @Override
    public void onStateChanged(int waiterId, String state) {
        WaiterVisual waiter = findWaiter(waiterId);
        if (waiter == null) {
            return;
        }

        String[] parts = state.split(":");
        String command = parts[0];
        
        int clientId = 0;
        if (parts.length > 1) {
            try {
                clientId = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return;
            }
        }
        
        boolean vip = parts.length > 2 && "1".equals(parts[2]);

        handleWaiterState(waiter, command, clientId, vip);
    }

    private WaiterVisual findWaiter(int id) {
        for (WaiterVisual w : waiters) {
            if (w.id == id) {
                return w;
            }
        }
        return null;
    }

    private void handleWaiterState(WaiterVisual w, String command, int clientId, boolean vip) {
        switch (command) {
            case "IDLE" -> {
                w.state = WaiterState.IDLE;
                w.targetX = w.homeX;
                w.targetY = w.homeY;
                w.hasFood = false;
                w.awaitingArrival = false;
            }
            
            case "TO_TABLE" -> {
                totalCount.incrementAndGet();
                TableVisual table = TableManager.findFreeTable(tables, vip);
                if (table != null) {
                    int seatIdx = TableManager.findFreeSeatIndex(table);
                    if (seatIdx >= 0) {
                        TableVisual.Seat seat = table.seats.get(seatIdx);
                        seat.clientId = clientId;
                        seat.vip = vip;
                        
                        Point pos = getSeatPosition(table, seatIdx);
                        w.state = WaiterState.GOING_TO_TABLE;
                        w.targetX = pos.x;
                        w.targetY = pos.y - 25;
                        w.currentClientId = clientId;
                        w.awaitingArrival = true;
                    }
                }
            }
            
            case "TO_KITCHEN" -> {
                TableManager.setWaitingForFood(tables, clientId);
                w.state = WaiterState.GOING_TO_KITCHEN;
                w.targetX = w.homeX;
                w.targetY = Constants.COUNTER_Y - 15;
                w.awaitingArrival = true;
            }
            
            case "WAIT" -> {
                w.state = WaiterState.WAITING_FOR_FOOD;
                w.awaitingArrival = false;
            }
            
            case "DELIVER" -> {
                TableVisual table = TableManager.findTableByClient(tables, clientId);
                if (table != null) {
                    TableVisual.Seat seat = TableManager.findSeatByClient(tables, clientId);
                    int seatIdx = table.seats.indexOf(seat);
                    Point pos = getSeatPosition(table, seatIdx);
                    
                    w.state = WaiterState.DELIVERING;
                    w.targetX = pos.x;
                    w.targetY = pos.y - 25;
                    w.hasFood = true;
                    w.awaitingArrival = true;
                }
            }
            
            case "DONE" -> {
                TableManager.setHasFood(tables, clientId);
                servedCount.incrementAndGet();
                ClientGenerator.served();
                w.hasFood = false;
                w.awaitingArrival = false;
                
                // Клиент уходит через некоторое время
                int cid = clientId;
                CompletableFuture.delayedExecutor(Constants.CLIENT_LEAVE_DELAY_MS, TimeUnit.MILLISECONDS)
                    .execute(() -> TableManager.releaseSeat(tables, cid));
            }
            
            case "RETURN" -> {
                w.state = WaiterState.RETURNING;
                w.targetX = w.homeX;
                w.targetY = w.homeY;
                w.awaitingArrival = true;
            }
        }
    }

    private Point getSeatPosition(TableVisual table, int seatIndex) {
        if (table.vipTable) {
            int cx = table.x + table.width / 2;
            int[] offsets = {-70, 0, 70};
            if (seatIndex < 3) {
                return new Point(cx + offsets[seatIndex], table.y - 8);
            } else {
                return new Point(cx + offsets[seatIndex - 3], table.y + table.height + 20);
            }
        } else {
            int cy = table.y + table.height / 2;
            if (seatIndex == 0) {
                return new Point(table.x - 18, cy);
            } else {
                return new Point(table.x + table.width + 18, cy);
            }
        }
    }

    private void updateWaiterPositions() {
        for (WaiterVisual w : waiters) {
            double dx = w.targetX - w.x;
            double dy = w.targetY - w.y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance > 3) {
                w.x += (dx / distance) * Constants.WAITER_SPEED;
                w.y += (dy / distance) * Constants.WAITER_SPEED;
            } else if (w.awaitingArrival) {
                w.awaitingArrival = false;
                if (restaurant != null) {
                    Waiter waiter = restaurant.getWaiter(w.id);
                    if (waiter != null) {
                        waiter.notifyArrived();
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RestaurantGUI().setVisible(true));
    }
}
