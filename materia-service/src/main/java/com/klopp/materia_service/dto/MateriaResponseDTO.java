package com.klopp.materia_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MateriaResponseDTO {

    private Long id;
    private String nombre;
    private String descripcion;
    private String usuarioId;
    private LocalDateTime createdAt;
}