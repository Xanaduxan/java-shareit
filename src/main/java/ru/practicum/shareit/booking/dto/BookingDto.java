package ru.practicum.shareit.booking.dto;

import lombok.Data;
import ru.practicum.shareit.booking.BookingStatus;

import java.time.LocalDateTime;


@Data
public class BookingDto {
    private Long id;
    private LocalDateTime start;
    private LocalDateTime end;
    private Long itemId;
    private ItemShortDto item;
    private UserShortDto booker;
    private BookingStatus status;

    @Data
    public static class ItemShortDto {
        private Long id;
        private String name;
    }

    @Data
    public static class UserShortDto {
        private Long id;
    }
}