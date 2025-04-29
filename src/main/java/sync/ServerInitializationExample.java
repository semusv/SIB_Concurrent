package sync;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * Пример инициализации сервера с использованием CountDownLatch.
 * Ожидает загрузки всех модулей перед запуском сервера.
 */
@Slf4j
public class ServerInitializationExample {

    // Количество модулей, которые должны быть загружены
    private static final int MODULES_COUNT = 5;

    public static void main(String[] args) {
        // Создаем CountDownLatch для синхронизации
        CountDownLatch latch = new CountDownLatch(MODULES_COUNT);

        // Создаем пул потоков для загрузки модулей
        ExecutorService executor = Executors.newFixedThreadPool(MODULES_COUNT);

        try {
            // Запускаем задачи загрузки модулей
            for (int i = 1; i <= MODULES_COUNT; i++) {
                executor.execute(new ModuleLoader("Module-" + i, latch));
            }

            log.info("[Сервер] Ожидаю загрузку всех модулей...");

            // Ожидаем завершения загрузки всех модулей с таймаутом 5 секунд
            if (latch.await(5, TimeUnit.SECONDS)) {
                log.info("[Сервер] Все модули загружены! Сервер запускается.");
            } else {
                log.error("[Сервер] Ошибка! Не все модули загрузились вовремя.");
            }
        } catch (InterruptedException e) {
            log.error("[Сервер] Прервано ожидание загрузки модулей");
            Thread.currentThread().interrupt();
        } finally {
            // Завершаем ExecutorService
            shutdownExecutor(executor);
        }
    }

    /**
     * Метод для безопасного завершения ExecutorService.
     */
    private static void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Класс, представляющий задачу загрузки одного модуля.
     */
    static class ModuleLoader implements Runnable {
        private final String moduleName;  // Имя модуля
        private final CountDownLatch latch;  // Счётчик для синхронизации

        ModuleLoader(String moduleName, CountDownLatch latch) {
            this.moduleName = moduleName;
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                // Имитация загрузки модуля (случайная задержка от 1 до 5.5 секунд)
                int loadTime = (int) (Math.random() * 5000) + 500;
                Thread.sleep(loadTime);

                log.info("[{}] Загружен за {} мс.", moduleName, loadTime);
            } catch (InterruptedException e) {
                log.warn("[{}] Загрузка прервана!", moduleName);
                Thread.currentThread().interrupt();
                return; // Не уменьшаем счётчик при прерывании
            } finally {
                latch.countDown(); // Уменьшаем счётчик
            }
        }
    }
}
