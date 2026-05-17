package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.common.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class BookingServiceImplIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void create_shouldCreateBookingInDatabase() {
        User owner = saveUser("Владелец", "owner1@mail.com");
        User booker = saveUser("Арендатор", "booker1@mail.com");
        Item item = saveItem("Дрель", "Мощная дрель", true, owner);

        BookingDto request = new BookingDto();
        request.setItemId(item.getId());
        request.setStart(LocalDateTime.now()
                                      .plusHours(1));
        request.setEnd(LocalDateTime.now()
                                    .plusHours(2));

        BookingDto result = bookingService.create(booker.getId(), request);

        assertNotNull(result.getId());
        assertEquals(BookingStatus.WAITING, result.getStatus());
        assertEquals(item.getId(), result.getItem()
                                         .getId());
        assertEquals(booker.getId(), result.getBooker()
                                           .getId());
    }

    @Test
    void approve_shouldApproveBookingInDatabase() {
        User owner = saveUser("Владелец", "owner2@mail.com");
        User booker = saveUser("Арендатор", "booker2@mail.com");
        Item item = saveItem("Книга", "Интересная книга", true, owner);

        BookingDto request = new BookingDto();
        request.setItemId(item.getId());
        request.setStart(LocalDateTime.now()
                                      .plusHours(1));
        request.setEnd(LocalDateTime.now()
                                    .plusHours(2));

        BookingDto created = bookingService.create(booker.getId(), request);

        BookingDto approved = bookingService.approve(owner.getId(), created.getId(), true);

        assertEquals(BookingStatus.APPROVED, approved.getStatus());
    }

    @Test
    void getById_shouldReturnBookingForOwner() {
        User owner = saveUser("Владелец", "owner3@mail.com");
        User booker = saveUser("Арендатор", "booker3@mail.com");
        Item item = saveItem("Велосипед", "Городской велосипед", true, owner);

        BookingDto request = new BookingDto();
        request.setItemId(item.getId());
        request.setStart(LocalDateTime.now()
                                      .plusHours(1));
        request.setEnd(LocalDateTime.now()
                                    .plusHours(2));

        BookingDto created = bookingService.create(booker.getId(), request);

        BookingDto result = bookingService.getById(owner.getId(), created.getId());

        assertEquals(created.getId(), result.getId());
        assertEquals(item.getId(), result.getItem()
                                         .getId());
    }

    @Test
    void getUserBookings_shouldReturnUserBookings() {
        User owner = saveUser("Владелец", "owner4@mail.com");
        User booker = saveUser("Арендатор", "booker4@mail.com");
        Item item = saveItem("Самокат", "Электросамокат", true, owner);

        BookingDto request = new BookingDto();
        request.setItemId(item.getId());
        request.setStart(LocalDateTime.now()
                                      .plusHours(1));
        request.setEnd(LocalDateTime.now()
                                    .plusHours(2));

        bookingService.create(booker.getId(), request);

        Collection<BookingDto> result = bookingService.getUserBookings(booker.getId(), BookingState.ALL);

        assertEquals(1, result.size());
    }

    @Test
    void getOwnerBookings_shouldReturnOwnerBookings() {
        User owner = saveUser("Владелец", "owner5@mail.com");
        User booker = saveUser("Арендатор", "booker5@mail.com");
        Item item = saveItem("Ноутбук", "Рабочий ноутбук", true, owner);

        BookingDto request = new BookingDto();
        request.setItemId(item.getId());
        request.setStart(LocalDateTime.now()
                                      .plusHours(1));
        request.setEnd(LocalDateTime.now()
                                    .plusHours(2));

        bookingService.create(booker.getId(), request);

        Collection<BookingDto> result = bookingService.getOwnerBookings(owner.getId(), BookingState.ALL);

        assertEquals(1, result.size());
    }

    @Test
    void create_shouldThrowNotFoundException_whenBookerDoesNotExist() {
        User owner = saveUser("Владелец", "owner6@mail.com");
        Item item = saveItem("Фотоаппарат", "Зеркальный фотоаппарат", true, owner);

        BookingDto request = new BookingDto();
        request.setItemId(item.getId());
        request.setStart(LocalDateTime.now()
                                      .plusHours(1));
        request.setEnd(LocalDateTime.now()
                                    .plusHours(2));

        assertThrows(NotFoundException.class, () -> bookingService.create(999L, request));
    }

    private User saveUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        return userRepository.save(user);
    }

    private Item saveItem(String name, String description, Boolean available, User owner) {
        Item item = new Item();
        item.setName(name);
        item.setDescription(description);
        item.setAvailable(available);
        item.setOwner(owner);
        return itemRepository.save(item);
    }
}