package info.mengnan.dialogerai.repository.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberRole {

    OWNER(1),
    MEMBER(2);

    @EnumValue
    @JsonValue
    private final int value;

    @JsonCreator
    public static MemberRole of(int value) {
        for (MemberRole role : values()) {
            if (role.value == value) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown member role: " + value);
    }
}
