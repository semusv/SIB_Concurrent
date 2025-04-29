package atomic;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * Демонстрация работы счетчика с использованием CAS (Compare-And-Swap) операции.
 * Показывает низкоуровневый принцип работы атомарных операций.
 */
@Slf4j
public class AtomicCasCounter {
    /**
     * Точка входа в программу.
     * @param args аргументы командной строки (не используются)
     */
    public static void main(String[] args) {
        // Логируем начало демонстрации
        log.info("Starting AtomicCasCounter demonstration");

        // Создаем атомарный счетчик (потокобезопасный)
        AtomicInteger counter = new AtomicInteger(0);
        log.debug("Инициализирован счетчик. Начальное значение: {}", counter.get());

        // Лямбда-выражение для задачи инкрементирования
        Runnable incrementTask = () -> {
            log.debug("Поток {} начал выполнение задачи", Thread.currentThread().getName());

            int attempts = 0;  // Счетчик попыток CAS
            int oldValue, newValue;

            do {
                oldValue = counter.get();       // 1. Читаем текущее значение
                newValue = oldValue + 1;       // 2. Вычисляем новое значение
                attempts++;

                log.trace("Попытка {}: oldValue={}, newValue={}", attempts, oldValue, newValue);

                // 3. Пытаемся установить новое значение атомарно
            } while (!counter.compareAndSet(oldValue, newValue));

            log.debug("Поток {} завершил работу после {} попыток. Новое значение: {}",
                    Thread.currentThread().getName(), attempts, newValue);
        };

        // Создаем и запускаем потоки
        Thread thread1 = new Thread(incrementTask, "IncrementThread-1");
        Thread thread2 = new Thread(incrementTask, "IncrementThread-2");

        log.info("Запускаем потоки...");
        thread1.start();
        thread2.start();

        try {
            log.debug("Основной поток ожидает завершения рабочих потоков...");
            thread1.join();
            thread2.join();

            // Выводим финальный результат
            log.info("Финальное значение счетчика: {}", counter.get());
        } catch (InterruptedException e) {
            log.error("Основной поток был прерван!", e);
            Thread.currentThread().interrupt();  // Восстанавливаем флаг прерывания
        }

        log.info("Демонстрация работы AtomicCasCounter завершена");
    }
}