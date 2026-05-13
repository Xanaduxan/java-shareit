package ru.practicum.shareit.booking.mapper;

import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.model.Booking;

public class BookingMapper {

    private BookingMapper() {
    }

    public static BookingDto toBookingDto(Booking booking) {
        BookingDto dto = new BookingDto();

        dto.setId(booking.getId());
        dto.setStart(booking.getStart());
        dto.setEnd(booking.getEnd());
        dto.setStatus(booking.getStatus());

        BookingDto.ItemShortDto item = new BookingDto.ItemShortDto();
        item.setId(booking.getItem()
                          .getId());
        item.setName(booking.getItem()
                            .getName());
        dto.setItem(item);

        BookingDto.UserShortDto booker = new BookingDto.UserShortDto();
        booker.setId(booking.getBooker()
                            .getId());
        dto.setBooker(booker);

        return dto;
    }

    public static Booking toBooking(BookingDto dto) {
        Booking booking = new Booking();

        booking.setStart(dto.getStart());
        booking.setEnd(dto.getEnd());

        return booking;
    }
}