package atomic;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тест демонстрирует потокобезопасный счетчик с использованием AtomicInteger.
 * В отличие от synchronized-подхода, обеспечивает лучшую производительность
 * за счет неблокирующих алгоритмов.
 */
public class IncrementingTestAtomic {
    // Потокобезопасный счетчик
    private final AtomicInteger globalVariable = new AtomicInteger(0);
    // Количество потоков для теста
    private final int THREADS_NUM = 10;
    // Количество инкрементов на каждый поток
    private final int INCREMENTS = 100;

    /**
     * Тест создает THREADS_NUM потоков, каждый из которых выполняет
     * INCREMENTS_PER_THREAD атомарных инкрементов.
     */
    @Test
    void shouldExecuteInParallel() throws InterruptedException {
        List<Thread> threads = new ArrayList<>(THREADS_NUM);  // Оптимизация размера

        // Создаем потоки
        for (int i = 0; i < THREADS_NUM; i++) {
            threads.add(new Thread(this::incrementInThread));
        }

        // Запускаем все потоки одновременно
        threads.forEach(Thread::start);

        // Ожидаем завершения всех потоков
        for (Thread thread : threads) {
            if (thread.isAlive()) {  // Более читаемая проверка
                thread.join();
            }
        }

        // Проверяем итоговое значение
        assertEquals(INCREMENTS * THREADS_NUM, globalVariable.get());
    }

    /**
     * Метод, выполняемый в каждом потоке.
     * Совершает заданное число атомарных инкрементов.
     */
    private void incrementInThread() {  // Изменен возвращаемый тип на void
        for (int i = 0; i < INCREMENTS; i++) {
            globalVariable.incrementAndGet();  // Атомарный инкремент

            try {
                Thread.sleep(1);  // Имитация работы (в реальных тестах не рекомендуется)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();  // Восстанавливаем флаг прерывания
                throw new RuntimeException("Thread interrupted", e);
            }
        }
    }
}