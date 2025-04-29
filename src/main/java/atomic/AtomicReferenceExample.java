package atomic;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Потокобезопасный класс пользователя с использованием AtomicReference.
 * Демонстрирует атомарные обновления состояния в многопоточной среде.
 */
@Slf4j
public class AtomicReferenceExample {

    /**
     * Класс пользователя с неизменяемыми свойствами.
     * Неизменяемость (immutable) важна для корректной работы с AtomicReference.
     */
    @Getter
    static class User {
        private final String name;
        private final int age;

        public User(@NonNull String name, int age) {
            this.name = name;
            this.age = age;
            log.trace("Создан новый пользователь: {}", this);
        }

        /**
         * Создает нового пользователя с увеличенным возрастом.
         * @return новый объект User с age + 1
         */
        public User withIncrementedAge() {
            User newUser = new User(this.name, this.age + 1);
            log.trace("Инкремент возраста: {} -> {}", this, newUser);
            return newUser;
        }

        @Override
        public String toString() {
            return String.format("age=%d",  age);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // Инициализация атомарной ссылки с начальным пользователем
        AtomicReference<User> atomicUser = new AtomicReference<>(new User("Alice", 30));
        log.info("Инициализирован AtomicReference: {}", atomicUser.get());

        // 1. Демонстрация атомарного обновления через updateAndGet
        atomicUser.updateAndGet(User::withIncrementedAge);
        log.info("После единичного обновления: {}", atomicUser.get());

        // 2. Демонстрация конкурентного обновления в нескольких потоках
        Runnable updateTask = () -> {
            for (int i = 0; i < 2; i++) {
                User currentUser;
                User newUser;
                int attempts = 0;

                do {
                    attempts++;
                    currentUser = atomicUser.get();
                    newUser = currentUser.withIncrementedAge();
                    log.debug("Попытка {}: пробуем обновить {} -> {}",
                            attempts, currentUser, newUser);
                } while (!atomicUser.compareAndSet(currentUser, newUser));

                log.trace("Успешное обновление после {} попыток: {} -> {}",
                        attempts, currentUser, newUser);
            }
            log.info("Поток {} завершил все обновления", Thread.currentThread().getName());
        };

        Thread thread1 = new Thread(updateTask, "IncrementThread-1");
        Thread thread2 = new Thread(updateTask, "IncrementThread-2");

        log.info("Запуск потоков для конкурентного обновления...");
        thread1.start();
        thread2.start();

        // Ожидаем завершения потоков
        thread1.join();
        thread2.join();

        // Проверяем результат - должно быть 30 + 1 (первое обновление) + 100*2 = 231
        log.info("Финальное значение после 200 инкрементов: {}", atomicUser.get());
    }
}