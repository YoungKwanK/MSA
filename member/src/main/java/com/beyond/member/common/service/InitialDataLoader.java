package com.beyond.member.common.service;

import com.beyond.member.member.domain.Member;
import com.beyond.member.member.domain.Role;
import com.beyond.member.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InitialDataLoader implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        if (memberRepository.findByEmail("admin@naver.com").isPresent()) {
            return;
        }

        Member member = Member.builder()
                .email("admin@naver.com")
                .password(passwordEncoder.encode("12341234"))
                .role(Role.ADMIN)
                .build();

        memberRepository.save(member);
    }
}
