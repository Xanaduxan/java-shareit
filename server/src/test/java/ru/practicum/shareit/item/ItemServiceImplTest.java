package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.comment.CommentRepository;
import ru.practicum.shareit.comment.dto.CommentDto;
import ru.practicum.shareit.comment.model.Comment;
import ru.practicum.shareit.common.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ItemRequestRepository itemRequestRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    @Test
    void create_shouldCreateItem_whenUserExistsAndRequestIdIsNull() {
        User owner = makeUser(1L, "Владелец");
        ItemDto inputDto = makeItemDto(null, "Дрель", "Мощная дрель", true, null);
        Item savedItem = makeItem(10L, "Дрель", "Мощная дрель", true, owner, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.save(any(Item.class))).thenReturn(savedItem);

        ItemDto result = itemService.create(1L, inputDto);

        assertEquals(10L, result.getId());
        assertEquals("Дрель", result.getName());
        assertEquals("Мощная дрель", result.getDescription());
        assertTrue(result.getAvailable());
        assertNull(result.getRequestId());

        verify(userRepository).findById(1L);
        verify(itemRequestRepository, never()).findById(anyLong());
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void create_shouldCreateItem_whenRequestExists() {
        User owner = makeUser(1L, "Владелец");
        ItemRequest request = new ItemRequest();
        request.setId(100L);

        ItemDto inputDto = makeItemDto(null, "Книга", "Интересная книга", true, 100L);
        Item savedItem = makeItem(10L, "Книга", "Интересная книга", true, owner, 100L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(itemRepository.save(any(Item.class))).thenReturn(savedItem);

        ItemDto result = itemService.create(1L, inputDto);

        assertEquals(10L, result.getId());
        assertEquals(100L, result.getRequestId());

        verify(userRepository).findById(1L);
        verify(itemRequestRepository).findById(100L);
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void create_shouldThrowNotFoundException_whenUserDoesNotExist() {
        ItemDto inputDto = makeItemDto(null, "Дрель", "Мощная дрель", true, null);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.create(99L, inputDto));

        verify(userRepository).findById(99L);
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void create_shouldThrowNotFoundException_whenRequestDoesNotExist() {
        User owner = makeUser(1L, "Владелец");
        ItemDto inputDto = makeItemDto(null, "Книга", "Интересная книга", true, 100L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRequestRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.create(1L, inputDto));

        verify(userRepository).findById(1L);
        verify(itemRequestRepository).findById(100L);
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void update_shouldUpdateAllFields_whenUserIsOwner() {
        User owner = makeUser(1L, "Владелец");
        Item item = makeItem(10L, "Старая дрель", "Старое описание", true, owner, null);
        ItemDto updateDto = makeItemDto(null, "Новая дрель", "Новое описание", false, null);
        Item savedItem = makeItem(10L, "Новая дрель", "Новое описание", false, owner, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(savedItem);

        ItemDto result = itemService.update(1L, 10L, updateDto);

        assertEquals("Новая дрель", result.getName());
        assertEquals("Новое описание", result.getDescription());
        assertFalse(result.getAvailable());

        verify(userRepository).findById(1L);
        verify(itemRepository).findById(10L);
        verify(itemRepository).save(item);
    }

    @Test
    void update_shouldThrowNotFoundException_whenUserIsNotOwner() {
        User owner = makeUser(1L, "Владелец");
        User anotherUser = makeUser(2L, "Другой пользователь");
        Item item = makeItem(10L, "Дрель", "Мощная дрель", true, owner, null);
        ItemDto updateDto = makeItemDto(null, "Новая дрель", null, null, null);

        when(userRepository.findById(2L)).thenReturn(Optional.of(anotherUser));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(NotFoundException.class, () -> itemService.update(2L, 10L, updateDto));

        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void update_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        User owner = makeUser(1L, "Владелец");
        Item item = makeItem(10L, "Дрель", "Мощная дрель", true, owner, null);
        ItemDto updateDto = makeItemDto(null, " ", null, null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(IllegalArgumentException.class, () -> itemService.update(1L, 10L, updateDto));

        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void update_shouldThrowIllegalArgumentException_whenDescriptionIsBlank() {
        User owner = makeUser(1L, "Владелец");
        Item item = makeItem(10L, "Дрель", "Мощная дрель", true, owner, null);
        ItemDto updateDto = makeItemDto(null, null, " ", null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(IllegalArgumentException.class, () -> itemService.update(1L, 10L, updateDto));

        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void search_shouldReturnItems_whenTextIsNotBlank() {
        User owner = makeUser(1L, "Владелец");
        Item item = makeItem(10L, "Дрель", "Мощная дрель", true, owner, null);

        when(itemRepository.search("дрель")).thenReturn(List.of(item));

        Collection<ItemDto> result = itemService.search("дрель");

        assertEquals(1, result.size());

        verify(itemRepository).search("дрель");
    }

    @Test
    void search_shouldReturnEmptyList_whenTextIsBlank() {
        Collection<ItemDto> result = itemService.search(" ");

        assertTrue(result.isEmpty());

        verify(itemRepository, never()).search(anyString());
    }

    @Test
    void addComment_shouldSaveComment_whenUserHasCompletedBooking() {
        User owner = makeUser(1L, "Владелец");
        User author = makeUser(2L, "Автор");
        Item item = makeItem(10L, "Дрель", "Мощная дрель", true, owner, null);
        CommentDto inputDto = makeCommentDto(null, "Хорошая вещь");
        Comment savedComment = makeComment(200L, "Хорошая вещь", item, author, LocalDateTime.now());

        when(userRepository.findById(2L)).thenReturn(Optional.of(author));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bookingRepository.existsByItem_IdAndBooker_IdAndStatusAndEndIsBefore(
                eq(10L),
                eq(2L),
                eq(BookingStatus.APPROVED),
                any(LocalDateTime.class)
        )).thenReturn(true);

        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        CommentDto result = itemService.addComment(2L, 10L, inputDto);

        assertEquals(200L, result.getId());
        assertEquals("Хорошая вещь", result.getText());
        assertEquals("Автор", result.getAuthorName());

        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void addComment_shouldThrowIllegalArgumentException_whenUserHasNoCompletedBooking() {
        User owner = makeUser(1L, "Владелец");
        User author = makeUser(2L, "Автор");
        Item item = makeItem(10L, "Дрель", "Мощная дрель", true, owner, null);
        CommentDto inputDto = makeCommentDto(null, "Хорошая вещь");

        when(userRepository.findById(2L)).thenReturn(Optional.of(author));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        when(bookingRepository.existsByItem_IdAndBooker_IdAndStatusAndEndIsBefore(
                eq(10L),
                eq(2L),
                eq(BookingStatus.APPROVED),
                any(LocalDateTime.class)
        )).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> itemService.addComment(2L, 10L, inputDto));

        verify(commentRepository, never()).save(any(Comment.class));
    }

    private User makeUser(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail("user" + id + "@mail.com");
        return user;
    }

    private ItemDto makeItemDto(Long id,
                                String name,
                                String description,
                                Boolean available,
                                Long requestId) {
        ItemDto dto = new ItemDto();
        dto.setId(id);
        dto.setName(name);
        dto.setDescription(description);
        dto.setAvailable(available);
        dto.setRequestId(requestId);
        return dto;
    }

    private Item makeItem(Long id,
                          String name,
                          String description,
                          Boolean available,
                          User owner,
                          Long requestId) {
        Item item = new Item();
        item.setId(id);
        item.setName(name);
        item.setDescription(description);
        item.setAvailable(available);
        item.setOwner(owner);
        item.setRequestId(requestId);
        return item;
    }

    private Booking makeBooking(Long id,
                                Item item,
                                User booker,
                                LocalDateTime start,
                                LocalDateTime end) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStart(start);
        booking.setEnd(end);
        booking.setStatus(BookingStatus.APPROVED);
        return booking;
    }

    private Comment makeComment(Long id,
                                String text,
                                Item item,
                                User author,
                                LocalDateTime created) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setText(text);
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setCreated(created);
        return comment;
    }

    private CommentDto makeCommentDto(Long id, String text) {
        CommentDto dto = new CommentDto();
        dto.setId(id);
        dto.setText(text);
        return dto;
    }
}