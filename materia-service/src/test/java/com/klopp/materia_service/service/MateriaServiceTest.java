package com.klopp.materia_service.service;

import com.klopp.materia_service.dto.MateriaDTO;
import com.klopp.materia_service.dto.MateriaResponseDTO;
import com.klopp.materia_service.model.Materia;
import com.klopp.materia_service.repository.MateriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MateriaServiceTest {

    @Mock
    private MateriaRepository materiaRepository;

    @InjectMocks
    private MateriaService materiaService;

    private Materia materia;

    private static final String USUARIO_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"; // simula un "sub" de Azure AD

    @BeforeEach
    void setUp() {
        materia = new Materia();
        materia.setId(1L);
        materia.setNombre("Matemáticas");
        materia.setDescripcion("Descripción de prueba");
        materia.setUsuarioId(USUARIO_ID);
        materia.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void crear_exitoso() {
        MateriaDTO dto = new MateriaDTO();
        dto.setNombre("Matemáticas");
        dto.setDescripcion("Descripción de prueba");

        when(materiaRepository.save(any(Materia.class))).thenReturn(materia);

        MateriaResponseDTO response = materiaService.crear(dto, USUARIO_ID);

        assertNotNull(response);
        assertEquals("Matemáticas", response.getNombre());
        assertEquals(USUARIO_ID, response.getUsuarioId());
        verify(materiaRepository, times(1)).save(any(Materia.class));
    }

    @Test
    void listarPorUsuario_retornaLista() {
        when(materiaRepository.findByUsuarioIdOrderByCreatedAtDesc(USUARIO_ID))
                .thenReturn(List.of(materia));

        List<MateriaResponseDTO> response = materiaService.listarPorUsuario(USUARIO_ID);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("Matemáticas", response.get(0).getNombre());
    }

    @Test
    void obtenerPorId_exitoso() {
        when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));

        MateriaResponseDTO response = materiaService.obtenerPorId(1L, USUARIO_ID);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Matemáticas", response.getNombre());
    }

    @Test
    void obtenerPorId_noEncontrado_lanzaExcepcion() {
        when(materiaRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> materiaService.obtenerPorId(99L, USUARIO_ID));

        assertEquals("Materia no encontrada", ex.getMessage());
    }

    @Test
    void obtenerPorId_sinPermiso_lanzaExcepcion() {
        when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> materiaService.obtenerPorId(1L, "otro-usuario-id"));

        assertEquals("No tienes permiso para ver esta materia", ex.getMessage());
    }

    @Test
    void eliminar_exitoso() {
        when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));

        materiaService.eliminar(1L, USUARIO_ID);

        verify(materiaRepository, times(1)).deleteById(1L);
    }

    @Test
    void eliminar_sinPermiso_lanzaExcepcion() {
        when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> materiaService.eliminar(1L, "otro-usuario-id"));

        assertEquals("No tienes permiso para eliminar esta materia", ex.getMessage());
    }
}