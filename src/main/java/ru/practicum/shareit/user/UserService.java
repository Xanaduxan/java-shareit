package ru.practicum.shareit.user;

import ru.practicum.shareit.user.dto.UserDto;

import java.util.Collection;

public interface UserService {

    UserDto create(UserDto userDto);

    UserDto update(Long userId, UserDto userDto);

    UserDto getById(Long userId);

    Collection<UserDto> getAll();

    void delete(Long userId);
}