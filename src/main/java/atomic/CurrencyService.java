package atomic;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.HashMap;
import java.util.Map;

/**
 * Сервис для работы с курсами валют, использующий потокобезопасный кэш
 */
public class CurrencyService {
    public static void main(String[] args) {
        // Создаем клиент для работы с API курсов валют
        CurrencyApiClient apiClient = new CurrencyApiClient();
        // Инициализируем кэш с клиентом API
        CurrencyCache cache = new CurrencyCache(apiClient);

        // Задача для конвертации валют, которая будет выполняться в разных потоках
        Runnable convertTask = () -> {
            String threadName = Thread.currentThread().getName();

            // Получаем курс EUR и конвертируем 100 USD в EUR
            double rate = cache.getRate("EUR");
            System.out.printf("%s: 100 USD = %.2f EUR%n", threadName, 100 * rate);

            // Получаем курс JPY и конвертируем 200 USD в JPY
            rate = cache.getRate("JPY");
            System.out.printf("%s: 200 USD = %.2f JPY%n", threadName, 200 * rate);
        };

        // Запускаем 3 потока для демонстрации работы кэша
        for (int i = 0; i < 3; i++) {
            new Thread(convertTask, "Клиент-" + (i + 1)).start();
        }
    }

    /**
     * Класс-клиент для работы с API курсов валют
     */
    private static class CurrencyApiClient {
        /**
         * Метод для получения текущих курсов валют
         * @return Map с курсами валют, где ключ - код валюты, значение - курс к USD
         */
        public Map<String, Double> fetchExchangeRates() {
            // Имитация запроса к реальному API
            System.out.println("---> Делаем реальный HTTP-запрос к API...");

            // Создаем и заполняем Map с курсами валют
            Map<String, Double> rates = new HashMap<>();
            rates.put("USD", 1.0);       // Базовый курс USD
            rates.put("EUR", 0.93);      // 1 USD = 0.93 EUR
            rates.put("JPY", 151.50);    // 1 USD = 151.50 JPY

            // Имитация задержки сети
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // В реальном приложении здесь должна быть обработка прерывания
            }

            return rates;
        }
    }

    /**
     * Класс для кэширования курсов валют с поддержкой TTL (Time-To-Live)
     */
    private static class CurrencyCache {
        private AtomicCacheCur<String, Double> cache;  // Потокобезопасный кэш
        private long lastUpdated;                     // Время последнего обновления
        private static final long TTL_MS = 5 * 60 * 1000; // Время жизни кэша - 5 минут

        /**
         * Конструктор кэша
         * @param apiClient клиент API для загрузки курсов валют
         */
        public CurrencyCache(CurrencyApiClient apiClient) {
            // Инициализируем кэш с функцией загрузки данных
            this.cache = new AtomicCacheCur<>(key -> {
                // При запросе отсутствующего ключа сначала обновляем кэш (если нужно)
                refreshIfNeeded(apiClient);
                // Затем возвращаем значение из обновленного кэша
                return cache.getCacheSnapshot().get(key);
            });
        }

        /**
         * Метод для обновления кэша, если истекло время его жизни
         * @param apiClient клиент API для загрузки данных
         */
        private synchronized void refreshIfNeeded(CurrencyApiClient apiClient) {
            // Проверяем, истекло ли время жизни кэша
            if (System.currentTimeMillis() - lastUpdated > TTL_MS) {
                System.out.println("Обновляем кэш курсов валют...");
                // Загружаем новые курсы валют
                Map<String, Double> newRates = apiClient.fetchExchangeRates();
                // Создаем новую карту для кэша
                Map<String, Double> newCache = new HashMap<>(newRates);
                // Очищаем кэш перед обновлением
                cache.clear();
                // Добавляем все новые значения
                cache.getCacheSnapshot().putAll(newCache);
                // Обновляем время последнего обновления
                lastUpdated = System.currentTimeMillis();
            }
        }

        /**
         * Метод для получения курса валюты
         * @param currency код валюты (например, "EUR")
         * @return курс валюты к USD
         * @throws IllegalArgumentException если валюта не найдена
         */
        public double getRate(String currency) {
            // Получаем курс из кэша
            Double rate = cache.get(currency);
            // Если курс не найден, выбрасываем исключение
            if (rate == null) throw new IllegalArgumentException("Неизвестная валюта: " + currency);
            return rate;
        }
    }

    /**
     * Потокобезопасная реализация кэша на основе AtomicReference
     * @param <K> тип ключа
     * @param <V> тип значения
     */
    static class AtomicCacheCur<K, V> {
        private final AtomicReference<Map<K, V>> cache = new AtomicReference<>(new HashMap<>());
        private final Function<K, V> loader;

        /**
         * Конструктор кэша
         * @param loader функция для загрузки значений по ключу
         */
        public AtomicCacheCur(Function<K, V> loader) {
            this.loader = loader;
        }

        /**
         * Метод для получения значения по ключу
         * @param key ключ
         * @return значение, соответствующее ключу
         */
        public V get(K key) {
            // Получаем текущее состояние кэша
            Map<K, V> currentCache = cache.get();
            // Если ключ есть в кэше, возвращаем соответствующее значение
            if (currentCache.containsKey(key)) {
                return currentCache.get(key);
            }
            // Если ключа нет, загружаем значение через функцию loader
            V value = loader.apply(key);
            // Пытаемся атомарно обновить кэш
            while (true) {
                currentCache = cache.get();
                // Создаем копию текущего кэша
                Map<K, V> newCache = new HashMap<>(currentCache);
                // Добавляем новое значение
                newCache.put(key, value);
                // Пытаемся атомарно обновить кэш
                if (cache.compareAndSet(currentCache, newCache)) {
                    return value;
                }
                // Если другой поток изменил кэш, повторяем операцию
            }
        }

        /**
         * Метод для очистки кэша
         */
        public void clear() {
            cache.set(new HashMap<>());
        }

        /**
         * Метод для получения текущего состояния кэша
         * @return неизменяемое представление кэша
         */
        public Map<K, V> getCacheSnapshot() {
            return cache.get();
        }
    }
}