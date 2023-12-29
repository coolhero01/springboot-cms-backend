package com.oneclicktech.spring.datajpa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.oneclicktech.spring.login.models.ERole;
import com.oneclicktech.spring.login.models.Role;
 

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
  Optional<Role> findByName(ERole name);
}
