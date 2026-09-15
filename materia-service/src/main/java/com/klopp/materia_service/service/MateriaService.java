package com.klopp.materia_service.service;

import com.klopp.materia_service.dto.MateriaDTO;
import com.klopp.materia_service.dto.MateriaResponseDTO;
import com.klopp.materia_service.model.Materia;
import com.klopp.materia_service.repository.MateriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MateriaService {

    private final MateriaRepository materiaRepository;

    public MateriaResponseDTO crear(MateriaDTO dto, String usuarioId) {
        Materia materia = new Materia();
        materia.setNombre(dto.getNombre());
        materia.setDescripcion(dto.getDescripcion());
        materia.setUsuarioId(usuarioId);
        Materia guardada = materiaRepository.save(materia);
        return mapToResponse(guardada);
    }

    public MateriaResponseDTO editar(Long id, String usuarioId, MateriaDTO dto) {
        Materia materia = materiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Materia no encontrada"));
        if (!materia.getUsuarioId().equals(usuarioId)) {
            throw new RuntimeException("No tienes permiso para editar esta materia");
        }
        materia.setNombre(dto.getNombre());
        materia.setDescripcion(dto.getDescripcion());
        return mapToResponse(materiaRepository.save(materia));
    }

    public List<MateriaResponseDTO> listarPorUsuario(String usuarioId) {
        return materiaRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<MateriaResponseDTO> buscarPorNombre(String usuarioId, String nombre) {
        return materiaRepository
                .findByUsuarioIdAndNombreContainingIgnoreCaseOrderByCreatedAtDesc(usuarioId, nombre)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public MateriaResponseDTO obtenerPorId(Long id, String usuarioId) {
        Materia materia = materiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Materia no encontrada"));
        if (!materia.getUsuarioId().equals(usuarioId)) {
            throw new RuntimeException("No tienes permiso para ver esta materia");
        }
        return mapToResponse(materia);
    }

    public void eliminar(Long id, String usuarioId) {
        Materia materia = materiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Materia no encontrada"));
        if (!materia.getUsuarioId().equals(usuarioId)) {
            throw new RuntimeException("No tienes permiso para eliminar esta materia");
        }
        materiaRepository.deleteById(id);
    }

    private MateriaResponseDTO mapToResponse(Materia materia) {
        return new MateriaResponseDTO(
                materia.getId(),
                materia.getNombre(),
                materia.getDescripcion(),
                materia.getUsuarioId(),
                materia.getCreatedAt()
        );
    }
}