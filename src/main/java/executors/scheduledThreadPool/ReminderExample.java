package executors.scheduledThreadPool;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ReminderExample {
    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        System.out.println("Товар добавлен в корзину. Напоминание через 30 минут...");

        // Задача отправки уведомления
        Runnable sendReminder = () -> {
            System.out.println("Напоминание: Завершите оформление заказа!");
        };

        // Запускаем через 10 секунд
        scheduler.schedule(sendReminder, 10, TimeUnit.SECONDS);

        // Можно добавить отмену, если пользователь передумал
        ScheduledFuture<?> reminderTask = scheduler.schedule(sendReminder, 10, TimeUnit.SECONDS);

        // Эмуляция отмены (например, если пользователь оформил заказ)
        boolean userCancelled = true; // Допустим, пользователь передумал
        if (userCancelled) {
            reminderTask.cancel(false); // Отменяем задачу
            System.out.println("Напоминание отменено!");
            scheduler.shutdown();
        }
    }
}