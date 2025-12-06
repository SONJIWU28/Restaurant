import javax.swing.*;
import java.awt.*;
import java.util.List;
public class RestaurantPanel extends JPanel {

    private static final Color BG_FLOOR = new Color(173, 216, 230);
    private static final Color TABLE_COLOR = new Color(139, 90, 43);
    private static final Color CHAIR_COLOR = new Color(160, 82, 45);
    private static final Color KITCHEN_BG = new Color(70, 70, 75);
    private static final Color KITCHEN_FLOOR = new Color(90, 90, 95);
    private static final Color COUNTER_COLOR = new Color(60, 60, 65);

    private final int KITCHEN_Y;
    private final int COUNTER_Y;

    private final List<TableVisual> tables;
    private final List<WaiterVisual> waiters;
    private final List<CookVisual> cooks;
    private final List<FoodVisual> counter;

    public RestaurantPanel(int kitchenY, List<TableVisual> tables,
                           List<WaiterVisual> waiters, List<CookVisual> cooks,
                           List<FoodVisual> counter) {
        this.KITCHEN_Y = kitchenY;
        this.COUNTER_Y = kitchenY - 15;
        this.tables = tables;
        this.waiters = waiters;
        this.cooks = cooks;
        this.counter = counter;
        setBackground(BG_FLOOR);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawKitchen(g2);
        drawTables(g2);
        drawFoodOnCounter(g2);
        drawCooks(g2);
        drawWaiters(g2);
        drawLegend(g2);
    }

    private void drawKitchen(Graphics2D g2) {
        int w = getWidth();

        g2.setColor(KITCHEN_FLOOR);
        g2.fillRect(0, KITCHEN_Y, w, getHeight() - KITCHEN_Y);

        g2.setColor(KITCHEN_BG);
        g2.fillRect(0, KITCHEN_Y, w, 20);

        g2.setColor(COUNTER_COLOR);
        g2.fillRoundRect(100, COUNTER_Y - 10, w - 200, 25, 8, 8);
        g2.setColor(new Color(80, 80, 85));
        g2.fillRoundRect(105, COUNTER_Y - 7, w - 210, 8, 4, 4);

        g2.setColor(new Color(200, 200, 200));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.drawString("КУХНЯ", w / 2 - 25, KITCHEN_Y + 80);

        for (int i = 0; i < 4; i++) {
            int px = 120 + i * 150;
            int py = KITCHEN_Y + 40;
            g2.setColor(new Color(50, 50, 55));
            g2.fillRoundRect(px, py, 80, 50, 6, 6);
            g2.setColor(new Color(40, 40, 45));
            g2.fillOval(px + 10, py + 10, 25, 25);
            g2.fillOval(px + 45, py + 10, 25, 25);
        }
    }

    private void drawTables(Graphics2D g2) {
        synchronized (tables) {
            for (TableVisual t : tables) {
                drawTable(g2, t);
            }
        }
    }

    private void drawTable(Graphics2D g2, TableVisual t) {
        int x = t.x, y = t.y;
        int tw = 50, th = 32;

        g2.setColor(new Color(0, 0, 0, 30));
        g2.fillRoundRect(x + 3, y + 3, tw, th, 6, 6);

        g2.setColor(TABLE_COLOR);
        g2.fillRoundRect(x, y, tw, th, 6, 6);
        g2.setColor(TABLE_COLOR.darker());
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, tw, th, 6, 6);

        g2.setColor(CHAIR_COLOR);
        g2.fillRoundRect(x - 12, y + 6, 10, 20, 4, 4);
        g2.fillRoundRect(x + tw + 2, y + 6, 10, 20, 4, 4);

        if (t.hasClient) {
            drawClient(g2, x + tw / 2, y - 20, t.clientId, t.waitingForFood);
        }

