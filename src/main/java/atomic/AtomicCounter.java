package atomic;

import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Демонстрация работы атомарного счетчика в многопоточной среде.
 * Использует AtomicInteger для гарантированно корректной работы
 * при одновременном доступе из нескольких потоков.
 */
public class AtomicCounter {
    private static final Logger log = LoggerFactory.getLogger(AtomicCounter.class);

    /**
     * Атомарный счетчик. Обеспечивает:
     * - Потокобезопасность без использования synchronized
     * - Высокую производительность за счет неблокирующих алгоритмов
     * - Гарантированную согласованность данных между потоками
     */
    private final AtomicInteger count = new AtomicInteger(0);

    /**
     * Атомарно увеличивает значение счетчика на 1.
     * Реализация использует hardware-оптимизированные CAS-инструкции процессора.
     */
    public void increment() {
        int newValue = count.incrementAndGet();
        log.trace("Incremented counter to {}", newValue);
    }

    /**
     * Возвращает текущее значение счетчика.
     * @return текущее значение атомарного счетчика
     */
    public int getCount() {
        return count.get();
    }

    /**
     * Сбрасывает счетчик в 0 атомарным способом.
     */
    public void reset() {
        count.set(0);
        log.debug("Counter reset to 0");
    }

    /**
     * Демонстрация работы счетчика в многопоточной среде.
     * @param args аргументы командной строки (не используются)
     * @throws InterruptedException если потоки были прерваны во время выполнения
     */
    public static void main(String[] args) throws InterruptedException {
        log.info("Starting AtomicCounter demonstration");

        final AtomicCounter counter = new AtomicCounter();
        final int incrementsPerThread = 1000;
        final int expectedTotal = 2 * incrementsPerThread;

        // Лямбда-выражение для задачи инкрементирования
        Runnable incrementTask = () -> {
            log.debug("Thread {} started increment task", Thread.currentThread().getName());

            for (int i = 0; i < incrementsPerThread; i++) {
                counter.increment();

                // Небольшая пауза для увеличения вероятности переключения потоков
                if (i % 100 == 0) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {

                        log.error("Thread {} was interrupted", Thread.currentThread().getName(), e);
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            log.debug("Thread {} completed increment task", Thread.currentThread().getName());
        };

        // Создаем и запускаем потоки с осмысленными именами
        Thread thread1 = new Thread(incrementTask, "IncrementThread-1");
        Thread thread2 = new Thread(incrementTask, "IncrementThread-2");

        log.info("Starting worker threads...");
        thread1.start();
        thread2.start();

        // Ожидаем завершения потоков
        log.debug("Main thread waiting for worker threads to complete...");
        thread1.join();
        thread2.join();

        // Проверяем результат
        int actualCount = counter.getCount();
        log.info("Expected: {}, Actual: {}", expectedTotal, actualCount);

        if (actualCount != expectedTotal) {
            log.warn("Counter value mismatch detected!");
        }

        // Дополнительные демонстрационные методы
        counter.reset();
        log.info("After reset: {}", counter.getCount());

        log.info("AtomicCounter demonstration completed");
    }
}