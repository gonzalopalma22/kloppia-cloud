package com.klopp.apunte_service.repository;

import com.klopp.apunte_service.model.Apunte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApunteRepository extends JpaRepository<Apunte, Long> {

    List<Apunte> findByMateriaIdAndUsuarioIdOrderByCreatedAtDesc(
            Long materiaId, Long usuarioId);

    List<Apunte> findByMateriaIdAndUsuarioIdAndTituloContainingIgnoreCaseOrderByCreatedAtDesc(
            Long materiaId, Long usuarioId, String titulo);
}