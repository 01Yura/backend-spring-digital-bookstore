package online.ityura.springdigitallibrary.testinfra.requests.steps;

import lombok.AllArgsConstructor;
import lombok.Data;
import online.ityura.springdigitallibrary.dto.request.LoginRequest;
import online.ityura.springdigitallibrary.dto.request.RegisterRequest;
import online.ityura.springdigitallibrary.dto.response.LoginResponse;
import online.ityura.springdigitallibrary.dto.response.RegisterResponse;
import online.ityura.springdigitallibrary.testinfra.database.DataBaseSteps;
import online.ityura.springdigitallibrary.testinfra.generators.RandomDtoGeneratorWithFaker;
import online.ityura.springdigitallibrary.testinfra.requests.clients.CrudRequester;
import online.ityura.springdigitallibrary.testinfra.requests.clients.Endpoint;
import online.ityura.springdigitallibrary.testinfra.specs.RequestSpecs;
import online.ityura.springdigitallibrary.testinfra.specs.ResponseSpecs;

/**
 * UserSteps - Фасад для работы с пользователями в тестах
 * 
 * Предоставляет высокоуровневые методы для выполнения сложных операций с пользователями:
 * - Регистрация и верификация пользователя в одном вызове
 * - Скрывает детали реализации (API вызовы, работу с БД)
 * 
 * Использует паттерн Фасад для упрощения тестового кода.
 */
public class UserSteps {

    /**
     * Класс-обертка для хранения данных регистрации и ответа
     * Использует Lombok для генерации геттеров и сеттеров
     */
    @Data
    @AllArgsConstructor
    public static class RegisteredUser {
        private RegisterRequest registerRequest;
        private RegisterResponse registerResponse;
    }

    /**
     * Класс-обертка для хранения данных регистрации, ответа регистрации и ответа логина
     * Использует Lombok для генерации геттеров и сеттеров
     */
    @Data
    @AllArgsConstructor
    public static class RegisteredAndLoggedInUser {
        private RegisterRequest registerRequest;
        private RegisterResponse registerResponse;
        private LoginResponse loginResponse;
    }

    private UserSteps() {
        // Utility class - не должен быть инстанциирован
    }

    /**
     * Регистрирует случайного пользователя и автоматически верифицирует его email.
     * 
     * Этот метод является фасадом, который скрывает сложную логику:
     * 1. Генерирует случайные данные для регистрации
     * 2. Выполняет API вызов для регистрации пользователя
     * 3. Верифицирует пользователя в базе данных (устанавливает isVerified = true)
     * 
     * @return RegisteredUser с данными зарегистрированного и верифицированного пользователя
     * @throws RuntimeException если регистрация или верификация не удались
     */
    public static RegisteredUser registerAndVerifyRandomUser() {
        // Генерируем случайные данные для регистрации
        RegisterRequest registerRequest = RandomDtoGeneratorWithFaker.generateRandomDtoObject(RegisterRequest.class);
        
        // Регистрируем пользователя через API
        RegisterResponse registerResponse = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.statusCode(201),
                Endpoint.AUTH_REGISTER)
                .post(registerRequest)
                .extract().as(RegisterResponse.class);
        
        // Верифицируем пользователя в базе данных
        DataBaseSteps.setUserVerified(registerResponse.getUserId().longValue());
        
        return new RegisteredUser(registerRequest, registerResponse);
    }

    /**
     * Регистрирует пользователя с указанными данными и автоматически верифицирует его email.
     * 
     * Этот метод является фасадом, который скрывает сложную логику:
     * 1. Выполняет API вызов для регистрации пользователя
     * 2. Верифицирует пользователя в базе данных (устанавливает isVerified = true)
     * 
     * @param registerRequest данные для регистрации пользователя
     * @return RegisteredUser с данными зарегистрированного и верифицированного пользователя
     * @throws RuntimeException если регистрация или верификация не удались
     */
    public static RegisteredUser registerAndVerifyUser(RegisterRequest registerRequest) {
        // Регистрируем пользователя через API
        RegisterResponse registerResponse = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.statusCode(201),
                Endpoint.AUTH_REGISTER)
                .post(registerRequest)
                .extract().as(RegisterResponse.class);
        
        // Верифицируем пользователя в базе данных
        DataBaseSteps.setUserVerified(registerResponse.getUserId().longValue());
        
        return new RegisteredUser(registerRequest, registerResponse);
    }

    /**
     * Регистрирует случайного пользователя, автоматически верифицирует его email и логинится под ним.
     * 
     * Этот метод является фасадом, который скрывает сложную логику:
     * 1. Генерирует случайные данные для регистрации
     * 2. Выполняет API вызов для регистрации пользователя
     * 3. Верифицирует пользователя в базе данных (устанавливает isVerified = true)
     * 4. Выполняет API вызов для логина пользователя
     * 
     * @return RegisteredAndLoggedInUser с данными зарегистрированного, верифицированного и залогиненного пользователя
     * @throws RuntimeException если регистрация, верификация или логин не удались
     */
    public static RegisteredAndLoggedInUser registerVerifyAndLoginRandomUser() {
        // Генерируем случайные данные для регистрации
        RegisterRequest registerRequest = RandomDtoGeneratorWithFaker.generateRandomDtoObject(RegisterRequest.class);
        
        // Регистрируем пользователя через API
        RegisterResponse registerResponse = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.statusCode(201),
                Endpoint.AUTH_REGISTER)
                .post(registerRequest)
                .extract().as(RegisterResponse.class);
        
        // Верифицируем пользователя в базе данных
        DataBaseSteps.setUserVerified(registerResponse.getUserId().longValue());
        
        // Логинимся под пользователем
        LoginRequest loginRequest = LoginRequest.builder()
                .email(registerRequest.getEmail())
                .password(registerRequest.getPassword())
                .build();
        
        LoginResponse loginResponse = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.statusCode(200),
                Endpoint.AUTH_LOGIN)
                .post(loginRequest)
                .extract().as(LoginResponse.class);
        
        return new RegisteredAndLoggedInUser(registerRequest, registerResponse, loginResponse);
    }

    /**
     * Регистрирует пользователя с указанными данными, автоматически верифицирует его email и логинится под ним.
     * 
     * Этот метод является фасадом, который скрывает сложную логику:
     * 1. Выполняет API вызов для регистрации пользователя
     * 2. Верифицирует пользователя в базе данных (устанавливает isVerified = true)
     * 3. Выполняет API вызов для логина пользователя
     * 
     * @param registerRequest данные для регистрации пользователя
     * @return RegisteredAndLoggedInUser с данными зарегистрированного, верифицированного и залогиненного пользователя
     * @throws RuntimeException если регистрация, верификация или логин не удались
     */
    public static RegisteredAndLoggedInUser registerVerifyAndLoginUser(RegisterRequest registerRequest) {
        // Регистрируем пользователя через API
        RegisterResponse registerResponse = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.statusCode(201),
                Endpoint.AUTH_REGISTER)
                .post(registerRequest)
                .extract().as(RegisterResponse.class);
        
        // Верифицируем пользователя в базе данных
        DataBaseSteps.setUserVerified(registerResponse.getUserId().longValue());
        
        // Логинимся под пользователем
        LoginRequest loginRequest = LoginRequest.builder()
                .email(registerRequest.getEmail())
                .password(registerRequest.getPassword())
                .build();
        
        LoginResponse loginResponse = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.statusCode(200),
                Endpoint.AUTH_LOGIN)
                .post(loginRequest)
                .extract().as(LoginResponse.class);
        
        return new RegisteredAndLoggedInUser(registerRequest, registerResponse, loginResponse);
    }
}
