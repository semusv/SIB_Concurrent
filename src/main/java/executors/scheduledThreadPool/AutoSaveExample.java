package executors.scheduledThreadPool;

import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoSaveExample {
    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Задача автосохранения
        Runnable autoSaveTask = () -> {
            System.out.println("Автосохранение данных..." + Instant.now().atZone(ZoneId.systemDefault()).toLocalTime() );
            // Здесь мог бы быть код сохранения в БД или файл
        };

        // Запускаем автосохранение каждые 10 секунд, первый раз — сразу
        scheduler.scheduleAtFixedRate(autoSaveTask, 0, 10, TimeUnit.SECONDS);

        // Эмуляция работы приложения (через 1 минуту завершаем)
        try {
            Thread.sleep(10 * 6 * 1000); // 1 минуту
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            scheduler.shutdown(); // Обязательно закрываем пул!
        }
    }
}