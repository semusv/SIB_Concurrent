package executors.scheduledThreadPool;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;

/**
 * Пример использования ScheduledExecutorService для реализации обратного отсчёта.
 * Демонстрирует:
 * - Планирование задачи с фиксированной периодичностью.
 * - Выполнение обратного отсчёта от 10 до 0.
 * - Автоматическое завершение работы планировщика после завершения отсчёта.
 */
@Slf4j
public class CountdownExample {

    public static void main(String[] args) {
        // Создаем планировщик с одним потоком
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Переменная для хранения текущего значения отсчёта (потокобезопасная)
        AtomicInteger count = new AtomicInteger(10);

        /**
         * Задача обратного отсчёта:
         * Каждый раз, когда задача выполняется, значение уменьшается на 1.
         * Если значение достигает 0, выводится "Пуск!", и планировщик завершает работу.
         */
        Runnable countdown = () -> {
            int current = count.getAndDecrement();
            log.info("Осталось: {}", current);

            if (current == 0) {
                log.info("Пуск!");
                scheduler.shutdown(); // Завершаем работу планировщика
            }
        };

        /**
         * Планируем выполнение задачи с фиксированной периодичностью:
         * - Задача начинает выполняться немедленно (initialDelay = 0 секунд).
         * - Выполняется каждую секунду (period = 1 секунда).
         */
        scheduler.scheduleAtFixedRate(
                countdown,
                0,
                1,
                TimeUnit.SECONDS
        );

        log.info("Обратный отсчёт начат.");
    }
}
