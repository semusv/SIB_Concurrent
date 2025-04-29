package executors.scheduledThreadPool;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Пример работы ScheduledExecutorService:
 * - Запуск задачи с задержкой
 * - Периодическое выполнение
 * - Корректная отмена задачи
 */
@Slf4j
public class DelayedSchedulerExample {

    public static void main(String[] args) throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        // Создаем планировщик с 1 потоком
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Задача для выполнения
        Runnable task = () -> {
            counter.incrementAndGet();
            log.info("Выполняю задачу..." + counter.get());
        };

        // 1. Запуск с однократной задержкой
        log.info("Запланировали задачу с задержкой 2 секунды");
        ScheduledFuture<?> scheduledTask = scheduler.schedule(task, 2, TimeUnit.SECONDS);

        // Ждем выполнения
        Thread.sleep(3000);

        // 2. Периодический запуск (с фиксированной задержкой)
        log.info("\nЗапускаем периодическую задачу (каждые 1 секунду)");
        ScheduledFuture<?> periodicTask = scheduler.scheduleWithFixedDelay(
                task,
                0,    // Начальная задержка
                1,    // Задержка между выполнениями
                TimeUnit.SECONDS
        );

        // Даем поработать 5 секунд
        Thread.sleep(5000);

        // 3. Отменяем периодическую задачу
        log.info("Отменяем периодическую задачу...");
        periodicTask.cancel(false); // false - не прерывать если выполняется

        // 4. Graceful shutdown
        scheduler.shutdown();
        if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
            log.warn("Принудительное завершение...");
            scheduler.shutdownNow();
        }
    }
}