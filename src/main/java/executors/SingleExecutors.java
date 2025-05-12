package executors;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Пример использования `newSingleThreadExecutor` для выполнения задач в одном потоке.
 * <p>
 * Особенности:
 * <ul>
 *   <li>Использует SingleThreadExecutor, который выполняет задачи в одном потоке последовательно.</li>
 *   <li>Демонстрирует выполнение задач типа `Runnable` и `Callable`.</li>
 *   <li>Показывает мониторинг состояния `Future` и получение результата после завершения задачи.</li>
 *   <li>Грамотно завершает работу `ExecutorService` после выполнения всех задач.</li>
 * </ul>
 */
@Slf4j
public class SingleExecutors {

    public static void main(String[] args) throws Exception {
        // Создаем ExecutorService с одним потоком
        // Все задачи будут выполняться последовательно в одном потоке
//        ExecutorService executor = Executors.newSingleThreadExecutor();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Запускаем 12 задач типа Runnable
        // Каждая задача будет выполняться последовательно в одном потоке
        for (int i = 0; i < 12; i++) {
            final int taskNumber = i + 1; // Номер задачи для удобства логирования
            executor.execute(() -> {
                try {
                    Thread.sleep((long) (Math.random()*100));
                    log.info("Задача {} выполняется в потоке {}", taskNumber, Thread.currentThread().getName());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            });
        }
        Thread.sleep(200);


        // Создаем Callable-задачу, которая возвращает результат через 3 секунды
        // Callable позволяет получить результат выполнения задачи после её завершения
        Callable<String> stringCallable = () -> {
            Thread.sleep(3000); // Имитация долгой операции
            return "Результат задачи Callable из потока --" + Thread.currentThread().getName();
        };
        Future<String> future = executor.submit(stringCallable);

        log.info("Инициируем завершение ExecutorService...");
        executor.shutdown(); // Запрещаем добавление новых задач и начинаем завершение

        // Мониторим состояние Future, пока задача не будет завершена
        // Это позволяет отслеживать прогресс выполнения задачи
        while (!future.isDone()) {
            log.debug("Ожидаем завершения Future... Текущий статус: {}", future.state());
            Thread.sleep(200); // Проверяем состояние каждые 200 мс
        }

        // Выводим финальный статус Future после завершения задачи
        log.info("Финальный статус Future: {}", future.state());

        // Получаем результат выполнения Callable-задачи
        // Метод get() блокирует выполнение, пока задача не завершится
        log.info("Результат Future: {}", future.get());
    }
}

