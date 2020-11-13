package com.xiaomi.mace.demo;

import java.util.List;

public class Utils {
    public static double mean(List<Long> list) {
        double total = 0;
        for (long item : list) {
            total += item;
        }
        return total / list.size();
    }

    public static double variance(List<Long> list) {
        double mean = mean(list);
        double totalSquaredDeviation = 0;
        for (long item : list) {
            totalSquaredDeviation += Math.pow((item - mean), 2);
        }
        return totalSquaredDeviation / list.size();
    }

    public static double std(List<Long> list) {
        return Math.sqrt(variance(list));
    }
}
