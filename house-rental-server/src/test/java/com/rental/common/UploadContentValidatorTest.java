package com.rental.common;

import com.rental.exception.BusinessException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class UploadContentValidatorTest {
    @Test
    void rejectsRenamedTextAndMismatchedImage() throws Exception {
        byte[] text = "<html>untrusted</html>".getBytes(StandardCharsets.UTF_8);
        for (String extension : new String[]{".jpg", ".png", ".webp", ".doc", ".docx", ".pdf"}) {
            assertThrows(BusinessException.class, () -> UploadContentValidator.validate(text, extension));
        }
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", png);
        assertDoesNotThrow(() -> UploadContentValidator.validate(png.toByteArray(), ".png"));
        assertThrows(BusinessException.class, () -> UploadContentValidator.validate(png.toByteArray(), ".jpg"));
    }

    @Test
    void docxMustContainWordPartsAndCannotHideMacros() throws Exception {
        assertThrows(BusinessException.class, () -> UploadContentValidator.validate(zip("unrelated.txt"), ".docx"));
        assertDoesNotThrow(() -> UploadContentValidator.validate(
                zip("[Content_Types].xml", "_rels/.rels", "word/document.xml"), ".docx"));
        assertThrows(BusinessException.class, () -> UploadContentValidator.validate(
                zip("[Content_Types].xml", "_rels/.rels", "word/document.xml", "word/vbaProject.bin"), ".docx"));
    }

    @Test
    void docxCannotExpandBeyondBudget() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            byte[] block = new byte[1024 * 1024];
            for (int i = 0; i < 31; i++) zip.write(block);
            zip.closeEntry();
        }
        assertThrows(BusinessException.class, () -> UploadContentValidator.validate(bytes.toByteArray(), ".docx"));
    }

    private byte[] zip(String... names) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (String name : names) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write("<document/>".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
