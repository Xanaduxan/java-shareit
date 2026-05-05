package ru.practicum.shareit.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.practicum.shareit.booking.BookingStatus;

import java.time.LocalDateTime;

@Data
public class BookingDto {
    private Long id;

    @NotNull
    @FutureOrPresent
    private LocalDateTime start;

    @NotNull
    @Future
    private LocalDateTime end;

    @NotNull
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