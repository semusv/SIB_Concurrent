package easy;

import java.util.concurrent.Exchanger;

import lombok.extern.slf4j.Slf4j;

/**
 * Упрощённый пример использования Exchanger для обмена данными между двумя потоками.
 */
@Slf4j
public class SimpleExchangerExample {
    public static void main(String[] args) {
        // Создаём экземпляр Exchanger
        Exchanger<String> exchanger = new Exchanger<>();

        // Запускаем два потока - производитель и потребитель
        new Thread(new Producer(exchanger)).start();
        new Thread(new Consumer(exchanger)).start();
    }

    /**
     * Поставщик (Producer) - отправляет данные потребителю.
     */
    static class Producer implements Runnable {
        private final Exchanger<String> exchanger;

        Producer(Exchanger<String> exchanger) {
            this.exchanger = exchanger;
        }

        @Override
        public void run() {
            String dataOut = "Data from Producer"; // Данные для отправки



            try {
                Thread.sleep((long) (Math.random()*5000));
                log.info("Producer has data: {}", dataOut);

                // Обмен данными с потребителем
                String dataIn = exchanger.exchange(dataOut);
                log.info("Producer received: {}", dataIn);
            } catch (InterruptedException e) {
                log.error("Producer was interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Потребитель (Consumer) - получает данные от поставщика.
     */
    static class Consumer implements Runnable {
        private final Exchanger<String> exchanger;

        Consumer(Exchanger<String> exchanger) {
            this.exchanger = exchanger;
        }

        @Override
        public void run() {
            try {
                String dataOut = "Data from Consumer"; // Данные для отправки
                Thread.sleep((long) (Math.random()*5000));
                log.info("Consumer has data: {}", dataOut);

                // Обмен данными с поставщиком
                String dataIn = exchanger.exchange(dataOut);
                log.info("Consumer received: {}", dataIn);
            } catch (InterruptedException e) {
                log.error("Consumer was interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }
}
