package com.amancay.infrastructure.adapter.out.ai;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.amancay.application.port.out.ProductRecommenderPort;
import com.amancay.domain.exception.RecommenderUnavailableException;
import com.amancay.domain.model.RecommendationCandidate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Recomendador sobre cualquier API con formato chat/completions de OpenAI. Por defecto usa Groq
 * (capa gratuita, sin tarjeta); cambiando {@code AI_BASE_URL} y {@code AI_MODEL} sirve para xAI,
 * OpenRouter, Ollama, etc. Sin {@code AI_API_KEY} el caso de uso usa el fallback por categorias.
 */
@Component
class LlmProductRecommender implements ProductRecommenderPort {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    private static final String SYSTEM_PROMPT = """
            Sos el motor de recomendaciones de Amancay, una tienda de equipo outdoor y de montana.
            Recibis los productos favoritos de un cliente y un catalogo de candidatos.
            Elegi los candidatos que mas probablemente le interesen: complementarios a sus favoritos
            (para la misma actividad) o similares en categoria y uso. Ordenalos de mas a menos relevante.
            Usa unicamente ids que aparezcan en la lista de candidatos.
            Responde solo con JSON con esta forma: {"productIds": ["<id>", "..."]}
            """;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    LlmProductRecommender(
            @Value("${amancay.ai.base-url:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${amancay.ai.api-key:}") String apiKey,
            @Value("${amancay.ai.model:openai/gpt-oss-120b}") String model,
            @Value("${amancay.ai.timeout:PT15S}") Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public List<UUID> rank(List<RecommendationCandidate> favorites, List<RecommendationCandidate> candidates,
            int limit) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RecommenderUnavailableException("AI_API_KEY is not configured");
        }

        ChatRequest request = new ChatRequest(model, 0.2, Map.of("type", "json_object"), List.of(
                new ChatMessage("system", SYSTEM_PROMPT),
                new ChatMessage("user", userPrompt(favorites, candidates, limit))));
        ChatResponse response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);
        } catch (RestClientException e) {
            throw new RecommenderUnavailableException("AI request failed: " + e.getMessage(), e);
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().getFirst().message() == null) {
            throw new RecommenderUnavailableException("AI returned an empty response");
        }
        return extractIds(response.choices().getFirst().message().content());
    }

    private static String userPrompt(List<RecommendationCandidate> favorites, List<RecommendationCandidate> candidates,
            int limit) {
        return """
                Favoritos del cliente:
                %s

                Candidatos (id | nombre | categorias | descripcion):
                %s

                Devolve como maximo %d ids.
                """.formatted(lines(favorites), lines(candidates), limit);
    }

    private static String lines(List<RecommendationCandidate> products) {
        return products.stream()
                .map(p -> "- %s | %s | %s | %s".formatted(p.id(), p.name(), String.join(", ", p.categoryNames()),
                        truncate(p.shortDescription())))
                .collect(Collectors.joining("\n"));
    }

    private static String truncate(String text) {
        if (text == null) {
            return "";
        }
        String singleLine = text.replaceAll("\\s+", " ").trim();
        return singleLine.length() <= MAX_DESCRIPTION_LENGTH ? singleLine
                : singleLine.substring(0, MAX_DESCRIPTION_LENGTH) + "...";
    }

    // En lugar de parsear el JSON a ciegas tomamos los UUID en orden: tolera texto extra o
    // bloques ```json``` alrededor. El caso de uso descarta los que no son candidatos.
    private static List<UUID> extractIds(String content) {
        if (content == null) {
            return List.of();
        }
        Matcher matcher = UUID_PATTERN.matcher(content);
        List<UUID> ids = new ArrayList<>();
        while (matcher.find()) {
            ids.add(UUID.fromString(matcher.group()));
        }
        return ids;
    }

    record ChatRequest(String model, double temperature,
            @JsonProperty("response_format") Map<String, String> responseFormat,
            List<ChatMessage> messages) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(ChatMessage message) {
    }
}
