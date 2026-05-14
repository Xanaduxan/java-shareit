package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.practicum.shareit.booking.dto.BookingDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class BookingDtoJsonTest {

    @Autowired
    private JacksonTester<BookingDto> json;

    @Test
    void testSerialize() throws Exception {
        BookingDto dto = new BookingDto();

        dto.setId(1L);
        dto.setStart(LocalDateTime.of(2026, 1, 1, 12, 0));
        dto.setEnd(LocalDateTime.of(2026, 1, 2, 12, 0));
        dto.setItemId(10L);
        dto.setStatus(BookingStatus.APPROVED);

        String result = json.write(dto)
                            .getJson();

        assertThat(result).contains("\"id\":1");
        assertThat(result).contains("\"itemId\":10");
        assertThat(result).contains("\"status\":\"APPROVED\"");
    }

    @Test
    void testDeserialize() throws Exception {
        String content = """
                {
                  "id": 1,
                  "start": "2026-01-01T12:00:00",
                  "end": "2026-01-02T12:00:00",
                  "itemId": 10,
                  "status": "APPROVED"
                }
                """;

        BookingDto result = json.parseObject(content);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getItemId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo(BookingStatus.APPROVED);
    }
}