package queue.notBlocking;

import lombok.Data;

import java.util.concurrent.*;

/**
 * Система обработки задач с использованием потокобезопасной очереди.
 *
 * <p>Основные компоненты:
 * <ul>
 *   <li>ConcurrentLinkedQueue - для безопасного хранения задач в многопоточной среде</li>
 *   <li>FixedThreadPool - для параллельного выполнения задач</li>
 *   <li>Механизм graceful shutdown - для корректного завершения работы</li>
 * </ul>
 *
 * <p>Пример использования приведен в методе main().
 */
public class TaskProcessingSystem {
    // Потокобезопасная очередь для хранения задач.
    // Используется ConcurrentLinkedQueue как наиболее эффективная для producer-consumer сценариев
    private final ConcurrentLinkedQueue<Task> taskQueue = new ConcurrentLinkedQueue<>();

    // Пул потоков фиксированного размера для выполнения задач.
    // Выбран FixedThreadPool, так как мы знаем оптимальное количество потоков заранее
    private final ExecutorService executor;

    /**
     * Класс задачи, реализующий Runnable для выполнения в потоках.
     * Использует Lombok @Data для автоматической генерации геттеров/сеттеров
     */
    @Data
    public static class Task implements Runnable {
        private final int id;  // Уникальный идентификатор задачи

        /**
         * Логика выполнения задачи.
         * В реальном приложении здесь должна быть бизнес-логика.
         */
        @Override
        public void run() {
            System.out.println("Processing task " + id + " by " +
                    Thread.currentThread().getName());
            try {
                // Имитация обработки задачи (случайная задержка)
                Thread.sleep((long) (Math.random() * 500));
            } catch (InterruptedException e) {
                // Правильная обработка прерывания потока
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Конструктор системы обработки задач.
     *
     * @param workerCount количество рабочих потоков (рекомендуется Runtime.getRuntime().availableProcessors())
     */
    public TaskProcessingSystem(int workerCount) {
        // Создаем пул потоков с фиксированным количеством рабочих
        this.executor = Executors.newFixedThreadPool(workerCount);
    }

    /**
     * Запускает рабочие потоки для обработки задач из очереди.
     * Создает N потоков (где N = workerCount), каждый из которых выполняет processTasks()
     */
    public void start() {
        int workerCount = ((ThreadPoolExecutor) executor).getMaximumPoolSize();

        for (int i = 0; i < workerCount; i++) {
            // Каждый поток выполняет метод processTasks()
            executor.execute(this::processTasks);
        }
    }

    /**
     * Добавляет новую задачу в очередь на выполнение.
     *
     * @param task задача для добавления (не null)
     */
    public void addTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }

        // offer() - потокобезопасный метод добавления в ConcurrentLinkedQueue
        taskQueue.offer(task);
        System.out.println("Task " + task.getId() + " added. Queue size: " + taskQueue.size());
    }

    /**
     * Основной цикл обработки задач в рабочем потоке.
     * Поток постоянно проверяет наличие задач в очереди и выполняет их.
     */
    private void processTasks() {
        // Работаем пока поток не будет прерван
        while (!Thread.currentThread().isInterrupted()) {
            Task task = taskQueue.poll();  // Безопасное извлечение задачи

            if (task != null) {
                try {
                    System.out.println("[" + Thread.currentThread().getName() +
                            "] Starting task " + task.getId());

                    // Выполняем задачу
                    task.run();

                    System.out.println("[" + Thread.currentThread().getName() +
                            "] Task " + task.getId() + " completed");
                } catch (Exception e) {
                    // Логируем ошибку, но не прерываем поток
                    System.err.println("Task " + task.getId() + " failed: " + e.getMessage());
                }
            } else {
                // Если задач нет - небольшая пауза, чтобы не нагружать CPU
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // Восстанавливаем флаг прерывания
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Корректно завершает работу системы.
     *
     * @throws InterruptedException если поток был прерван во время ожидания завершения
     */
    public void shutdown() throws InterruptedException {
        // Инициируем мягкое завершение
        executor.shutdown();

        // Даем время на завершение текущих задач (но не более 5 секунд)
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            // Принудительное завершение, если задачи не завершились за отведенное время
            executor.shutdownNow();
        }
    }

    /**
     * Демонстрационный метод работы системы.
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting task processing system with 4 workers");

        // Создаем систему с 4 рабочими потоками
        TaskProcessingSystem system = new TaskProcessingSystem(4);
        system.start();

        // Создаем пул производителей задач
        ExecutorService producers = Executors.newFixedThreadPool(3);

        // Добавляем 10 задач через производителей
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            Thread.sleep(100);  // Имитируем неравномерное поступление задач
            producers.execute(() -> {
                system.addTask(new Task(taskId));
            });
        }

        // Завершаем работу производителей
        producers.shutdown();
        producers.awaitTermination(2, TimeUnit.SECONDS);

        System.out.println("Waiting for tasks to complete...");
        Thread.sleep(10000);  // Даем время на обработку оставшихся задач

        System.out.println("Shutting down the system");
        system.shutdown();
        System.out.println("System shutdown complete");
    }
    void    wrq() throws InterruptedException {
    }

}

