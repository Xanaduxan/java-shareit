package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.practicum.shareit.common.ConflictException;
import ru.practicum.shareit.common.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void create_shouldSaveUser_whenEmailIsUnique() {
        UserDto inputDto = makeUserDto(null, "User", "user@mail.com");
        User savedUser = makeUser(1L, "User", "user@mail.com");

        when(userRepository.findAll()).thenReturn(List.of());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserDto result = userService.create(inputDto);

        assertEquals(1L, result.getId());
        assertEquals("User", result.getName());
        assertEquals("user@mail.com", result.getEmail());

        verify(userRepository).findAll();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_shouldThrowConflictException_whenEmailAlreadyExists() {
        User existingUser = makeUser(1L, "Old", "user@mail.com");
        UserDto inputDto = makeUserDto(null, "New", "user@mail.com");

        when(userRepository.findAll()).thenReturn(List.of(existingUser));

        assertThrows(ConflictException.class, () -> userService.create(inputDto));

        verify(userRepository).findAll();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void create_shouldThrowConflictException_whenRepositoryThrowsDataIntegrityViolationException() {
        UserDto inputDto = makeUserDto(null, "User", "user@mail.com");

        when(userRepository.findAll()).thenReturn(List.of());
        when(userRepository.save(any(User.class))).thenThrow(DataIntegrityViolationException.class);

        assertThrows(ConflictException.class, () -> userService.create(inputDto));

        verify(userRepository).findAll();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void update_shouldUpdateNameAndEmail_whenUserExistsAndEmailIsUnique() {
        User existingUser = makeUser(1L, "Old name", "old@mail.com");
        UserDto updateDto = makeUserDto(null, "New name", "new@mail.com");
        User updatedUser = makeUser(1L, "New name", "new@mail.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.findAll()).thenReturn(List.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(updatedUser);

        UserDto result = userService.update(1L, updateDto);

        assertEquals(1L, result.getId());
        assertEquals("New name", result.getName());
        assertEquals("new@mail.com", result.getEmail());

        verify(userRepository).findById(1L);
        verify(userRepository).findAll();
        verify(userRepository).save(existingUser);
    }

    @Test
    void update_shouldUpdateOnlyName_whenEmailIsNull() {
        User existingUser = makeUser(1L, "Old name", "old@mail.com");
        UserDto updateDto = makeUserDto(null, "New name", null);
        User updatedUser = makeUser(1L, "New name", "old@mail.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(updatedUser);

        UserDto result = userService.update(1L, updateDto);

        assertEquals("New name", result.getName());
        assertEquals("old@mail.com", result.getEmail());

        verify(userRepository).findById(1L);
        verify(userRepository, never()).findAll();
        verify(userRepository).save(existingUser);
    }

    @Test
    void update_shouldThrowNotFoundException_whenUserDoesNotExist() {
        UserDto updateDto = makeUserDto(null, "New name", "new@mail.com");

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.update(99L, updateDto));

        verify(userRepository).findById(99L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void update_shouldThrowConflictException_whenEmailBelongsToAnotherUser() {
        User existingUser = makeUser(1L, "User", "old@mail.com");
        User anotherUser = makeUser(2L, "Another", "new@mail.com");
        UserDto updateDto = makeUserDto(null, null, "new@mail.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.findAll()).thenReturn(List.of(existingUser, anotherUser));

        assertThrows(ConflictException.class, () -> userService.update(1L, updateDto));

        verify(userRepository).findById(1L);
        verify(userRepository).findAll();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void update_shouldThrowIllegalArgumentException_whenEmailIsInvalid() {
        User existingUser = makeUser(1L, "User", "old@mail.com");
        UserDto updateDto = makeUserDto(null, null, "invalid-email");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        assertThrows(IllegalArgumentException.class, () -> userService.update(1L, updateDto));

        verify(userRepository).findById(1L);
        verify(userRepository, never()).findAll();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getById_shouldReturnUser_whenUserExists() {
        User user = makeUser(1L, "User", "user@mail.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDto result = userService.getById(1L);

        assertEquals(1L, result.getId());
        assertEquals("User", result.getName());
        assertEquals("user@mail.com", result.getEmail());

        verify(userRepository).findById(1L);
    }

    @Test
    void getById_shouldThrowNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getById(99L));

        verify(userRepository).findById(99L);
    }

    @Test
    void getAll_shouldReturnAllUsers() {
        User firstUser = makeUser(1L, "First", "first@mail.com");
        User secondUser = makeUser(2L, "Second", "second@mail.com");

        when(userRepository.findAll()).thenReturn(List.of(firstUser, secondUser));

        Collection<UserDto> result = userService.getAll();

        assertEquals(2, result.size());

        verify(userRepository).findAll();
    }

    @Test
    void delete_shouldDeleteUser_whenUserExists() {
        User user = makeUser(1L, "User", "user@mail.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository).findById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.delete(99L));

        verify(userRepository).findById(99L);
        verify(userRepository, never()).deleteById(anyLong());
    }

    private User makeUser(Long id, String name, String email) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        return user;
    }

    private UserDto makeUserDto(Long id, String name, String email) {
        UserDto dto = new UserDto();
        dto.setId(id);
        dto.setName(name);
        dto.setEmail(email);
        return dto;
    }
}