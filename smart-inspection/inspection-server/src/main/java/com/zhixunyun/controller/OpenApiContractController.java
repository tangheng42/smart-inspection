package com.zhixunyun.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Knife4j 4.5 的多文档分组按 JSON 解析响应。
 * 本适配器将唯一维护的 YAML 接口契约转换为 JSON，避免同时维护两份文档。
 */
@Hidden
@RestController
public class OpenApiContractController {

    private static final String CONTRACT_PATH = "static/openapi/zhixunyun-openapi.yaml";
    private static final Object NO_EXAMPLE = new Object();

    private final Map<String, Object> openApiContract;

    public OpenApiContractController() throws IOException {
        ClassPathResource contractResource = new ClassPathResource(CONTRACT_PATH);

        try (Reader reader = new InputStreamReader(
                contractResource.getInputStream(), StandardCharsets.UTF_8)) {
            Object document = new Yaml().load(reader);
            if (!(document instanceof Map<?, ?> documentMap)) {
                throw new IllegalStateException("OpenAPI 接口契约不是合法的对象结构");
            }

            Map<String, Object> parsedContract = new LinkedHashMap<>();
            documentMap.forEach((key, value) -> parsedContract.put(String.valueOf(key), value));
            addResponseExamples(parsedContract);
            this.openApiContract = parsedContract;
        }
    }

    @GetMapping(value = "/openapi/zhixunyun-openapi.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getOpenApiContract() {
        return openApiContract;
    }

    /**
     * Knife4j 4.5 在响应含有 schema 时会忽略 MediaType.example/examples，
     * 转而读取 schema.example。因此这里保留标准的 MediaType.example，
     * 同时同步兼容示例，并根据字段 example 补齐缺失的完整 JSON。
     */
    private static void addResponseExamples(Map<String, Object> document) {
        Map<String, Object> paths = asMap(document.get("paths"));

        for (Object pathValue : paths.values()) {
            Map<String, Object> pathItem = asMap(pathValue);
            for (Object operationValue : pathItem.values()) {
                Map<String, Object> operation = asMap(operationValue);
                Map<String, Object> responses = asMap(operation.get("responses"));

                for (Object responseValue : responses.values()) {
                    Map<String, Object> response = resolveNode(asMap(responseValue), document);
                    Map<String, Object> content = asMap(response.get("content"));

                    for (Object mediaTypeValue : content.values()) {
                        Map<String, Object> mediaType = asMap(mediaTypeValue);
                        Map<String, Object> existingExamples = asMap(mediaType.get("examples"));
                        Object example = !existingExamples.isEmpty()
                                ? firstNamedExampleValue(existingExamples)
                                : mediaType.containsKey("example") ? mediaType.get("example") : NO_EXAMPLE;
                        mediaType.remove("examples");

                        if (example == NO_EXAMPLE) {
                            example = buildExample(mediaType.get("schema"), document, new HashSet<>());
                        }
                        if (example != NO_EXAMPLE) {
                            mediaType.put("example", example);
                            addKnife4jSchemaExample(mediaType, example);
                        }
                    }
                }
            }
        }
    }

    private static Object firstNamedExampleValue(Map<String, Object> examples) {
        if (examples.isEmpty()) {
            return NO_EXAMPLE;
        }
        Map<String, Object> firstExample = asMap(examples.values().iterator().next());
        return firstExample.containsKey("value") ? firstExample.get("value") : NO_EXAMPLE;
    }

    /**
     * Knife4j 4.5 的文档页在响应含有 schema 时只从 schema.example 读取示例，
     * MediaType.example 会被忽略，因此同步一份兼容示例。
     */
    private static void addKnife4jSchemaExample(Map<String, Object> mediaType, Object example) {
        if (example == NO_EXAMPLE) {
            return;
        }
        Map<String, Object> schema = asMap(mediaType.get("schema"));
        if (!schema.isEmpty()) {
            schema.put("example", example);
        }
    }

