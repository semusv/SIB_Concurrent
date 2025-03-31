package executors;

import java.util.concurrent.*;

public class ExecutorExample {
    public static void main(String[] args) throws Exception {
        // Создаём пул из 3 потоков (фиксированный размер)
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // Запускаем 12 одинаковых задач (Runnable)
        for (int i = 0; i < 12; i++) {
            final int taskNumber = i + 1; // Номер задачи для логов
            executor.execute(() -> {
                System.out.println(
                        "Задача " + taskNumber + " выполняется в " +
                                Thread.currentThread().getName()
                );
            });
        }

        // Запускаем Callable-задачу, которая вернёт результат через 3 секунды
        Callable<String> stringCallable = () -> {
            Thread.sleep(3000); // Имитация долгой операции
            return "Результат задачи Callable из потока " + Thread.currentThread().getName();
        };
        Future<String> future = executor.submit((stringCallable));

        System.out.println("Инициируем завершение ExecutorService...");
        executor.shutdown(); // Прекращаем принимать новые задачи

        // Мониторим состояние Future
        while (!future.isDone()) {
            System.out.println("Ожидаем завершения Future... Текущий статус: " + future.state());
            Thread.sleep(200); // Проверяем каждые 200 мс
        }

        // Выводим финальный статус
        System.out.println("Финальный статус Future: " + future.state());

        // Получаем результат (блокирующий вызов, но мы уже уверены, что задача завершена)
        System.out.println("Результат Future: " + future.get());
    }
}