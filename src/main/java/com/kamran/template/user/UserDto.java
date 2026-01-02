package com.kamran.template.user;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDto {
    private Long Id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String email;
    private String firstName;
    private String lastName;

    public static UserDto formEntity(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
