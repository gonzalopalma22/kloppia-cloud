package com.klopp.apunte_service.service;

import com.klopp.apunte_service.dto.ApunteResponseDTO;
import com.klopp.apunte_service.model.Apunte;
import com.klopp.apunte_service.repository.ApunteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApunteServiceTest {

    @Mock
    private ApunteRepository apunteRepository;

    @Mock
    private GeminiService geminiService;

    @Mock
    private MultipartFile archivo;

    @InjectMocks
    private ApunteService apunteService;

    private Apunte apunte;

    private static final String USUARIO_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
    private static final String OTRO_USUARIO_ID = "otro-usuario-id";

    @BeforeEach
    void setUp() {
        apunte = new Apunte();
        apunte.setId(1L);
        apunte.setTitulo("Mi apunte");
        apunte.setResumen("Resumen generado por IA");
        apunte.setNombreArchivo("apunte.pdf");
        apunte.setMateriaId(1L);
        apunte.setUsuarioId(USUARIO_ID);
        apunte.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void crear_exitoso() throws IOException {
        when(archivo.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(archivo.getOriginalFilename()).thenReturn("apunte.pdf");
        when(geminiService.generarResumen(any(byte[].class))).thenReturn("Resumen generado por IA");
        when(apunteRepository.save(any(Apunte.class))).thenReturn(apunte);

        ApunteResponseDTO response = apunteService.crear("Mi apunte", archivo, 1L, USUARIO_ID);

        assertNotNull(response);
        assertEquals("Mi apunte", response.getTitulo());
        assertEquals("Resumen generado por IA", response.getResumen());
        verify(apunteRepository, times(1)).save(any(Apunte.class));
    }

    @Test
    void listarPorMateria_retornaLista() {
        when(apunteRepository.findByMateriaIdAndUsuarioIdOrderByCreatedAtDesc(1L, USUARIO_ID))
                .thenReturn(List.of(apunte));

        List<ApunteResponseDTO> response = apunteService.listarPorMateria(1L, USUARIO_ID);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("Mi apunte", response.get(0).getTitulo());
    }

    @Test
    void obtenerPorId_exitoso() {
        when(apunteRepository.findById(1L)).thenReturn(Optional.of(apunte));

        ApunteResponseDTO response = apunteService.obtenerPorId(1L, USUARIO_ID);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Mi apunte", response.getTitulo());
    }

    @Test
    void obtenerPorId_noEncontrado_lanzaExcepcion() {
        when(apunteRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> apunteService.obtenerPorId(99L, USUARIO_ID));

        assertEquals("Apunte no encontrado", ex.getMessage());
    }

    @Test
    void obtenerPorId_sinPermiso_lanzaExcepcion() {
        when(apunteRepository.findById(1L)).thenReturn(Optional.of(apunte));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> apunteService.obtenerPorId(1L, OTRO_USUARIO_ID));

        assertEquals("No tienes permiso para acceder a este apunte", ex.getMessage());
    }

    @Test
    void eliminar_exitoso() {
        when(apunteRepository.findById(1L)).thenReturn(Optional.of(apunte));

        apunteService.eliminar(1L, USUARIO_ID);

        verify(apunteRepository, times(1)).deleteById(1L);
    }

    @Test
    void eliminar_sinPermiso_lanzaExcepcion() {
        when(apunteRepository.findById(1L)).thenReturn(Optional.of(apunte));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> apunteService.eliminar(1L, OTRO_USUARIO_ID));

        assertEquals("No tienes permiso para acceder a este apunte", ex.getMessage());
    }
}