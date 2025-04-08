package atomic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
public class IncrementingTestAtomic {
    private AtomicInteger globalVariable = new AtomicInteger(0);

    private final int THREDS_NUM = 100;
    private final int INCREMENTS = 100;

    @Test
    void shouldExecuteInParallel() throws InterruptedException {
        // Создаем список из 10 потоков, каждый будет выполнять increment100times()
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < THREDS_NUM; i++) {
            threads.add(new Thread(this::increment100times));
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            if ( thread.getState() != Thread.State.TERMINATED ) {
                thread.join();
            }
        }
        assertEquals(INCREMENTS * THREDS_NUM, globalVariable.get());
    }

    private Void increment100times() {
        for (int iter = 0; iter < INCREMENTS; iter++) {
            globalVariable.incrementAndGet();
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}