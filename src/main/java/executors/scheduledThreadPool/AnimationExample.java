package executors.scheduledThreadPool;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;

/**
 * Пример использования ScheduledExecutorService для выполнения задачи с фиксированной периодичностью.
 * Демонстрирует:
 * - Планирование задачи с периодическим выполнением.
 * - Увеличение скорости до достижения максимального значения.
 * - Автоматическое завершение работы планировщика при достижении максимальной скорости.
 */
@Slf4j
public class AnimationExample {

    public static void main(String[] args) {
        // Создаем планировщик с одним потоком
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Переменная для хранения текущей скорости (используем AtomicInteger для потокобезопасности)
        AtomicInteger speed = new AtomicInteger(1);

        /**
         * Задача для увеличения скорости:
         * Каждый раз, когда задача выполняется, скорость увеличивается на 1.
         * Если скорость достигает 5, планировщик завершает свою работу.
         */
        Runnable increaseSpeed = () -> {
            int currentSpeed = speed.incrementAndGet();
            log.info("Скорость увеличена: {}", currentSpeed);

            if (currentSpeed >= 5) {
                log.info("Максимальная скорость достигнута!");
                scheduler.shutdown(); // Завершаем работу планировщика
            }
        };

        /**
         * Планируем выполнение задачи с фиксированной периодичностью:
         * - Задача начинает выполняться немедленно (initialDelay = 0 секунд).
         * - Выполняется каждые 2 секунды (period = 2 секунды).
         */
        scheduler.scheduleAtFixedRate(
                increaseSpeed,
                0,
                2,
                TimeUnit.SECONDS
        );

        log.info("Планировщик запущен, скорость увеличивается каждые 2 секунды.");
    }
}
