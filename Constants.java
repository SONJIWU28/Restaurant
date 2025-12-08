import java.awt.Color;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class Constants {

    private Constants() {}

    // ==================== ЛОГИРОВАНИЕ ====================
    
    private static final Object LOG_LOCK = new Object();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    /**
     * Потокобезопасное логирование с меткой времени.
     * Гарантирует атомарность вывода и предотвращает перемешивание строк.
     */
    public static void log(String message) {
        synchronized (LOG_LOCK) {
            String timestamp = LocalTime.now().format(TIME_FORMAT);
            System.out.println("[" + timestamp + "] " + message);
        }
    }

    // ==================== РЕСТОРАН ====================
    
    public static final int KITCHEN_SIZE = 20;
    public static final int CLIENT_QUEUE = 50;
    
    public static final String[] COOK_NAMES = {
        "Шеф", "Су-шеф", "Соусье", "Гриль", "Горячий", "Холодный", "Кондитер"
    };

    // ==================== ОКНО ====================
    
    public static final int WINDOW_WIDTH = 1000;
    public static final int WINDOW_HEIGHT = 700;
    public static final String FONT_NAME = "JetBrains Mono";

    // ==================== КООРДИНАТЫ ====================
    
    public static final int KITCHEN_Y = 480;
    public static final int COUNTER_Y = 465;
    public static final double WAITER_SPEED = 5.0;

    // ==================== СТОЛЫ ====================
    
    public static final int TABLE_START_X = 80;
    public static final int TABLE_START_Y = 70;
    public static final int TABLE_SPACING_X = 140;
    public static final int TABLE_SPACING_Y = 90;
    public static final int TABLE_ROWS = 4;
    public static final int TABLE_COLUMNS = 5;
    
    public static final int REGULAR_TABLE_CAPACITY = 2;
    public static final int REGULAR_TABLE_WIDTH = 50;
    public static final int REGULAR_TABLE_HEIGHT = 30;
    
    public static final int VIP_TABLE_CAPACITY = 6;
    public static final int VIP_TABLE_WIDTH = 200;
    public static final int VIP_TABLE_HEIGHT = 90;

    // ==================== ЦВЕТА ====================
    
    public static final Color COLOR_BACKGROUND = new Color(173, 216, 230);
    public static final Color COLOR_TABLE = new Color(139, 90, 43);
    public static final Color COLOR_TABLE_VIP = new Color(180, 120, 60);
    public static final Color COLOR_CHAIR = new Color(160, 82, 45);
    public static final Color COLOR_KITCHEN_BG = new Color(70, 70, 75);
    public static final Color COLOR_COUNTER = new Color(60, 60, 65);
    public static final Color COLOR_CONTROL_PANEL = new Color(45, 45, 50);
    public static final Color COLOR_BTN_START = new Color(76, 175, 80);
    public static final Color COLOR_BTN_STOP = new Color(244, 67, 54);
    
    public static final Color COLOR_CLIENT_READY = new Color(100, 200, 100);
    public static final Color COLOR_CLIENT_WAITING = new Color(255, 200, 100);
    public static final Color COLOR_CLIENT_VIP = new Color(120, 170, 255);
    public static final Color COLOR_SKIN = new Color(255, 220, 185);
    public static final Color COLOR_HAIR = new Color(80, 60, 40);
    public static final Color COLOR_VIP_CROWN = new Color(255, 215, 0);

    // ==================== ТАЙМАУТЫ ====================
    
    public static final int WAITER_POLL_TIMEOUT_MS = 300;
    public static final int WAITER_ARRIVAL_TIMEOUT_SEC = 10;
    public static final int ORDER_TIMEOUT_SEC = 90;
    public static final long CLIENT_LEAVE_DELAY_MS = 1500;
    public static final int ANIMATION_INTERVAL_MS = 30;

    // ==================== ГЕНЕРАЦИЯ КЛИЕНТОВ ====================
    
    public static final int CLIENT_GEN_MIN_DELAY_MS = 400;
    public static final int CLIENT_GEN_MAX_DELAY_MS = 1000;
    
    public static final double VIP_START_PROGRESS = 0.4;
    public static final double VIP_END_PROGRESS = 0.6;
    public static final double VIP_GUARANTEED_PROGRESS = 0.7;
    public static final double VIP_CHANCE = 0.30;
    public static final int VIP_BATCH_MIN = 2;
    public static final int VIP_BATCH_MAX = 4;
    public static final double VIP_COOK_MULTIPLIER = 1.5;
}
