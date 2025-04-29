package locks;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class BoundedBuffer {
    private final Lock lock = new ReentrantLock(); // Блокировка для синхронизации
    private final Condition notFull = lock.newCondition(); // Условие "не полон"
    private final Condition notEmpty = lock.newCondition(); // Условие "не пуст"

    private final Object[] buffer; // Буфер для хранения элементов
    private int putIndex = 0; // Индекс для добавления
    private int takeIndex = 0; // Индекс для извлечения
    private int count = 0; // Текущее количество элементов

    // Конструктор, инициализирует буфер
    public BoundedBuffer(int capacity) {
        if (capacity <= 0) {
            log.error("Некорректный размер буфера: {}", capacity);
            throw new IllegalArgumentException("Размер буфера должен быть больше 0");
        }
        this.buffer = new Object[capacity];
    }

    // Добавляет элемент в буфер
    public void put(Object element) throws InterruptedException {
        lock.lock();
        try {
            // Если буфер полон, ждем, пока не освободится место
            while (count == buffer.length) {
                log.debug("Буфер полон, ожидание...");
                notFull.await();
            }
            // Добавляем элемент в буфер
            buffer[putIndex] = element;
            putIndex = (putIndex + 1) % buffer.length; // Кольцевой буфер
            count++;
            log.debug("Добавлен элемент: {} (размер буфера: {})", element, count);
            // Уведомляем потребителей, что появились данные
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    // Извлекает элемент из буфера
    public Object take() throws InterruptedException {
        lock.lock();
        try {
            // Если буфер пуст, ждем, пока не появятся данные
            while (count == 0) {
                log.debug("Буфер пуст, ожидание...");
                notEmpty.await();
            }
            // Извлекаем элемент из буфера
            Object element = buffer[takeIndex];
            buffer[takeIndex] = null; // Очищаем ячейку
            takeIndex = (takeIndex + 1) % buffer.length; // Кольцевой буфер
            count--;
            log.debug("Извлечен элемент: {} (размер буфера: {})", element, count);
            // Уведомляем производителей, что появилось место
            notFull.signal();
            return element;
        } finally {
            lock.unlock();
        }
    }

    // Проверяет, полон ли буфер
    private boolean isFull() {
        return count == buffer.length;
    }

    // Проверяет, пуст ли буфер
    private boolean isEmpty() {
        return count == 0;
    }

    // Тестовый пример
    public static void main(String[] args) {
        BoundedBuffer buffer = new BoundedBuffer(3);

        // Производитель
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    buffer.put("Item-" + i);
                    Thread.sleep(100); // Имитация работы
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Производитель был прерван", e);
            }
        }, "Producer");

        // Потребитель
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    buffer.take();
                    Thread.sleep(300); // Имитация работы
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Потребитель был прерван", e);
            }
        }, "Consumer");

        producer.start();
        consumer.start();
    }
}
