package com.docsshare_web_backend.documents.enums;

public enum DocumentFileType {
    PDF("pdf"),
    DOCX("docx"),
    SHEET("sheet"),
    SLIDE("slide"),
    TEXT("text"),
    IMAGE("image"),
    AUDIO("audio"),
    VIDEO("video");

    private final String value;

    DocumentFileType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DocumentFileType fromExtension(String ext) {
        String lower = ext.toLowerCase();
        if (lower.equals("pdf")) return PDF;
        if (lower.equals("docx")) return DOCX;
        if (lower.equals("txt")) return TEXT;
        if (lower.matches("jpg|jpeg|png|gif|webp")) return IMAGE;
        if (lower.matches("mp4|webm|mov|avi")) return VIDEO;
        if (lower.matches("mp3|wav|ogg")) return AUDIO;
        if (lower.matches("xls|xlsx|csv")) return SHEET;
        if (lower.matches("ppt|pptx")) return SLIDE;
        throw new IllegalArgumentException("Unsupported file extension: " + ext);
    }
}