    private static Object buildExample(
            Object schemaValue,
            Map<String, Object> document,
            Set<String> resolvingRefs) {

        Map<String, Object> schema = asMap(schemaValue);
        if (schema.isEmpty()) {
            return NO_EXAMPLE;
        }
        if (schema.containsKey("example")) {
            return schema.get("example");
        }
        if (schema.containsKey("default")) {
            return schema.get("default");
        }

        String ref = schema.get("$ref") instanceof String value ? value : null;
        if (ref != null) {
            if (!resolvingRefs.add(ref)) {
                return NO_EXAMPLE;
            }
            Object example = buildExample(resolveRef(ref, document), document, resolvingRefs);
            resolvingRefs.remove(ref);
            return example;
        }

        Object allOfExample = mergeCompositeExample(schema.get("allOf"), document, resolvingRefs);
        if (allOfExample != NO_EXAMPLE) {
            return allOfExample;
        }

        for (String compositeKey : List.of("oneOf", "anyOf")) {
            List<Object> alternatives = asList(schema.get(compositeKey));
            if (!alternatives.isEmpty()) {
                return buildExample(alternatives.get(0), document, resolvingRefs);
            }
        }

        Map<String, Object> properties = asMap(schema.get("properties"));
        if (!properties.isEmpty()) {
            Map<String, Object> objectExample = new LinkedHashMap<>();
            properties.forEach((name, propertySchema) -> {
                Object propertyExample = buildExample(propertySchema, document, new HashSet<>(resolvingRefs));
                if (propertyExample != NO_EXAMPLE) {
                    objectExample.put(name, propertyExample);
                }
            });
            return objectExample;
        }

        List<Object> enumValues = asList(schema.get("enum"));
        if (!enumValues.isEmpty()) {
            return enumValues.get(0);
        }

        String type = schema.get("type") instanceof String value ? value : null;
        if ("array".equals(type)) {
            Object itemExample = buildExample(schema.get("items"), document, resolvingRefs);
            List<Object> arrayExample = new ArrayList<>();
            if (itemExample != NO_EXAMPLE) {
                arrayExample.add(itemExample);
            }
            return arrayExample;
        }
        if ("object".equals(type)) {
            return new LinkedHashMap<>();
        }
        if ("boolean".equals(type)) {
            return true;
        }
        if ("integer".equals(type)) {
            return 0;
        }
        if ("number".equals(type)) {
            return 0.0;
        }
        if ("string".equals(type)) {
            return switch (String.valueOf(schema.get("format"))) {
                case "date" -> "2026-08-28";
                case "date-time" -> "2026-08-28T09:30:00+08:00";
                case "uri" -> "https://example.com/resource";
                default -> "string";
            };
        }
        return NO_EXAMPLE;
    }

    private static Object mergeCompositeExample(
            Object compositeValue,
            Map<String, Object> document,
            Set<String> resolvingRefs) {

        List<Object> schemas = asList(compositeValue);
        if (schemas.isEmpty()) {
            return NO_EXAMPLE;
        }

        Map<String, Object> merged = new LinkedHashMap<>();
        Object lastExample = NO_EXAMPLE;
        for (Object childSchema : schemas) {
            Object childExample = buildExample(childSchema, document, new HashSet<>(resolvingRefs));
            if (childExample instanceof Map<?, ?> childMap) {
                childMap.forEach((key, value) -> merged.put(String.valueOf(key), value));
            } else if (childExample != NO_EXAMPLE) {
                lastExample = childExample;
            }
        }
        return !merged.isEmpty() ? merged : lastExample;
    }

    private static Map<String, Object> resolveNode(
            Map<String, Object> node,
            Map<String, Object> document) {
        Object ref = node.get("$ref");
        return ref instanceof String value ? asMap(resolveRef(value, document)) : node;
    }

    private static Object resolveRef(String ref, Map<String, Object> document) {
        if (!ref.startsWith("#/")) {
            return Map.of();
        }

        Object current = document;
        for (String rawSegment : ref.substring(2).split("/")) {
            String segment = rawSegment.replace("~1", "/").replace("~0", "~");
            current = asMap(current).get(segment);
            if (current == null) {
                return Map.of();
            }
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return new LinkedHashMap<>();
        }
        return (Map<String, Object>) source;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object value) {
        return value instanceof List<?> list ? (List<Object>) list : List.of();
    }
}
