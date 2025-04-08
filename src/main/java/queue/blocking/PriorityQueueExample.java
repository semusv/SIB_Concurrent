package queue.blocking;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * Класс Task представляет задачу с приоритетом.
 * Реализует Comparable для сравнения задач по приоритету.
 */
class Task implements Comparable<Task> {
    private final int priority;  // Приоритет задачи (чем выше число, тем выше приоритет)
    private final String name;   // Название задачи

    /**
     * Конструктор задачи
     * @param priority приоритет (целое число)
     * @param name название задачи
     */
    public Task(int priority, String name) {
        this.priority = priority;
        this.name = name;
    }

    /**
     * Сравнение задач по приоритету (для сортировки в очереди)
     * @param other другая задача для сравнения
     * @return результат сравнения (сортировка по убыванию приоритета)
     */
    @Override
    public int compareTo(Task other) {
        return Integer.compare(other.priority, this.priority); // Сортировка по убыванию
    }

    /**
     * Строковое представление задачи
     * @return строка в формате "Название (приоритет: X)"
     */
    @Override
    public String toString() {
        return name + " (приоритет: " + priority + ")";
    }
}

/**
 * Демонстрация работы PriorityBlockingQueue - потокобезопасной очереди с приоритетами.
 * Особенности:
 * - Автоматическая сортировка элементов по приоритету
 * - Блокирующие операции при пустой очереди
 * - Потокобезопасность
 */
public class PriorityQueueExample {
    private static final int PROCESSING_DELAY_MS = 2000;  // Задержка обработки задачи
    private static final int PROCESSING_DELAY_WAITING_MS = 3000;

    public static void main(String[] args) {
        // Создаем очередь с приоритетом (не требует начальной емкости)
        // Элементы автоматически сортируются согласно compareTo()
        PriorityBlockingQueue<Task> queue = new PriorityBlockingQueue<>();

        // Добавляем задачи с разными приоритетами
        // Они будут автоматически упорядочиваться в очереди
        // Чем выше число, тем выше приоритет
        queue.put(new Task(3, "Обычная задача"));
        queue.put(new Task(5, "Срочная задача"));      // Эта задача будет обработана первой
        queue.put(new Task(1, "Неважная задача"));     // Эта задача будет обработана последней

        // Создаем и запускаем поток-обработчик
        Thread processorThread = new Thread(() -> {
            System.out.println("Обработчик задач запущен");

            try {
                while (!Thread.currentThread().isInterrupted()) {
                    // take() блокирует поток, пока в очереди нет элементов
                    // Возвращает задачу с наивысшим приоритетом
                    Task task = queue.take();

                    System.out.println("Начата обработка: " + task);

                    // Имитация обработки задачи
                    Thread.sleep(PROCESSING_DELAY_MS);

                    System.out.println("Завершена обработка: " + task);
                }
            } catch (InterruptedException e) {
                //Это историческое поведение Java: при выбрасывании InterruptedException JVM автоматически сбрасывает флаг прерывания.
                //Поэтому лучшая практика — всегда восстанавливать его вручную, если вы не собираетесь завершать поток сразу.
                System.out.println("Обработчик задач прерван");
                Thread.currentThread().interrupt();  // Восстанавливаем статус прерывания
            }

            System.out.println("Обработчик задач завершил работу");
        });

        processorThread.start();

        // Добавим обработчик завершения работы
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nПолучен сигнал завершения работы...");
            // Прерываем поток-обработчик
            processorThread.interrupt();
            try {
                // Даем время на корректное завершение
                processorThread.join(PROCESSING_DELAY_WAITING_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Выводим оставшиеся задачи
            System.out.println("Необработанные задачи в очереди: " + queue.size());
        }));
    }
}