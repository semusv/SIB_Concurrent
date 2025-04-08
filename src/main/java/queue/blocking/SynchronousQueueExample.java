package queue.blocking;
import java.util.concurrent.SynchronousQueue;

/**
 * Демонстрация работы SynchronousQueue - блокирующей очереди с нулевой емкостью,
 * где передача данных происходит только при синхронном взаимодействии производителя и потребителя.
 *
 * Особенности:
 * - Гарантирует, что каждый элемент будет передан непосредственно от одного потока другому
 * - Поддерживает graceful shutdown через shutdown hook
 */
public class SynchronousQueueExample {
    // Задержка между сообщениями производителя (в миллисекундах)
    private static final int MESSAGE_DELAY_MS = 1000;

    // Общее количество сообщений для передачи
    private static final int MESSAGE_COUNT = 3;

    // Флаг для контроля работы потоков (volatile для гарантии видимости между потоками)
    private static volatile boolean running = true;

    public static void main(String[] args) {
        // Создаем SynchronousQueue - очередь без буферизации
        SynchronousQueue<String> queue = new SynchronousQueue<>();

        // ================= ПОТОК-ПРОИЗВОДИТЕЛЬ =================
        Thread producer = new Thread(() -> {
            try {
                String[] messages = {"Привет", "Как дела?", "Пока"};

                for (String msg : messages) {
                    // Проверяем флаг перед каждой операцией
                    if (!running) {
                        System.out.println("[Производитель] Получен сигнал остановки");
                        break;
                    }

                    System.out.println("[Производитель] Пытаюсь отправить: '" + msg + "'");

                    // Блокирующая операция - поток ждет, пока потребитель не заберет сообщение
                    queue.put(msg);
                    System.out.println("[Производитель] Сообщение '" + msg + "' отправлено");

                    // Имитация обработки перед отправкой следующего сообщения
                    Thread.sleep(MESSAGE_DELAY_MS);
                }

                System.out.println("[Производитель] Завершил отправку сообщений");
            } catch (InterruptedException e) {
                // Корректная обработка прерывания
                System.out.println("[Производитель] Поток прерван во время работы");
                Thread.currentThread().interrupt();  // Восстановление статуса прерывания
            }
        }, "Producer-Thread");  // Имя потока для удобства отладки

        // ================= ПОТОК-ПОТРЕБИТЕЛЬ =================
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < MESSAGE_COUNT && running; i++) {
                    System.out.println("[Потребитель] Ожидаю сообщение...");

                    // Блокирующая операция - поток ждет новое сообщение
                    String msg = queue.take();
                    System.out.println("[Потребитель] Получено: '" + msg + "'");
                }

                System.out.println("[Потребитель] Завершил обработку сообщений");
            } catch (InterruptedException e) {
                System.out.println("[Потребитель] Поток прерван во время работы");
                Thread.currentThread().interrupt();
            }
        }, "Consumer-Thread");

        // ================= SHUTDOWN HOOK =================
        /*
         * Механизм graceful shutdown:
         * 1. Устанавливаем флаг running=false для остановки потоков
         * 2. Прерываем потоки, если они заблокированы
         * 3. Даем время на корректное завершение
         */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Shutdown Hook] Получен сигнал завершения...");

            // 1. Запрещаем дальнейшую обработку
            running = false;

            // 2. Прерываем потоки (разблокируем, если они ждут в put/take)
            producer.interrupt();
            consumer.interrupt();

            try {
                // 3. Даем потокам 500мс на завершение
                System.out.println("[Shutdown Hook] Ожидаю завершения потоков...");
                producer.join(500);
                consumer.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("[Shutdown Hook] Программа завершена");
        }, "Shutdown-Hook"));

        // ================= ЗАПУСК ПРОГРАММЫ =================
        System.out.println("Запуск программы...");
        producer.start();
        consumer.start();

        try {
            // Основной поток ждет завершения рабочих потоков
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            System.out.println("Основной поток был прерван");
            Thread.currentThread().interrupt();
        }

        System.out.println("Программа завершила работу");
    }
}