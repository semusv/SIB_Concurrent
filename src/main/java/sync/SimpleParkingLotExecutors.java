package sync;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * Простой пример использования Semaphore для ограничения доступа к ресурсу. Работает через Executors
 * Моделирует парковку с ограниченным количеством мест.
 */
@Slf4j
public class SimpleParkingLotExecutors {

    // Количество парковочных мест
    private static final int PARKING_CAPACITY = 3;

    // Семафор для управления доступом к местам // FAIR
    private static final Semaphore parkingSpots = new Semaphore(PARKING_CAPACITY, true);
    public static final int CARS = 5;

    public static void main(String[] args) throws InterruptedException {
        log.info("Парковка открыта! Свободных мест: {}", parkingSpots.availablePermits());

        ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(CARS);
        // Запускаем 5 машин
        for (int i = 1; i <= CARS; i++) {
            threadPoolExecutor.execute( new Car("Машина-" + i));
        }

        // Завершаем ExecutorService после выполнения всех задач
        shutdownService(threadPoolExecutor);

        log.info("Парковка закрыта. Все машины уехали.");

    }

    private static void shutdownService(ExecutorService threadPoolExecutor) throws InterruptedException {
        // Завершаем ExecutorService после выполнения всех задач
        threadPoolExecutor.shutdown();
        // Ожидаем завершения всех задач с таймаутом
        if (!threadPoolExecutor.awaitTermination(10, TimeUnit.SECONDS))
            threadPoolExecutor.shutdownNow();
    }

    /**
     * Класс, представляющий машину на парковке.
     */
    static class Car implements Runnable {
        private final String name;

        Car(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            try {
                log.info("{} подъехала к парковке.", name);

                // Пытаемся занять место с таймаутом 2 секунды
                if (parkingSpots.tryAcquire(2, TimeUnit.SECONDS)) {
                    try {
                        log.info("✅ {} припарковалась! Свободных мест: {}", name, parkingSpots.availablePermits());

                        // Имитация стоянки
                        Thread.sleep((long) (Math.random() * 3000 + 1000));
                    } finally {
                        parkingSpots.release(); // Освобождаем место
                    }
                    log.info("🚗 {} уехала. Свободных мест: {}", name, parkingSpots.availablePermits());
                } else {
                    log.info("❌ {} не дождалась места и уехала.", name);
                }
            } catch (InterruptedException e) {
                log.warn("{} была прервана во время парковки", name);
                Thread.currentThread().interrupt();
            }
        }
    }
}
