package atomic;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тест для демонстрации работы синхронизации при многопоточном доступе.
 * В отличие от несинхронизированной версии, здесь используется synchronized,
 * что гарантирует корректный результат.
 */
public class IncrementingTestSync {
    // Разделяемая переменная, доступная всем потокам
    private Integer globalVariable = 0;
    // Количество потоков для теста
    private final int THREADS_NUM = 10;
    // Количество инкрементов на каждый поток
    private final int INCREMENTS = 100;

    /**
     * Тест, запускающий THREADS_NUM потоков для инкрементирования общей переменной.
     * Каждый поток выполняет INCREMENTS операций инкремента.
     * Благодаря синхронизации, итоговое значение должно быть точно равно THREADS_NUM * INCREMENTS.
     */
    @Test
    void shouldExecuteInParallel() throws InterruptedException {
        List<Thread> threads = new ArrayList<>();

        // Создаем и запускаем потоки
        for (int i = 0; i < THREADS_NUM; i++) {
            threads.add(new Thread(this::increment100times));
        }

        // Параллельный запуск
        threads.forEach(Thread::start);

        // Ожидание завершения всех потоков
        for (Thread thread : threads) {
            if (thread.isAlive()) {  // Более читаемая проверка, чем getState()
                thread.join();
            }
        }

        // Проверка результата (должно быть 10_000)
        assertEquals(INCREMENTS * THREADS_NUM, globalVariable);
    }

    /**
     * Метод, выполняемый в каждом потоке.
     * Выполняет INCREMENTS раз синхронизированный инкремент.
     */
    private Void increment100times() {
        for (int iter = 0; iter < INCREMENTS; iter++) {
            synchronizedIncrement();
            try {
                Thread.sleep(1);  // Имитация работы (в реальных тестах обычно не используется)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();  // Правильная обработка прерывания
                throw new RuntimeException("Thread interrupted", e);
            }
        }
        return null;
    }

    /**
     * Синхронизированный метод инкремента.
     * Гарантирует атомарность операции globalVariable++.
     */
    private synchronized void synchronizedIncrement() {
        globalVariable++;  // Атомарная операция благодаря synchronized
    }
}