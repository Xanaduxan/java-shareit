package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.common.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl implements ItemService {

    private static final String CREATE_ITEM_LOG = "Создание вещи для userId = {}";
    private static final String UPDATE_ITEM_LOG = "Обновление вещи itemId = {} пользователем userId = {}";
    private static final String GET_ITEM_LOG = "Получение вещи по itemId = {}";
    private static final String GET_OWNER_ITEMS_LOG = "Получение всех вещей владельца ownerId = {}";
    private static final String SEARCH_ITEMS_LOG = "Поиск вещей по тексту = {}";
    private static final String EMPTY_SEARCH_LOG = "Текст поиска пустой";
    private static final String USER_NOT_FOUND_LOG = "Пользователь с id = {} не найден";
    private static final String ITEM_NOT_FOUND_LOG = "Вещь с id = {} не найдена";
    private static final String ACCESS_DENIED_LOG = "Пользователь с id = {} не является владельцем вещи itemId = {}";
    private static final String ITEM_CREATED_LOG = "Вещь успешно создана с id = {}";
    private static final String ITEM_UPDATED_LOG = "Вещь успешно обновлена с id = {}";
    private static final String ITEMS_FOUND_LOG = "Найдено {} вещей для владельца ownerId = {}";
    private static final String SEARCH_RESULT_LOG = "Найдено {} вещей по тексту = {}";

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    public ItemDto create(Long userId, ItemDto itemDto) {
        log.info(CREATE_ITEM_LOG, userId);

        User owner = getUserOrThrow(userId);

        Item item = ItemMapper.toItem(itemDto);
        item.setOwner(owner);

        Item createdItem = itemRepository.create(item);
        log.info(ITEM_CREATED_LOG, createdItem.getId());

        return ItemMapper.toItemDto(createdItem);
    }

    @Override
    public ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
        log.info(UPDATE_ITEM_LOG, itemId, userId);

        getUserOrThrow(userId);
        Item item = getItemOrThrow(itemId);

        if (item.getOwner() == null || !item.getOwner()
                                            .getId()
                                            .equals(userId)) {
            log.warn(ACCESS_DENIED_LOG, userId, itemId);
            throw new NotFoundException("Редактировать вещь может только владелец");
        }

        if (itemDto.getName() != null) {
            if (itemDto.getName()
                       .isBlank()) {
                throw new IllegalArgumentException("Название не может быть пустым");
            }
            item.setName(itemDto.getName());
        }

        if (itemDto.getDescription() != null) {
            if (itemDto.getDescription()
                       .isBlank()) {
                throw new IllegalArgumentException("Описание не может быть пустым");
            }
            item.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }

        Item updatedItem = itemRepository.update(item);
        log.info(ITEM_UPDATED_LOG, updatedItem.getId());

        return ItemMapper.toItemDto(updatedItem);
    }

    @Override
    public ItemDto getById(Long itemId) {
        log.info(GET_ITEM_LOG, itemId);
        return ItemMapper.toItemDto(getItemOrThrow(itemId));
    }

    @Override
    public Collection<ItemDto> getAllByOwner(Long ownerId) {
        log.info(GET_OWNER_ITEMS_LOG, ownerId);

        getUserOrThrow(ownerId);

        Collection<ItemDto> items = itemRepository.getAllByOwner(ownerId)
                                                  .stream()
                                                  .map(ItemMapper::toItemDto)
                                                  .collect(Collectors.toList());

        log.info(ITEMS_FOUND_LOG, items.size(), ownerId);
        return items;
    }

    @Override
    public Collection<ItemDto> search(String text) {
        log.info(SEARCH_ITEMS_LOG, text);

        if (text == null || text.isBlank()) {
            log.info(EMPTY_SEARCH_LOG);
            return List.of();
        }

        Collection<ItemDto> items = itemRepository.search(text)
                                                  .stream()
                                                  .map(ItemMapper::toItemDto)
                                                  .collect(Collectors.toList());

        log.info(SEARCH_RESULT_LOG, items.size(), text);
        return items;
    }

    private User getUserOrThrow(Long userId) {
        User user = userRepository.getById(userId);
        if (user == null) {
            log.warn(USER_NOT_FOUND_LOG, userId);
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        return user;
    }

    private Item getItemOrThrow(Long itemId) {
        Item item = itemRepository.getById(itemId);
        if (item == null) {
            log.warn(ITEM_NOT_FOUND_LOG, itemId);
            throw new NotFoundException("Вещь с id=" + itemId + " не найдена");
        }
        return item;
    }


}