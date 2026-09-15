package com.klopp.apunte_service.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApunteDTO {

    @NotBlank(message = "El título es obligatorio")
    private String titulo;
} 