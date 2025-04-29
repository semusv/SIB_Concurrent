package executors.scheduledThreadPool;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * Пример использования ScheduledExecutorService для отправки напоминаний.
 * Демонстрирует:
 * - Планирование задачи с задержкой.
 * - Возможность отмены запланированной задачи.
 * - Управление напоминаниями с помощью ScheduledExecutorService.
 */
@Slf4j
public class ReminderExample {

    public static void main(String[] args) {
        // Создаем планировщик с одним потоком
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        log.info("Товар добавлен в корзину. Напоминание через 30 минут...");

        // Задача для отправки уведомления
        Runnable sendReminder = () -> {
            log.info("Напоминание: Завершите оформление заказа!");
        };


        // Сохраняем ссылку на задачу для возможной отмены
        ScheduledFuture<?> reminderTask = scheduler.schedule(sendReminder, 10, TimeUnit.SECONDS);

        // Эмуляция отмены (например, если пользователь оформил заказ)
        boolean userCancelled = true; // Допустим, пользователь передумал
        if (userCancelled) {
            reminderTask.cancel(false); // Отменяем задачу
            log.info("Напоминание отменено!");
            // Немедленно завершаем планировщик (удаляет все задачи)
            scheduler.shutdownNow();
        }
    }
}
