package com.zhixunyun.controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiContractControllerTest {

    @Test
    void shouldGenerateCompleteSuccessResponseExamples() throws IOException {
        Map<String, Object> contract = new OpenApiContractController().getOpenApiContract();

        Map<String, Object> loginExample = responseExample(
                contract, "/api/auth/login", "post", "200");
        Map<String, Object> loginData = asMap(loginExample.get("data"));

        assertThat(loginExample)
                .containsEntry("code", 0)
                .containsEntry("message", "success");
        assertThat(loginData)
                .containsEntry("userId", 1)
                .containsEntry("username", "admin")
                .containsEntry("roleCode", "ADMIN")
                .containsKey("token");

        Map<String, Object> pageExample = responseExample(
                contract, "/api/users", "get", "200");
        Map<String, Object> pageData = asMap(pageExample.get("data"));
        assertThat(pageData)
                .containsEntry("pageNum", 1)
                .containsEntry("pageSize", 10)
                .containsKey("records");

        assertEveryJsonResponseHasExample(contract);
    }

    private static void assertEveryJsonResponseHasExample(Map<String, Object> contract) {
        Map<String, Object> paths = asMap(contract.get("paths"));

        paths.forEach((path, pathValue) -> asMap(pathValue).forEach((method, operationValue) -> {
            Map<String, Object> responses = asMap(asMap(operationValue).get("responses"));
            responses.forEach((status, responseValue) -> {
                Map<String, Object> response = resolveResponse(contract, asMap(responseValue));
                Map<String, Object> json = asMap(asMap(response.get("content")).get("application/json"));
                if (!json.isEmpty()) {
                    assertThat(json)
                            .as("%s %s response %s", method.toUpperCase(), path, status)
                            .containsKey("example")
                            .doesNotContainKey("examples");
                    assertThat(asMap(json.get("schema")))
                            .as("%s %s response %s Knife4j schema example", method.toUpperCase(), path, status)
                            .containsKey("example");
                }
            });
        }));
    }

    private static Map<String, Object> responseExample(
            Map<String, Object> contract,
            String path,
            String method,
            String status) {
        Map<String, Object> operation = asMap(asMap(asMap(contract.get("paths")).get(path)).get(method));
        Map<String, Object> response = resolveResponse(
                contract, asMap(asMap(operation.get("responses")).get(status)));
        Map<String, Object> json = asMap(asMap(response.get("content")).get("application/json"));
        return asMap(json.get("example"));
    }

    private static Map<String, Object> resolveResponse(
            Map<String, Object> contract,
            Map<String, Object> response) {
        Object ref = response.get("$ref");
        if (!(ref instanceof String value)) {
            return response;
        }
        String responseName = value.substring(value.lastIndexOf('/') + 1);
        return asMap(asMap(asMap(contract.get("components")).get("responses")).get(responseName));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
