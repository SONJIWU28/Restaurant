/**
 * Состояния официанта для визуализации.
 */
public enum WaiterState {
    IDLE(""),
    GOING_TO_TABLE("К столу"),
    GOING_TO_KITCHEN("На кухню"),
    WAITING_FOR_FOOD("Ждёт"),
    DELIVERING("Несёт"),
    RETURNING("Назад");

    private final String displayName;

    WaiterState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
