package queue;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class WaitNotifyExample {
    // Общая очередь для Producer-Consumer
    private final Queue<Integer> queue = new LinkedList<>();
    // Максимальный размер очереди
    private final int CAPACITY = 5;
    // Объект для синхронизации (монитор)
    private final Object locker = new Object();
    // Флаг для управления работой потоков (volatile для видимости между потоками)
    private volatile boolean isRunning = true;

    public static void main(String[] args) {
        WaitNotifyExample example = new WaitNotifyExample();

        // Создаем поток-производитель (Producer)
        Thread producer = new Thread(() -> {
            try {
                example.produce();
            } catch (InterruptedException e) {
                System.out.println("[Производитель] Поток прерван!");
                Thread.currentThread().interrupt(); // Восстанавливаем флаг прерывания
            }
        }, "Producer");

        // Создаем пул потребителей (Consumers)
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

        // Даем потокам поработать 20 секунд
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Корректное завершение работы потоков
        example.stop(); // Устанавливаем isRunning=false и будим все потоки
        producer.interrupt(); // На случай, если поток ждет в wait()
        consumers.forEach(Thread::interrupt); // Прерываем потребителей
    }

    // Метод для остановки работы Producer-Consumer
    public void stop() {
        isRunning = false; // Говорим потокам, что нужно завершаться
        synchronized (locker) {
            locker.notifyAll(); // Будим все ожидающие потоки
        }
    }

    // Метод Producer (добавляет элементы в очередь)
    public void produce() throws InterruptedException {
        int value = 0;
        while (isRunning) {
            synchronized (locker) {
                // Ждем, если очередь заполнена (пока Consumer не освободит место)
                while (queue.size() == CAPACITY && isRunning) {
                    System.out.println("[Производитель] Очередь полна. Ждем...");
                    locker.wait(); // Освобождает монитор и ждет notify()
                }

                // Проверяем, не нужно ли завершать работу
                if (!isRunning) break;

                // Добавляем элемент в очередь
                queue.add(value);
                System.out.println("[Производитель] Добавлено: " + value +
                        " Размер очереди: " + queue.size());
                value++;

                locker.notifyAll(); // Будим Consumer'ов
                Thread.sleep(500); // Имитируем работу
            }
        }
        System.out.println("[Производитель] Завершает работу");
    }

    // Метод Consumer (забирает элементы из очереди)
    public void consume(int consumerId) throws InterruptedException {
        while (isRunning) {
            synchronized (locker) {
                // Ждем, если очередь пуста (пока Producer не добавит данные)
                while (queue.isEmpty() && isRunning) {
                    System.out.printf("[c-%d]- Очередь пуста. Ждем...%n", consumerId);
                    locker.wait();
                }

                // Если очередь пуста и isRunning=false, завершаем работу
                if (!isRunning && queue.isEmpty()) break;

                // Забираем элемент из очереди
                int value = queue.poll();
                System.out.printf("[c-%d] Обработано: %d Размер очереди: %d %n",
                        consumerId, value, queue.size());

                locker.notifyAll(); // Будим Producer'ов
                Thread.sleep(1000); // Имитируем обработку
            }
        }
        System.out.println("[Потребитель " + consumerId + "] Завершает работу");

        // Демонстрация работы ConcurrentLinkedDeque (не относится к Producer-Consumer)
        Deque<String> deque = new ConcurrentLinkedDeque<>();
        deque.addFirst("A"); // Добавляем в начало
        deque.addLast("B");  // Добавляем в конец
        String first = deque.pollFirst(); // Забираем первый элемент
    }
}

