package com.xaxage.be.adapter.out.persistence;

import com.xaxage.be.application.port.out.UserRepositoryPort;
import com.xaxage.be.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;

    UserPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsById(username);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return repository.findById(username).map(UserEntity::toUser);
    }

    @Override
    public User save(User user) {
        return repository.save(UserEntity.from(user)).toUser();
    }
}
