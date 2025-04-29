package executors.scheduledThreadPool;

import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * Пример использования ScheduledExecutorService для выполнения задачи автосохранения.
 * Демонстрирует:
 * - Планирование задачи с фиксированной периодичностью.
 * - Автосохранение данных через заданные интервалы времени.
 * - Грамотное завершение работы планировщика после завершения работы приложения.
 */
@Slf4j
public class AutoSaveExample {

    public static void main(String[] args) {
        // Создаем планировщик с одним потоком
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        /**
         * Задача автосохранения:
         * Выполняет сохранение данных (в данном случае просто логирует время выполнения).
         * В реальном приложении здесь мог бы быть код сохранения в БД или файл.
         */
        Runnable autoSaveTask = () -> {
            log.info("Автосохранение данных... Время: {}", Instant.now().atZone(ZoneId.systemDefault()).toLocalTime());
        };

        /**
         * Планируем выполнение задачи с фиксированной периодичностью:
         * - Задача начинает выполнение немедленно (initialDelay = 0 секунд).
         * - Выполняется каждые 5 секунд (period = 5 секунд).
         */
        scheduler.scheduleAtFixedRate(
                autoSaveTask,
                0,
                5,
                TimeUnit.SECONDS
        );

        log.info("Планировщик запущен, автосохранение будет выполняться каждые 5 секунд.");

        // Обработка завершения работы
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Получен сигнал завершения работы...");

            try {
                scheduler.shutdown(); // Завершаем работу планировщика
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    log.warn("Планировщик не завершил работу в течение 2 секунд. Принудительное завершение...");
                }
            } catch (InterruptedException e) {
                log.error("Ожидание завершения работы планировщика было прервано: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }));

        // Эмуляция работы приложения (через 1 минуту завершаем)
        try {
            Thread.sleep(60 * 1000); // 1 минута
        } catch (InterruptedException e) {
            log.error("Работа приложения была прервана: {}", e.getMessage());
            Thread.currentThread().interrupt(); // Восстанавливаем статус прерывания
        } finally {
            // Завершаем работу планировщика
            scheduler.shutdown();
            log.info("Планировщик завершил работу.");
        }
    }
}
