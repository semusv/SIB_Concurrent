package locks;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Пример использования ReentrantReadWriteLock для thread-safe кэша.
 * Демонстрирует:
 * - Разделение блокировок на чтение и запись
 * - Правильное управление блокировками
 * - Логирование операций
 */
@Slf4j
public class ReadWriteLockExample {

    // Наш thread-safe кэш
    private static class Cache {
        private final ReadWriteLock lock = new ReentrantReadWriteLock(true); // Честный режим (FIFO)
        private String data = "Initial Data";
        private int version = 0;

        /**
         * Безопасное чтение данных (множественный доступ)
         */
        public String readData() {

            lock.readLock().lock(); // Захватываем блокировку чтения
            try {
                log.debug("Read lock acquired by {}", Thread.currentThread().getName());
                // Имитация чтения
                Thread.sleep((long) (Math.random()*2000));
                log.info("Read data: {} (v{})", data, version);
                return data;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Read operation interrupted");
                return null;
            } finally {
                lock.readLock().unlock(); // Всегда отпускаем в finally!
                log.debug("Read lock released by {}", Thread.currentThread().getName());
            }
        }

        /**
         * Безопасное обновление данных (эксклюзивный доступ)
         */
        public void updateData(String newData) {
            lock.writeLock().lock(); // Захватываем блокировку записи
            try {
                log.debug("Write lock acquired by {}", Thread.currentThread().getName());
                // Имитация долгой записи
                Thread.sleep(500);
                this.data = newData;
                version++;
                log.info("Data updated to: {} (v{})", newData, version);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Write operation interrupted");
            } finally {
                lock.writeLock().unlock();
                log.debug("Write lock released by {}", Thread.currentThread().getName());
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Cache cache = new Cache();

        // Создаем несколько потоков для чтения
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    cache.readData();
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "Reader-" + i).start();
        }

        // Создаем 2 потока для записи
        for (int i = 0; i < 2; i++) {
            final int writerId = i;
            new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    String newData = "Writer-" + writerId + " data";
                    cache.updateData(newData);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "Writer-" + i).start();
        }

        // Даем поработать 5 секунд
        Thread.sleep(5000);

        // Прерываем все потоки
        Thread.getAllStackTraces().keySet().forEach(t -> {
            if (!t.getName().equals("main")) {
                t.interrupt();
            }
        });

        log.info("Application finished");
    }
}