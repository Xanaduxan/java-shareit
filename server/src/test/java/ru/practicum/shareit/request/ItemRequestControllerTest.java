package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.common.NotFoundException;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemRequestController.class)
class ItemRequestControllerTest {

    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ItemRequestService itemRequestService;

    @Test
    void create_shouldReturnCreatedRequest() throws Exception {
        ItemRequestDto request = makeRequestDto(null, "Нужна дрель", null, List.of());
        ItemRequestDto response = makeRequestDto(1L, "Нужна дрель", LocalDateTime.now(), List.of());

        Mockito.when(itemRequestService.create(eq(1L), any(ItemRequestDto.class)))
               .thenReturn(response);

        mvc.perform(post("/requests")
                   .header(USER_ID_HEADER, 1L)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(request)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(1))
           .andExpect(jsonPath("$.description").value("Нужна дрель"))
           .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void create_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        ItemRequestDto request = makeRequestDto(null, "Нужна дрель", null, List.of());

        Mockito.when(itemRequestService.create(eq(99L), any(ItemRequestDto.class)))
               .thenThrow(new NotFoundException("Пользователь с id=99 не найден"));

        mvc.perform(post("/requests")
                   .header(USER_ID_HEADER, 99L)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(request)))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.error").value("Пользователь с id=99 не найден"));
    }

    @Test
    void getById_shouldReturnRequest() throws Exception {
        ItemRequestDto.ItemShortDto item = makeItemShortDto(10L, "Дрель", 2L);
        ItemRequestDto response = makeRequestDto(
                1L,
                "Нужна дрель",
                LocalDateTime.now(),
                List.of(item)
        );

        Mockito.when(itemRequestService.getById(1L, 1L))
               .thenReturn(response);

        mvc.perform(get("/requests/1")
                   .header(USER_ID_HEADER, 1L))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(1))
           .andExpect(jsonPath("$.description").value("Нужна дрель"))
           .andExpect(jsonPath("$.items[0].id").value(10))
           .andExpect(jsonPath("$.items[0].name").value("Дрель"))
           .andExpect(jsonPath("$.items[0].ownerId").value(2));
    }

    @Test
    void getById_shouldReturnNotFound_whenRequestDoesNotExist() throws Exception {
        Mockito.when(itemRequestService.getById(1L, 99L))
               .thenThrow(new NotFoundException("Запрос с id=99 не найден"));

        mvc.perform(get("/requests/99")
                   .header(USER_ID_HEADER, 1L))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.error").value("Запрос с id=99 не найден"));
    }

    @Test
    void getMyRequests_shouldReturnRequests() throws Exception {
        ItemRequestDto firstRequest = makeRequestDto(
                1L,
                "Нужна дрель",
                LocalDateTime.now(),
                List.of(makeItemShortDto(10L, "Дрель", 2L))
        );

        ItemRequestDto secondRequest = makeRequestDto(
                2L,
                "Нужна книга",
                LocalDateTime.now()
                             .minusDays(1),
                List.of()
        );

        Mockito.when(itemRequestService.getMyRequests(1L))
               .thenReturn(List.of(firstRequest, secondRequest));

        mvc.perform(get("/requests")
                   .header(USER_ID_HEADER, 1L))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].id").value(1))
           .andExpect(jsonPath("$[0].description").value("Нужна дрель"))
           .andExpect(jsonPath("$[0].items[0].name").value("Дрель"))
           .andExpect(jsonPath("$[1].id").value(2))
           .andExpect(jsonPath("$[1].description").value("Нужна книга"));
    }

    @Test
    void getMyRequests_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        Mockito.when(itemRequestService.getMyRequests(99L))
               .thenThrow(new NotFoundException("Пользователь с id=99 не найден"));

        mvc.perform(get("/requests")
                   .header(USER_ID_HEADER, 99L))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.error").value("Пользователь с id=99 не найден"));
    }

    @Test
    void getAllRequests_shouldReturnOtherUsersRequests() throws Exception {
        ItemRequestDto request = makeRequestDto(
                1L,
                "Нужен велосипед",
                LocalDateTime.now(),
                List.of(makeItemShortDto(20L, "Велосипед", 3L))
        );

        Mockito.when(itemRequestService.getAllRequests(1L))
               .thenReturn(List.of(request));

        mvc.perform(get("/requests/all")
                   .header(USER_ID_HEADER, 1L))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].id").value(1))
           .andExpect(jsonPath("$[0].description").value("Нужен велосипед"))
           .andExpect(jsonPath("$[0].items[0].name").value("Велосипед"))
           .andExpect(jsonPath("$[0].items[0].ownerId").value(3));
    }

    @Test
    void getAllRequests_shouldReturnEmptyList() throws Exception {
        Mockito.when(itemRequestService.getAllRequests(1L))
               .thenReturn(List.of());

        mvc.perform(get("/requests/all")
                   .header(USER_ID_HEADER, 1L))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$").isArray())
           .andExpect(jsonPath("$").isEmpty());
    }

    private ItemRequestDto makeRequestDto(Long id,
                                          String description,
                                          LocalDateTime created,
                                          List<ItemRequestDto.ItemShortDto> items) {
        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(id);
        dto.setDescription(description);
        dto.setCreated(created);
        dto.setItems(items);
        return dto;
    }

    private ItemRequestDto.ItemShortDto makeItemShortDto(Long id, String name, Long ownerId) {
        ItemRequestDto.ItemShortDto dto = new ItemRequestDto.ItemShortDto();
        dto.setId(id);
        dto.setName(name);
        dto.setOwnerId(ownerId);
        return dto;
    }
}