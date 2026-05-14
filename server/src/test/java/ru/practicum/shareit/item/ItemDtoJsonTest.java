package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.practicum.shareit.item.dto.ItemDto;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemDtoJsonTest {

    @Autowired
    private JacksonTester<ItemDto> json;

    @Test
    void testSerialize() throws Exception {
        ItemDto dto = new ItemDto();

        dto.setId(1L);
        dto.setName("Дрель");
        dto.setDescription("Мощная дрель");
        dto.setAvailable(true);
        dto.setRequestId(100L);

        String result = json.write(dto)
                            .getJson();

        assertThat(result).contains("\"id\":1");
        assertThat(result).contains("\"name\":\"Дрель\"");
        assertThat(result).contains("\"description\":\"Мощная дрель\"");
        assertThat(result).contains("\"available\":true");
        assertThat(result).contains("\"requestId\":100");
    }

    @Test
    void testDeserialize() throws Exception {
        String content = """
                {
                  "id": 1,
                  "name": "Дрель",
                  "description": "Мощная дрель",
                  "available": true,
                  "requestId": 100
                }
                """;

        ItemDto result = json.parseObject(content);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Дрель");
        assertThat(result.getDescription()).isEqualTo("Мощная дрель");
        assertThat(result.getAvailable()).isTrue();
        assertThat(result.getRequestId()).isEqualTo(100L);
    }
}