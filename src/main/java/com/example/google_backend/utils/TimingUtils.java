package com.example.google_backend.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class TimingUtils {
    private static final Logger logger = Logger.getLogger(TimingUtils.class.getName());

    public static <T> T measureExecutionTime(String operationName, Supplier<T> operation) {
        Instant startTime = Instant.now();
        try {
            T result = operation.get();
            Instant endTime = Instant.now();
            long timeElapsed = Duration.between(startTime, endTime).toMillis();
            logger.info(operationName + " 耗时: " + timeElapsed + " ms");
            return result;
        } catch (Exception e) {
            Instant endTime = Instant.now();
            long timeElapsed = Duration.between(startTime, endTime).toMillis();
            logger.severe(operationName + " 失败,耗时: " + timeElapsed + " ms, 错误: " + e.getMessage());
            throw e;
        }
    }
}
