package sync;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Пример инициализации сервера с использованием CountDownLatch.
 * Демонстрирует ожидание загрузки всех модулей перед запуском сервера.
 */
public class ServerInitializationExample {
    static final int MODULES_COUNT = 5;

    public static void main(String[] args) {
        // Количество модулей, которые должны быть загружены


        /**
         * CountDownLatch - синхронизатор, который позволяет потокам ждать завершения
         * набора операций в других потоках.
         * Инициализируется счётчиком, равным количеству модулей.
         */
        CountDownLatch latch = new CountDownLatch(MODULES_COUNT);

        /**
         * ExecutorService - пул потоков для управления потоками загрузки модулей.
         * Используем фиксированный пул с количеством потоков = количеству модулей.
         */
        ExecutorService executor = Executors.newFixedThreadPool(MODULES_COUNT);

        try {
            // Запускаем задачи загрузки модулей в пуле потоков
            for (int i = 1; i <= MODULES_COUNT; i++) {
                /**
                 * Каждая задача получает:
                 * - уникальное имя модуля
                 * - общий CountDownLatch для синхронизации
                 */
                executor.execute(new ModuleLoader("Module-" + i, latch));
            }

            System.out.println("[Сервер] Ожидаю загрузку всех модулей...");

            /**
             * Ожидаем завершения загрузки всех модулей с таймаутом 5 секунд.
             * await() возвращает:
             * - true, если все модули загрузились (счётчик достиг 0)
             * - false, если истёк таймаут
             */
            if (latch.await(5, TimeUnit.SECONDS)) {
                System.out.println("[Сервер] Все модули загружены! Сервер запускается.");
            } else {
                System.out.println("[Сервер] Ошибка! Не все модули загрузились вовремя.");
                // В реальном приложении здесь должна быть обработка частичной загрузки:
                // - отмена операций
                // - попытка восстановления
                // - уведомление администратора
            }
        } catch (InterruptedException e) {
            /**
             * Обработка прерывания главного потока.
             * Важно восстановить флаг прерывания после обработки исключения.
             */
            System.err.println("[Сервер] Прервано ожидание загрузки модулей");
            Thread.currentThread().interrupt();
        } finally {
            /**
             * Гарантированное завершение ExecutorService.
             * Важно для освобождения ресурсов, даже если произошла ошибка.
             */
            shutdownExecutor(executor);
        }
    }

    /**
     * Метод для безопасного завершения ExecutorService.
     * @param executor сервис исполнения, который нужно завершить
     */
    private static void shutdownExecutor(ExecutorService executor) {
        // Запрещаем добавление новых задач
        executor.shutdown();
        try {
            /**
             * Ожидаем завершения уже запущенных задач в течение 1 секунды.
             * Если задачи не завершились за это время - принудительно прерываем.
             */
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                // Отменяем все выполняющиеся задачи
                executor.shutdownNow();
                /**
                 * В реальном приложении здесь можно добавить:
                 * - логирование не завершившихся задач
                 * - дополнительные попытки корректного завершения
                 */
            }
        } catch (InterruptedException e) {
            /**
             * Если поток был прерван во время ожидания завершения,
             * немедленно прерываем все задачи.
             */
            executor.shutdownNow();
            // Восстанавливаем статус прерывания
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Класс, представляющий задачу загрузки одного модуля.
     */
    static class ModuleLoader implements Runnable {
        private final String moduleName;  // Имя модуля для идентификации
        private final CountDownLatch latch;  // Общий счётчик для синхронизации

        /**
         * Конструктор задачи загрузки модуля.
         * @param moduleName имя модуля
         * @param latch общий счётчик для синхронизации
         */
        public ModuleLoader(String moduleName, CountDownLatch latch) {
            this.moduleName = moduleName;
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                /**
                 * Имитация загрузки модуля:
                 * - случайная задержка от 1 до 3 секунд
                 * - в реальном приложении здесь может быть:
                 *   * загрузка конфигурации
                 *   * инициализация подключений
                 *   * проверка зависимостей
                 */
                int loadTime = (int) (Math.random() * 3000) + 1000;
                Thread.sleep(loadTime);

                // Сообщаем об успешной загрузке
                System.out.printf("[%s] Загружен за %d мс.%n", moduleName, loadTime);
            } catch (InterruptedException e) {
                /**
                 * Обработка прерывания потока модуля.
                 * Важно:
                 * 1. Зарегистрировать факт прерывания
                 * 2. Восстановить флаг прерывания
                 * 3. Прекратить выполнение
                 */
                System.err.printf("[%s] Загрузка прервана!%n", moduleName);
                Thread.currentThread().interrupt();
                return; // Не вызываем countDown() при прерывании
            } finally {
                /**
                 * Гарантированно уменьшаем счётчик, даже если:
                 * - произошло исключение
                 * - поток был прерван
                 *
                 * Важно: в данном примере countDown() вызывается всегда,
                 * кроме случая явного прерывания потока.
                 * В реальном приложении нужно определить политику:
                 * - всегда ли уменьшать счётчик
                 * - как обрабатывать частичную загрузку
                 */
                latch.countDown();
            }
        }
    }
}