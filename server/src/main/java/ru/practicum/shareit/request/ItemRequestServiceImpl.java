package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.common.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.mapper.ItemRequestMapper;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

        List<ItemRequest> requests = itemRequestRepository.findByRequestor_IdOrderByCreatedDesc(userId);

        return toItemRequestDtos(requests);
    }

    @Override
    public Collection<ItemRequestDto> getAllRequests(Long userId) {
        getUserOrThrow(userId);

        List<ItemRequest> requests = itemRequestRepository.findByRequestor_IdNotOrderByCreatedDesc(userId);

        return toItemRequestDtos(requests);
    }

    private Collection<ItemRequestDto> toItemRequestDtos(List<ItemRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }

        List<Long> requestIds = requests.stream()
                                        .map(ItemRequest::getId)
                                        .collect(Collectors.toList());

        Map<Long, List<Item>> itemsByRequestId = itemRepository.findByRequestIdIn(requestIds)
                                                               .stream()
                                                               .collect(Collectors.groupingBy(Item::getRequestId));

        return requests.stream()
                       .map(request -> ItemRequestMapper.toItemRequestDto(
                               request,
                               itemsByRequestId.getOrDefault(request.getId(), List.of())
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