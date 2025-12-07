import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Управление столами ресторана.
 */
public final class TableManager {

    private TableManager() {}

    /**
     * Найти случайный свободный стол подходящего типа.
     */
    public static TableVisual findFreeTable(List<TableVisual> tables, boolean needVip) {
        List<TableVisual> candidates = new ArrayList<>();
        // Собираем подходящие столы
        for (TableVisual table : tables) {
            if (needVip && !table.vipTable) {
                continue;
            }
            if (!needVip && table.vipTable) {
                continue;
            }
            if (hasFreeSeat(table)) {
                candidates.add(table);
            }
        }
        // Выбираем случайный
        if (!candidates.isEmpty()) {
            return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        }
        
        // VIP может сесть за обычный стол
        if (needVip) {
            candidates.clear();
            for (TableVisual table : tables) {
                if (!table.vipTable && hasFreeSeat(table)) {
                    candidates.add(table);
                }
            }
            if (!candidates.isEmpty()) {
                return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            }
        }
        
        return null;
    }

    /**
     * Проверить есть ли свободное место за столом.
     */
    public static boolean hasFreeSeat(TableVisual table) {
        for (TableVisual.Seat seat : table.seats) {
            if (seat.isFree()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Найти индекс первого свободного места.
     */
    public static int findFreeSeatIndex(TableVisual table) {
        for (int i = 0; i < table.seats.size(); i++) {
            if (table.seats.get(i).isFree()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Найти место клиента.
     */
    public static TableVisual.Seat findSeatByClient(List<TableVisual> tables, int clientId) {
        for (TableVisual table : tables) {
            for (TableVisual.Seat seat : table.seats) {
                if (seat.clientId == clientId) {
                    return seat;
                }
            }
        }
        return null;
    }

    /**
     * Найти стол клиента.
     */
    public static TableVisual findTableByClient(List<TableVisual> tables, int clientId) {
        for (TableVisual table : tables) {
            for (TableVisual.Seat seat : table.seats) {
                if (seat.clientId == clientId) {
                    return table;
                }
            }
        }
        return null;
    }

    /**
     * Установить статус "ожидает еду".
     */
    public static void setWaitingForFood(List<TableVisual> tables, int clientId) {
        TableVisual.Seat seat = findSeatByClient(tables, clientId);
        if (seat != null) {
            seat.waitingForFood = true;
        }
    }

    /**
     * Установить статус "еда доставлена".
     */
    public static void setHasFood(List<TableVisual> tables, int clientId) {
        TableVisual.Seat seat = findSeatByClient(tables, clientId);
        if (seat != null) {
            seat.hasFood = true;
            seat.waitingForFood = false;
        }
    }

    /**
     * Освободить место клиента.
     */
    public static void releaseSeat(List<TableVisual> tables, int clientId) {
        TableVisual.Seat seat = findSeatByClient(tables, clientId);
        if (seat != null) {
            seat.reset();
        }
    }

    /**
     * Сбросить все столы.
     */
    public static void resetAll(List<TableVisual> tables) {
        for (TableVisual table : tables) {
            for (TableVisual.Seat seat : table.seats) {
                seat.reset();
            }
        }
    }
}