        if (t.hasFood) {
            drawPlate(g2, x + 15, y + 6);
        }
    }

    private void drawClient(Graphics2D g2, int x, int y, int id, boolean waiting) {
        Color bodyColor = waiting ? new Color(255, 200, 100) : new Color(100, 200, 100);
        g2.setColor(bodyColor);
        g2.fillOval(x - 10, y + 8, 20, 24);

        g2.setColor(new Color(255, 220, 185));
        g2.fillOval(x - 8, y - 6, 16, 16);

        g2.setColor(new Color(80, 60, 40));
        g2.fillArc(x - 8, y - 8, 16, 12, 0, 180);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 9));
        String s = String.valueOf(id);
        int sw = g2.getFontMetrics().stringWidth(s);
        g2.drawString(s, x - sw / 2, y + 24);
    }

    private void drawPlate(Graphics2D g2, int x, int y) {
        g2.setColor(Color.WHITE);
        g2.fillOval(x, y, 22, 18);
        g2.setColor(new Color(230, 230, 230));
        g2.drawOval(x, y, 22, 18);
        g2.setColor(new Color(180, 80, 60));
        g2.fillOval(x + 5, y + 4, 12, 10);
    }

    private void drawFoodOnCounter(Graphics2D g2) {
        for (FoodVisual f : counter) {
            g2.setColor(Color.WHITE);
            g2.fillOval(f.x, f.y, 20, 14);
            g2.setColor(new Color(200, 100, 80));
            g2.fillOval(f.x + 4, f.y + 3, 12, 8);

            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.PLAIN, 8));
            g2.drawString("#" + f.clientId, f.x + 2, f.y - 2);
        }
    }

    private void drawCooks(Graphics2D g2) {
        for (CookVisual c : cooks) {
            drawCook(g2, c);
        }
    }

    private void drawCook(Graphics2D g2, CookVisual c) {
        int x = c.x, y = c.y;

        g2.setColor(c.color);
        g2.fillOval(x - 12, y, 24, 30);

        g2.setColor(new Color(255, 220, 185));
        g2.fillOval(x - 9, y - 16, 18, 18);

        g2.setColor(Color.WHITE);
        g2.fillRect(x - 7, y - 28, 14, 14);
        g2.fillOval(x - 9, y - 30, 18, 8);

        if (c.isCooking) {
            int offset = (c.animFrame / 5) % 3;
            g2.setColor(new Color(255, 150, 50, 150));
            g2.fillOval(x - 20 + offset, y + 20, 8, 8);
            g2.fillOval(x + 12 - offset, y + 22, 6, 6);
        }

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 9));
        g2.drawString("П" + c.id, x - 6, y + 45);

        if (c.currentDish != null) {
            g2.setFont(new Font("Arial", Font.PLAIN, 8));
            g2.setColor(new Color(255, 200, 100));
            String dish = c.currentDish.length() > 6 ? c.currentDish.substring(0, 6) : c.currentDish;
            g2.drawString(dish, x - 15, y + 55);
        }
    }

    private void drawWaiters(Graphics2D g2) {
        for (WaiterVisual w : waiters) {
            drawWaiter(g2, w);
        }
    }

    private void drawWaiter(Graphics2D g2, WaiterVisual w) {
        int x = (int) w.x, y = (int) w.y;

        g2.setColor(new Color(0, 0, 0, 40));
        g2.fillOval(x - 10, y + 28, 20, 8);

        g2.setColor(w.color);
        g2.fillOval(x - 11, y, 22, 28);

        g2.setColor(new Color(255, 220, 185));
        g2.fillOval(x - 8, y - 14, 16, 16);

        g2.setColor(new Color(60, 40, 20));
        g2.fillArc(x - 8, y - 15, 16, 10, 0, 180);

        g2.setColor(new Color(30, 30, 30));
        g2.fillRect(x - 3, y + 2, 6, 4);

        if (w.hasFood) {
            g2.setColor(new Color(100, 100, 100));
            g2.fillOval(x + 8, y + 3, 18, 7);
            g2.setColor(new Color(200, 100, 70));
            g2.fillOval(x + 11, y - 1, 12, 8);
        }

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 9));
        g2.drawString("" + w.id, x - 3, y + 18);

        String status = getStatus(w);
        g2.setFont(new Font("Arial", Font.PLAIN, 8));
        g2.setColor(new Color(80, 80, 80));
        int sw = g2.getFontMetrics().stringWidth(status);
        g2.drawString(status, x - sw / 2, y + 42);
    }

    private String getStatus(WaiterVisual w) {
        return switch (w.state) {
            case GOING_TO_TABLE -> "К столу";
            case GOING_TO_KITCHEN -> "На кухню";
            case DELIVERING -> "Несёт";
            case RETURNING -> "Назад";
            case WAITING_FOR_FOOD -> "Ждёт";
            default -> "Свободен";
        };
    }

    private void drawLegend(Graphics2D g2) {
        int x = 15, y = getHeight() - 25;
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));

        g2.setColor(new Color(100, 200, 100));
        g2.fillOval(x, y, 10, 10);
        g2.setColor(Color.BLACK);
        g2.drawString("Клиент (готов)", x + 14, y + 9);

        g2.setColor(new Color(255, 200, 100));
        g2.fillOval(x + 100, y, 10, 10);
        g2.setColor(Color.BLACK);
        g2.drawString("Ждёт еду", x + 114, y + 9);

        g2.setColor(new Color(65, 105, 225));
        g2.fillOval(x + 190, y, 10, 10);
        g2.setColor(Color.BLACK);
        g2.drawString("Официант", x + 204, y + 9);
    }
}
