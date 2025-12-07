import java.util.concurrent.ThreadLocalRandom;

/**
 * Блюда ресторана с временем приготовления.
 */
public enum Dish {
    
    // Обычные блюда
    SALAD("Салат", 1000, 2000),
    SOUP("Суп", 2000, 3000),
    PASTA("Паста", 2500, 4000),
    STEAK("Стейк", 3500, 5000),
    DESSERT("Десерт", 800, 1500),

    // VIP блюда
    VIP_SET("VIP сет", 6000, 9000),
    VIP_STEAK("Блэк ангус", 5500, 8000),
    VIP_DESSERT("Дегустация", 5000, 7000);

    private static final Dish[] REGULAR = {SALAD, SOUP, PASTA, STEAK, DESSERT};
    private static final Dish[] VIP_ONLY = {VIP_SET, VIP_STEAK, VIP_DESSERT};

    private final String name;
    private final int minTime;
    private final int maxTime;

    Dish(String name, int minTime, int maxTime) {
        this.name = name;
        this.minTime = minTime;
        this.maxTime = maxTime;
    }

    public String getName() {
        return name;
    }

    public int getTime() {
        return ThreadLocalRandom.current().nextInt(minTime, maxTime + 1);
    }

    public static Dish randomRegular() {
        return REGULAR[ThreadLocalRandom.current().nextInt(REGULAR.length)];
    }

    public static Dish randomVip() {
        return VIP_ONLY[ThreadLocalRandom.current().nextInt(VIP_ONLY.length)];
    }

    @Override
    public String toString() {
        return name;
    }
}
