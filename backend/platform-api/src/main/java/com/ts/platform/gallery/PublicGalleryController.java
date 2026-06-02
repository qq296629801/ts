package com.ts.platform.gallery;

import com.ts.platform.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/gallery")
public class PublicGalleryController {

    private final PublicGalleryService publicGalleryService;

    public PublicGalleryController(PublicGalleryService publicGalleryService) {
        this.publicGalleryService = publicGalleryService;
    }

    @GetMapping("/public")
    public ApiResponse<Map<String, Object>> listPublic(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(publicGalleryService.listPublic(page, size));
    }
}
