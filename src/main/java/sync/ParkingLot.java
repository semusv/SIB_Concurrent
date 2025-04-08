package sync;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Класс, моделирующий работу парковки с ограниченным количеством мест
 * с использованием Semaphore для контроля доступа
 */
public class ParkingLot {

    // Общее количество парковочных мест
    private static final int PARKING_CAPACITY = 5;
    private static final int WAIT_GENERIC_MS = 1000;

    /**
     * Семафор для контроля доступа к парковочным местам.
     * Количество разрешений равно количеству парковочных мест.
     * Параметр fair = true обеспечивает справедливый режим (FIFO)
     */
    private static final Semaphore parkingSpots = new Semaphore(PARKING_CAPACITY, true);

    /**
     * Флаг для контроля работы потоков.
     * volatile гарантирует видимость изменений между потоками
     */
    private static volatile boolean isRunning = true;

    /**
     * Список для хранения всех созданных потоков-машин
     */
    private static final List<Thread> carThreads = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Парковка открыта! Свободных мест: " + parkingSpots.availablePermits());

        /**
         * Добавляем shutdown hook для корректного завершения работы.
         * Этот код выполнится при получении сигнала на завершение работы JVM
         * (Ctrl+C в консоли или System.exit())
         */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isRunning = false; // Устанавливаем флаг завершения
            System.out.println("\nПарковка закрывается... Ждем пока все машины уедут");

            // Ожидаем завершения всех потоков
            waitForAllCarsToLeave();
        }));

        // Запускаем 10 машин (каждая в отдельном потоке)
        for (int i = 1; i <= 10; i++) {
            // Проверяем флаг перед созданием нового потока
            if (!isRunning) {
                System.out.println("Новые машины не принимаются - парковка закрывается");
                break;
            }

            // Создаем и запускаем поток, представляющий машину
            Thread car = new Thread(new Car("Машина-" + i));
            carThreads.add(car); // Добавляем поток в список для отслеживания
            car.start();

            try {
                // Небольшая задержка между запуском машин для наглядности
                Thread.sleep(new Random().nextInt(WAIT_GENERIC_MS));
            } catch (InterruptedException e) {
                System.out.println("Главный поток был прерван");
                Thread.currentThread().interrupt(); // Восстанавливаем статус прерывания
                break;
            }
        }

        // Ожидаем завершения всех потоков перед выходом из main
        waitForAllCarsToLeave();
        System.out.println("Все машины уехали. Парковка закрыта.");
    }

    /**
     * Метод для ожидания завершения всех потоков-машин
     */
    private static void waitForAllCarsToLeave() {
        // Ожидаем завершения всех потоков
        for (Thread carThread : carThreads) {
            try {
                // Ожидаем завершения потока с таймаутом 3 секунды
                carThread.join(3000);
                if (carThread.isAlive()) {
                    System.out.println("Предупреждение: поток " + carThread.getName() + " не завершился вовремя");
                }
            } catch (InterruptedException e) {
                System.out.println("Прервано ожидание завершения потока " + carThread.getName());
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Класс, представляющий машину на парковке
     */
    static class Car implements Runnable {
        private final String name; // Идентификатор машины

        public Car(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            try {
                // Проверяем флаг работы в цикле (хотя в данном случае достаточно однократной проверки)
                while (isRunning) {
                    System.out.println(name + " подъехала к парковке.");

                    /**
                     * Пытаемся занять парковочное место с таймаутом 2 секунды.
                     * tryAcquire возвращает true, если удалось получить разрешение
                     */
                    if (parkingSpots.tryAcquire(2, TimeUnit.SECONDS)) {
                        try {
                            // Сообщаем об успешной парковке
                            System.out.println("✅ " + name + " припарковалась! Свободных мест: " +
                                    (parkingSpots.availablePermits()));

                            /**
                             * Имитируем время стоянки (случайное от 1 до 10 секунд)
                             * В реальном приложении здесь была бы работа с ресурсом
                             */
                            Thread.sleep((long) (Math.random() * 10000 + 1000));

                            // Сообщаем об отъезде
                            System.out.println("🚗 " + name + " уехала. Свободных мест: " +
                                    (PARKING_CAPACITY - parkingSpots.availablePermits()));

                            return; // Завершаем поток после успешной парковки
                        } finally {
                            /**
                             * Гарантированно освобождаем место в блоке finally.
                             * Это важно для предотвращения утечки ресурсов,
                             * даже если в try блоке произошло исключение
                             */
                            parkingSpots.release();
                        }
                    } else {
                        // Если не удалось получить место за 2 секунды
                        System.out.println("❌ " + name + " не дождалась места и уехала.");
                        return; // Завершаем поток
                    }
                }
            } catch (InterruptedException e) {
                /**
                 * Обрабатываем прерывание потока.
                 * Важно восстановить статус прерывания после обработки
                 */
                System.out.println(name + " была прервана во время парковки");
                Thread.currentThread().interrupt();
            }
        }
    }
}