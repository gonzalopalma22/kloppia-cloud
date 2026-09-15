package com.klopp.apunte_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Resumen ───────────────────────────────────────────────────────────────

    public String generarResumen(byte[] pdfBytes) {
        String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

        Map<String, Object> request = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text",
                        "Eres un asistente académico experto en resumir apuntes universitarios. " +
                        "Resume el siguiente apunte en español, de forma clara y detallada. " +
                        "El resumen debe explicar los conceptos principales con sus ideas clave, " +
                        "no solo listar títulos o temas. " +
                        "Organiza el contenido por secciones temáticas con una breve introducción general al inicio. " +
                        "El resumen debe ser útil para estudiar, con explicaciones que tengan sentido por sí solas. " +
                        "Responde únicamente en español, sin importar el idioma del documento original."
                    ),
                    Map.of("inline_data", Map.of(
                        "mime_type", "application/pdf",
                        "data", base64Pdf
                    ))
                ))
            )
        );

        return llamarGemini(request);
    }

    // ── Flashcards ────────────────────────────────────────────────────────────

    public List<Map<String, String>> generarFlashcards(String resumen, int cantidad) {
        if (cantidad < 1 || cantidad > 20) {
            throw new IllegalArgumentException("La cantidad de flashcards debe estar entre 1 y 20");
        }

        String prompt = String.format(
            """
            Eres un asistente académico. A partir del siguiente resumen universitario,
            genera exactamente %d flashcards para estudiar.

            Reglas estrictas:
            - Cada flashcard debe tener una pregunta concreta y una respuesta clara y concisa.
            - Las preguntas deben cubrir conceptos clave, definiciones o relaciones importantes.
            - No repitas preguntas similares.
            - Responde ÚNICAMENTE con un JSON válido, sin texto adicional, sin markdown, sin bloques de código.
            - Formato exacto:
            [
              {"pregunta": "...", "respuesta": "..."},
              {"pregunta": "...", "respuesta": "..."}
            ]

            Resumen:
            %s
            """,
            cantidad, resumen
        );

        Map<String, Object> request = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            )
        );

        String texto = llamarGemini(request);
        return parsearFlashcards(texto);
    }

    // ── Chat ──────────────────────────────────────────────────────────────────

    public String chat(String resumen, List<Map<String, String>> historial, String pregunta) {
        List<Map<String, Object>> contents = new ArrayList<>();

       
        contents.add(Map.of(
            "role", "user",
            "parts", List.of(Map.of("text",
                "Eres un asistente académico que ayuda a estudiantes con sus apuntes universitarios. " +
                "Responde sobre el contenido del apunte o temas directamente relacionados con él. " +
                "Puedes explicar conceptos de otra manera, responder dudas, dar ejemplos, " +
                "mejorar o condensar el resumen, siempre basándote en el contenido del apunte. " +
                "Si te preguntan algo que no tiene ninguna relación con el apunte, " +
                "indícalo amablemente y redirige al contenido. " +
                "Responde siempre en español, de forma clara y concisa.\n\n" +
                "Resumen del apunte:\n" + resumen
            ))
        ));

        contents.add(Map.of(
            "role", "model",
            "parts", List.of(Map.of("text",
                "Entendido, estoy listo para ayudarte con este apunte."
            ))
        ));

        // Agregar historial validando que no haya roles consecutivos iguales
        String rolAnterior = "model";
        for (Map<String, String> mensaje : historial) {
            String role    = mensaje.get("role");
            String content = mensaje.get("content");

            if (role == null || content == null || content.isBlank()) continue;
            if (role.equals(rolAnterior)) continue;

            contents.add(Map.of(
                "role", role,
                "parts", List.of(Map.of("text", content))
            ));
            rolAnterior = role;
        }

        
        contents.add(Map.of(
            "role", "user",
            "parts", List.of(Map.of("text", pregunta))
        ));

        Map<String, Object> request = Map.of("contents", contents);
        return llamarGemini(request);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String llamarGemini(Map<String, Object> request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        String urlConKey = apiUrl + "?key=" + apiKey;
        ResponseEntity<String> response = restTemplate.postForEntity(urlConKey, entity, String.class);

        return extraerTexto(response.getBody());
    }

    private String extraerTexto(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("candidates")
                       .get(0)
                       .path("content")
                       .path("parts")
                       .get(0)
                       .path("text")
                       .asText();
        } catch (Exception e) {
            throw new RuntimeException("Error al procesar respuesta de Gemini");
        }
    }

    private List<Map<String, String>> parsearFlashcards(String texto) {
        try {
            String limpio = texto
                .replaceAll("(?s)```json", "")
                .replaceAll("```", "")
                .trim();

            JsonNode array = objectMapper.readTree(limpio);
            if (!array.isArray()) {
                throw new RuntimeException("Respuesta de Gemini no es un array JSON");
            }

            return objectMapper.convertValue(
                array,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al parsear flashcards: " + e.getMessage());
        }
    }
}