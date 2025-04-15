package sync;

import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;

/**
 * Пример демонстрирует использование Phaser для синхронизации потоков
 * в нескольких фазах выполнения задачи.
 */
public class SimplePhaserExample {

    public static final int PHASES = 3;

    public static void main(String[] args) {
        /**
         * 1. Создаем Phaser с начальным количеством участников = 1 (главный поток).
         *    Переопределяем метод onAdvance для логирования перехода между фазами.
         */
        Phaser phaser = new Phaser(1) {
            /**
             * Вызывается при переходе между фазами.
             * @param phase номер завершенной фазы
             * @param registeredParties количество зарегистрированных участников
             * @return true - завершить Phaser, false - продолжить выполнение
             */
            @Override
            protected boolean onAdvance(int phase, int registeredParties) {
                if (phase < PHASES) {
                    System.out.println("Фаза " + phase + " завершена!\n");
                    System.out.println("Участников следующей фазы: " + registeredParties);
                    return false; // Продолжаем работу
                } else
                    return true;
            }
        };

        /**
         * 2. Создаем и запускаем 3 рабочих потока.
         *    Каждый новый поток регистрируется в Phaser перед запуском.
         */
        for (int i = 1; i <= PHASES; i++) {
            // Регистрируем нового участника в Phaser
            phaser.register();

            // Создаем и запускаем рабочий поток
            new Thread(new Worker(phaser, i), "Worker-" + i).start();
        }

        /**
         * 3. Главный поток контролирует выполнение 3 фаз.
         *    В каждой фазе:
         *    - Выводим сообщение о начале фазы
         *    - Ожидаем завершения фазы всеми потоками
         *    - Выводим сообщение о синхронизации
         */
        for (int phase = 0; phase < 3; phase++) {
            System.out.println("\nГлавный поток: начало фазы " + phase);

            // Ожидаем завершения текущей фазы всеми участниками
            phaser.arriveAndAwaitAdvance();

            System.out.println("Главный поток: фаза " + phase + " синхронизирована");
        }

        /**
         * 4. Завершаем работу Phaser.
         *    Главный поток выходит из числа участников.
         */
        phaser.arriveAndDeregister();
        System.out.println("\nВсе фазы завершены!");
    }

    /**
     * Класс Worker реализует поток, который выполняет работу в нескольких фазах.
     */
    static class Worker implements Runnable {
        private final Phaser phaser; // Общий Phaser для синхронизации
        private final int id;       // Идентификатор рабочего потока

        Worker(Phaser phaser, int id) {
            this.phaser = phaser;
            this.id = id;
        }

        @Override
        public void run() {
            // Рабочий поток выполняет задачи, пока Phaser не завершен
            while (!phaser.isTerminated()) {
                try {
                    /**
                     * 1. Фаза выполнения работы:
                     *    - Выводим сообщение о начале работы
                     *    - Имитируем обработку с разной длительностью для каждого потока
                     */
                    System.out.println(Thread.currentThread().getName() +
                            " выполняет работу в фазе " + phaser.getPhase());
                    Thread.sleep(1000 + id * 200); // Имитация работы

                    /**
                     * 2. Синхронизация:
                     *    - Сообщаем о завершении фазы
                     *    - Ожидаем остальных участников
                     *    - Получаем номер новой фазы
                     */
                    int currentPhase = phaser.arriveAndAwaitAdvance();

                    /**
                     * 3. Подготовка к следующей фазе:
                     *    - Выводим сообщение о готовности
                     */
                    System.out.println(Thread.currentThread().getName() +
                            " готов к фазе " + currentPhase);

                } catch (InterruptedException e) {
                    // Обработка прерывания потока
                    Thread.currentThread().interrupt();

                    // Выходим из Phaser при прерывании
                    phaser.arriveAndDeregister();
                    break;
                }
            }

            // Сообщение о завершении работы потока
            System.out.println(Thread.currentThread().getName() + " завершил работу");
        }
    }

    public void getDataWithRetry(int maxRetries) {
        StampedLock lock = new StampedLock();
// Оптимистичное чтение
        long stamp = lock.tryOptimisticRead();
// Чтение данных
        if (!lock.validate(stamp)) {
            // Данные изменились, получаем полную блокировку
            stamp = lock.readLock();
            try { // Чтение снова
            } finally {lock.unlockRead(stamp); }
        }
// Запись
        long writeStamp = lock.writeLock();
        try { // Модификация данных
        } finally { lock.unlockWrite(writeStamp); }
    }
}


