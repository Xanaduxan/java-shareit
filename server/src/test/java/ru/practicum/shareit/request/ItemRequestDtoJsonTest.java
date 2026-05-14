package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemRequestDtoJsonTest {

    @Autowired
    private JacksonTester<ItemRequestDto> json;

    @Test
    void testSerialize() throws Exception {
        ItemRequestDto.ItemShortDto item = new ItemRequestDto.ItemShortDto();
        item.setId(10L);
        item.setName("Дрель");
        item.setOwnerId(2L);

        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(1L);
        dto.setDescription("Нужна дрель");
        dto.setCreated(LocalDateTime.of(2026, 1, 1, 12, 0));
        dto.setItems(List.of(item));

        String result = json.write(dto)
                            .getJson();

        assertThat(result).contains("\"id\":1");
        assertThat(result).contains("\"description\":\"Нужна дрель\"");
        assertThat(result).contains("\"name\":\"Дрель\"");
        assertThat(result).contains("\"ownerId\":2");
    }

    @Test
    void testDeserialize() throws Exception {
        String content = "{"
                + "\"id\": 1,"
                + "\"description\": \"Нужна дрель\","
                + "\"created\": \"2026-01-01T12:00:00\","
                + "\"items\": ["
                + "{"
                + "\"id\": 10,"
                + "\"name\": \"Дрель\","
                + "\"ownerId\": 2"
                + "}"
                + "]"
                + "}";

        ItemRequestDto result = json.parseObject(content);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDescription()).isEqualTo("Нужна дрель");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems()
                         .get(0)
                         .getName()).isEqualTo("Дрель");
    }
}