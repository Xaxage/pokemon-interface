package com.xaxage.be.adapter.in.web;

import com.xaxage.be.adapter.in.web.dto.RegisterUserRequest;
import com.xaxage.be.adapter.in.web.dto.UserResponse;
import com.xaxage.be.application.port.in.RegisterUserUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final RegisterUserUseCase registerUser;

    AuthController(RegisterUserUseCase registerUser) {
        this.registerUser = registerUser;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return UserResponse.from(registerUser.register(request.username(), request.password()));
    }
}
