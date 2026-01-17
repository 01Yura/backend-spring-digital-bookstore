package online.ityura.springdigitallibrary.testinfra.requests.interfaces;

import io.restassured.response.ValidatableResponse;
import online.ityura.springdigitallibrary.dto.BaseDto;

public interface CrudEndpointInterface {
    ValidatableResponse post(BaseDto dto);
    ValidatableResponse delete (Long id);
    ValidatableResponse get();
    ValidatableResponse put(BaseDto dto);
}
