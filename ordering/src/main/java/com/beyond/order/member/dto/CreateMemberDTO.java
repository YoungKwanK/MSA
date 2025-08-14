package com.beyond.order.member.dto;

import com.beyond.order.member.domain.Member;
import com.beyond.order.member.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMemberDTO {

    private String name;
    private String email;
    private String password;

    public Member toEntity(String encodedPassword) {
        return Member.builder()
                .name(this.name)
                .email(this.email)
                .password(encodedPassword)
                .role(Role.USER)
                .build();
    }
}
