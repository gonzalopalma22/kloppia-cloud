package com.klopp.apunte_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApunteResponseDTO {

    private Long id;
    private String titulo;
    private String resumen;
    private String nombreArchivo;
    private Long materiaId;
    private Long usuarioId;
    private LocalDateTime createdAt;
}