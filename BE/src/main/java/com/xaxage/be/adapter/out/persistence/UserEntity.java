package com.xaxage.be.adapter.out.persistence;

import com.xaxage.be.domain.model.User;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
class UserEntity {

    @Id
    private String username;

    private String password;

    protected UserEntity() {
    }

    static UserEntity from(User user) {
        var entity = new UserEntity();
        entity.username = user.username();
        entity.password = user.password();
        return entity;
    }

    User toUser() {
        return new User(username, password);
    }
}
