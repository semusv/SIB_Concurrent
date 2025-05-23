package queue.blocking;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Продвинутая реализация паттерна Producer-Consumer с использованием BlockingQueue.
 *
 * <p>Особенности:
 * - Динамическая скорость работы производителя и потребителя.
 * - Возможность плавного завершения работы.
 * - Подробное логирование состояния системы.
 * - Конфигурируемые параметры работы.
 */
@Slf4j
public class ProducerConsumerExample {
    // Конфигурационные параметры
    private static final int QUEUE_CAPACITY = 5; // Размер очереди
    private static final int PRODUCTION_COUNT = 10; // Количество элементов для производства
    private static final int MAX_PRODUCER_DELAY_MS = 700; // Максимальная задержка производителя
    private static final int CONSUMER_DELAY_MS = 1200; // Задержка потребителя
    private static volatile boolean isRunning = true; // Флаг для управления потоками

    public static void main(String[] args) {
        // Очередь с ограниченной емкостью
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        // Поток-производитель
        Thread producer = createProducerThread(queue);
        producer.start();

        // Поток-потребитель
        Thread consumer = createConsumerThread(queue);
        consumer.start();

        // Обработчик завершения работы
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isRunning = false;
            log.info("\nПолучен сигнал завершения работы...");

            try {
                // Даем потокам время на завершение
                producer.join(3000);
                consumer.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
    }

    /**
     * Создает поток-производитель.
     *
     * @param очередь для обмена данными
     * @return поток-производитель
     */
    private static Thread createProducerThread(BlockingQueue<Integer> queue) {
        return new Thread(() -> {
            Random random = new Random();
            int produced = 1; // Счетчик произведенных элементов

            try {
                while (isRunning && produced <= PRODUCTION_COUNT) {
                    int item = produced++; // Генерация нового элемента

                    // Добавление элемента с таймаутом
                    boolean added = queue.offer(item, 500, TimeUnit.MILLISECONDS);

                    if (added) {
                        log.info("[Producer] Отправлен: {} (Размер очереди: {}/{})", item, queue.size(), QUEUE_CAPACITY);
                    } else {
                        log.warn("[Producer] Таймаут при добавлении элемента");
                    }

                    // Имитация задержки обработки
                    Thread.sleep(random.nextInt(MAX_PRODUCER_DELAY_MS));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[Producer] Прерван во время работы", e);
            } finally {
                log.info("[Producer] Завершил работу. Произведено элементов: {}", produced);
            }
        }, "PRODUCER");
    }

    /**
     * Создает поток-потребитель.
     *
     * @param очередь для обмена данными
     * @return поток-потребитель
     */
    private static Thread createConsumerThread(BlockingQueue<Integer> queue) {
        return new Thread(() -> {
            int consumed = 0; // Счетчик обработанных элементов

            try {
                while (isRunning && consumed < PRODUCTION_COUNT) {
                    // Извлечение элемента с таймаутом
                    Integer item = queue.poll(1, TimeUnit.SECONDS);

                    if (item != null) {
                        consumed++;
                        log.info("[Consumer] Обработан: {} (Осталось: {}/{})", item, queue.size(), QUEUE_CAPACITY);

                        // Имитация задержки обработки
                        Thread.sleep(CONSUMER_DELAY_MS);
                    } else {
                        log.warn("[Consumer] Очередь пуста, ожидание данных...");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[Consumer] Прерван во время работы", e);
            } finally {
                log.info("[Consumer] Завершил работу. Обработано элементов: {}", consumed);
            }
        }, "CONSUMER");
    }
}
