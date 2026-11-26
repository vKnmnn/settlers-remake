package jsettlers.ai.original;

import jsettlers.common.player.ECivilisation;

/** Independently expressed building-industry rules from S3.EXE. See docs/original-ai.md. */
public final class S3ConstructionPolicy {
    private static final int[] POPULATION = {0, 40, 100, 500, 2000};
    // Rows use original civilisation IDs: Roman, Egyptian, Asian, Amazon.
    private static final int[][] LUMBERJACKS = {{0,4,6,8,8}, {0,3,4,5,8}, {0,4,8,10,10}, {0,4,6,8,10}};
    private static final int[][] WOOD = {{0,30,50,70,100}, {0,20,35,55,90}, {0,45,75,100,140}, {0,30,50,70,100}};
    private static final int[][] STONECUTTERS = {{0,2,3,5,7}, {0,3,5,7,9}, {0,1,2,3,4}, {0,2,3,5,7}};
    private static final int[][] STONE = {{0,20,40,60,90}, {0,30,50,70,100}, {0,10,20,30,45}, {0,20,40,60,90}};
    private static final int[] BUILDING_PRIORITY = {65000,55000,45000,25000,10000};
    private static final int[] STOCK_PRIORITY = {65000,45000,35000,15000,5000};
    private static final int[] BANDS = {10,30,50,70,100};

    private S3ConstructionPolicy() { }

    public static int lumberjackPriority(ECivilisation civilisation, int population, int buildings, int wood) {
        int race = race(civilisation);
        return industryPriority(buildings, target(population, LUMBERJACKS[race]), wood,
                target(population, WOOD[race]), civilisation == ECivilisation.ASIAN);
    }

    public static int stonecutterPriority(ECivilisation civilisation, int population, int buildings, int stone) {
        if (buildings == 0) return 65000;
        int race = race(civilisation);
        return industryPriority(buildings, target(population, STONECUTTERS[race]), stone,
                target(population, STONE[race]), civilisation == ECivilisation.EGYPTIAN);
    }

    public static int sawmillPriority(int lumberjacks, int sawmills) {
        return sawmills == 0 || sawmills < (lumberjacks + 2) / 3 ? 65000 : 0;
    }

    public static int foresterPriority(ECivilisation civilisation, int lumberjacks, int foresters, int wood) {
        int capacity = civilisation == ECivilisation.ROMAN ? 250 : civilisation == ECivilisation.EGYPTIAN ? 300 : 200;
        int shortage = lumberjacks * 100 - foresters * capacity - wood;
        if (shortage > 80) return 65000;
        if (shortage > 60) return 65000 / 2;
        if (shortage > 40) return 65000 / 3;
        if (shortage > 20) return 65000 / 4;
        if (shortage > 0) return 65000 / 6;
        return 0;
    }

    private static int industryPriority(int buildings, int desired, int stock, int desiredStock, boolean allowOneExtra) {
        int capacityPriority = priority(buildings, desired, BUILDING_PRIORITY);
        int stockPriority = priority(stock, desiredStock, STOCK_PRIORITY);
        int result = Math.max(capacityPriority, stockPriority);
        if (stockPriority > capacityPriority && buildings >= desired) {
            int penalty = allowOneExtra ? (buildings - desired) * 65000 : 65000;
            result = Math.max(0, result - penalty);
        }
        return result;
    }

    private static int priority(int actual, int desired, int[] priorities) {
        if (desired <= 0) return 0;
        int percentage = actual * 100 / desired;
        for (int i = 0; i < BANDS.length; i++) {
            if (percentage < BANDS[i]) return priorities[i];
        }
        return 0;
    }

    // The executable stores single-precision slopes, then truncates the interpolated target.
    static int target(int population, int[] values) {
        int stage = population < 40 ? 0 : population < 100 ? 1 : population < 500 ? 2 : 3;
        float slope = (float) (values[stage + 1] - values[stage]) / (POPULATION[stage + 1] - POPULATION[stage]);
        return (int) (values[stage] + (double) slope * (population - POPULATION[stage]));
    }

    private static int race(ECivilisation civilisation) {
        switch (civilisation) {
        case ROMAN: return 0;
        case EGYPTIAN: return 1;
        case ASIAN: return 2;
        case AMAZON: return 3;
        default: throw new IllegalArgumentException("Unknown civilisation: " + civilisation);
        }
    }
}
