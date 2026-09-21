package com.lealtixservice.controller;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.entity.Allergy;
import com.lealtixservice.service.AllergyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "Allergy", description = "Catálogo de alergias")
@RestController
@RequestMapping("/api/allergies")
public class AllergyController {

    @Autowired
    private AllergyService allergyService;

    @Operation(summary = "Lista todas las alergias registradas (catálogo)")
    @GetMapping
    public ResponseEntity<GenericResponse> getAll() {
        try {
            List<Map<String, Object>> items = new ArrayList<>();
            for (Allergy a : allergyService.findAll()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", a.getId());
                m.put("name", a.getName());
                items.add(m);
            }
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new GenericResponse(200, "SUCCESS", items));
        } catch (Exception e) {
            log.error("Error retrieving allergies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, e.getMessage(), null));
        }
    }
}