package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.common.ConflictException;
import ru.practicum.shareit.common.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private static final String USER_NOT_FOUND_LOG = "Пользователь с id={} не найден";
    private static final String EMAIL_CONFLICT_LOG = "Email уже используется: {}";
    private static final String CREATE_USER_LOG = "Создание пользователя: email={}";
    private static final String UPDATE_USER_LOG = "Обновление пользователя с id={}";
    private static final String DELETE_USER_LOG = "Удаление пользователя с id={}";

    @Override
    public UserDto create(UserDto userDto) {
        log.info(CREATE_USER_LOG, userDto.getEmail());


        checkEmailUnique(userDto.getEmail(), null);

        User user = UserMapper.toUser(userDto);
        User createdUser = userRepository.create(user);

        return UserMapper.toUserDto(createdUser);
    }

    @Override
    public UserDto update(Long userId, UserDto userDto) {
        log.info(UPDATE_USER_LOG, userId);

        User user = getUserOrThrow(userId);

        if (userDto.getName() != null && !userDto.getName()
                                                 .isBlank()) {
            user.setName(userDto.getName());
        }

        if (userDto.getEmail() != null && !userDto.getEmail()
                                                  .isBlank()) {
            validateEmail(userDto.getEmail());
            checkEmailUnique(userDto.getEmail(), userId);
            user.setEmail(userDto.getEmail());
        }

        User updatedUser = userRepository.update(user);
        return UserMapper.toUserDto(updatedUser);
    }

    @Override
    public UserDto getById(Long userId) {
        log.info("Получение пользователя с id={}", userId);
        return UserMapper.toUserDto(getUserOrThrow(userId));
    }

    @Override
    public Collection<UserDto> getAll() {
        log.info("Получение всех пользователей");
        return userRepository.getAll()
                             .stream()
                             .map(UserMapper::toUserDto)
                             .collect(Collectors.toList());
    }

    @Override
    public void delete(Long userId) {
        log.info(DELETE_USER_LOG, userId);

        getUserOrThrow(userId);
        userRepository.delete(userId);
    }

    private User getUserOrThrow(Long userId) {
        User user = userRepository.getById(userId);
        if (user == null) {
            log.warn(USER_NOT_FOUND_LOG, userId);
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        return user;
    }


    private void checkEmailUnique(String email, Long currentUserId) {
        for (User user : userRepository.getAll()) {
            if (user.getEmail() == null || !user.getEmail()
                                                .equals(email)) {
                continue;
            }

            log.warn(EMAIL_CONFLICT_LOG, email);

            if (currentUserId == null || !Objects.equals(user.getId(), currentUserId)) {
                throw new ConflictException("Email уже используется");
            }
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("Некорректный email");
        }
    }
}