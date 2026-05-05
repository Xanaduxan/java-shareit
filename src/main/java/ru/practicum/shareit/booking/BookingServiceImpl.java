package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.common.ForbiddenException;
import ru.practicum.shareit.common.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BookingDto create(Long userId, BookingDto bookingDto) {
        User booker = getUserOrThrow(userId);
        Item item = getItemOrThrow(bookingDto.getItemId());

        if (item.getOwner()
                .getId()
                .equals(userId)) {
            throw new NotFoundException("Владелец не может бронировать свою вещь");
        }

        if (!Boolean.TRUE.equals(item.getAvailable())) {
            throw new IllegalArgumentException("Вещь недоступна");
        }

        if (!bookingDto.getEnd()
                       .isAfter(bookingDto.getStart())) {
            throw new IllegalArgumentException("Дата окончания должна быть позже даты начала");
        }

        Booking booking = BookingMapper.toBooking(bookingDto);
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.WAITING);

        return BookingMapper.toBookingDto(bookingRepository.save(booking));
    }


    @Override
    @Transactional
    public BookingDto approve(Long userId, Long bookingId, Boolean approved) {
        Booking booking = getBookingOrThrow(bookingId);

        if (!booking.getItem()
                    .getOwner()
                    .getId()
                    .equals(userId)) {
            throw new ForbiddenException("Подтверждать может только владелец");
        }
        if (!BookingStatus.WAITING.equals(booking.getStatus())) {
            throw new IllegalArgumentException("Можно изменить только бронирование в статусе WAITING");
        }
        booking.setStatus(Boolean.TRUE.equals(approved)
                ? BookingStatus.APPROVED
                : BookingStatus.REJECTED);

        return BookingMapper.toBookingDto(bookingRepository.save(booking));
    }

    @Override
    public BookingDto getById(Long userId, Long bookingId) {
        getUserOrThrow(userId);

        Booking booking = getBookingOrThrow(bookingId);

        if (!booking.getBooker()
                    .getId()
                    .equals(userId) &&
                !booking.getItem()
                        .getOwner()
                        .getId()
                        .equals(userId)) {
            throw new NotFoundException("Нет доступа");
        }

        return BookingMapper.toBookingDto(booking);
    }

    @Override
    public Collection<BookingDto> getUserBookings(Long userId, BookingState state) {
        getUserOrThrow(userId);

        LocalDateTime now = LocalDateTime.now();
        Sort sort = Sort.by(Sort.Direction.DESC, "start");

        List<Booking> bookings = switch (state) {
            case ALL -> bookingRepository.findByBooker_Id(userId, sort);
            case CURRENT -> bookingRepository.findByBooker_IdAndStartIsBeforeAndEndIsAfter(userId, now, now, sort);
            case PAST -> bookingRepository.findByBooker_IdAndEndIsBefore(userId, now, sort);
            case FUTURE -> bookingRepository.findByBooker_IdAndStartIsAfter(userId, now, sort);
            case WAITING -> bookingRepository.findByBooker_IdAndStatus(userId, BookingStatus.WAITING, sort);
            case REJECTED -> bookingRepository.findByBooker_IdAndStatus(userId, BookingStatus.REJECTED, sort);
        };

        return bookings.stream()
                       .map(BookingMapper::toBookingDto)
                       .collect(Collectors.toList());
    }

    @Override
    public Collection<BookingDto> getOwnerBookings(Long userId, BookingState state) {
        getUserOrThrow(userId);

        LocalDateTime now = LocalDateTime.now();
        Sort sort = Sort.by(Sort.Direction.DESC, "start");

        List<Booking> bookings = switch (state) {
            case ALL -> bookingRepository.findByItem_Owner_Id(userId, sort);
            case CURRENT -> bookingRepository.findByItem_Owner_IdAndStartIsBeforeAndEndIsAfter(userId, now, now, sort);
            case PAST -> bookingRepository.findByItem_Owner_IdAndEndIsBefore(userId, now, sort);
            case FUTURE -> bookingRepository.findByItem_Owner_IdAndStartIsAfter(userId, now, sort);
            case WAITING -> bookingRepository.findByItem_Owner_IdAndStatus(userId, BookingStatus.WAITING, sort);
            case REJECTED -> bookingRepository.findByItem_Owner_IdAndStatus(userId, BookingStatus.REJECTED, sort);
        };

        return bookings.stream()
                       .map(BookingMapper::toBookingDto)
                       .collect(Collectors.toList());
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                             .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private Item getItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                             .orElseThrow(() -> new NotFoundException("Вещь с id=" + itemId + " не найдена"));
    }

    private Booking getBookingOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                                .orElseThrow(
                                        () -> new NotFoundException("Бронирование с id=" + bookingId + " не найдено"));
    }
}