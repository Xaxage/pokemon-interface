package com.xaxage.be.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserJpaRepository extends JpaRepository<UserEntity, String> {
}
