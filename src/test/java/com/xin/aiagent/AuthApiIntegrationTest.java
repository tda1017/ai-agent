package com.xin.aiagent;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * 后端 API 集成测试：直接访问正在运行的本地服务。
 * 测试依赖于服务运行在 localhost:8080。
 */
public class AuthApiIntegrationTest {

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
    }

    @Test
    void register_then_login_success() {
        String username = "testuser_" + System.currentTimeMillis();
        String password = "Passw0rd!";
        String email = username + "@example.com";

        // 注册
        ValidatableResponse reg = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "username", username,
                        "password", password,
                        "email", email
                ))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.id", notNullValue());

        // 登录
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "username", username,
                        "password", password
                ))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.token", notNullValue())
                .body("data.user.username", equalTo(username));
    }

    @Test
    void login_invalid_password_should_fail() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "username", "non_exist_user",
                        "password", "wrong"
                ))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200) // 业务错误仍返回 200 + code != 200
                .body("code", anyOf(equalTo(1001), equalTo(1002))) // 账号不存在或密码错误
                .body("message", not(emptyString()));
    }
}
