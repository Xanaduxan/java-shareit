package ru.practicum.shareit.request.mapper;

import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.model.ItemRequest;

import java.util.List;
import java.util.stream.Collectors;

public class ItemRequestMapper {

    private ItemRequestMapper() {
    }

    public static ItemRequestDto toItemRequestDto(ItemRequest itemRequest, List<Item> items) {
        ItemRequestDto dto = new ItemRequestDto();

        dto.setId(itemRequest.getId());
        dto.setDescription(itemRequest.getDescription());
        dto.setCreated(itemRequest.getCreated());
        dto.setItems(items.stream()
                          .map(ItemRequestMapper::toItemShortDto)
                          .collect(Collectors.toList()));

        return dto;
    }

    public static ItemRequest toItemRequest(ItemRequestDto dto) {
        ItemRequest itemRequest = new ItemRequest();

        itemRequest.setDescription(dto.getDescription());

        return itemRequest;
    }

    private static ItemRequestDto.ItemShortDto toItemShortDto(Item item) {
        ItemRequestDto.ItemShortDto dto = new ItemRequestDto.ItemShortDto();

        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setOwnerId(item.getOwner()
                           .getId());

        return dto;
    }
}