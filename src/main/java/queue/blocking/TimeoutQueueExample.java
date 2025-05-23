package queue.blocking;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Демонстрация работы с ограниченной блокирующей очередью с таймаутами.
 * Показывает:
 * 1) Добавление элементов с ожиданием при заполненной очереди
 * 2) Извлечение элементов с ожиданием при пустой очереди
 * 3) Корректное завершение работы через Shutdown Hook
 */
@Slf4j
public class TimeoutQueueExample {
    // Константы для настройки поведения
    private static final int QUEUE_CAPACITY = 3;
    private static final int PRODUCER_DELAY_MS = 200;
    private static final int CONSUMER_DELAY_MS = 800;
    private static final int OFFER_TIMEOUT_MS = 500;
    private static final int POLL_TIMEOUT_MS = 1000;
    private static final int CONSUMER_START_DELAY_MS = 1000;

    // Флаг для корректного завершения работы потребителя
    private static final AtomicBoolean running = new AtomicBoolean(true);

    public static void main(String[] args) {
        // Создаем ограниченную очередь (capacity = 3)
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

        // Поток-производитель (добавляет элементы)
        Thread producerThread = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    String item = "Item-" + i;

                    // Пытаемся добавить элемент с таймаутом
                    boolean added = queue.offer(item, OFFER_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                    if (added) {
                        log.info("[Производитель] Успешно добавлен: {}", item);
                    } else {
                        log.warn("[Производитель] Таймаут! Не удалось добавить: {} (очередь полна)", item);
                    }

                    // Имитация времени между добавлением элементов
                    Thread.sleep(PRODUCER_DELAY_MS);
                }

                log.info("[Производитель] Завершил добавление элементов");
            } catch (InterruptedException e) {
                log.warn("[Производитель] Прерван во время работы");
                Thread.currentThread().interrupt();
            }
        }, "ProducerThread");

        // Поток-потребитель (извлекает элементы)
        Thread consumerThread = new Thread(() -> {
            try {
                // Даем время производителю заполнить очередь
                Thread.sleep(CONSUMER_START_DELAY_MS);

                while (running.get()) {
                    // Пытаемся извлечь элемент с таймаутом
                    String item = queue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                    if (item == null) {
                        // Проверяем, нужно ли продолжать работу
                        if (!running.get()) {
                            log.info("[Потребитель] Получен сигнал завершения");
                            break;
                        }
                        log.warn("[Потребитель] Таймаут! Очередь пуста, продолжаю ожидание...");
                        continue;
                    }

                    log.info("[Потребитель] Обработано: {}", item);

                    // Имитация времени обработки элемента
                    Thread.sleep(CONSUMER_DELAY_MS);
                }
                log.info("[Потребитель] Корректно завершил работу");
            } catch (InterruptedException e) {
                log.warn("[Потребитель] Прерван во время работы");
                Thread.currentThread().interrupt();
            }
        }, "ConsumerThread");

        // Shutdown Hook для корректного завершения
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("\nПолучен сигнал завершения работы...");
            running.set(false);

            // Прерываем потоки для быстрого завершения
            producerThread.interrupt();
            consumerThread.interrupt();

            try {
                // Даем потокам время на завершение
                producerThread.join(1000);
                consumerThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            log.info("Очередь при завершении: {}", queue);
            log.info("Программа завершена");
        }));

        // Запускаем потоки
        producerThread.start();
        consumerThread.start();

        // Ждем завершения потоков (для демонстрации)
        try {
            producerThread.join();
            consumerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Главный поток был прерван");
        }

        log.info("Основной поток завершен");
    }
}
