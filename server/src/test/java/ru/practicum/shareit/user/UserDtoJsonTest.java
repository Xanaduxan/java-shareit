package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.practicum.shareit.user.dto.UserDto;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class UserDtoJsonTest {

    @Autowired
    private JacksonTester<UserDto> json;

    @Test
    void testSerialize() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setId(1L);
        userDto.setName("Иван");
        userDto.setEmail("ivan@mail.com");

        String result = json.write(userDto)
                            .getJson();

        assertThat(result).contains("\"id\":1");
        assertThat(result).contains("\"name\":\"Иван\"");
        assertThat(result).contains("\"email\":\"ivan@mail.com\"");
    }

    @Test
    void testDeserialize() throws Exception {
        String content = """
                {
                  "id": 1,
                  "name": "Иван",
                  "email": "ivan@mail.com"
                }
                """;

        UserDto result = json.parseObject(content);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Иван");
        assertThat(result.getEmail()).isEqualTo("ivan@mail.com");
    }
}