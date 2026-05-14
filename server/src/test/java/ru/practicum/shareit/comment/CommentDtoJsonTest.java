package ru.practicum.shareit.comment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.practicum.shareit.comment.dto.CommentDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class CommentDtoJsonTest {

    @Autowired
    private JacksonTester<CommentDto> json;

    @Test
    void testSerialize() throws Exception {
        CommentDto dto = new CommentDto();

        dto.setId(1L);
        dto.setText("Хорошая вещь");
        dto.setAuthorName("Иван");
        dto.setCreated(LocalDateTime.of(2026, 1, 1, 12, 0));

        String result = json.write(dto)
                            .getJson();

        assertThat(result).contains("\"id\":1");
        assertThat(result).contains("\"text\":\"Хорошая вещь\"");
        assertThat(result).contains("\"authorName\":\"Иван\"");
    }

    @Test
    void testDeserialize() throws Exception {
        String content = "{"
                + "\"id\": 1,"
                + "\"text\": \"Хорошая вещь\","
                + "\"authorName\": \"Иван\","
                + "\"created\": \"2026-01-01T12:00:00\""
                + "}";

        CommentDto result = json.parseObject(content);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getText()).isEqualTo("Хорошая вещь");
        assertThat(result.getAuthorName()).isEqualTo("Иван");
    }
}