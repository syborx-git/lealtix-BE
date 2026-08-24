package com.lealtixservice.controller;

import com.lealtixservice.dto.ImageDTO;
import com.lealtixservice.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Tag(name = "Image Controller", description = "Operaciones para la carga de imágenes")
@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Autowired
    private  ImageService imageService;

    @Value("${lealtix.upload-dir:/home/nexus/lealtix/uploads}")
    private String uploadDir;

    private String toAbsoluteUrl(HttpServletRequest request, String url) {
        if (url != null && url.startsWith("/api/")) {
            String proto = request.getHeader("X-Forwarded-Proto");
            String scheme = (proto != null && !proto.isBlank()) ? proto : "https";
            String host = request.getHeader("X-Forwarded-Host");
            String hostname = (host != null && !host.isBlank()) ? host : request.getServerName();
            return scheme + "://" + hostname + url;
        }
        return url;
    }

    @Operation(summary = "Subir imagen en base64", description = "Sube una imagen en base64 (Cloudinary o local) y retorna la URL.")
    @PostMapping(value = "/upload", consumes = "application/json")
    public ResponseEntity<String> uploadImage(@RequestBody ImageDTO imageDTO, HttpServletRequest request) {
        String urlLogo = null;
        try {
            if (imageDTO.getBase64File() == null || imageDTO.getBase64File().isEmpty()) {
                return ResponseEntity.badRequest().body("La imagen en base64 no puede estar vacía.");
            }
            if (imageDTO.getType() == null || imageDTO.getType().isEmpty()) {
                return ResponseEntity.badRequest().body("El tipo de imagen es obligatorio.");
            }
            urlLogo = imageService.uploadImageBase64(imageDTO);
        } catch (Exception e) {
            log.error("Error al subir la imagen: ", e);
            return ResponseEntity.status(500).body("Error al procesar la solicitud: " + e.getMessage());
        }
        return ResponseEntity.ok(toAbsoluteUrl(request, urlLogo));
    }

    @Operation(summary = "Subir imagen para producto en base64", description = "Sube una imagen para producto en base64 (Cloudinary o local) y retorna la URL.")
    @PostMapping(value = "/uploadImgProd", consumes = "application/json")
    public ResponseEntity<String> uploadImgProd(@RequestBody ImageDTO imageDTO, HttpServletRequest request) {
        String url = null;
        try {
            if (imageDTO.getBase64File() == null || imageDTO.getBase64File().isEmpty()) {
                return ResponseEntity.badRequest().body("La imagen en base64 no puede estar vacía.");
            }
            if (imageDTO.getType() == null || imageDTO.getType().isEmpty()) {
                return ResponseEntity.badRequest().body("El tipo de imagen es obligatorio.");
            }
            url = imageService.uploadProdImageBase64(imageDTO);
        } catch (Exception e) {
            log.error("Error al subir la imagen de producto: ", e);
            return ResponseEntity.status(500).body("Error al procesar la solicitud: " + e.getMessage());
        }
        return ResponseEntity.ok(toAbsoluteUrl(request, url));
    }

    @Operation(summary = "Subir imagen para promoción en base64", description = "Sube una imagen para promoción en base64 (Cloudinary o local) y retorna la URL.")
    @PostMapping(value = "/uploadImgPromo", consumes = "application/json")
    public ResponseEntity<String> uploadImgPromo(@RequestBody ImageDTO imageDTO, HttpServletRequest request) {
        String imageUrl = "";
        try {
            if (imageDTO.getBase64File() == null || imageDTO.getBase64File().isEmpty()) {
                return ResponseEntity.badRequest().body("La imagen en base64 no puede estar vacía.");
            }
            if (imageDTO.getType() == null || imageDTO.getType().isEmpty()) {
                return ResponseEntity.badRequest().body("El tipo de imagen es obligatorio.");
            }
            imageUrl = imageService.uploadPromoImageBase64(imageDTO);
        } catch (Exception e) {
            log.error("Error al subir la imagen de promoción: ", e);
            return ResponseEntity.status(500).body("Error al procesar la solicitud: " + e.getMessage());
        }
        return ResponseEntity.ok(toAbsoluteUrl(request, imageUrl));
    }

    @Operation(summary = "Servir imagen subida localmente", description = "Devuelve el archivo de imagen guardado en el servidor.")
    @GetMapping(value = "/file/{filename}")
    public ResponseEntity<byte[]> serveFile(@PathVariable String filename) {
        try {
            String safe = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path file = Paths.get(uploadDir).resolve(safe).normalize();
            if (!Files.exists(file)) {
                return ResponseEntity.notFound().build();
            }
            byte[] data = Files.readAllBytes(file);
            String type = MediaType.IMAGE_PNG_VALUE;
            String lower = filename.toLowerCase();
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) type = MediaType.IMAGE_JPEG_VALUE;
            else if (lower.endsWith(".gif")) type = MediaType.IMAGE_GIF_VALUE;
            else if (lower.endsWith(".svg")) type = "image/svg+xml";
            else if (lower.endsWith(".webp")) type = "image/webp";
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(type))
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(data);
        } catch (IOException e) {
            log.error("Error sirviendo imagen local: {}", filename, e);
            return ResponseEntity.status(500).build();
        }
    }
}
