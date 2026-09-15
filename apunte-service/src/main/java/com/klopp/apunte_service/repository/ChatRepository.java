package com.klopp.apunte_service.repository;

import com.klopp.apunte_service.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    List<Chat> findByApunteIdAndUsuarioIdOrderByCreatedAtAsc(
        Long apunteId, Long usuarioId
    );

    void deleteByApunteIdAndUsuarioId(Long apunteId, Long usuarioId);
}