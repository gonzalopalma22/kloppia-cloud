package com.klopp.apunte_service.controller;

import com.klopp.apunte_service.dto.ApunteResponseDTO;
import com.klopp.apunte_service.dto.ChatRequestDTO;
import com.klopp.apunte_service.dto.ChatResponseDTO;
import com.klopp.apunte_service.dto.FlashcardDTO;
import com.klopp.apunte_service.service.ApunteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
            @RequestHeader("X-User-Id") Long userId) throws IOException {
        return ResponseEntity.ok(apunteService.crear(titulo, archivo, materiaId, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApunteResponseDTO> editar(
            @PathVariable Long materiaId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(apunteService.editarTitulo(id, userId, body.get("titulo")));
    }

    @GetMapping
    public ResponseEntity<List<ApunteResponseDTO>> listar(
            @PathVariable Long materiaId,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(apunteService.listarPorMateria(materiaId, userId));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<ApunteResponseDTO>> buscar(
            @PathVariable Long materiaId,
            @RequestParam String titulo,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(apunteService.buscarPorTitulo(materiaId, userId, titulo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApunteResponseDTO> obtener(
            @PathVariable Long materiaId,
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(apunteService.obtenerPorId(id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(
            @PathVariable Long materiaId,
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        apunteService.eliminar(id, userId);
        return ResponseEntity.ok("Apunte eliminado correctamente");
    }

    // ── Flashcards ────────────────────────────────────────────────────────────

    @GetMapping("/{id}/flashcards")
    public ResponseEntity<List<FlashcardDTO>> generarFlashcards(
            @PathVariable Long materiaId,
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int cantidad,
            @RequestHeader("X-User-Id") Long userId) {

        if (cantidad < 1 || cantidad > 20) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(apunteService.generarFlashcards(id, userId, cantidad));
    }

    // ── Chat ──────────────────────────────────────────────────────────────────

    @GetMapping("/{id}/chat/historial")
    public ResponseEntity<List<Map<String, String>>> obtenerHistorial(
            @PathVariable Long materiaId,
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(apunteService.obtenerHistorialChat(id, userId));
    }

    @PostMapping("/{id}/chat")
    public ResponseEntity<ChatResponseDTO> chat(
            @PathVariable Long materiaId,
            @PathVariable Long id,
            @RequestBody ChatRequestDTO body,
            @RequestHeader("X-User-Id") Long userId) {

        if (body.pregunta() == null || body.pregunta().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        
        return ResponseEntity.ok(apunteService.chat(id, userId, body.pregunta()));
    }
}