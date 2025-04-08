package atomic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IncrementingTestSync {
    // Общая переменная для инкрементирования из нескольких потоков
    // Проблема: без синхронизации возможны race conditions
    private Integer globalVariable = 0;

    private final int THREDS_NUM = 100;
    private final int INCREMENTS = 100;

    @Test
    void shouldExecuteInParallel() throws InterruptedException {
        // Создаем список из 10 потоков, каждый будет выполнять increment100times()
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < THREDS_NUM; i++) {
            threads.add(new Thread(this::increment100times));
        }

        // Запускаем все потоки
        for (Thread thread : threads) {
            thread.start();
        }

        // Ожидаем завершения всех потоков
        for (Thread thread : threads) {
            // Проверяем, не завершился ли поток уже
            if (thread.getState() != Thread.State.TERMINATED) {
                // join() блокирует текущий поток до завершения целевого потока
                thread.join();
            }
        }

        // Проверяем, что общее количество инкрементов равно 1000 (10 потоков * 100 раз)
        assertEquals(INCREMENTS * THREDS_NUM, globalVariable);
    }

    // Метод, который будет вызываться в каждом потоке
    private Void increment100times() {
        // Каждый поток выполняет 100 итераций инкремента
        for (int iter = 0; iter < INCREMENTS; iter++) {
            // Синхронизированный инкремент
            syncronizedIncrement100times();

            try {
                // Искусственная задержка для имитации работы
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // В случае прерывания потока
                e.printStackTrace();
            }
        }
        return null;
    }

    // Синхронизированный метод для атомарного инкремента
    private synchronized void syncronizedIncrement100times() {
        // Операция инкремента защищена synchronized
        globalVariable++;
    }
}