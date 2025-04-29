package queue.blocking;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.SynchronousQueue;

/**
 * Простой пример использования SynchronousQueue.
 * Очередь без буферизации, где данные передаются напрямую между потоками.
 */
@Slf4j
public class SynchronousQueueExample {
    private static final int MESSAGE_DELAY_MS = 1000; // Задержка между сообщениями
    private static volatile boolean running = true;   // Флаг для управления потоками

    public static void main(String[] args) {
        SynchronousQueue<String> queue = new SynchronousQueue<>();

        // Поток-производитель
        Thread producer = new Thread(() -> {
            try {
                for (String msg : new String[]{"Привет", "Как дела?", "Пока"}) {
                    if (!running) break; // Проверка флага

                    log.info("[Производитель] Отправляю: '{}'", msg);
                    queue.put(msg); // Блокирующая отправка
                    log.info("[Производитель] Сообщение '{}' отправлено", msg);

                    Thread.sleep(MESSAGE_DELAY_MS); // Имитация задержки
                }
                running = false;
                log.info("[Производитель] Завершил работу");
            } catch (InterruptedException e) {
                log.warn("[Производитель] Прерван");
                Thread.currentThread().interrupt();
            }
        });

        // Поток-потребитель
        Thread consumer = new Thread(() -> {
            try {
                while ( running ) {
                    log.info("[Потребитель] Ожидаю сообщение...");
                    String msg = queue.take(); // Блокирующее получение
                    log.info("[Потребитель] Получено: '{}'", msg);
                }
                log.info("[Потребитель] Завершил работу");
            } catch (InterruptedException e) {
                log.warn("[Потребитель] Прерван");
                Thread.currentThread().interrupt();
            }
        });

        // Обработчик завершения
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("\n[Завершение] Получен сигнал остановки...");
            running = false;

            producer.interrupt();
            consumer.interrupt();

            try {
                producer.join(500);
                consumer.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            log.info("[Завершение] Программа остановлена");
        }));

        // Запуск потоков
        log.info("Запуск программы...");
        producer.start();
        consumer.start();

        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            log.warn("Основной поток прерван");
            Thread.currentThread().interrupt();
        }

        log.info("Программа завершена");
    }
}
