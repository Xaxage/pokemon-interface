package com.xaxage.be.adapter.out.persistence;

import com.xaxage.be.domain.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
class UserEntity {

    @Id
    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    protected UserEntity() {
    }

    static UserEntity from(User user) {
        var entity = new UserEntity();
        entity.username = user.username();
        entity.passwordHash = user.passwordHash();
        return entity;
    }

    User toUser() {
        return new User(username, passwordHash);
    }
}
