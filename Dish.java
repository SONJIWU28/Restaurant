import java.util.concurrent.ThreadLocalRandom;

public enum Dish {
    SALAD("Салат", 1000, 2000),
    SOUP("Суп", 2000, 3000),
    PASTA("Паста", 2500, 4000),
    STEAK("Стейк", 3500, 5000),
    DESSERT("Десерт", 800, 1500);

    private final String name;
    private final int min, max;
    private static final Dish[] ALL = values();

    Dish(String name, int min, int max) {
        this.name = name;
        this.min = min;
        this.max = max;
    }

    public String getName() { return name; }

    public int getTime() {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static Dish random() {
        return ALL[ThreadLocalRandom.current().nextInt(ALL.length)];
    }

    public String toString() { return name; }
}