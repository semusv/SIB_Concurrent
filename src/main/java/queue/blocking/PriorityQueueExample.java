package queue.blocking;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Пример использования PriorityBlockingQueue для управления задачами.
 * <br>
 * Класс Task представляет задачу с приоритетом.
 * <br>
 * Использует `@Slf4j` для автоматического предоставления метода логгирования.
 */
@Slf4j
public class PriorityQueueExample {

    public static void main(String[] args) {
        // Создаем PriorityBlockingQueue для хранения задач.
        PriorityBlockingQueue<Task> taskQueue = new PriorityBlockingQueue<>();

        // Пример добавления задач в очередь.
        taskQueue.add(new Task(3, "Низкий приоритет"));
        taskQueue.add(new Task(1, "Высокий приоритет"));

        // Выполняем обработку задач.
        while (!taskQueue.isEmpty()) {
            try {
                Task task = taskQueue.take();
                log.info("Обрабатываем задачу: {}", task);
                // Имитация обработки.
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                log.error("Поток был прерван: {}", e.getMessage(), e);
                Thread.currentThread().interrupt(); // Восстанавливаем статус прерывания
            }
        }

        log.info("Очередь задач пуста.");
    }

    /**
     * Класс Task представляет задачу с приоритетом.
     */
    static class Task implements Comparable<Task> {
        private final int priority;
        private final String description;

        /**
         * Конструктор задачи.
         *
         * @param priority    Приоритет (чем меньше число, тем выше приоритет).
         * @param description Описание задачи.
         */
        Task(int priority, String description) {
            this.priority = priority;
            this.description = description;
        }

        /**
         * Определение порядка задач по приоритету.
         * Задачи сортируются по возрастанию численного приоритета.
         *
         * @param other Другая задача для сравнения.
         * @return Результат сравнения.
         */
        @Override
        public int compareTo(Task other) {
            return Integer.compare(this.priority, other.priority);
        }

        /**
         * Строковое представление задачи для логов.
         *
         * @return Строка с описанием задачи и её приоритетом.
         */
        @Override
        public String toString() {
            return description + " (приоритет: " + priority + ")";
        }
    }
}
