package sync;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * Простой пример использования Semaphore для ограничения доступа к ресурсу.
 * Моделирует парковку с ограниченным количеством мест.
 */
@Slf4j
public class SimpleParkingLot {

    // Количество парковочных мест
    private static final int PARKING_CAPACITY = 3;

    // Семафор для управления доступом к местам // FAIR
    private static final Semaphore parkingSpots = new Semaphore(PARKING_CAPACITY, true);

    public static void main(String[] args) {
        log.info("Парковка открыта! Свободных мест: {}", parkingSpots.availablePermits());

        // Запускаем 5 машин
        for (int i = 1; i <= 5; i++) {
            new Thread(new Car("Машина-" + i)).start();
        }
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
