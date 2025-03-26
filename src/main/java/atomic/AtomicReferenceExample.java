package atomic;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Класс пользователя с именем и возрастом
 */
@Getter
@RequiredArgsConstructor
class User {
    @NonNull
    private String name;
    @NonNull
    private int age;

    // Переопределение toString для красивого вывода
    @Override
    public String toString() {
        return "User{name='" + name + "', age=" + age + "}";
    }
}

/**
 * Демонстрация работы AtomicReference для потокобезопасного
 * обновления объектов
 */
public class AtomicReferenceExample {
    public static void main(String[] args) {
        // Создаем атомарную ссылку на объект User
        AtomicReference<User> atomicUser = new AtomicReference<>(new User("Alice", 30)  // Начальное значение
        );

        // Атомарное обновление объекта через лямбда-функцию
        // updateAndGet принимает функцию, которая создает новый объект на основе текущего
        atomicUser.updateAndGet(user -> new User(user.getName(), user.getAge() + 1)  // Создаем нового пользователя
        );

        // Выводим обновленный объект
        // Обратите внимание на вложенность в имени из-за вызова toString()
        System.out.println(atomicUser.get()); //User{name='Alice', age=31}

        // Задача для потокобезопасного обновления возраста пользователя
        /**
         * !!!CAS в многопоточной среде!!!
         * Как это работает?
         * Оба потока читают oldValue (например, 0).
         * Один поток успешно делает CAS (0 → 1), другой — нет.
         * Второй поток повторяет операцию, теперь oldValue = 1, и CAS (1 → 2) успешен.
         */
        Runnable updateTask = () -> {
            User currentUser;
            User newUser;
            do {
                // Получаем текущее значение
                currentUser = atomicUser.get();
                // Создаем нового пользователя с увеличенным возрастом
                newUser = new User(currentUser.getName(), currentUser.getAge() + 1);
                // Пытаемся атомарно установить новое значение
                // compareAndSet вернет true только если текущее значение не изменилось
                // с момента последнего чтения (currentUser)
            } while (!atomicUser.compareAndSet(currentUser, newUser));
        };

        // Запускаем два потока, которые будут конкурентно обновлять возраст
        new Thread(updateTask).start();
        new Thread(updateTask).start();

        // Даем потокам время на выполнение
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Выводим финальное значение
        System.out.println("Final value: " + atomicUser.get());
    }
}
