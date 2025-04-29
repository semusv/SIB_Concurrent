package sync;

import java.util.concurrent.*;

import lombok.extern.slf4j.Slf4j;

/**
 * Пример использования CyclicBarrier для синхронизации потоков.
 * Потоки выполняют несколько циклов обработки, синхронизируясь после каждого цикла.
 */
@Slf4j
public class DataProcessor {

    // Количество потоков-обработчиков
    private static final int THREAD_COUNT = 3;

    // Количество обязательных циклов обработки
    private static final int REQUIRED_CYCLES = 3;

    /**
     * CyclicBarrier для синхронизации потоков в каждом цикле.
     * После достижения барьера всеми потоками выполняется действие (логирование).
     */
    private static final CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT,
            () -> log.info("--- Все потоки завершили цикл. Барьер сброшен ---"));

    public static void main(String[] args) {
        // Создаем пул потоков
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        try {
            // Запускаем потоки-обработчики
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.execute(new Worker(i));
            }
        } finally {
            // Плавное завершение работы ExecutorService
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Класс Worker реализует обработку данных в несколько циклов.
     */
    static class Worker implements Runnable {
        private final int id;  // Идентификатор потока
        private int cycleCount = 0;  // Счетчик выполненных циклов

        Worker(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                // Выполняем ровно REQUIRED_CYCLES циклов обработки
                while (cycleCount < REQUIRED_CYCLES) {
                    processCycle();
                    cycleCount++;
                }

                log.info("Поток {} завершил все {} циклов", id, REQUIRED_CYCLES);
            } catch (InterruptedException e) {
                log.warn("Поток {} был прерван", id);
                Thread.currentThread().interrupt();
            } catch (BrokenBarrierException e) {
                log.warn("Поток {}: барьер сломан", id);
            }
        }

        /**
         * Один цикл обработки данных с синхронизацией через барьер.
         */
        private void processCycle() throws InterruptedException, BrokenBarrierException {
            // Этап 1: Обработка данных
            log.info("Поток {} начал цикл {}", id, cycleCount + 1);

            // Имитация работы (случайная задержка)
            Thread.sleep(500 + (int) (Math.random() * 1000));

            log.info("Поток {} завершил обработку в цикле {}", id, cycleCount + 1);

            // Синхронизация в конце цикла
            try {
                int arrivalIndex = barrier.await(2, TimeUnit.SECONDS); // Таймаут ожидания

                // Этап 2: Продолжение после синхронизации
                log.info("Поток {} прошел барьер в цикле {} (позиция: {})", id, cycleCount + 1, arrivalIndex);
                Thread.sleep(10); //ДЛя красоты логов
            } catch (TimeoutException e) {
                log.warn("Поток {} превысил время ожидания в цикле {}", id, cycleCount + 1);
                // При таймауте барьер "ломается", другие потоки получат BrokenBarrierException
                throw new BrokenBarrierException();
            }
        }
    }
}
