package ru.practicum.shareit.request;


import ru.practicum.shareit.request.dto.ItemRequestDto;


import java.util.Collection;

public interface ItemRequestService {

    ItemRequestDto create(Long userId, ItemRequestDto itemRequestDto);

    ItemRequestDto getById(Long userId, Long itemRequestId);

    Collection<ItemRequestDto> getMyRequests(Long userId);

    Collection<ItemRequestDto> getAllRequests(Long userId);
}
