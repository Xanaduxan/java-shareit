package ru.practicum.shareit.user;

import java.util.Collection;

public interface UserRepository {

    User create(User user);

    User update(User user);

    User getById(Long id);

    Collection<User> getAll();

    void delete(Long id);
}