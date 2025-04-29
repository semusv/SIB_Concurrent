package executors;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * Пример использования ExecutorService для выполнения задач с фиксированным пулом потоков.
 * <p>
 * Особенности:
 * <ul>
 *   <li>Использует FixedThreadPool для управления потоками.</li>
 *   <li>Запускает несколько задач типа Runnable и одну задачу типа Callable.</li>
 *   <li>Демонстрирует мониторинг состояния Future и получение результата.</li>
 *   <li>Грамотно завершает работу ExecutorService.</li>
 * </ul>
 */
@Slf4j
public class ExecutorExample {

    public static void main(String[] args) throws Exception {
        // Создаём пул из 3 потоков (фиксированный размер)
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // Запускаем 12 одинаковых задач (Runnable)
        for (int i = 0; i < 12; i++) {
            final int taskNumber = i + 1; // Номер задачи для логов
            executor.execute(() -> {
                log.info("Задача {} выполняется в {}", taskNumber, Thread.currentThread().getName());
            });
        }

        // Запускаем Callable-задачу, которая вернёт результат через 3 секунды
        Callable<String> stringCallable = () -> {
            Thread.sleep(3000); // Имитация долгой операции
            return "Результат задачи Callable из потока --" + Thread.currentThread().getName();
        };
        Future<String> future = executor.submit(stringCallable);

        log.info("Инициируем завершение ExecutorService...");
        executor.shutdown(); // Прекращаем принимать новые задачи

        // Мониторим состояние Future
        while (!future.isDone()) {
            log.debug("Ожидаем завершения Future... Текущий статус: {}", future.state());
            Thread.sleep(200); // Проверяем каждые 200 мс
        }

        // Выводим финальный статус
        log.info("Финальный статус Future: {}", future.state());

        // Получаем результат (блокирующий вызов, но мы уже уверены, что задача завершена)
        log.info("Результат Future: {}", future.get());
    }
}
