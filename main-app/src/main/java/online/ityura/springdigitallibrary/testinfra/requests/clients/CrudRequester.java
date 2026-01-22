package online.ityura.springdigitallibrary.testinfra.requests.clients;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import online.ityura.springdigitallibrary.dto.BaseDto;
import online.ityura.springdigitallibrary.testinfra.requests.interfaces.CrudEndpointInterface;

import static io.restassured.RestAssured.given;

/*
Класс CrudRequester — это универсальный класс для выполнения CRUD-запросов (GET, PUT, POST, DELETE и т.д.) на определенный эндпоинт,
с заранее настроенными RequestSpecification и ResponseSpecification.
CrudRequester — это фасад над REST Assured, который:
 - знает, куда обращаться (Endpoint endpoint)
 - знает, с какими настройками (RequestSpecification, ResponseSpecification)
 - знает, как выполнять стандартные операции (get(), put() и т.д.)
Возвращает ValidatableResponse из RestAssured. Это объект из REST Assured, который позволяет:
 - работать с “сырым” ответом или нестандартно извлекать данные из ответа (например токен из хедера)
 - делать ассерты над телом/статусом/заголовками ответа
 - проверять JSON, XML, текст и т.д.
* */

public class CrudRequester extends HttpClient implements CrudEndpointInterface {
    public CrudRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification, Endpoint endpoint) {
        super(requestSpecification, responseSpecification, endpoint);
    }
//    Вариант реализации когда тело может быть пустое
//    @Override
//    public ValidatableResponse post(BaseDto dto) {
//            var request = given()
//                    .spec(requestSpecification);
//
//            if (dto != null) request.body(dto);
//
//            return request
//                    .when()
//                    .post(endpoint.getRelativePath())
//                    .then()
//                    .spec(responseSpecification);
//    }

    @Override
    public ValidatableResponse post(BaseDto dto) {
        return given()
                .spec(requestSpecification)
                .body(dto)
                .when()
                .post(endpoint.getRelativePath())
                .then()
                .spec(responseSpecification);
    }
    
    /**
     * POST запрос с path variables
     * @param dto тело запроса
     * @param pathVariables значения для замены в пути (например, bookId)
     * @return ValidatableResponse
     */
    public ValidatableResponse post(BaseDto dto, Object... pathVariables) {
        return given()
                .spec(requestSpecification)
                .body(dto)
                .when()
                .post(endpoint.getPath(pathVariables))
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse delete(Long id) {
        return null;
    }

    @Override
    public ValidatableResponse get() {
        return given()
                .spec(requestSpecification)
                .when()
                .get(endpoint.getRelativePath())
                .then()
                .spec(responseSpecification);
    }
    
    /**
     * GET запрос с path variables
     * @param pathVariables значения для замены в пути (например, bookId)
     * @return ValidatableResponse
     */
    public ValidatableResponse get(Object... pathVariables) {
        return given()
                .spec(requestSpecification)
                .when()
                .get(endpoint.getPath(pathVariables))
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse put(BaseDto dto) {
        return null;
    }
}
