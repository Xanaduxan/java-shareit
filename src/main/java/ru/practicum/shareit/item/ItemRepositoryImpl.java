package ru.practicum.shareit.item;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Item;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class ItemRepositoryImpl implements ItemRepository {

    private final Map<Long, Item> items = new HashMap<>();
    private long nextId = 1;

    @Override
    public Item create(Item item) {
        item.setId(nextId++);
        items.put(item.getId(), item);
        return item;
    }

    @Override
    public Item update(Item item) {
        items.put(item.getId(), item);
        return item;
    }

    @Override
    public Item getById(Long itemId) {
        return items.get(itemId);
    }

    @Override
    public Collection<Item> getAll() {
        return items.values();
    }

    @Override
    public Collection<Item> getAllByOwner(Long ownerId) {
        return items.values()
                    .stream()
                    .filter(item -> item.getOwner() != null && item.getOwner()
                                                                   .getId()
                                                                   .equals(ownerId))
                    .collect(Collectors.toList());
    }

    @Override
    public Collection<Item> search(String text) {
        String lowerText = text.toLowerCase();

        return items.values()
                    .stream()
                    .filter(item -> Boolean.TRUE.equals(item.getAvailable()))
                    .filter(item -> item.getName()
                                        .toLowerCase()
                                        .contains(lowerText) || item.getDescription()
                                                                    .toLowerCase()
                                                                    .contains(lowerText))
                    .collect(Collectors.toList());
    }
}