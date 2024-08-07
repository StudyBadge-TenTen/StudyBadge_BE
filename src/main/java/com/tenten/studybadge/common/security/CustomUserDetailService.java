package com.tenten.studybadge.common.security;

import com.tenten.studybadge.common.exception.member.NotFoundMemberException;
import com.tenten.studybadge.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        return new CustomUserDetails(memberRepository.findById(Long.valueOf(username)).orElseThrow(NotFoundMemberException::new));


    }
}
