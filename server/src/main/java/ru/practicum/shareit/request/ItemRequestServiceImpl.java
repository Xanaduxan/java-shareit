package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.common.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.mapper.ItemRequestMapper;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {

    private final ItemRequestRepository itemRequestRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public ItemRequestDto create(Long userId, ItemRequestDto itemRequestDto) {
        User user = getUserOrThrow(userId);

        ItemRequest itemRequest = ItemRequestMapper.toItemRequest(itemRequestDto);
        itemRequest.setRequestor(user);
        itemRequest.setCreated(LocalDateTime.now());

        ItemRequest savedRequest = itemRequestRepository.save(itemRequest);

        return ItemRequestMapper.toItemRequestDto(
                savedRequest,
                itemRepository.findByRequestId(savedRequest.getId())
        );
    }

    @Override
    public ItemRequestDto getById(Long userId, Long itemRequestId) {
        getUserOrThrow(userId);

        ItemRequest itemRequest = getItemRequestOrThrow(itemRequestId);

        return ItemRequestMapper.toItemRequestDto(
                itemRequest,
                itemRepository.findByRequestId(itemRequest.getId())
        );
    }

    @Override
    public Collection<ItemRequestDto> getMyRequests(Long userId) {
        getUserOrThrow(userId);

        return itemRequestRepository.findByRequestor_IdOrderByCreatedDesc(userId)
                                    .stream()
                                    .map(request -> ItemRequestMapper.toItemRequestDto(
                                            request,
                                            itemRepository.findByRequestId(request.getId())
                                    ))
                                    .collect(Collectors.toList());
    }

    @Override
    public Collection<ItemRequestDto> getAllRequests(Long userId) {
        getUserOrThrow(userId);

        return itemRequestRepository.findByRequestor_IdNotOrderByCreatedDesc(userId)
                                    .stream()
                                    .map(request -> ItemRequestMapper.toItemRequestDto(
                                            request,
                                            itemRepository.findByRequestId(request.getId())
                                    ))
                                    .collect(Collectors.toList());
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                             .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private ItemRequest getItemRequestOrThrow(Long itemRequestId) {
        return itemRequestRepository.findById(itemRequestId)
                                    .orElseThrow(
                                            () -> new NotFoundException("Запрос с id=" + itemRequestId + " не найден"));
    }
}