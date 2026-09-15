package com.klopp.apunte_service.controller;

import com.klopp.apunte_service.dto.ApunteResponseDTO;
import com.klopp.apunte_service.dto.ChatRequestDTO;
import com.klopp.apunte_service.dto.ChatResponseDTO;
import com.klopp.apunte_service.dto.FlashcardDTO;
import com.klopp.apunte_service.service.ApunteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/materias/{materiaId}/apuntes")
@RequiredArgsConstructor
public class ApunteController {

    private final ApunteService apunteService;

    // ── Apuntes ───────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApunteResponseDTO> crear(
            @PathVariable Long materiaId,
            @RequestParam("titulo") String titulo,
            @RequestParam("archivo") MultipartFile archivo,
            Authentication authentication) throws IOException {
        String userId = authentication.getName();
        return ResponseEntity.ok(apunteService.crear(titulo, archivo, materiaId, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApunteResponseDTO> editar(
            @PathVariable Long materiaId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(apunteService.editarTitulo(id, userId, body.get("titulo")));
    }

    @GetMapping
    public ResponseEntity<List<ApunteResponseDTO>> listar(
            @PathVariable Long materiaId,
            Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(apunteService.listarPorMateria(materiaId, userId));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<ApunteResponseDTO>> buscar(
            @PathVariable Long materiaId,
            @RequestParam String titulo,
            Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(apunteService.buscarPorTitulo(materiaId, userId, titulo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApunteResponseDTO> obtener(
            @PathVariable Long materiaId,
            @PathVariable Long id,
            Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(apunteService.obtenerPorId(id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(
            @PathVariable Long materiaId,
            @PathVariable Long id,
            Authentication authentication) {
        String userId = authentication.getName();
        apunteService.eliminar(id, userId);
        return ResponseEntity.ok("Apunte eliminado correctamente");
    }

    // ── Flashcards ────────────────────────────────────────────────────────────

    @GetMapping("/{id}/flashcards")
    public ResponseEntity<List<FlashcardDTO>> generarFlashcards(
            @PathVariable Long materiaId,
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int cantidad,
            Authentication authentication) {

        if (cantidad < 1 || cantidad > 20) {
            return ResponseEntity.badRequest().build();
        }

        String userId = authentication.getName();
        return ResponseEntity.ok(apunteService.generarFlashcards(id, userId, cantidad));
    }

    // ── Chat ──────────────────────────────────────────────────────────────────

    @GetMapping("/{id}/chat/historial")
    public ResponseEntity<List<Map<String, String>>> obtenerHistorial(
            @PathVariable Long materiaId,
            @PathVariable Long id,
            Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(apunteService.obtenerHistorialChat(id, userId));
    }

    @PostMapping("/{id}/chat")
    public ResponseEntity<ChatResponseDTO> chat(
            @PathVariable Long materiaId,
            @PathVariable Long id,
            @RequestBody ChatRequestDTO body,
            Authentication authentication) {

        if (body.pregunta() == null || body.pregunta().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String userId = authentication.getName();
        return ResponseEntity.ok(apunteService.chat(id, userId, body.pregunta()));
    }
}