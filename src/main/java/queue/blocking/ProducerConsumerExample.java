package queue.blocking;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * ArrayBlockingQueue
 *
 * <p>Продвинутая реализация паттерна Producer-Consumer с использованием BlockingQueue.
 *
 * <p>Особенности реализации:
 * <ul>
 *     <li>Динамическая скорость работы производителя и потребителя</li>
 *     <li>Возможность плавного завершения работы</li>
 *     <li>Подробное логирование состояния системы</li>
 *     <li>Конфигурируемые параметры работы</li>
 *     </ul>
 */
public class ProducerConsumerExample {
    // Конфигурационные параметры
    private static final int QUEUE_CAPACITY = 5;
    private static final int PRODUCTION_COUNT = 10;
    private static final int MAX_PRODUCER_DELAY_MS = 700;
    private static final int CONSUMER_DELAY_MS = 1200;
    private static volatile boolean isRunning = true;

    public static void main(String[] args) {
        // Инициализация потокобезопасной очереди с ограниченной емкостью
        // Использование ArrayBlockingQueue обеспечивает:
        // - FIFO (первый вошел, первый вышел) порядок элементов
        // - Автоматическую блокировку потоков при переполнении/опустошении
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        // Создание и запуск потока-производителя
        Thread producer = createProducerThread(queue);
        producer.start();

        // Создание и запуск потока-потребителя
        Thread consumer = createConsumerThread(queue);
        consumer.start();

        // Обработка завершения работы
        // Этот код добавляет Shutdown Hook - специальный поток, который выполняется при завершении работы JVM
        // (например, по сигналу Ctrl+C в консоли или при системном завершении).
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isRunning = false;
            System.out.println("\nПолучен сигнал завершения работы...");

            try {
                // Даем потокам время на корректное завершение
                producer.join(3000);
                consumer.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
    }

    /**
     * Создает поток-производитель с конфигурируемыми параметрами.
     *
     * @param queue очередь для обмена данными
     * @return настроенный поток-производитель
     */
    private static Thread createProducerThread(BlockingQueue<Integer> queue) {
        return new Thread(() -> {
            Random random = new Random();
            int produced = 0;

            try {
                while (isRunning && produced < PRODUCTION_COUNT) {
                    // Генерация нового элемента
                    int item = produced++;

                    // Попытка добавить элемент с таймаутом (для демонстрации)
                    boolean added = queue.offer(item, 500, TimeUnit.MILLISECONDS);

                    if (added) {
                        System.out.printf("[Producer] Отправлен: %d (Размер очереди: %d/%d)%n",
                                item, queue.size(), QUEUE_CAPACITY);
                    } else {
                        System.out.println("[Producer] Таймаут при добавлении элемента");
                    }

                    // Динамическая задержка для имитации реальной нагрузки
                    int delay = random.nextInt(MAX_PRODUCER_DELAY_MS);
                    Thread.sleep(delay);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[Producer] Прерван во время работы");
            } finally {
                System.out.println("[Producer] Завершил работу. Произведено элементов: " + produced);
            }
        }, "PRODUCER");
    }

    /**
     * Создает поток-потребитель с конфигурируемыми параметрами.
     *
     * @param queue очередь для обмена данными
     * @return настроенный поток-потребитель
     */
    private static Thread createConsumerThread(BlockingQueue<Integer> queue) {
        return new Thread(() -> {
            int consumed = 0;

            try {
                while (isRunning && consumed < PRODUCTION_COUNT) {
                    // Извлечение элемента с таймаутом
                    Integer item = queue.poll(1, TimeUnit.SECONDS);

                    if (item != null) {
                        consumed++;
                        System.out.printf("[Consumer] Обработан: %d (Осталось: %d/%d)%n",
                                item, queue.size(), QUEUE_CAPACITY);

                        // Фиксированная задержка для имитации обработки
                        Thread.sleep(CONSUMER_DELAY_MS);
                    } else {
                        System.out.println("[Consumer] Очередь пуста, ожидание данных...");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[Consumer] Прерван во время работы");
            } finally {
                System.out.println("[Consumer] Завершил работу. Обработано элементов: " + consumed);
            }
        }, "CONSUMER");
    }
}