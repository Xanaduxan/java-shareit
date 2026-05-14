package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.comment.CommentRepository;
import ru.practicum.shareit.comment.dto.CommentDto;
import ru.practicum.shareit.comment.mapper.CommentMapper;
import ru.practicum.shareit.comment.model.Comment;
import ru.practicum.shareit.common.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequestRepository;
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
@Slf4j
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final ItemRequestRepository itemRequestRepository;

    @Override
    @Transactional
    public ItemDto create(Long userId, ItemDto itemDto) {
        User owner = getUserOrThrow(userId);

        if (itemDto.getRequestId() != null) {
            itemRequestRepository.findById(itemDto.getRequestId())
                                 .orElseThrow(() -> new NotFoundException(
                                         "Запрос с id=" + itemDto.getRequestId() + " не найден"
                                 ));
        }

        Item item = ItemMapper.toItem(itemDto);
        item.setOwner(owner);

        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    @Transactional
    public ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
        getUserOrThrow(userId);
        Item item = getItemOrThrow(itemId);

        if (item.getOwner() == null || !item.getOwner()
                                            .getId()
                                            .equals(userId)) {
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

        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    public ItemDto getById(Long userId, Long itemId) {
        getUserOrThrow(userId);

        Item item = getItemOrThrow(itemId);
        ItemDto itemDto = ItemMapper.toItemDto(item);

        if (item.getOwner() != null && item.getOwner()
                                           .getId()
                                           .equals(userId)) {
            addBookings(itemDto);
        }

        addComments(itemDto);

        return itemDto;
    }

    @Override
    public Collection<ItemDto> getAllByOwner(Long ownerId) {
        getUserOrThrow(ownerId);

        List<Item> items = itemRepository.findByOwner_Id(ownerId);

        if (items.isEmpty()) {
            return List.of();
        }

        List<Long> itemIds = items.stream()
                                  .map(Item::getId)
                                  .collect(Collectors.toList());

        Map<Long, List<Booking>> bookingsByItemId = bookingRepository.findByItem_IdInAndStatusOrderByStartAsc(
                                                                             itemIds,
                                                                             BookingStatus.APPROVED
                                                                     )
                                                                     .stream()
                                                                     .collect(Collectors.groupingBy(
                                                                             booking -> booking.getItem()
                                                                                               .getId()));

        Map<Long, List<Comment>> commentsByItemId = commentRepository.findByItem_IdInOrderByCreatedAsc(itemIds)
                                                                     .stream()
                                                                     .collect(Collectors.groupingBy(
                                                                             comment -> comment.getItem()
                                                                                               .getId()));

        return items.stream()
                    .map(item -> {
                        ItemDto itemDto = ItemMapper.toItemDto(item);

                        addBookingsFromMap(itemDto, bookingsByItemId.getOrDefault(item.getId(), List.of()));
                        addCommentsFromMap(itemDto, commentsByItemId.getOrDefault(item.getId(), List.of()));

                        return itemDto;
                    })
                    .collect(Collectors.toList());
    }

    @Override
    public Collection<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        return itemRepository.search(text)
                             .stream()
                             .map(ItemMapper::toItemDto)
                             .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long itemId, CommentDto commentDto) {
        User author = getUserOrThrow(userId);
        Item item = getItemOrThrow(itemId);

        boolean hasCompletedBooking = bookingRepository.existsByItem_IdAndBooker_IdAndStatusAndEndIsBefore(
                itemId,
                userId,
                BookingStatus.APPROVED,
                LocalDateTime.now()
        );

        if (!hasCompletedBooking) {
            throw new IllegalArgumentException(
                    "Оставить комментарий можно только после завершённого бронирования"
            );
        }

        Comment comment = CommentMapper.toComment(commentDto);
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now());

        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }

    private void addBookings(ItemDto itemDto) {
        LocalDateTime now = LocalDateTime.now();

        bookingRepository.findFirstByItem_IdAndStatusAndEndIsBeforeOrderByEndDesc(
                                 itemDto.getId(),
                                 BookingStatus.APPROVED,
                                 now
                         )
                         .ifPresent(booking -> itemDto.setLastBooking(toBookingShortDto(booking)));

        bookingRepository.findFirstByItem_IdAndStatusAndStartIsAfterOrderByStartAsc(
                                 itemDto.getId(),
                                 BookingStatus.APPROVED,
                                 now
                         )
                         .ifPresent(booking -> itemDto.setNextBooking(toBookingShortDto(booking)));
    }

    private void addBookingsFromMap(ItemDto itemDto, List<Booking> bookings) {
        LocalDateTime now = LocalDateTime.now();

        bookings.stream()
                .filter(booking -> booking.getEnd()
                                          .isBefore(now))
                .reduce((first, second) -> second)
                .ifPresent(booking -> itemDto.setLastBooking(toBookingShortDto(booking)));

        bookings.stream()
                .filter(booking -> booking.getStart()
                                          .isAfter(now))
                .findFirst()
                .ifPresent(booking -> itemDto.setNextBooking(toBookingShortDto(booking)));
    }

    private void addComments(ItemDto itemDto) {
        itemDto.setComments(commentRepository.findByItem_IdOrderByCreatedAsc(itemDto.getId())
                                             .stream()
                                             .map(CommentMapper::toCommentDto)
                                             .collect(Collectors.toList()));
    }

    private void addCommentsFromMap(ItemDto itemDto, List<Comment> comments) {
        itemDto.setComments(comments.stream()
                                    .map(CommentMapper::toCommentDto)
                                    .collect(Collectors.toList()));
    }

    private ItemDto.BookingShortDto toBookingShortDto(Booking booking) {
        ItemDto.BookingShortDto dto = new ItemDto.BookingShortDto();
        dto.setId(booking.getId());
        dto.setBookerId(booking.getBooker()
                               .getId());
        return dto;
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                             .orElseThrow(() -> new NotFoundException(
                                     "Пользователь с id=" + userId + " не найден"
                             ));
    }

    private Item getItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                             .orElseThrow(() -> new NotFoundException(
                                     "Вещь с id=" + itemId + " не найдена"
                             ));
    }
}