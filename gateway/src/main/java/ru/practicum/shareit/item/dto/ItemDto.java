package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.practicum.shareit.comment.dto.CommentDto;

import java.util.List;

@Data
public class ItemDto {
    private Long id;

    @NotBlank(message = "Название вещи не должно быть пустым")
    private String name;

    @NotBlank(message = "Описание вещи не должно быть пустым")
    private String description;

    @NotNull(message = "Статус доступности обязателен")
    private Boolean available;

    private Long requestId;
    private BookingShortDto lastBooking;
    private BookingShortDto nextBooking;
    private List<CommentDto> comments;

    @Data
    public static class BookingShortDto {
        private Long id;
        private Long bookerId;
    }
}