package atomic;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * Демонстрация решения ABA-проблемы с помощью AtomicStampedReference.
 * Показывает, как избежать проблем при параллельных операциях над счетом.
 */
@Slf4j
public class AtomicStampedReferenceExample {
    public static void main(String[] args) throws InterruptedException {
        log.info("Создаем банковский счет для Alice с начальным балансом 100");
        BankAccount account = new BankAccount("Alice", 100);

        // Поток 1: пытается снять 50 со счета
        Thread thread1 = new Thread(() -> {
            log.debug("Поток 1 начал операцию снятия 50");
            boolean success = account.withdraw(50);
            log.info("Поток 1: результат снятия 50 - {}",
                    success ? "Успешно" : "Недостаточно средств");
        }, "WithdrawThread");

        // Поток 2: имитирует ABA-проблему
        Thread thread2 = new Thread(() -> {
            log.debug("Поток 2 начинает операции внесения/снятия");
            account.deposit(20);
            log.info("Поток 2: Внес 20, новый баланс = {}", account.getBalance());

            account.withdraw(20);
            log.info("Поток 2: Снял 20, новый баланс = {}", account.getBalance());
        }, "ABA-SimulationThread");

        log.info("Запускаем потоки (намеренно с задержкой для демонстрации ABA)");
        thread2.start();
        Thread.sleep(100); // Искусственная задержка для демонстрации
        thread1.start();

        log.debug("Ожидаем завершения потоков...");
        thread2.join();
        thread1.join();

        log.info("Финальный результат:");
        log.info("Баланс: {}", account.getBalance());
        log.info("Версия: {}", account.getVersion());
    }
}

/**
 * Класс банковского счета с защитой от ABA-проблемы.
 * Использует AtomicStampedReference для хранения баланса и версии.
 */
@Data // Lombok аннотация для генерации геттеров/toString
@Slf4j
class BankAccount {
    private final String owner;

    /**
     * AtomicStampedReference хранит:
     * - reference: текущий баланс (Integer)
     * - stamp: версия/метка времени (int)
     *
     * Позволяет обнаруживать изменения между чтением и записью.
     */
    private final AtomicStampedReference<Integer> balance;

    public BankAccount(String owner, int initialBalance) {
        this.owner = owner;
        log.debug("Инициализация счета для {}. Начальный баланс: {}", owner, initialBalance);
        this.balance = new AtomicStampedReference<>(initialBalance, 0);
    }

    /**
     * Внесение средств на счет.
     * @param amount сумма для внесения
     */
    public void deposit(int amount) {
        int[] stampHolder = new int[1]; // Хранит текущую версию
        int currentBalance, newBalance;

        do {
            currentBalance = balance.get(stampHolder); // Получаем баланс+версию
            newBalance = currentBalance + amount;
            log.trace("Попытка внесения: текущий баланс={}, версия={}, новый баланс={}",
                    currentBalance, stampHolder[0], newBalance);
        } while (!balance.compareAndSet(
                currentBalance, newBalance,
                stampHolder[0], stampHolder[0] + 1));

        log.debug("Успешное внесение. Новый баланс: {}", newBalance);
    }

    /**
     * Снятие средств со счета.
     * @param amount сумма для снятия
     * @return true если операция успешна, false если недостаточно средств
     */
    public boolean withdraw(int amount) {
        int[] stampHolder = new int[1];
        int currentBalance, newBalance;

        do {
            currentBalance = balance.get(stampHolder);
            if (currentBalance < amount) {
                log.warn("Недостаточно средств для снятия. Требуется: {}, доступно: {}",
                        amount, currentBalance);
                return false;
            }
            newBalance = currentBalance - amount;
            log.trace("Попытка снятия: текущий баланс={}, версия={}, новый баланс={}",
                    currentBalance, stampHolder[0], newBalance);
        } while (!balance.compareAndSet(
                currentBalance, newBalance,
                stampHolder[0], stampHolder[0] + 1));

        log.debug("Успешное снятие. Новый баланс: {}", newBalance);
        return true;
    }

    /**
     * @return текущий баланс (без проверки версии)
     */
    public int getBalance() {
        int currentBalance = balance.getReference();
        log.trace("Запрос баланса: {}", currentBalance);
        return currentBalance;
    }

    /**
     * @return текущую версию данных
     */
    public int getVersion() {
        int version = balance.getStamp();
        log.trace("Запрос версии: {}", version);
        return version;
    }
}