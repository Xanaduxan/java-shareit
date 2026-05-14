package ru.practicum.shareit.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.common.ConflictException;
import ru.practicum.shareit.common.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @Test
    void create_shouldReturnCreatedUser() throws Exception {
        UserDto request = makeUserDto(null, "User", "user@mail.com");
        UserDto response = makeUserDto(1L, "User", "user@mail.com");

        Mockito.when(userService.create(any(UserDto.class)))
               .thenReturn(response);

        mvc.perform(post("/users")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(request)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(1))
           .andExpect(jsonPath("$.name").value("User"))
           .andExpect(jsonPath("$.email").value("user@mail.com"));
    }

    @Test
    void create_shouldCallService_whenEmailIsInvalidOnServer() throws Exception {
        UserDto request = makeUserDto(null, "User", "bad-email");
        UserDto response = makeUserDto(1L, "User", "bad-email");

        Mockito.when(userService.create(any(UserDto.class)))
               .thenReturn(response);

        mvc.perform(post("/users")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(request)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(1))
           .andExpect(jsonPath("$.email").value("bad-email"));
    }

    @Test
    void create_shouldReturnConflict_whenEmailAlreadyExists() throws Exception {
        UserDto request = makeUserDto(null, "User", "user@mail.com");

        Mockito.when(userService.create(any(UserDto.class)))
               .thenThrow(new ConflictException("Email уже используется"));

        mvc.perform(post("/users")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(request)))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.error").value("Email уже используется"));
    }

    @Test
    void update_shouldReturnUpdatedUser() throws Exception {
        UserDto request = makeUserDto(null, "New name", "new@mail.com");
        UserDto response = makeUserDto(1L, "New name", "new@mail.com");

        Mockito.when(userService.update(eq(1L), any(UserDto.class)))
               .thenReturn(response);

        mvc.perform(patch("/users/1")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(request)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(1))
           .andExpect(jsonPath("$.name").value("New name"))
           .andExpect(jsonPath("$.email").value("new@mail.com"));
    }

    @Test
    void getById_shouldReturnUser() throws Exception {
        UserDto response = makeUserDto(1L, "User", "user@mail.com");

        Mockito.when(userService.getById(1L))
               .thenReturn(response);

        mvc.perform(get("/users/1"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(1))
           .andExpect(jsonPath("$.name").value("User"))
           .andExpect(jsonPath("$.email").value("user@mail.com"));
    }

    @Test
    void getById_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        Mockito.when(userService.getById(99L))
               .thenThrow(new NotFoundException("Пользователь с id=99 не найден"));

        mvc.perform(get("/users/99"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.error").value("Пользователь с id=99 не найден"));
    }

    @Test
    void getAll_shouldReturnUsers() throws Exception {
        Mockito.when(userService.getAll())
               .thenReturn(List.of(
                       makeUserDto(1L, "First", "first@mail.com"),
                       makeUserDto(2L, "Second", "second@mail.com")
               ));

        mvc.perform(get("/users"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].id").value(1))
           .andExpect(jsonPath("$[0].name").value("First"))
           .andExpect(jsonPath("$[1].id").value(2))
           .andExpect(jsonPath("$[1].name").value("Second"));
    }

    @Test
    void delete_shouldReturnOk() throws Exception {
        mvc.perform(delete("/users/1"))
           .andExpect(status().isOk());

        Mockito.verify(userService)
               .delete(1L);
    }

    private UserDto makeUserDto(Long id, String name, String email) {
        UserDto dto = new UserDto();
        dto.setId(id);
        dto.setName(name);
        dto.setEmail(email);
        return dto;
    }
}