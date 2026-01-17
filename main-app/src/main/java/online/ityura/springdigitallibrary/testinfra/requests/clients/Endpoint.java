package online.ityura.springdigitallibrary.testinfra.requests.clients;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.ityura.springdigitallibrary.dto.BaseDto;
import online.ityura.springdigitallibrary.dto.request.RegisterRequest;
import online.ityura.springdigitallibrary.dto.response.AdminUserResponse;
import online.ityura.springdigitallibrary.dto.response.RegisterResponse;

@Getter
@AllArgsConstructor
public enum Endpoint {
    AUTH_REGISTER("/auth/register", RegisterRequest.class, RegisterResponse.class),
    ADMIN_USERS("/admin/users", BaseDto.class, AdminUserResponse.class);

    private final String relativePath;
    private final Class<? extends BaseDto> requestDto;
    private final Class<? extends BaseDto> responseDto;
}
