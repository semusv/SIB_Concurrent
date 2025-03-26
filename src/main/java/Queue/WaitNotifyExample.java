package Queue;
import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
import java.util.List;

public class WaitNotifyExample {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int CAPACITY = 5;
    private final Object locker = new Object();
    private volatile boolean isRunning = true; // Флаг для управления работой потоков

    public static void main(String[] args) {
        WaitNotifyExample example = new WaitNotifyExample();

        // Создаем поток-производитель
        Thread producer = new Thread(() -> {
            try {
                example.produce();
            } catch (InterruptedException e) {
                System.out.println("[Производитель] Поток прерван!");
                Thread.currentThread().interrupt();
            }
        });

        // Создаем пул потребителей
        List<Thread> consumers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int consumerId = i + 1;
            consumers.add(new Thread(() -> {
                try {
                    example.consume(consumerId);
                } catch (InterruptedException e) {
                    System.out.println("[Потребитель " + consumerId + "] Поток прерван!");
                    Thread.currentThread().interrupt();
                }
            }, "Consumer-" + consumerId));
        }

        // Запускаем потоки
        producer.start();
        consumers.forEach(Thread::start);

        // Даем поработать 20 секунд
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Корректное завершение
        example.stop();
        producer.interrupt();
        consumers.forEach(Thread::interrupt);
    }

    public void stop() {
        isRunning = false;
        synchronized (locker) {
            locker.notifyAll(); // Будим все потоки для завершения
        }
    }

    public void produce() throws InterruptedException {
        int value = 0;
        while (isRunning) {
            synchronized (locker) {
                while (queue.size() == CAPACITY && isRunning) {
                    System.out.println("[Производитель] Очередь полна. Ждем...");
                    locker.wait();
                }

                if (!isRunning) break;

                queue.add(value);
                System.out.println("[Производитель] Добавлено: " + value +
                        " Размер очереди: " + queue.size());
                value++;

                locker.notifyAll();
                Thread.sleep(500);
            }
        }
        System.out.println("[Производитель] Завершает работу");
    }

    public void consume(int consumerId) throws InterruptedException {
        while (isRunning) {
            synchronized (locker) {
                while (queue.isEmpty() && isRunning) {
                    System.out.printf("[c-%d]- Очередь пуста. Ждем...%n", consumerId);
                    locker.wait();
                }

                if (!isRunning && queue.isEmpty()) break;

                int value = queue.poll();
                System.out.printf("[c-%d] Обработано: %d Размер очереди: %d %n",
                        consumerId, value, queue.size() );

                locker.notifyAll();
                Thread.sleep(1000);
            }
        }
        System.out.println("[Потребитель " + consumerId + "] Завершает работу");
    }
}