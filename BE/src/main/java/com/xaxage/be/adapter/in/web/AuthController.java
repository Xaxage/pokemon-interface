package com.xaxage.be.adapter.in.web;

import com.xaxage.be.adapter.in.web.dto.AccessTokenResponse;
import com.xaxage.be.adapter.in.web.dto.RegisterUserRequest;
import com.xaxage.be.adapter.in.web.dto.SignInRequest;
import com.xaxage.be.adapter.in.web.dto.UserResponse;
import com.xaxage.be.application.port.in.RegisterUserUseCase;
import com.xaxage.be.application.port.in.SignInUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final RegisterUserUseCase registerUser;
    private final SignInUseCase signIn;

    AuthController(RegisterUserUseCase registerUser, SignInUseCase signIn) {
        this.registerUser = registerUser;
        this.signIn = signIn;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return UserResponse.from(registerUser.register(request.username(), request.password()));
    }

    @PostMapping("/sign-in")
    AccessTokenResponse signIn(@Valid @RequestBody SignInRequest request) {
        return AccessTokenResponse.from(signIn.signIn(request.username(), request.password()));
    }
}
