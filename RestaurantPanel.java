import java.awt.*;
import java.util.List;
import javax.swing.*;

/**
 * Панель визуализации ресторана.
 */
public class RestaurantPanel extends JPanel {

    private final List<TableVisual> tables;
    private final List<WaiterVisual> waiters;
    private final List<CookVisual> cooks;

    public RestaurantPanel(List<TableVisual> tables, List<WaiterVisual> waiters, List<CookVisual> cooks) {
        this.tables = tables;
        this.waiters = waiters;
        this.cooks = cooks;
        setBackground(Constants.COLOR_BACKGROUND);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawKitchen(g2);
        drawTables(g2);
        drawCooks(g2);
        drawWaiters(g2);
        drawLegend(g2);
    }

    private void drawKitchen(Graphics2D g2) {
        int w = getWidth();

        // Фон кухни
        g2.setColor(new Color(90, 90, 95));
        g2.fillRect(0, Constants.KITCHEN_Y, w, 200);

        // Верхняя граница
        g2.setColor(Constants.COLOR_KITCHEN_BG);
        g2.fillRect(0, Constants.KITCHEN_Y, w, 20);

        // Стол раздачи
        g2.setColor(Constants.COLOR_COUNTER);
        g2.fillRoundRect(100, Constants.COUNTER_Y, w - 200, 25, 8, 8);

        // Плиты
        for (int i = 0; i < 4; i++) {
            int px = 120 + i * 150;
            int py = Constants.KITCHEN_Y + 40;
            
            g2.setColor(new Color(50, 50, 55));
            g2.fillRoundRect(px, py, 80, 50, 6, 6);
            
            g2.setColor(new Color(40, 40, 45));
            g2.fillOval(px + 10, py + 10, 25, 25);
            g2.fillOval(px + 45, py + 10, 25, 25);
        }
    }

    private void drawTables(Graphics2D g2) {
        synchronized (tables) {
            for (TableVisual table : tables) {
                drawTable(g2, table);
            }
        }
    }

    private void drawTable(Graphics2D g2, TableVisual t) {
        // Тень
        g2.setColor(new Color(0, 0, 0, 30));
        g2.fillRoundRect(t.x + 3, t.y + 3, t.width, t.height, 8, 8);

        // Стол
        Color tableColor;
        if (t.vipTable) {
            tableColor = Constants.COLOR_TABLE_VIP;
        } else {
            tableColor = Constants.COLOR_TABLE;
        }
        g2.setColor(tableColor);
        g2.fillRoundRect(t.x, t.y, t.width, t.height, 8, 8);
        g2.setColor(tableColor.darker());
        g2.drawRoundRect(t.x, t.y, t.width, t.height, 8, 8);

        // Стулья
        drawChairs(g2, t);

        // Клиенты
        for (int i = 0; i < t.seats.size(); i++) {
            TableVisual.Seat seat = t.seats.get(i);
            if (!seat.isFree()) {
                Point pos = getSeatPosition(t, i);
                drawClient(g2, pos.x, pos.y, seat);
            }
            if (seat.hasFood) {
                Point pos = getSeatPosition(t, i);
                drawPlate(g2, pos.x - 8, pos.y + 18);
            }
        }
    }

    private void drawChairs(Graphics2D g2, TableVisual t) {
        g2.setColor(Constants.COLOR_CHAIR);
        
        if (t.vipTable) {
            int cx = t.x + t.width / 2;
            int[] offsets = {-70, 0, 70};
            for (int offset : offsets) {
                g2.fillRoundRect(cx + offset - 6, t.y - 16, 12, 14, 4, 4);
                g2.fillRoundRect(cx + offset - 6, t.y + t.height + 2, 12, 14, 4, 4);
            }
        } else {
            int cy = t.y + t.height / 2;
            g2.fillRoundRect(t.x - 12, cy - 8, 10, 16, 3, 3);
            g2.fillRoundRect(t.x + t.width + 2, cy - 8, 10, 16, 3, 3);
        }
    }

    private Point getSeatPosition(TableVisual t, int seatInd) {
        if (t.vipTable) {
            int cx = t.x + t.width / 2;
            int[] offsets = {-70, 0, 70};
            if (seatInd < 3) {
                return new Point(cx + offsets[seatInd], t.y - 8);
            } else {
                return new Point(cx + offsets[seatInd - 3], t.y + t.height + 20);
            }
        } else {
            int cy = t.y + t.height / 2;
            if (seatInd == 0) {
                return new Point(t.x - 18, cy);
            } else {
                return new Point(t.x + t.width + 18, cy);
            }
        }
    }

