package sync;
import java.util.concurrent.*;

import java.util.concurrent.*;

public class DataProcessor {
    // Количество потоков-обработчиков
    private static final int THREAD_COUNT = 3;
    // Количество обязательных циклов обработки
    private static final int REQUIRED_CYCLES = 3;

    /**
     * CyclicBarrier для синхронизации потоков в каждом цикле.
     * Особенности:
     * - Автоматически сбрасывается после каждого цикла
     * - Выполняет barrier action при каждом достижении барьера
     */
    private static final CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT,
            () -> System.out.println("--- Все потоки завершили цикл. Барьер сброшен ---"));

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
     * Класс Worker реализует обработку данных в несколько циклов
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

                System.out.println("Поток " + id + " завершил все " + REQUIRED_CYCLES + " цикла");
            } catch (InterruptedException e) {
                System.out.println("Поток " + id + " был прерван");
                Thread.currentThread().interrupt();
            } catch (BrokenBarrierException e) {
                System.out.println("Поток " + id + ": барьер сломан");
            }
        }

        /**
         * Один цикл обработки данных с синхронизацией через барьер
         */
        private void processCycle() throws InterruptedException, BrokenBarrierException {
            // Этап 1: Обработка данных
            System.out.println("Поток " + id + " начал цикл " + (cycleCount + 1));

            // Имитация работы (случайная задержка)
            Thread.sleep(500 + (int)(Math.random() * 1000));

            System.out.println("Поток " + id + " завершил обработку в цикле " + (cycleCount + 1));

            // Синхронизация в конце цикла
            try {
                int arrivalIndex = barrier.await(2, TimeUnit.SECONDS); // Таймаут ожидания

                // Этап 2: Продолжение после синхронизации
                System.out.println("Поток " + id + " прошел барьер в цикле " +
                        (cycleCount + 1) + " (позиция: " + arrivalIndex + ")");
            } catch (TimeoutException e) {
                System.out.println("Поток " + id + " превысил время ожидания в цикле " +
                        (cycleCount + 1));
                // При таймауте барьер "ломается", другие потоки получат BrokenBarrierException
                throw new BrokenBarrierException();
            }
        }
    }
}