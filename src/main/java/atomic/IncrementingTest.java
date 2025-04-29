package atomic;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тест для демонстрации проблем многопоточного доступа к разделяемой переменной.
 * Ожидается, что 100 потоков, каждый увеличивающий переменную 100 раз,
 * в итоге дадут значение 10_000, но из-за race condition это не гарантируется.
 */
public class IncrementingTest {
    // Разделяемая переменная, доступная всем потокам
    private Integer globalVariable = 0;
    // Количество потоков для теста
    private final int THREADS_NUM = 10;
    // Количество инкрементов на каждый поток
    private final int INCREMENTS = 100;

    /**
     * Тест, запускающий THREDS_NUM потоков, каждый из которых
     * инкрементирует globalVariable INCREMENTS раз.
     * Из-за отсутствия синхронизации фактический результат может отличаться от ожидаемого.
     * @throws InterruptedException если поток был прерван во время join()
     */
    @Test
    void shouldExecuteInParallel() throws InterruptedException {
        // Создаем и запускаем потоки
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < THREADS_NUM; i++) {
            threads.add(new Thread(this::increment100times));
        }

        // Стартуем все потоки
        for (Thread thread : threads) {
            thread.start();
        }

        // Ожидаем завершения всех потоков
        for (Thread thread : threads) {
            if (thread.getState() != Thread.State.TERMINATED) {
                thread.join();
            }
        }

        // Проверяем результат (ожидаем 10_000, но из-за race condition может быть меньше)
        assertEquals(INCREMENTS * THREADS_NUM, globalVariable);
    }

    /**
     * Метод, выполняющий INCREMENTS инкрементов globalVariable.
     * Между инкрементами добавлена небольшая задержка для увеличения
     * вероятности race condition.
     * @return null (возвращаемое значение не используется)
     */
    private Void increment100times() {
        for (int iter = 0; iter < INCREMENTS; iter++) {
            globalVariable++; // Неатомарная операция (read-modify-write)
            try {
                Thread.sleep(1); // Искусственная задержка для демонстрации проблемы
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}