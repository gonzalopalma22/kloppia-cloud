package com.klopp.materia_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MateriaDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;
} 
