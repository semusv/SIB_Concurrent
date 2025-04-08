package atomic;

import lombok.Data;

import java.util.concurrent.atomic.AtomicStampedReference;

// Пример использования AtomicStampedReference для решения ABA-проблемы
public class AtomicStampedReferenceExample {
    public static void main(String[] args) throws InterruptedException {
        // Создаем банковский счет с начальным балансом 100 и версией 0
        BankAccount account = new BankAccount("Alice", 100);

        // Поток 1: пытается снять 50 со счета
        Thread thread1 = new Thread(() -> {
            boolean success = account.withdraw(50);
            System.out.println("Thread1: Снял 50 - " + (success ? "Успешно" : "Недостаточно средств"));
        });

        // Поток 2: имитирует ABA-проблему - вносит и сразу снимает 20
        Thread thread2 = new Thread(() -> {
            account.deposit(20);
            System.out.println("Thread2: Внес 20, новый баланс = " + account.getBalance());
            account.withdraw(20);
            System.out.println("Thread2: Снял 20, новый баланс = " + account.getBalance());
        });

        // Запускаем потоки (намеренно сначала thread2 для демонстрации ABA)
        thread2.start();
        Thread.sleep(10000);
        thread1.start();

        // Ожидаем завершения обоих потоков
        thread2.join();
        thread1.join();

        // Выводим итоговый баланс и версию
        System.out.println("Итоговый баланс: " + account.getBalance());
        System.out.println("Версия: " + account.getVersion());
    }
}

// Класс банковского счета с использованием AtomicStampedReference
class BankAccount {
    private final String owner; // Владелец счета
    // Баланс счета с версионным контролем для предотвращения ABA-проблемы
    private final AtomicStampedReference<Integer> balance;

    public BankAccount(String owner, int initialBalance) {
        this.owner = owner;
        // Инициализируем AtomicStampedReference с начальным балансом и версией 0
        this.balance = new AtomicStampedReference<>(initialBalance, 0);
    }

    // Метод для внесения средств на счет
    public void deposit(int amount) {
        int[] stampHolder = new int[1]; // Массив для хранения текущей версии
        int currentBalance;
        int newBalance;

        // Цикл повторяется, пока CAS операция не выполнится успешно
        do {
            // Получаем текущий баланс и версию
            currentBalance = balance.get(stampHolder);
            // Вычисляем новый баланс
            newBalance = currentBalance + amount;
            // Пытаемся атомарно обновить баланс, если версия не изменилась
        } while (!balance.compareAndSet(currentBalance, newBalance,
                stampHolder[0], stampHolder[0] + 1));
    }

    // Метод для снятия средств со счета
    public boolean withdraw(int amount) {
        int[] stampHolder = new int[1]; // Массив для хранения текущей версии
        int currentBalance;
        int newBalance;

        // Цикл повторяется, пока CAS операция не выполнится успешно
        do {
            // Получаем текущий баланс и версию
            currentBalance = balance.get(stampHolder);
            // Проверяем достаточно ли средств
            if (currentBalance < amount) {
                return false;
            }
            // Вычисляем новый баланс
            newBalance = currentBalance - amount;
            // Пытаемся атомарно обновить баланс, если версия не изменилась
        } while (!balance.compareAndSet(currentBalance, newBalance,
                stampHolder[0], stampHolder[0] + 1));

        return true;
    }

    // Метод для получения текущего баланса (без учета версии)
    public int getBalance() {
        return balance.getReference();
    }

    // Метод для получения текущей версии баланса
    public int getVersion() {
        return balance.getStamp();
    }
}