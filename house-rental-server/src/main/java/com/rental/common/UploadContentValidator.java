package com.rental.common;

import com.rental.exception.BusinessException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class UploadContentValidator {
    private static final long MAX_PIXELS = 16_000_000;
    private static final long MAX_UNPACKED_BYTES = 30L * 1024 * 1024;

    private UploadContentValidator() {
    }

    public static void validate(byte[] content, String extension) {
        try {
            switch (extension) {
                case ".jpg", ".jpeg", ".png", ".gif" -> validateImage(content, extension);
                case ".webp" -> validateWebp(content);
                case ".pdf" -> {
                    require(ascii(content, 0, 5).equals("%PDF-"));
                    String end = new String(content, Math.max(0, content.length - 1024),
                            Math.min(content.length, 1024), StandardCharsets.ISO_8859_1);
                    require(end.stripTrailing().endsWith("%%EOF"));
                }
                case ".doc" -> {
                    // 旧版 Word 的 OLE 文件签名；格式校验不能替代恶意附件扫描。
                    require(content.length >= 512 && Arrays.equals(Arrays.copyOf(content, 8),
                            new byte[]{(byte) 0xd0, (byte) 0xcf, 0x11, (byte) 0xe0,
                                    (byte) 0xa1, (byte) 0xb1, 0x1a, (byte) 0xe1}));
                }
                case ".docx" -> validateDocx(content);
                default -> throw new BusinessException("文件类型不支持");
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new BusinessException("文件内容损坏或与扩展名不符");
        }
    }

    private static void validateImage(byte[] content, String extension) throws IOException {
        // 在解码像素之前检查尺寸，避免小体积文件展开为超大图片耗尽内存。
        try (MemoryCacheImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            require(readers.hasNext());
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                String expected = extension.equals(".jpg") ? "jpeg" : extension.substring(1);
                require(reader.getFormatName().equalsIgnoreCase(expected));
                int frames = reader.getNumImages(true);
                require(frames > 0 && frames <= 200);
                long pixels = 0;
                for (int frame = 0; frame < frames; frame++) {
                    int width = reader.getWidth(frame);
                    int height = reader.getHeight(frame);
                    pixels += (long) width * height;
                    require(width > 0 && height > 0 && pixels <= MAX_PIXELS);
                }
                require(reader.read(0) != null);
            } finally {
                reader.dispose();
            }
        }
    }

    private static void validateWebp(byte[] content) {
        // JDK 不自带 WebP 解码器；校验 RIFF 容器、块长度和画布尺寸，保留原格式支持。
        require(content.length >= 30 && ascii(content, 0, 4).equals("RIFF")
                && ascii(content, 8, 4).equals("WEBP") && littleEndian(content, 4, 4) + 8 == content.length);
        int offset = 12;
        boolean image = false;
        while (offset + 8 <= content.length) {
            String chunk = ascii(content, offset, 4);
            long size = littleEndian(content, offset + 4, 4);
            long next = offset + 8L + size + (size % 2);
            require(next <= content.length);
            int data = offset + 8;
            if (chunk.equals("VP8X")) {
                require(size == 10);
                checkPixels(littleEndian(content, data + 4, 3) + 1, littleEndian(content, data + 7, 3) + 1);
            } else if (chunk.equals("VP8 ")) {
                require(size >= 10 && (content[data] & 1) == 0
                        && (content[data + 3] & 255) == 0x9d && content[data + 4] == 1 && content[data + 5] == 0x2a);
                checkPixels(littleEndian(content, data + 6, 2) & 0x3fff, littleEndian(content, data + 8, 2) & 0x3fff);
                image = true;
            } else if (chunk.equals("VP8L")) {
                require(size >= 5 && content[data] == 0x2f);
                long bits = littleEndian(content, data + 1, 4);
                checkPixels((bits & 0x3fff) + 1, ((bits >> 14) & 0x3fff) + 1);
                image = true;
            } else if (chunk.equals("ANMF")) {
                require(size >= 24);
                checkPixels(littleEndian(content, data + 6, 3) + 1, littleEndian(content, data + 9, 3) + 1);
                String frameType = ascii(content, data + 16, 4);
                require(Set.of("VP8 ", "VP8L", "ALPH").contains(frameType));
                image = true;
            }
            offset = (int) next;
        }
        require(image && offset == content.length);
    }

    private static void validateDocx(byte[] content) throws IOException {
        Set<String> entries = new HashSet<>();
        long unpacked = 0;
        byte[] buffer = new byte[8192];
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                require(entries.add(name) && entries.size() <= 1000
                        && !name.startsWith("/") && !name.contains("..") && !name.contains("\\"));
                // docx 不应包含宏项目；宏文件不得通过改后缀混入普通合同。
                require(!name.toLowerCase(Locale.ROOT).contains("vbaproject"));
                int count;
                while ((count = zip.read(buffer)) != -1) {
                    unpacked += count;
                    require(unpacked <= MAX_UNPACKED_BYTES);
                }
            }
        }
        require(entries.contains("[Content_Types].xml") && entries.contains("word/document.xml")
                && entries.contains("_rels/.rels"));
    }

    private static String ascii(byte[] content, int offset, int length) {
        require(offset >= 0 && offset + length <= content.length);
        return new String(content, offset, length, StandardCharsets.ISO_8859_1);
    }

    private static long littleEndian(byte[] content, int offset, int length) {
        require(offset >= 0 && offset + length <= content.length);
        long value = 0;
        for (int i = 0; i < length; i++) value |= (long) (content[offset + i] & 255) << (i * 8);
        return value;
    }

    private static void checkPixels(long width, long height) {
        require(width > 0 && height > 0 && width * height <= MAX_PIXELS);
    }

    private static void require(boolean condition) {
        if (!condition) throw new BusinessException("文件内容损坏、超出安全限制或与扩展名不符");
    }
}
