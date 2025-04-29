package queue.blocking;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Продвинутая реализация паттерна Producer-Consumer с использованием BlockingQueue.
 *
 * <p>Особенности:
 * - Динамическая скорость работы производителей и потребителей.
 * - Возможность плавного завершения работы.
 * - Подробное логирование состояния системы.
 * - Конфигурируемые параметры работы.
 */
@Slf4j
public class ProducerConsumerExampleExec {
    // Конфигурационные параметры
    private static final int QUEUE_CAPACITY = 5; // Размер очереди
    private static final int PRODUCTION_COUNT = 10; // Количество элементов для производства
    private static final int MAX_PRODUCER_DELAY_MS = 700; // Максимальная задержка производителя
    private static final int CONSUMER_DELAY_MS = 1200; // Задержка потребителя
    private static volatile boolean isRunning = true; // Флаг для управления потоками

    public static void main(String[] args) {
        // Очередь с ограниченной емкостью
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        // Использование пула потоков для производителей
        ExecutorService producer = Executors.newFixedThreadPool(3);
        startProducers(producer, queue);

        // Поток-потребитель
        Thread consumer = createConsumerThread(queue);
        consumer.start();

        // Обработчик завершения работы
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isRunning = false; // Останавливаем работу потоков
            log.info("\nПолучен сигнал завершения работы...");

            try {
                // Даем потокам время на завершение
                producer.awaitTermination(3, TimeUnit.SECONDS);
                consumer.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
    }

    /**
     * Запускает несколько производителей с использованием пула потоков.
     *
     * @param producer Пул потоков для производителей.
     * @param queue    Очередь для обмена данными.
     */
    private static void startProducers(ExecutorService producer, BlockingQueue<Integer> queue) {
        AtomicInteger produced = new AtomicInteger(0); // Счетчик произведенных элементов
        Random random = new Random();

        for (int i = 0; i < 3; i++) {
            producer.execute(() -> {
                try {
                    while (isRunning && produced.get() < PRODUCTION_COUNT) {
                        int item = produced.incrementAndGet(); // Генерация нового элемента

                        // Добавление элемента с блокировкой
                        queue.put(item);
                        log.info("[Producer] Отправлен: {} (Размер очереди: {}/{})", item, queue.size(), QUEUE_CAPACITY);

                        // Имитация задержки обработки
                        Thread.sleep(random.nextInt(MAX_PRODUCER_DELAY_MS));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("[Producer] Прерван во время работы", e);
                } finally {
                    log.info("[Producer] Завершил работу. Произведено элементов: {}", produced.get());
                }
            });
        }
    }

    /**
     * Создает поток-потребитель.
     *
     * @param queue Очередь для обмена данными.
     * @return Поток-потребитель.
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
