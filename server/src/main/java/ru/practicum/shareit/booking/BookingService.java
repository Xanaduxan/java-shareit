package ru.practicum.shareit.booking;

import ru.practicum.shareit.booking.dto.BookingDto;

import java.util.Collection;

public interface BookingService {
    BookingDto create(Long userId, BookingDto bookingDto);

    BookingDto approve(Long userId, Long bookingId, Boolean approved);

    BookingDto getById(Long userId, Long bookingId);

    Collection<BookingDto> getUserBookings(Long userId, BookingState state);

    Collection<BookingDto> getOwnerBookings(Long userId, BookingState state);
}