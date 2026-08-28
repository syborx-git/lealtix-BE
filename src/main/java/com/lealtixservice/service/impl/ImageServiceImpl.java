package com.lealtixservice.service.impl;


import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.lealtixservice.dto.ImageDTO;
import com.lealtixservice.entity.AppUser;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.repository.AppUserRepository;
import com.lealtixservice.repository.TenantRepository;
import com.lealtixservice.service.ImageService;
import com.lealtixservice.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class ImageServiceImpl implements ImageService {

    private final Cloudinary cloudinary;

    @Value("${lealtix.upload-dir:/app/uploads}")
    private String uploadDir;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private TenantRepository tenantRepository;

    public ImageServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String uploadImage(MultipartFile file, String type, ImageDTO tenant) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede ser nulo o vacío");
        }
        AppUser user = appUserRepository.findByEmail(tenant.getEmail());
        if (user == null) {
            throw new IllegalArgumentException("Usuario no encontrado con el email proporcionado");
        }
        Tenant tenantEntity = new Tenant();
        tenantEntity.setAppUser(user);
        tenantEntity.setActive(true);
        tenantEntity.setCreatedAt(LocalDateTime.now());
        tenantEntity = tenantRepository.save(tenantEntity);
        String logoName = "logo_" + tenantEntity.getId();

        String folder = type.equals("logo") ? "lealtix/logos" : "lealtix/products";
        Map params = ObjectUtils.asMap(
                "folder", folder,
                "public_id", logoName,
                "unique_filename", false,
                "overwrite", true,
                "transformation", new Transformation()
                        .width(800)
                        .height(800)
                        .crop("limit")
                        .quality("auto")
                        .fetchFormat("auto")
        );

        Map<String, Object> uploadResult = cloudinary.uploader().upload(file, params);
        log.info("Resultado de la subida: " + uploadResult);

        Object url = uploadResult.get("secure_url");
        if (url == null) {
            throw new IOException("No se pudo obtener la URL de la imagen subida");
        }
        return url.toString();
    }

    @Override
    public String uploadImageBase64(ImageDTO imageDTO) throws IOException {
            validateImageDTO(imageDTO);

            AppUser user = appUserRepository.findByEmail(imageDTO.getEmail());
            Tenant tenantEntity = null;
            if (user != null) {
                tenantEntity = tenantRepository.findByAppUserId(user.getId()).orElse(null);
                if (tenantEntity == null) {
                    tenantEntity = createAndSaveTenant(imageDTO, user);
                }
            }
            String logoName = "logo_" + (tenantEntity != null ? tenantEntity.getSlug() : "default");
            String folder = getFolderByType(imageDTO.getType());

            byte[] imageBytes = Base64.getDecoder().decode(imageDTO.getBase64File());
            String url = resolveImageUrl(logoName, folder, imageBytes);

            if (tenantEntity != null) {
                tenantEntity.setLogoUrl(url);
                tenantEntity.setUIDTenant("UID-" + tenantEntity.getId());
                tenantRepository.save(tenantEntity);
            }

            return url;
        }

    @Override
    public String uploadProdImageBase64(ImageDTO imageDTO) throws IOException {
        validateImageDTO(imageDTO);

        Tenant tenant;
        Optional<Tenant> tenantOptional = tenantRepository.findById(imageDTO.getTenantId());
        if (tenantOptional.isPresent()) {
            tenant = tenantOptional.get();
        }else{
            throw new IllegalArgumentException("Tenant no encontrado con el ID proporcionado");
        }

        String imgName = "img_" + StringUtils.createSlug(imageDTO.getProductName(), tenant.getId());
        String folder = getFolderByType(imageDTO.getType());

        byte[] imageBytes = Base64.getDecoder().decode(imageDTO.getBase64File());
        return resolveImageUrl(imgName, folder, imageBytes);
    }

    @Override
    public String uploadPromoImageBase64(ImageDTO imageDTO) throws IOException {
        validateImageDTO(imageDTO);

        Tenant tenant;
        Optional<Tenant> tenantOptional = tenantRepository.findById(imageDTO.getTenantId());
        if (tenantOptional.isPresent()) {
            tenant = tenantOptional.get();
        }else{
            throw new IllegalArgumentException("Tenant no encontrado con el ID proporcionado");
        }

        String imgName = "promo_" + StringUtils.createSlug(imageDTO.getPromoName(), tenant.getId());
        String folder = getFolderByType(imageDTO.getType());

        byte[] imageBytes = Base64.getDecoder().decode(imageDTO.getBase64File());
        return resolveImageUrl(imgName, folder, imageBytes);
    }

    private void validateImageDTO(ImageDTO imageDTO) {
            if (imageDTO.getBase64File() == null || imageDTO.getBase64File().isEmpty()) {
                throw new IllegalArgumentException("La imagen en base64 no puede estar vacía");
            }
        }

        private Tenant createAndSaveTenant(ImageDTO imageDTO, AppUser user) {
            Tenant tenant = new Tenant();
            tenant.setAppUser(user);
            tenant.setActive(true);
            tenant.setNombreNegocio(imageDTO.getNombreNegocio());
            tenant.setSlogan(imageDTO.getSlogan());
            tenant.setCreatedAt(LocalDateTime.now());
            tenant = tenantRepository.save(tenant);

            String slug = StringUtils.createSlug(imageDTO.getNombreNegocio(), tenant.getId());
            tenant.setSlug(slug);
            return tenantRepository.save(tenant);
        }

        private String getFolderByType(String type) {
            if (type == null) return "lealtix/products";
            switch (type.toLowerCase()) {
                case "logo":
                    return "lealtix/logos";
                case "product":
                    return "lealtix/products";
                case "promotion":
                    return "lealtix/promos";
                default:
                    return "lealtix/products";
            }
        }

        private Map<String, Object> uploadToCloudinary(byte[] imageBytes, String folder, String logoName) throws IOException {
            Map params = ObjectUtils.asMap(
                "folder", folder,
                "public_id", logoName,
                "unique_filename", false,
                "overwrite", true,
                "transformation", new Transformation()
                        .width(800)
                        .height(800)
                        .crop("limit")
                        .quality("auto")
                        .fetchFormat("auto")
            );
            return cloudinary.uploader().upload(imageBytes, params);
        }

        /**
         * Intenta subir a Cloudinary; si falla (claves no configuradas o red),
         * guarda la imagen localmente y devuelve una URL relativa servida por el backend.
         */
        private String resolveImageUrl(String name, String folder, byte[] imageBytes) throws IOException {
            try {
                Map<String, Object> uploadResult = uploadToCloudinary(imageBytes, folder, name);
                Object url = uploadResult.get("secure_url");
                if (url != null && !url.toString().isBlank()) {
                    return url.toString();
                }
            } catch (Exception e) {
                log.warn("Cloudinary no disponible (claves sin configurar). Guardando imagen localmente. {}", e.getMessage());
            }
            return saveImageLocally(imageBytes, name);
        }

        private String saveImageLocally(byte[] imageBytes, String name) throws IOException {
            String safeName = name.replaceAll("[^a-zA-Z0-9._-]", "_") + "_" + System.currentTimeMillis() + ".png";
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);
            Path file = dir.resolve(safeName);
            Files.write(file, imageBytes);
            log.info("Imagen guardada localmente: {}", file.toAbsolutePath());
            return "/api/images/file/" + safeName;
        }
}
