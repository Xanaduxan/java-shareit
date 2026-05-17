package ru.practicum.shareit.comment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentDto {
    private Long id;

    @NotBlank(message = "Текст комментария не может быть пустым")
    private String text;

    private String authorName;
    private LocalDateTime created;
}