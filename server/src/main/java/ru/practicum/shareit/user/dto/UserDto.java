package ru.practicum.shareit.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserDto {

    private Long id;

    @NotBlank(message = "Имя не должно быть пустым")
    private String name;

    @Email(message = "Некорректный email")
    @NotBlank(message = "Email не должен быть пустым")
    private String email;
}