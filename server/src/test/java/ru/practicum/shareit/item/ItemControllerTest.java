package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.comment.dto.CommentDto;
import ru.practicum.shareit.common.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ItemService itemService;

    @Test
    void create_shouldReturnCreatedItem() throws Exception {
        ItemDto request = makeItemDto(null, "Drill", "Powerful drill", true);
        ItemDto response = makeItemDto(1L, "Drill", "Powerful drill", true);

        Mockito.when(itemService.create(eq(1L), any(ItemDto.class)))
               .thenReturn(response);

        mvc.perform(post("/items")
                   .header(USER_ID_HEADER, 1L)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(request)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(1))
           .andExpect(jsonPath("$.name").value("Drill"))
           .andExpect(jsonPath("$.description").value("Powerful drill"))
           .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void update_shouldReturnUpdatedItem() throws Exception {
        ItemDto request = makeItemDto(null, "Updated drill", "Updated description", true);
        ItemDto response = makeItemDto(1L, "Updated drill", "Updated description", true);

        Mockito.when(itemService.update(eq(1L), eq(1L), any(ItemDto.class)))
               .thenReturn(response);

        mvc.perform(patch("/items/1")
                   .header(USER_ID_HEADER, 1L)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(request)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(1))
           .andExpect(jsonPath("$.name").value("Updated drill"))
           .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    void getById_shouldReturnItem() throws Exception {
        ItemDto response = makeItemDto(1L, "Drill", "Powerful drill", true);

        Mockito.when(itemService.getById(1L, 1L))
               .thenReturn(response);

        mvc.perform(get("/items/1")
                   .header(USER_ID_HEADER, 1L))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(1))
           .andExpect(jsonPath("$.name").value("Drill"))
           .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void getById_shouldReturnNotFound_whenItemDoesNotExist() throws Exception {
        Mockito.when(itemService.getById(1L, 99L))
               .thenThrow(new NotFoundException("Вещь с id=99 не найдена"));

        mvc.perform(get("/items/99")
                   .header(USER_ID_HEADER, 1L))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.error").value("Вещь с id=99 не найдена"));
    }

    @Test
    void getAllByOwner_shouldReturnItems() throws Exception {
        Mockito.when(itemService.getAllByOwner(1L))
               .thenReturn(List.of(
                       makeItemDto(1L, "Drill", "Powerful drill", true),
                       makeItemDto(2L, "Book", "Interesting book", true)
               ));

        mvc.perform(get("/items")
                   .header(USER_ID_HEADER, 1L))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].id").value(1))
           .andExpect(jsonPath("$[0].name").value("Drill"))
           .andExpect(jsonPath("$[1].id").value(2))
           .andExpect(jsonPath("$[1].name").value("Book"));
    }

    @Test
    void search_shouldReturnItems() throws Exception {
        Mockito.when(itemService.search("drill"))
               .thenReturn(List.of(makeItemDto(1L, "Drill", "Powerful drill", true)));

        mvc.perform(get("/items/search")
                   .param("text", "drill"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].id").value(1))
           .andExpect(jsonPath("$[0].name").value("Drill"));
    }

    @Test
    void search_shouldReturnEmptyList_whenTextIsBlank() throws Exception {
        Mockito.when(itemService.search(""))
               .thenReturn(List.of());

        mvc.perform(get("/items/search")
                   .param("text", ""))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$").isArray())
           .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void addComment_shouldReturnComment() throws Exception {
        CommentDto request = makeCommentDto(null, "Good item", null, null);
        CommentDto response = makeCommentDto(1L, "Good item", "User", LocalDateTime.now());

        Mockito.when(itemService.addComment(eq(2L), eq(1L), any(CommentDto.class)))
               .thenReturn(response);

        mvc.perform(post("/items/1/comment")
                   .header(USER_ID_HEADER, 2L)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(request)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(1))
           .andExpect(jsonPath("$.text").value("Good item"))
           .andExpect(jsonPath("$.authorName").value("User"));
    }

    @Test
    void addComment_shouldReturnBadRequest_whenServiceThrowsIllegalArgumentException() throws Exception {
        CommentDto request = makeCommentDto(null, "Good item", null, null);

        Mockito.when(itemService.addComment(eq(2L), eq(1L), any(CommentDto.class)))
               .thenThrow(new IllegalArgumentException("Нельзя оставить комментарий"));

        mvc.perform(post("/items/1/comment")
                   .header(USER_ID_HEADER, 2L)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(request)))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error").value("Нельзя оставить комментарий"));
    }

    private ItemDto makeItemDto(Long id, String name, String description, Boolean available) {
        ItemDto dto = new ItemDto();
        dto.setId(id);
        dto.setName(name);
        dto.setDescription(description);
        dto.setAvailable(available);
        dto.setComments(List.of());
        return dto;
    }

    private CommentDto makeCommentDto(Long id, String text, String authorName, LocalDateTime created) {
        CommentDto dto = new CommentDto();
        dto.setId(id);
        dto.setText(text);
        dto.setAuthorName(authorName);
        dto.setCreated(created);
        return dto;
    }
}