package locks;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Реализация ограниченного буфера (Bounded Buffer) с использованием
 * ReentrantLock и Condition для координации производителей и потребителей.
 *
 * Особенности:
 * - Потокобезопасная работа с буфером
 * - Использование двух Condition для эффективного ожидания
 * - Кольцевая структура буфера
 * - Корректная обработка прерываний
 */
public class BoundedBuffer {
    private final Lock lock = new ReentrantLock();

    // Условия для ожидания
    private final Condition notFull = lock.newCondition();  // Ожидание освобождения места
    private final Condition notEmpty = lock.newCondition(); // Ожидание появления данных

    // Кольцевой буфер и указатели
    private final Object[] buffer;
    private int putIndex = 0;  // Индекс для добавления
    private int takeIndex = 0; // Индекс для извлечения
    private int count = 0;     // Текущее количество элементов

    /**
     * Создает буфер указанного размера
     * @param capacity максимальная емкость буфера
     */
    public BoundedBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Размер буфера должен быть > 0");
        }
        this.buffer = new Object[capacity];
    }

    /**
     * Добавляет элемент в буфер.
     * Если буфер полон, поток блокируется до освобождения места.
     *
     * @param element элемент для добавления
     * @throws InterruptedException если поток был прерван во время ожидания
     */
    public void put(Object element) throws InterruptedException {
        lock.lock();
        try {
            // Ожидаем, пока не появится свободное место
            while (isFull()) {
                System.out.println(Thread.currentThread().getName() + ": Буфер полон, ожидание...");
                notFull.await();
            }

            // Добавляем элемент
            buffer[putIndex] = element;
            putIndex = (putIndex + 1) % buffer.length; // Кольцевой буфер
            count++;

            System.out.println(Thread.currentThread().getName() +
                    " добавил: " + element + " (размер: " + count + ")");

            // Сигнализируем потребителям, что появились данные
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Извлекает элемент из буфера.
     * Если буфер пуст, поток блокируется до появления данных.
     *
     * @return извлеченный элемент
     * @throws InterruptedException если поток был прерван во время ожидания
     */
    public Object take() throws InterruptedException {
        lock.lock();
        try {
            // Ожидаем, пока не появятся данные
            while (isEmpty()) {
                System.out.println(Thread.currentThread().getName() + ": Буфер пуст, ожидание...");
                notEmpty.await();
            }

            // Извлекаем элемент
            Object element = buffer[takeIndex];
            buffer[takeIndex] = null; // Помогаем сборщику мусора
            takeIndex = (takeIndex + 1) % buffer.length; // Кольцевой буфер
            count--;

            System.out.println(Thread.currentThread().getName() +
                    " извлек: " + element + " (размер: " + count + ")");

            // Сигнализируем производителям, что появилось место
            notFull.signal();

            return element;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Проверяет, полон ли буфер
     * @return true если буфер полон
     */
    private boolean isFull() {
        return count == buffer.length;
    }

    /**
     * Проверяет, пуст ли буфер
     * @return true если буфер пуст
     */
    private boolean isEmpty() {
        return count == 0;
    }

    /**
     * Тестовый пример работы буфера
     */
    public static void main(String[] args) {
        final BoundedBuffer buffer = new BoundedBuffer(3);

        // Производитель
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    buffer.put("Item-" + i);
                    Thread.sleep(100); // Имитация работы
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer");

        // Потребитель (медленнее производителя)
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    buffer.take();
                    Thread.sleep(300); // Имитация работы
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer");

        producer.start();
        consumer.start();
    }
}