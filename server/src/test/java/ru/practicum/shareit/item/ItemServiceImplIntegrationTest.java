package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.comment.dto.CommentDto;
import ru.practicum.shareit.common.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ItemServiceImplIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void create_shouldCreateItemInDatabase() {
        User owner = saveUser("Владелец", "item-owner1@mail.com");

        ItemDto request = new ItemDto();
        request.setName("Дрель");
        request.setDescription("Мощная дрель");
        request.setAvailable(true);

        ItemDto result = itemService.create(owner.getId(), request);

        assertNotNull(result.getId());
        assertEquals("Дрель", result.getName());
        assertEquals("Мощная дрель", result.getDescription());
        assertTrue(result.getAvailable());
    }

    @Test
    void update_shouldUpdateItemInDatabase() {
        User owner = saveUser("Владелец", "item-owner2@mail.com");
        Item item = saveItem("Старая дрель", "Старое описание", true, owner);

        ItemDto update = new ItemDto();
        update.setName("Новая дрель");
        update.setDescription("Новое описание");
        update.setAvailable(false);

        ItemDto result = itemService.update(owner.getId(), item.getId(), update);

        assertEquals(item.getId(), result.getId());
        assertEquals("Новая дрель", result.getName());
        assertEquals("Новое описание", result.getDescription());
        assertFalse(result.getAvailable());
    }

    @Test
    void update_shouldThrowNotFoundException_whenUserIsNotOwner() {
        User owner = saveUser("Владелец", "item-owner3@mail.com");
        User anotherUser = saveUser("Другой пользователь", "item-user3@mail.com");
        Item item = saveItem("Книга", "Интересная книга", true, owner);

        ItemDto update = new ItemDto();
        update.setName("Новая книга");

        assertThrows(NotFoundException.class,
                () -> itemService.update(anotherUser.getId(), item.getId(), update));
    }

    @Test
    void getById_shouldReturnItemWithCommentsForAnyUser() {
        User owner = saveUser("Владелец", "item-owner4@mail.com");
        User booker = saveUser("Арендатор", "item-booker4@mail.com");
        Item item = saveItem("Велосипед", "Городской велосипед", true, owner);

        Booking booking = saveBooking(
                item,
                booker,
                LocalDateTime.now()
                             .minusDays(3),
                LocalDateTime.now()
                             .minusDays(2),
                BookingStatus.APPROVED
        );

        assertNotNull(booking.getId());

        CommentDto comment = new CommentDto();
        comment.setText("Хорошая вещь");

        itemService.addComment(booker.getId(), item.getId(), comment);

        ItemDto result = itemService.getById(booker.getId(), item.getId());

        assertEquals(item.getId(), result.getId());
        assertEquals(1, result.getComments()
                              .size());
        assertEquals("Хорошая вещь", result.getComments()
                                           .get(0)
                                           .getText());
    }

    @Test
    void getById_shouldReturnLastAndNextBookingForOwner() {
        User owner = saveUser("Владелец", "item-owner5@mail.com");
        User booker = saveUser("Арендатор", "item-booker5@mail.com");
        Item item = saveItem("Самокат", "Электросамокат", true, owner);

        Booking pastBooking = saveBooking(
                item,
                booker,
                LocalDateTime.now()
                             .minusDays(3),
                LocalDateTime.now()
                             .minusDays(2),
                BookingStatus.APPROVED
        );

        Booking futureBooking = saveBooking(
                item,
                booker,
                LocalDateTime.now()
                             .plusDays(1),
                LocalDateTime.now()
                             .plusDays(2),
                BookingStatus.APPROVED
        );

        ItemDto result = itemService.getById(owner.getId(), item.getId());

        assertEquals(item.getId(), result.getId());
        assertNotNull(result.getLastBooking());
        assertNotNull(result.getNextBooking());
        assertEquals(pastBooking.getId(), result.getLastBooking()
                                                .getId());
        assertEquals(futureBooking.getId(), result.getNextBooking()
                                                  .getId());
    }

    @Test
    void getAllByOwner_shouldReturnOwnerItemsWithBookingsAndComments() {
        User owner = saveUser("Владелец", "item-owner6@mail.com");
        User booker = saveUser("Арендатор", "item-booker6@mail.com");
        Item item = saveItem("Ноутбук", "Рабочий ноутбук", true, owner);

        saveBooking(
                item,
                booker,
                LocalDateTime.now()
                             .minusDays(3),
                LocalDateTime.now()
                             .minusDays(2),
                BookingStatus.APPROVED
        );

        saveBooking(
                item,
                booker,
                LocalDateTime.now()
                             .plusDays(1),
                LocalDateTime.now()
                             .plusDays(2),
                BookingStatus.APPROVED
        );

        CommentDto comment = new CommentDto();
        comment.setText("Отличная вещь");
        itemService.addComment(booker.getId(), item.getId(), comment);

        Collection<ItemDto> result = itemService.getAllByOwner(owner.getId());

        assertEquals(1, result.size());

        ItemDto dto = result.iterator()
                            .next();

        assertEquals(item.getId(), dto.getId());
        assertNotNull(dto.getLastBooking());
        assertNotNull(dto.getNextBooking());
        assertEquals(1, dto.getComments()
                           .size());
        assertEquals("Отличная вещь", dto.getComments()
                                         .get(0)
                                         .getText());
    }

    @Test
    void search_shouldReturnAvailableItems() {
        User owner = saveUser("Владелец", "item-owner7@mail.com");
        saveItem("Уникальный перфоратор", "Для поиска только в этом тесте", true, owner);
        saveItem("Закрытый перфоратор", "Недоступная вещь", false, owner);

        Collection<ItemDto> result = itemService.search("перфоратор");

        assertEquals(1, result.size());

        ItemDto dto = result.iterator()
                            .next();

        assertEquals("Уникальный перфоратор", dto.getName());
        assertTrue(dto.getAvailable());
    }

    @Test
    void addComment_shouldThrowIllegalArgumentException_whenBookingIsNotCompleted() {
        User owner = saveUser("Владелец", "item-owner8@mail.com");
        User booker = saveUser("Арендатор", "item-booker8@mail.com");
        Item item = saveItem("Фотоаппарат", "Зеркальный фотоаппарат", true, owner);

        saveBooking(
                item,
                booker,
                LocalDateTime.now()
                             .plusDays(1),
                LocalDateTime.now()
                             .plusDays(2),
                BookingStatus.APPROVED
        );

        CommentDto comment = new CommentDto();
        comment.setText("Хорошая вещь");

        assertThrows(IllegalArgumentException.class,
                () -> itemService.addComment(booker.getId(), item.getId(), comment));
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

    private Booking saveBooking(Item item,
                                User booker,
                                LocalDateTime start,
                                LocalDateTime end,
                                BookingStatus status) {
        Booking booking = new Booking();
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStart(start);
        booking.setEnd(end);
        booking.setStatus(status);
        return bookingRepository.save(booking);
    }
}