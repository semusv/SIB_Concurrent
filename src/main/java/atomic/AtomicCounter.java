package atomic;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Класс для демонстрации работы с атомарным счетчиком
 * в многопоточной среде
 *
 */
public class AtomicCounter {
    // Атомарный счетчик (потокобезопасный)
    private AtomicInteger count = new AtomicInteger(0);

    /**
     * Метод для инкрементации счетчика
     * Гарантирует атомарность операции (потокобезопасность)
     */
    public void increment() {
        count.incrementAndGet();  // Атомарно увеличивает значение на 1
    }

    /**
     * Метод для получения текущего значения счетчика
     *
     * @return текущее значение счетчика
     */
    public int getCount() {
        return count.get();  // Возвращает текущее значение
    }

    /**
     * Основной метод для демонстрации работы счетчика
     *
     * @param args аргументы командной строки (не используются)
     * @throws InterruptedException если поток был прерван во время ожидания
     */
    public static void main(String[] args) throws InterruptedException {
        // Создаем экземпляр счетчика
        AtomicCounter counter = new AtomicCounter();

        // Задача, которую будут выполнять потоки
        // Каждый поток увеличит счетчик 1000 раз
        Runnable task = () -> {
            for (int i = 0; i < 1000; i++) {
                counter.increment();  // Вызываем атомарный инкремент
            }
        };

        // Создаем два потока с одинаковой задачей
        Thread thread1 = new Thread(task);
        Thread thread2 = new Thread(task);

        // Запускаем потоки
        thread1.start();
        thread2.start();

        // Ожидаем завершения работы потоков
        thread1.join();
        thread2.join();

        // Выводим итоговое значение счетчика
        // Благодаря AtomicInteger всегда будет 2000
        System.out.println("Final count: " + counter.getCount());
    }
}

