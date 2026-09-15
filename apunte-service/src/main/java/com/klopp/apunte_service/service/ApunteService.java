package com.klopp.apunte_service.service;

import com.klopp.apunte_service.dto.ApunteResponseDTO;
import com.klopp.apunte_service.dto.ChatResponseDTO;
import com.klopp.apunte_service.dto.FlashcardDTO;
import com.klopp.apunte_service.model.Apunte;
import com.klopp.apunte_service.model.Chat;
import com.klopp.apunte_service.repository.ApunteRepository;
import com.klopp.apunte_service.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApunteService {

    private final ApunteRepository apunteRepository;
    private final ChatRepository chatRepository;
    private final GeminiService geminiService;

    // ── Apuntes ───────────────────────────────────────────────────────────────

    public ApunteResponseDTO crear(String titulo, MultipartFile archivo,
            Long materiaId, Long usuarioId) throws IOException {
        byte[] pdfBytes = archivo.getBytes();
        String resumen = geminiService.generarResumen(pdfBytes);
        Apunte apunte = new Apunte();
        apunte.setTitulo(titulo);
        apunte.setResumen(resumen);
        apunte.setNombreArchivo(archivo.getOriginalFilename());
        apunte.setMateriaId(materiaId);
        apunte.setUsuarioId(usuarioId);
        return mapToResponse(apunteRepository.save(apunte));
    }

    public ApunteResponseDTO editarTitulo(Long id, Long usuarioId, String titulo) {
        Apunte apunte = obtenerApunteValidado(id, usuarioId);
        apunte.setTitulo(titulo);
        return mapToResponse(apunteRepository.save(apunte));
    }

    public List<ApunteResponseDTO> listarPorMateria(Long materiaId, Long usuarioId) {
        return apunteRepository
                .findByMateriaIdAndUsuarioIdOrderByCreatedAtDesc(materiaId, usuarioId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ApunteResponseDTO> buscarPorTitulo(Long materiaId, Long usuarioId, String titulo) {
        return apunteRepository
                .findByMateriaIdAndUsuarioIdAndTituloContainingIgnoreCaseOrderByCreatedAtDesc(
                        materiaId, usuarioId, titulo)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ApunteResponseDTO obtenerPorId(Long id, Long usuarioId) {
        return mapToResponse(obtenerApunteValidado(id, usuarioId));
    }

    public void eliminar(Long id, Long usuarioId) {
        obtenerApunteValidado(id, usuarioId);
        chatRepository.deleteByApunteIdAndUsuarioId(id, usuarioId);
        apunteRepository.deleteById(id);
    }

    // ── Flashcards ────────────────────────────────────────────────────────────

    public List<FlashcardDTO> generarFlashcards(Long id, Long usuarioId, int cantidad) {
        Apunte apunte = obtenerApunteValidado(id, usuarioId);

        if (apunte.getResumen() == null || apunte.getResumen().isBlank()) {
            throw new RuntimeException("El apunte no tiene resumen para generar flashcards");
        }

        List<Map<String, String>> flashcards = geminiService.generarFlashcards(apunte.getResumen(), cantidad);

        return flashcards.stream()
                .map(f -> new FlashcardDTO(f.get("pregunta"), f.get("respuesta")))
                .collect(Collectors.toList());
    }

    // ── Chat ──────────────────────────────────────────────────────────────────

    public List<Map<String, String>> obtenerHistorialChat(Long id, Long usuarioId) {
        obtenerApunteValidado(id, usuarioId);
        return chatRepository
                .findByApunteIdAndUsuarioIdOrderByCreatedAtAsc(id, usuarioId)
                .stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .collect(Collectors.toList());
    }

    public ChatResponseDTO chat(Long id, Long usuarioId, String pregunta) {
        Apunte apunte = obtenerApunteValidado(id, usuarioId);

        if (apunte.getResumen() == null || apunte.getResumen().isBlank()) {
            throw new RuntimeException("El apunte no tiene resumen para iniciar el chat");
        }

        if (pregunta == null || pregunta.isBlank()) {
            throw new IllegalArgumentException("La pregunta no puede estar vacía");
        }

        // Leer historial desde la DB — fuente de verdad única
        List<Map<String, String>> historial = chatRepository
                .findByApunteIdAndUsuarioIdOrderByCreatedAtAsc(id, usuarioId)
                .stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .collect(Collectors.toList());

        String respuesta = geminiService.chat(apunte.getResumen(), historial, pregunta);

        // Guardar mensaje del usuario
        Chat mensajeUsuario = new Chat();
        mensajeUsuario.setApunteId(id);
        mensajeUsuario.setUsuarioId(usuarioId);
        mensajeUsuario.setRole("user");
        mensajeUsuario.setContent(pregunta);
        chatRepository.save(mensajeUsuario);

        // Guardar respuesta del modelo
        Chat mensajeModelo = new Chat();
        mensajeModelo.setApunteId(id);
        mensajeModelo.setUsuarioId(usuarioId);
        mensajeModelo.setRole("model");
        mensajeModelo.setContent(respuesta);
        chatRepository.save(mensajeModelo);

        return new ChatResponseDTO(respuesta);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Apunte obtenerApunteValidado(Long id, Long usuarioId) {
        Apunte apunte = apunteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Apunte no encontrado"));
        if (!apunte.getUsuarioId().equals(usuarioId)) {
            throw new RuntimeException("No tienes permiso para acceder a este apunte");
        }
        return apunte;
    }

    private ApunteResponseDTO mapToResponse(Apunte apunte) {
        return new ApunteResponseDTO(
                apunte.getId(),
                apunte.getTitulo(),
                apunte.getResumen(),
                apunte.getNombreArchivo(),
                apunte.getMateriaId(),
                apunte.getUsuarioId(),
                apunte.getCreatedAt()
        );
    }
}