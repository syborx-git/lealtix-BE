package com.lealtixservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Sirve la landing (SPA de lealtix-main) desde el backend en el mismo origen.
 * Cualquier ruta bajo /landing-page resuelve al index.html de la app Angular
 * (fallback de enrutado cliente). Los assets (main.js, styles.css, etc.) se
 * sirven como recursos estáticos desde la carpeta de build de lealtix-main.
 */
@Controller
@Tag(name = "Landing", description = "Landing page servida por el backend")
public class LandingPageController {

    @GetMapping(value = {
            "/landing-page",
            "/landing-page/{slug:[^\\.]*}",
            "/landing-page/{slug:[^\\.]*}/**"
    })
    public String landingPage() {
        return "forward:/index.html";
    }
}
