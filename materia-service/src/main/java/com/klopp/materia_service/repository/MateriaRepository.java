package com.klopp.materia_service.repository;

import com.klopp.materia_service.model.Materia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MateriaRepository extends JpaRepository<Materia, Long> {

    List<Materia> findByUsuarioIdOrderByCreatedAtDesc(String usuarioId);

    List<Materia> findByUsuarioIdAndNombreContainingIgnoreCaseOrderByCreatedAtDesc(
            String usuarioId, String nombre);
}