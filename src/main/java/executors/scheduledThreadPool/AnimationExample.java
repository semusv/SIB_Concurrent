package executors.scheduledThreadPool;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AnimationExample {
    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AtomicInteger speed = new AtomicInteger(1); // Начальная скорость = 1

        // Задача увеличения скорости
        Runnable increaseSpeed = () -> {
            int currentSpeed = speed.incrementAndGet();
            System.out.println("Скорость увеличена: " + currentSpeed);

            if (currentSpeed >= 5) {
                System.out.println("Максимальная скорость достигнута!");
                scheduler.shutdown();
            }
        };

        // Увеличиваем скорость каждые 2 секунды
        scheduler.scheduleAtFixedRate(increaseSpeed, 0, 2, TimeUnit.SECONDS);
    }
}