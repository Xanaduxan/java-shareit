package ru.practicum.shareit.item;

import ru.practicum.shareit.comment.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.Collection;

public interface ItemService {
    ItemDto create(Long userId, ItemDto itemDto);

    ItemDto update(Long userId, Long itemId, ItemDto itemDto);

    ItemDto getById(Long userId, Long itemId);

    Collection<ItemDto> getAllByOwner(Long ownerId);

    Collection<ItemDto> search(String text);

    CommentDto addComment(Long userId, Long itemId, CommentDto commentDto);

}