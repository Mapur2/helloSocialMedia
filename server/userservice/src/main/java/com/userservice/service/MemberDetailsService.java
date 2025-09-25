package com.userservice.service;

import com.userservice.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MemberDetailsService implements UserDetailsService {

    @Autowired
    private UserService memberService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User member = memberService.getUserByEmailId(username);
        if (member == null)
            throw  new UsernameNotFoundException("Invalid email id");

        return new MemberDetails(member);
    }
}
