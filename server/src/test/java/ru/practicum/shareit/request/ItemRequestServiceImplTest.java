package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.common.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemRequestServiceImplTest {

    @Mock
    private ItemRequestRepository itemRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemRequestServiceImpl itemRequestService;

    @Test
    void create_shouldCreateRequest_whenUserExists() {
        User user = makeUser(1L, "User", "user@mail.com");

        ItemRequestDto inputDto = new ItemRequestDto();
        inputDto.setDescription("Need drill");

        ItemRequest savedRequest = makeRequest(10L, "Need drill", user, LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(itemRequestRepository.save(any(ItemRequest.class))).thenReturn(savedRequest);
        when(itemRepository.findByRequestId(10L)).thenReturn(List.of());

        ItemRequestDto result = itemRequestService.create(1L, inputDto);

        assertEquals(10L, result.getId());
        assertEquals("Need drill", result.getDescription());
        assertNotNull(result.getCreated());
        assertTrue(result.getItems()
                         .isEmpty());

        verify(userRepository).findById(1L);
        verify(itemRequestRepository).save(any(ItemRequest.class));
        verify(itemRepository).findByRequestId(10L);
    }

    @Test
    void create_shouldThrowNotFoundException_whenUserDoesNotExist() {
        ItemRequestDto inputDto = new ItemRequestDto();
        inputDto.setDescription("Need drill");

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemRequestService.create(99L, inputDto));

        verify(userRepository).findById(99L);
        verify(itemRequestRepository, never()).save(any(ItemRequest.class));
        verify(itemRepository, never()).findByRequestId(anyLong());
    }

    @Test
    void getById_shouldReturnRequestWithItems_whenUserAndRequestExist() {
        User requestor = makeUser(1L, "Requestor", "requestor@mail.com");
        User owner = makeUser(2L, "Owner", "owner@mail.com");

        ItemRequest request = makeRequest(10L, "Need book", requestor, LocalDateTime.now());
        Item item = makeItem(100L, "Book", owner, 10L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(itemRequestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(itemRepository.findByRequestId(10L)).thenReturn(List.of(item));

        ItemRequestDto result = itemRequestService.getById(2L, 10L);

        assertEquals(10L, result.getId());
        assertEquals("Need book", result.getDescription());
        assertEquals(1, result.getItems()
                              .size());
        assertEquals(100L, result.getItems()
                                 .get(0)
                                 .getId());
        assertEquals("Book", result.getItems()
                                   .get(0)
                                   .getName());
        assertEquals(2L, result.getItems()
                               .get(0)
                               .getOwnerId());

        verify(userRepository).findById(2L);
        verify(itemRequestRepository).findById(10L);
        verify(itemRepository).findByRequestId(10L);
    }

    @Test
    void getById_shouldThrowNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemRequestService.getById(99L, 10L));

        verify(userRepository).findById(99L);
        verify(itemRequestRepository, never()).findById(anyLong());
        verify(itemRepository, never()).findByRequestId(anyLong());
    }

    @Test
    void getById_shouldThrowNotFoundException_whenRequestDoesNotExist() {
        User user = makeUser(1L, "User", "user@mail.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(itemRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemRequestService.getById(1L, 99L));

        verify(userRepository).findById(1L);
        verify(itemRequestRepository).findById(99L);
        verify(itemRepository, never()).findByRequestId(anyLong());
    }

    @Test
    void getMyRequests_shouldReturnUserRequests() {
        User user = makeUser(1L, "User", "user@mail.com");

        ItemRequest firstRequest = makeRequest(10L, "Need drill", user, LocalDateTime.now());
        ItemRequest secondRequest = makeRequest(11L, "Need book", user, LocalDateTime.now()
                                                                                     .minusDays(1));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(itemRequestRepository.findByRequestor_IdOrderByCreatedDesc(1L))
                .thenReturn(List.of(firstRequest, secondRequest));
        when(itemRepository.findByRequestIdIn(List.of(10L, 11L))).thenReturn(List.of());

        Collection<ItemRequestDto> result = itemRequestService.getMyRequests(1L);

        assertEquals(2, result.size());

        verify(userRepository).findById(1L);
        verify(itemRequestRepository).findByRequestor_IdOrderByCreatedDesc(1L);
        verify(itemRepository).findByRequestIdIn(List.of(10L, 11L));
        verify(itemRepository, never()).findByRequestId(10L);
        verify(itemRepository, never()).findByRequestId(11L);
    }

    @Test
    void getMyRequests_shouldThrowNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemRequestService.getMyRequests(99L));

        verify(userRepository).findById(99L);
        verify(itemRequestRepository, never()).findByRequestor_IdOrderByCreatedDesc(anyLong());
    }

    @Test
    void getAllRequests_shouldReturnOtherUsersRequests() {
        User currentUser = makeUser(1L, "Current", "current@mail.com");
        User otherUser = makeUser(2L, "Other", "other@mail.com");

        ItemRequest request = makeRequest(10L, "Need saw", otherUser, LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        when(itemRequestRepository.findByRequestor_IdNotOrderByCreatedDesc(1L))
                .thenReturn(List.of(request));
        when(itemRepository.findByRequestIdIn(List.of(10L))).thenReturn(List.of());

        Collection<ItemRequestDto> result = itemRequestService.getAllRequests(1L);

        assertEquals(1, result.size());

        verify(userRepository).findById(1L);
        verify(itemRequestRepository).findByRequestor_IdNotOrderByCreatedDesc(1L);
        verify(itemRepository).findByRequestIdIn(List.of(10L));
        verify(itemRepository, never()).findByRequestId(10L);
    }

    @Test
    void getAllRequests_shouldThrowNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemRequestService.getAllRequests(99L));

        verify(userRepository).findById(99L);
        verify(itemRequestRepository, never()).findByRequestor_IdNotOrderByCreatedDesc(anyLong());
    }

    private User makeUser(Long id, String name, String email) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        return user;
    }

    private ItemRequest makeRequest(Long id, String description, User requestor, LocalDateTime created) {
        ItemRequest request = new ItemRequest();
        request.setId(id);
        request.setDescription(description);
        request.setRequestor(requestor);
        request.setCreated(created);
        return request;
    }

    private Item makeItem(Long id, String name, User owner, Long requestId) {
        Item item = new Item();
        item.setId(id);
        item.setName(name);
        item.setOwner(owner);
        item.setRequestId(requestId);
        return item;
    }
}