package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.common.ForbiddenException;
import ru.practicum.shareit.common.NotFoundException;

import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    void create_shouldCreateBooking_whenDataIsValid() {
        User owner = makeUser(1L);
        User booker = makeUser(2L);
        Item item = makeItem(10L, "Drill", true, owner);

        BookingDto dto = makeBookingDto(
                10L,
                LocalDateTime.now()
                             .plusHours(1),
                LocalDateTime.now()
                             .plusHours(2)
        );

        Booking savedBooking = makeBooking(100L, dto.getStart(), dto.getEnd(), item, booker, BookingStatus.WAITING);

        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingDto result = bookingService.create(2L, dto);

        assertEquals(100L, result.getId());
        assertEquals(BookingStatus.WAITING, result.getStatus());
        assertEquals(10L, result.getItem()
                                .getId());
        assertEquals(2L, result.getBooker()
                               .getId());

        verify(userRepository).findById(2L);
        verify(itemRepository).findById(10L);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void create_shouldThrowNotFoundException_whenUserDoesNotExist() {
        BookingDto dto = makeBookingDto(
                10L,
                LocalDateTime.now()
                             .plusHours(1),
                LocalDateTime.now()
                             .plusHours(2)
        );

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.create(99L, dto));

        verify(userRepository).findById(99L);
        verify(itemRepository, never()).findById(anyLong());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowNotFoundException_whenItemDoesNotExist() {
        User booker = makeUser(2L);

        BookingDto dto = makeBookingDto(
                99L,
                LocalDateTime.now()
                             .plusHours(1),
                LocalDateTime.now()
                             .plusHours(2)
        );

        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.create(2L, dto));

        verify(userRepository).findById(2L);
        verify(itemRepository).findById(99L);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowNotFoundException_whenOwnerTriesToBookOwnItem() {
        User owner = makeUser(1L);
        Item item = makeItem(10L, "Drill", true, owner);

        BookingDto dto = makeBookingDto(
                10L,
                LocalDateTime.now()
                             .plusHours(1),
                LocalDateTime.now()
                             .plusHours(2)
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(NotFoundException.class, () -> bookingService.create(1L, dto));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowIllegalArgumentException_whenItemIsNotAvailable() {
        User owner = makeUser(1L);
        User booker = makeUser(2L);
        Item item = makeItem(10L, "Drill", false, owner);

        BookingDto dto = makeBookingDto(
                10L,
                LocalDateTime.now()
                             .plusHours(1),
                LocalDateTime.now()
                             .plusHours(2)
        );

        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(2L, dto));

        verify(bookingRepository, never()).save(any());
    }


    @Test
    void approve_shouldApproveBooking_whenUserIsOwner() {
        User owner = makeUser(1L);
        User booker = makeUser(2L);
        Item item = makeItem(10L, "Drill", true, owner);

        Booking booking = makeBooking(
                100L,
                LocalDateTime.now()
                             .plusHours(1),
                LocalDateTime.now()
                             .plusHours(2),
                item,
                booker,
                BookingStatus.WAITING
        );

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        BookingDto result = bookingService.approve(1L, 100L, true);

        assertEquals(BookingStatus.APPROVED, result.getStatus());

        verify(bookingRepository).findById(100L);
        verify(bookingRepository).save(booking);
    }

    @Test
    void approve_shouldRejectBooking_whenUserIsOwnerAndApprovedFalse() {
        User owner = makeUser(1L);
        User booker = makeUser(2L);
        Item item = makeItem(10L, "Drill", true, owner);

        Booking booking = makeBooking(
                100L,
                LocalDateTime.now()
                             .plusHours(1),
                LocalDateTime.now()
                             .plusHours(2),
                item,
                booker,
                BookingStatus.WAITING
        );

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        BookingDto result = bookingService.approve(1L, 100L, false);

        assertEquals(BookingStatus.REJECTED, result.getStatus());
    }

    @Test
    void approve_shouldThrowForbiddenException_whenUserIsNotOwner() {
        User owner = makeUser(1L);
        User booker = makeUser(2L);
        User anotherUser = makeUser(3L);
        Item item = makeItem(10L, "Drill", true, owner);

        Booking booking = makeBooking(
                100L,
                LocalDateTime.now()
                             .plusHours(1),
                LocalDateTime.now()
                             .plusHours(2),
                item,
                booker,
                BookingStatus.WAITING
        );

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThrows(ForbiddenException.class, () -> bookingService.approve(anotherUser.getId(), 100L, true));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void approve_shouldThrowIllegalArgumentException_whenBookingIsNotWaiting() {
        User owner = makeUser(1L);
        User booker = makeUser(2L);
        Item item = makeItem(10L, "Drill", true, owner);

        Booking booking = makeBooking(
                100L,
                LocalDateTime.now()
                             .plusHours(1),
                LocalDateTime.now()
                             .plusHours(2),
                item,
                booker,
                BookingStatus.APPROVED
        );

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalArgumentException.class, () -> bookingService.approve(1L, 100L, true));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void getById_shouldReturnBooking_whenUserIsBooker() {
        User owner = makeUser(1L);
        User booker = makeUser(2L);
        Item item = makeItem(10L, "Drill", true, owner);

        Booking booking = makeBooking(
                100L,
                LocalDateTime.now()
                             .plusHours(1),
                LocalDateTime.now()
                             .plusHours(2),
                item,
                booker,
                BookingStatus.WAITING
        );

        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        BookingDto result = bookingService.getById(2L, 100L);

        assertEquals(100L, result.getId());
        assertEquals(2L, result.getBooker()
                               .getId());
    }

    @Test
    void getById_shouldReturnBooking_whenUserIsOwner() {
        User owner = makeUser(1L);
        User booker = makeUser(2L);
        Item item = makeItem(10L, "Drill", true, owner);

        Booking booking = makeBooking(
                100L,
                LocalDateTime.now()
                             .plusHours(1),
                LocalDateTime.now()
                             .plusHours(2),
                item,
                booker,
                BookingStatus.WAITING
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        BookingDto result = bookingService.getById(1L, 100L);

        assertEquals(100L, result.getId());
    }

    @Test
    void getById_shouldThrowNotFoundException_whenUserHasNoAccess() {
        User owner = makeUser(1L);
        User booker = makeUser(2L);
        User anotherUser = makeUser(3L);
        Item item = makeItem(10L, "Drill", true, owner);

        Booking booking = makeBooking(
                100L,
                LocalDateTime.now()
                             .plusHours(1),
                LocalDateTime.now()
                             .plusHours(2),
                item,
                booker,
                BookingStatus.WAITING
        );

        when(userRepository.findById(3L)).thenReturn(Optional.of(anotherUser));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThrows(NotFoundException.class, () -> bookingService.getById(3L, 100L));
    }

    @Test
    void getUserBookings_shouldReturnAllBookings() {
        User booker = makeUser(2L);
        User owner = makeUser(1L);
        Item item = makeItem(10L, "Drill", true, owner);
        Booking booking = makeBooking(100L, LocalDateTime.now()
                                                         .plusHours(1), LocalDateTime.now()
                                                                                     .plusHours(2), item, booker,
                BookingStatus.WAITING);

        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findByBooker_Id(eq(2L), any())).thenReturn(List.of(booking));

        assertEquals(1, bookingService.getUserBookings(2L, BookingState.ALL)
                                      .size());

        verify(bookingRepository).findByBooker_Id(eq(2L), any());
    }

    @Test
    void getUserBookings_shouldReturnWaitingBookings() {
        User booker = makeUser(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findByBooker_IdAndStatus(eq(2L), eq(BookingStatus.WAITING), any()))
                .thenReturn(List.of());

        assertTrue(bookingService.getUserBookings(2L, BookingState.WAITING)
                                 .isEmpty());

        verify(bookingRepository).findByBooker_IdAndStatus(eq(2L), eq(BookingStatus.WAITING), any());
    }

    @Test
    void getOwnerBookings_shouldReturnAllBookings() {
        User owner = makeUser(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.findByItem_Owner_Id(eq(1L), any())).thenReturn(List.of());

        assertTrue(bookingService.getOwnerBookings(1L, BookingState.ALL)
                                 .isEmpty());

        verify(bookingRepository).findByItem_Owner_Id(eq(1L), any());
    }

    @Test
    void getOwnerBookings_shouldReturnRejectedBookings() {
        User owner = makeUser(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.findByItem_Owner_IdAndStatus(eq(1L), eq(BookingStatus.REJECTED), any()))
                .thenReturn(List.of());

        assertTrue(bookingService.getOwnerBookings(1L, BookingState.REJECTED)
                                 .isEmpty());

        verify(bookingRepository).findByItem_Owner_IdAndStatus(eq(1L), eq(BookingStatus.REJECTED), any());
    }

    private User makeUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("User " + id);
        user.setEmail("user" + id + "@mail.com");
        return user;
    }

    private Item makeItem(Long id, String name, Boolean available, User owner) {
        Item item = new Item();
        item.setId(id);
        item.setName(name);
        item.setDescription("Description");
        item.setAvailable(available);
        item.setOwner(owner);
        return item;
    }

    private BookingDto makeBookingDto(Long itemId, LocalDateTime start, LocalDateTime end) {
        BookingDto dto = new BookingDto();
        dto.setItemId(itemId);
        dto.setStart(start);
        dto.setEnd(end);
        return dto;
    }

    private Booking makeBooking(Long id,
                                LocalDateTime start,
                                LocalDateTime end,
                                Item item,
                                User booker,
                                BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setStart(start);
        booking.setEnd(end);
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(status);
        return booking;
    }
}