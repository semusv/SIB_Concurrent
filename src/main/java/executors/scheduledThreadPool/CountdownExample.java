package executors.scheduledThreadPool;


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CountdownExample {
    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AtomicInteger count = new AtomicInteger(10); // Начинаем с 10

        // Задача обратного отсчёта
        Runnable countdown = () -> {
            int current = count.getAndDecrement();
            System.out.println(current);

            if (current == 0) {
                System.out.println("Пуск!");
                scheduler.shutdown(); // Останавливаем после завершения
            }
        };

        // Запускаем каждую секунду
        scheduler.scheduleAtFixedRate(countdown, 0, 1, TimeUnit.SECONDS);
    }
}