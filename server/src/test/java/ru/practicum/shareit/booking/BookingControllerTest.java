package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.common.ForbiddenException;
import ru.practicum.shareit.common.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BookingService bookingService;

    @Test
    void create_shouldReturnCreatedBooking() throws Exception {
        BookingDto request = makeBookingDto(
                null,
                10L,
                LocalDateTime.now()
                             .plusHours(1),
                LocalDateTime.now()
                             .plusHours(2),
                BookingStatus.WAITING
        );

        BookingDto response = makeBookingDto(
                1L,
                10L,
                request.getStart(),
                request.getEnd(),
                BookingStatus.WAITING
        );

        Mockito.when(bookingService.create(eq(2L), any(BookingDto.class)))
               .thenReturn(response);

        mvc.perform(post("/bookings")
                   .header(USER_ID_HEADER, 2L)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(request)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(1))
           .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    void approve_shouldReturnApprovedBooking() throws Exception {
        BookingDto response = makeBookingDto(
                1L,
                10L,
                LocalDateTime.now()
                             .plusHours(1),
                LocalDateTime.now()
                             .plusHours(2),
                BookingStatus.APPROVED
        );

        Mockito.when(bookingService.approve(1L, 1L, true))
               .thenReturn(response);

        mvc.perform(patch("/bookings/1")
                   .header(USER_ID_HEADER, 1L)
                   .param("approved", "true"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(1))
           .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approve_shouldReturnForbidden_whenUserIsNotOwner() throws Exception {
        Mockito.when(bookingService.approve(3L, 1L, true))
               .thenThrow(new ForbiddenException("Подтверждать может только владелец"));

        mvc.perform(patch("/bookings/1")
                   .header(USER_ID_HEADER, 3L)
                   .param("approved", "true"))
           .andExpect(status().isForbidden())
           .andExpect(jsonPath("$.error").value("Подтверждать может только владелец"));
    }

    @Test
    void getById_shouldReturnBooking() throws Exception {
        BookingDto response = makeBookingDto(
                1L,
                10L,
                LocalDateTime.now()
                             .plusHours(1),
                LocalDateTime.now()
                             .plusHours(2),
                BookingStatus.WAITING
        );

        Mockito.when(bookingService.getById(2L, 1L))
               .thenReturn(response);

        mvc.perform(get("/bookings/1")
                   .header(USER_ID_HEADER, 2L))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(1))
           .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    void getById_shouldReturnNotFound_whenBookingDoesNotExist() throws Exception {
        Mockito.when(bookingService.getById(2L, 99L))
               .thenThrow(new NotFoundException("Бронирование с id=99 не найдено"));

        mvc.perform(get("/bookings/99")
                   .header(USER_ID_HEADER, 2L))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.error").value("Бронирование с id=99 не найдено"));
    }

    @Test
    void getUserBookings_shouldReturnBookingsWithDefaultStateAll() throws Exception {
        BookingDto booking = makeBookingDto(
                1L,
                10L,
                LocalDateTime.now()
                             .plusHours(1),
                LocalDateTime.now()
                             .plusHours(2),
                BookingStatus.WAITING
        );

        Mockito.when(bookingService.getUserBookings(2L, BookingState.ALL))
               .thenReturn(List.of(booking));

        mvc.perform(get("/bookings")
                   .header(USER_ID_HEADER, 2L))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].id").value(1))
           .andExpect(jsonPath("$[0].status").value("WAITING"));
    }

    @Test
    void getUserBookings_shouldReturnBookingsWithWaitingState() throws Exception {
        Mockito.when(bookingService.getUserBookings(2L, BookingState.WAITING))
               .thenReturn(List.of());

        mvc.perform(get("/bookings")
                   .header(USER_ID_HEADER, 2L)
                   .param("state", "WAITING"))
           .andExpect(status().isOk());

        Mockito.verify(bookingService)
               .getUserBookings(2L, BookingState.WAITING);
    }

    @Test
    void getOwnerBookings_shouldReturnBookingsWithDefaultStateAll() throws Exception {
        BookingDto booking = makeBookingDto(
                1L,
                10L,
                LocalDateTime.now()
                             .plusHours(1),
                LocalDateTime.now()
                             .plusHours(2),
                BookingStatus.APPROVED
        );

        Mockito.when(bookingService.getOwnerBookings(1L, BookingState.ALL))
               .thenReturn(List.of(booking));

        mvc.perform(get("/bookings/owner")
                   .header(USER_ID_HEADER, 1L))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].id").value(1))
           .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    @Test
    void getOwnerBookings_shouldReturnBookingsWithRejectedState() throws Exception {
        Mockito.when(bookingService.getOwnerBookings(1L, BookingState.REJECTED))
               .thenReturn(List.of());

        mvc.perform(get("/bookings/owner")
                   .header(USER_ID_HEADER, 1L)
                   .param("state", "REJECTED"))
           .andExpect(status().isOk());

        Mockito.verify(bookingService)
               .getOwnerBookings(1L, BookingState.REJECTED);
    }

    private BookingDto makeBookingDto(Long id,
                                      Long itemId,
                                      LocalDateTime start,
                                      LocalDateTime end,
                                      BookingStatus status) {
        BookingDto dto = new BookingDto();
        dto.setId(id);
        dto.setItemId(itemId);
        dto.setStart(start);
        dto.setEnd(end);
        dto.setStatus(status);

        BookingDto.ItemShortDto item = new BookingDto.ItemShortDto();
        item.setId(itemId);
        item.setName("Item");
        dto.setItem(item);

        BookingDto.UserShortDto booker = new BookingDto.UserShortDto();
        booker.setId(2L);
        dto.setBooker(booker);

        return dto;
    }
}