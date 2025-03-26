package atomic;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Потокобезопасный кэш вычислений с автоматическим обновлением значений по истечении TTL.
 * Демонстрирует эффективное использование ConcurrentHashMap для многопоточного доступа.
 */
public class ComputationCache {
    /**
     * Внутренний класс для хранения кэшированного значения и времени его создания.
     * @param <T> тип кэшированного значения
     */
    private static class CachedValue<T> {
        private final T value;          // Само кэшированное значение
        private final long timestamp;  // Время создания записи (в миллисекундах)

        public CachedValue(T value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }

        /**
         * Проверяет, истекло ли время жизни значения.
         * @param ttl время жизни в миллисекундах
         * @return true если значение устарело и требует обновления
         */
        public boolean isExpired(long ttl) {
            return System.currentTimeMillis() - timestamp > ttl;
        }
    }

    // Потокобезопасная хэш-таблица для хранения кэша
    private final ConcurrentHashMap<String, CachedValue<Double>> cache = new ConcurrentHashMap<>();

    // Время жизни кэшированных значений (5 секунд)
    private static final long TTL = TimeUnit.SECONDS.toMillis(5);


    public static void main(String[] args) throws InterruptedException {
        ComputationCache cache = new ComputationCache();

        // Создаем пул из 10 потоков
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicLong requestCounter = new AtomicLong();

        // Имитируем 20 запросов к кэшу
        System.out.println("=== Первая волна запросов (значения кэшируются) ===");
        for (int i = 0; i < 20; i++) {
            executor.submit(() -> {
                // Циклически используем 3 ключа: key-0, key-1, key-2
                String key = "key-" + requestCounter.incrementAndGet() % 3;
                double value = cache.get(key);
                System.out.printf("[%s] %s = %.2f%n",
                        Thread.currentThread().getName(), key, value);
            });
        }

        // Ждем, пока TTL истечет (6 секунд > 5 секунд TTL)
        System.out.println("\nОжидаем истечения TTL (6 секунд)...");
        Thread.sleep(6000);

        // Делаем еще запросы - значения должны пересчитаться
        System.out.println("\n=== Вторая волна запросов (значения устарели) ===");
        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                String key = "key-" + requestCounter.incrementAndGet() % 3;
                double value = cache.get(key);
                System.out.printf("[%s] %s = %.2f%n",
                        Thread.currentThread().getName(), key, value);
            });
        }

        // Завершаем работу пула потоков
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    /**
     * Имитация ресурсоемкого вычисления.
     * @param key ключ для вычисления (может использоваться в реальной логике)
     * @return результат вычисления
     */
    private double expensiveComputation(String key) {
        System.out.printf("[%s] Вычисление нового значения для ключа '%s'...%n",
                Thread.currentThread().getName(), key);

        try {
            Thread.sleep(1000); // Имитация долгой операции (1 секунда)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Вычисление прервано", e);
        }

        return Math.random() * 100; // Генерация случайного результата
    }

    /**
     * Получает значение из кэша. Если значение отсутствует или устарело,
     * выполняет вычисление и сохраняет результат.
     * @param key ключ для поиска значения
     * @return актуальное значение
     */
    public double get(String key) {
        return cache.compute(key, (k, cached) -> {
            // Если значение отсутствует или устарело - вычисляем заново
            if (cached == null || cached.isExpired(TTL)) {
                return new CachedValue<>(expensiveComputation(k));
            }
            // Возвращаем существующее значение
            return cached;
        }).value;
    }


}