    private void drawClient(Graphics2D g2, int x, int y, TableVisual.Seat seat) {
        // Цвет тела
        Color bodyColor;
        if (seat.vip) {
            bodyColor = Constants.COLOR_CLIENT_VIP;
        } else if (seat.waitingForFood) {
            bodyColor = Constants.COLOR_CLIENT_WAITING;
        } else {
            bodyColor = Constants.COLOR_CLIENT_READY;
        }

        // Тело
        g2.setColor(bodyColor);
        g2.fillOval(x - 10, y, 20, 22);

        // Голова
        g2.setColor(Constants.COLOR_SKIN);
        g2.fillOval(x - 7, y - 12, 14, 14);

        // Волосы
        g2.setColor(Constants.COLOR_HAIR);
        g2.fillArc(x - 7, y - 14, 14, 10, 0, 180);

        // Корона VIP
        if (seat.vip) {
            g2.setColor(Constants.COLOR_VIP_CROWN);
            g2.fillPolygon(
                new int[]{x - 5, x, x + 5}, 
                new int[]{y - 10, y - 17, y - 10}, 
                3
            );
        }

        // Номер клиента
        g2.setColor(Color.WHITE);
        g2.setFont(new Font(Constants.FONT_NAME, Font.BOLD, 8));
        String idStr = String.valueOf(seat.clientId);
        int textWidth = g2.getFontMetrics().stringWidth(idStr);
        g2.drawString(idStr, x - textWidth / 2, y + 15);
    }

    private void drawPlate(Graphics2D g2, int x, int y) {
        g2.setColor(Color.WHITE);
        g2.fillOval(x, y, 16, 10);
        g2.setColor(new Color(180, 80, 60));
        g2.fillOval(x + 3, y + 2, 10, 6);
    }

    private void drawCooks(Graphics2D g2) {
        for (CookVisual cook : cooks) {
            // Тело
            g2.setColor(Color.WHITE);
            g2.fillOval(cook.x - 10, cook.y, 20, 26);

            // Голова
            g2.setColor(Constants.COLOR_SKIN);
            g2.fillOval(cook.x - 7, cook.y - 14, 14, 14);

            // Колпак
            g2.setColor(Color.WHITE);
            g2.fillRect(cook.x - 5, cook.y - 24, 10, 10);
            g2.fillOval(cook.x - 7, cook.y - 26, 14, 6);
        }
    }

    private void drawWaiters(Graphics2D g2) {
        for (WaiterVisual w : waiters) {
            int x = (int) w.x;
            int y = (int) w.y;

            // Тень
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillOval(x - 8, y + 24, 16, 6);

            // Тело
            g2.setColor(w.color);
            g2.fillOval(x - 9, y, 18, 24);

            // Голова
            g2.setColor(Constants.COLOR_SKIN);
            g2.fillOval(x - 6, y - 12, 12, 12);

            // Волосы
            g2.setColor(new Color(60, 40, 20));
            g2.fillArc(x - 6, y - 13, 12, 8, 0, 180);

            // Поднос с едой
            if (w.hasFood) {
                g2.setColor(new Color(100, 100, 100));
                g2.fillOval(x + 6, y + 2, 14, 5);
                g2.setColor(new Color(200, 100, 70));
                g2.fillOval(x + 8, y - 1, 10, 6);
            }

            // Номер
            g2.setColor(Color.WHITE);
            g2.setFont(new Font(Constants.FONT_NAME, Font.BOLD, 8));
            g2.drawString(String.valueOf(w.id), x - 2, y + 15);

            // Статус
            String status = w.state.getDisplayName();
            if (!status.isEmpty()) {
                g2.setFont(new Font(Constants.FONT_NAME, Font.PLAIN, 7));
                g2.setColor(new Color(80, 80, 80));
                int textWidth = g2.getFontMetrics().stringWidth(status);
                g2.drawString(status, x - textWidth / 2, y + 36);
            }
        }
    }

    private void drawLegend(Graphics2D g2) {
        int y = Constants.KITCHEN_Y + 115;
        g2.setFont(new Font(Constants.FONT_NAME, Font.PLAIN, 9));

        // Готов
        g2.setColor(Constants.COLOR_CLIENT_READY);
        g2.fillOval(15, y, 8, 8);
        g2.setColor(Color.WHITE);
        g2.drawString("Готов", 26, y + 7);

        // Ждёт
        g2.setColor(Constants.COLOR_CLIENT_WAITING);
        g2.fillOval(70, y, 8, 8);
        g2.setColor(Color.WHITE);
        g2.drawString("Ждёт", 81, y + 7);

        // VIP
        g2.setColor(Constants.COLOR_CLIENT_VIP);
        g2.fillOval(120, y, 8, 8);
        g2.setColor(Color.WHITE);
        g2.drawString("VIP", 131, y + 7);

        // Официант
        g2.setColor(Color.BLUE);
        g2.fillOval(160, y, 8, 8);
        g2.setColor(Color.WHITE);
        g2.drawString("Официант", 171, y + 7);
    }
}
