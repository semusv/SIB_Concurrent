package atomic;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicCasCounter {
    public static void main(String[] args) {
        AtomicInteger counter = new AtomicInteger(0);

        Runnable incrementTask = () -> {
            int oldValue;
            int newValue;
            do {
                oldValue = counter.get(); // Читаем текущее значение
                newValue = oldValue + 1;  // Вычисляем новое
            } while (!counter.compareAndSet(oldValue, newValue)); // Повторяем, пока не получится
        };

// Запускаем 2 потока
        new Thread(incrementTask).start();
        new Thread(incrementTask).start();
        try {
            Thread.sleep(100); // Даем потокам время завершиться
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(counter.get()); // Всегда 2
    }
}
