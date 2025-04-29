package sync;

import java.util.concurrent.Phaser;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimplePhaserExample {
    static final int phasesCount = 3;

    public static void main(String[] args) throws InterruptedException {
        // 1. Создаем Phaser с 1 участником (главный поток)
        Phaser phaser = new Phaser(1);
        final int workerCount = 3;


        // 2. Запускаем рабочих
        for (int i = 1; i <= workerCount; i++) {
            phaser.register(); // Регистрируем перед запуском
            new Thread(new Worker(phaser, i), "Worker-" + i).start();
        }

        // 3. Управление фазами
        for (int phase = 0; phase < phasesCount; phase++) {
            log.info("MAIN: Начало фазы {}", phase);
            phaser.arriveAndAwaitAdvance(); // Ждем завершения фазы
            log.info("MAIN: Фаза {} завершена", phase);
        }

        // 4. Корректное завершение
        phaser.arriveAndDeregister(); // Главный поток выходит

        // Даем время рабочим потокам завершиться
        Thread.sleep(2000);

        log.info("MAIN: Все фазы завершены!");
    }

    static class Worker implements Runnable {
        private final Phaser phaser;
        private final int id;

        Worker(Phaser phaser, int id) {
            this.phaser = phaser;
            this.id = id;
        }

        @Override
        public void run() {
            try {
                // Работаем только заданное количество фаз
                for (int phase = 0; phase <  phasesCount; phase++) {
                    // Выполнение работы
                    log.info("{}: Работаю в фазе {}",
                            Thread.currentThread().getName(),
                            phaser.getPhase());

                    Thread.sleep(1000 + id * 200); // Имитация работы

                    // Синхронизация
                    int newPhase = phaser.arriveAndAwaitAdvance();
                    log.info("{}: Перешел в фазу {}",
                            Thread.currentThread().getName(),
                            newPhase);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("{}: Прерван", Thread.currentThread().getName());
            } finally {
                phaser.arriveAndDeregister(); // Обязательно выходим
                log.info("{}: Завершил работу", Thread.currentThread().getName());
            }
        }
    }
}