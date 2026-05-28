package com.acltabontabon.modscope.scan;

import java.util.List;

public final class BinaryHintScorer {

    // Vendor/API context patterns that indicate the hit is not game content
    private static final List<String> VENDOR_NOISE_PATTERNS = List.of(
        "d3d12_", "d3d11_", "d3d10_", "d3d9_", "dxgi_", "id3d12", "id3d11", "d3d12ddi",
        "video_encoder", "video_decode", "video_process", "video_coding",
        "checkfeaturesupport", "driver reported", "device does not have",
        "nvencodeapi", "nvencode", "nvdecode", "nvidia dlss", "streamline ",
        "physx", "px_", "physicsengine",
        "steamapi_", "steam_api", "isa_gamepad",
        "vk_video", "vkformat", "vulkan validation",
        "directstorage", "idirectstorage"
    );

    // Context patterns that strongly suggest actual game content
    private static final List<String> GAME_CONTENT_PATTERNS = List.of(
        ".pc_rp", ".pc_packagedefinition", ".rpkg", ".uasset", ".pak",
        "chunk", "scene", "level", "asset", "resource", "world",
        "config/", "settings/", "gamedata/", "content/"
    );

    // Context patterns that look like config assignments
    private static final List<String> ASSIGNMENT_PATTERNS = List.of("=", ": ", "true", "false", "enabled", "disabled");

    private BinaryHintScorer() {}

    public static BinaryHintRelevance score(FileCategory sourceCategory, String context) {
        if (FileClassifier.isVendorLibrary(sourceCategory)) {
            return BinaryHintRelevance.NOISE;
        }

        String lower = context.toLowerCase();

        // Check context for vendor API noise regardless of file category
        for (String pattern : VENDOR_NOISE_PATTERNS) {
            if (lower.contains(pattern)) return BinaryHintRelevance.NOISE;
        }

        if (sourceCategory == FileCategory.ARCHIVE || sourceCategory == FileCategory.PACKAGE_DEFINITION) {
            for (String indicator : GAME_CONTENT_PATTERNS) {
                if (lower.contains(indicator)) return BinaryHintRelevance.HIGH;
            }
            for (String indicator : ASSIGNMENT_PATTERNS) {
                if (lower.contains(indicator)) return BinaryHintRelevance.HIGH;
            }
            return BinaryHintRelevance.MEDIUM;
        }

        if (sourceCategory == FileCategory.OTHER) {
            return BinaryHintRelevance.LOW;
        }

        if (sourceCategory == FileCategory.UNKNOWN_LARGE) {
            return BinaryHintRelevance.LOW;
        }

        return BinaryHintRelevance.NOISE;
    }

    public static String suppressionReason(FileCategory sourceCategory, String context) {
        if (FileClassifier.isVendorLibrary(sourceCategory)) {
            return switch (sourceCategory) {
                case GRAPHICS_LIBRARY -> "Direct3D/graphics runtime library";
                case NVIDIA_LIBRARY -> "NVIDIA library";
                case STREAMLINE_LIBRARY -> "NVIDIA Streamline library";
                case PHYSX_LIBRARY -> "PhysX physics library";
                case DIRECTSTORAGE_LIBRARY -> "DirectStorage runtime library";
                case STEAM_LIBRARY -> "Steam API library";
                case SYSTEM_COMPAT_LIBRARY -> "System compatibility library (MSVC CRT)";
                default -> "Vendor/runtime library";
            };
        }

        String lower = context.toLowerCase();
        if (lower.contains("d3d12_") || lower.contains("d3d11_")) return "Direct3D API/debug string";
        if (lower.contains("id3d12") || lower.contains("d3d12ddi")) return "Direct3D 12 interface/DDI string";
        if (lower.contains("dxgi_")) return "DXGI API string";
        if (lower.contains("video_encoder") || lower.contains("video_decode") || lower.contains("video_process"))
            return "Graphics video codec API (not game content)";
        if (lower.contains("nvidia dlss") || lower.contains("nvencodeapi")) return "NVIDIA DLSS/encode API string";
        if (lower.contains("streamline ")) return "NVIDIA Streamline API string";
        if (lower.contains("physx") || lower.contains("px_")) return "PhysX API/debug string";
        if (lower.contains("steamapi_") || lower.contains("steam_api")) return "Steam API string";
        if (lower.contains("checkfeaturesupport")) return "Direct3D feature query string";
        if (lower.contains("directstorage")) return "DirectStorage API string";
        return "Vendor/API string";
    }

    public static String confidenceExplanation(FileCategory sourceCategory, String context) {
        if (sourceCategory == FileCategory.PACKAGE_DEFINITION) {
            return "Found in Glacier package definition file";
        }
        if (sourceCategory == FileCategory.ARCHIVE) {
            String lower = context.toLowerCase();
            for (String indicator : GAME_CONTENT_PATTERNS) {
                if (lower.contains(indicator)) return "Found in archive sample near game resource path";
            }
            for (String indicator : ASSIGNMENT_PATTERNS) {
                if (lower.contains(indicator)) return "Found in archive sample near config assignment";
            }
            return "Found in game archive";
        }
        return null;
    }
}
