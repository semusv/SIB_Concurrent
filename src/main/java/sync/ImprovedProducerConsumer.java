package sync;

import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Пример реализации шаблона Producer-Consumer с использованием Exchanger.
 * Демонстрирует, как два потока могут обмениваться данными в точке синхронизации.
 */
public class ImprovedProducerConsumer {
    // Максимальное количество попыток обмена при таймауте
    private static final int MAX_RETRIES = 3;

    // Таймаут для операции обмена (в секундах)
    private static final long EXCHANGE_TIMEOUT = 2;

    public static void main(String[] args) {
        /**
         * 1. Создаем обменник с контролем типов.
         *    Передаем объекты DataBuffer,
         *    которые содержат дополнительные метаданные.
         */
        Exchanger<DataBuffer> exchanger = new Exchanger<>();

        /**
         * 2. Используем ExecutorService для управления потоками.
         *    Преимущества:
         *    - Лучший контроль за жизненным циклом потоков
         *    - Возможность плавного завершения
         */
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);

        try {
            // Запускаем producer и consumer
            executor.execute(new Producer(exchanger, 3));
            executor.execute(new Consumer(exchanger, 3));

            /**
             * 3. Плавное завершение работы:
             *    - shutdown() запрещает новые задачи
             *    - awaitTermination() ждет завершения текущих задач
             *    - shutdownNow() принудительно прерывает при превышении таймаута
             */
            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            // Обработка прерывания основного потока
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Класс для передачи данных между потоками.
     * Содержит:
     * - Сами данные (String)
     * - Временную метку создания
     * - Метод для красивого вывода
     */
    static class DataBuffer {
        final String data;       // Полезная нагрузка
        final long timestamp;    // Когда был создан буфер

        DataBuffer(String data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return String.format("[%tT] %s", timestamp, data);
        }
    }

    /**
     * Поток-производитель данных.
     * Особенности:
     * - Создает заданное количество элементов
     * - Поддерживает повторные попытки при таймаутах
     * - Логирует все ключевые события
     */
    static class Producer implements Runnable {
        private final Exchanger<DataBuffer> exchanger;  // Общий обменник
        private final int maxItems;                     // Сколько элементов создать
        private int retryCount = 0;                     // Счетчик неудачных попыток

        Producer(Exchanger<DataBuffer> exchanger, int maxItems) {
            this.exchanger = exchanger;
            this.maxItems = maxItems;
        }

        @Override
        public void run() {
            try {
                for (int i = 1; i <= maxItems; i++) {
                    // 1. Создаем новый буфер с данными
                    DataBuffer outBuffer = new DataBuffer("Data-" + i);
                    System.out.printf("Producer: создал %s %n",
                            outBuffer);

                    boolean exchanged = false;
                    // 2. Пытаемся передать данные (с повторными попытками)
                    while (!exchanged && retryCount < MAX_RETRIES) {
                        try {
                            // 3. Попытка обмена с таймаутом
                            DataBuffer inBuffer = exchanger.exchange(
                                    outBuffer,
                                    EXCHANGE_TIMEOUT,
                                    TimeUnit.SECONDS
                            );

                            // 4. Успешный обмен
                            System.out.println("Producer: получил " + inBuffer);
                            exchanged = true;
                            retryCount = 0;  // Сбрасываем счетчик попыток

                        } catch (TimeoutException e) {
                            // 5. Обработка таймаута
                            retryCount++;
                            System.out.printf("Producer: таймаут (попытка %d)%n", retryCount);

                            if (retryCount >= MAX_RETRIES) {
                                // 6. Превышен лимит попыток
                                System.out.println("Producer: достигнут лимит попыток");
                                throw new IllegalStateException("Не удалось обменяться данными");
                            }
                        }
                    }

                    // 7. Имитация обработки между отправками
                    Thread.sleep(500 + (long)(Math.random() * 1000));
                }
            } catch (InterruptedException e) {
                // 8. Обработка прерывания потока
                System.out.println("Producer: прерван");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                // 9. Обработка других ошибок
                System.out.println("Producer: ошибка - " + e.getMessage());
            }
        }
    }

    /**
     * Поток-потребитель данных.
     * Особенности:
     * - Ожидает заданное количество элементов
     * - Имеет увеличенный таймаут для большей надежности
     * - Подробно логирует процесс обработки
     */
    static class Consumer implements Runnable {
        private final Exchanger<DataBuffer> exchanger;  // Общий обменник
        private final int expectedItems;                // Сколько элементов ожидаем
        private int itemsProcessed = 0;                 // Счетчик обработанных элементов

        Consumer(Exchanger<DataBuffer> exchanger, int expectedItems) {
            this.exchanger = exchanger;
            this.expectedItems = expectedItems;
        }

        @Override
        public void run() {
            try {
                while (itemsProcessed < expectedItems) {
                    // 1. Создаем пустой буфер для обмена
                    DataBuffer inBuffer = exchanger.exchange(
                            new DataBuffer("EMPTY"),
                            EXCHANGE_TIMEOUT + 1, // Чуть больше таймаута Producer
                            TimeUnit.SECONDS
                    );

                    // 2. Проверяем, что получили реальные данные
                    if (!"EMPTY".equals(inBuffer.data)) {
                        itemsProcessed++;
                        System.out.println("Consumer: обработал " + inBuffer);

                        // 3. Имитация обработки данных
                        Thread.sleep(6000 + (long)(Math.random() * 1500));
                    }
                }
                // 4. Завершение работы
                System.out.println("Consumer: завершил обработку " + itemsProcessed + " элементов");
            } catch (InterruptedException e) {
                // 5. Обработка прерывания
                System.out.println("Consumer: прерван");
                Thread.currentThread().interrupt();
            } catch (TimeoutException e) {
                // 6. Обработка таймаута
                System.out.println("Consumer: таймаут ожидания данных");
            } catch (Exception e) {
                // 7. Обработка других ошибок
                System.out.println("Consumer: ошибка - " + e.getMessage());
            }
        }
    }
}