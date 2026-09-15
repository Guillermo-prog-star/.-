package com.integrityfamily.hud.permissions;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

public class AdaptiveHudArchitectureTest {

    @Test
    public void testFamilyHudViewDoesNotDependOnProfessionalPackages() throws IOException {
        Path path = Paths.get("src/main/java/com/integrityfamily/hud/dto/FamilyHudView.java");
        assertTrue(Files.exists(path), "FamilyHudView.java source file should exist");

        String content = Files.readString(path);
        
        // Assert that FamilyHudView does not reference professional or clinical components/DTOs
        assertFalse(content.contains("import com.integrityfamily.hud.dto.professional"), 
                "FamilyHudView must not import professional components");
        assertFalse(content.contains("ProfessionalHudView"), 
                "FamilyHudView must not reference ProfessionalHudView");
    }

    @Test
    public void testCommonDtoPackageDoesNotImportClinicalOrProfessionalPackages() throws IOException {
        Path path = Paths.get("src/main/java/com/integrityfamily/hud/dto");
        assertTrue(Files.exists(path), "DTO directory should exist");

        try (Stream<Path> paths = Files.walk(path)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.getFileName().toString().endsWith(".java"))
                 .filter(p -> !p.getFileName().toString().equals("ProfessionalHudView.java"))
                 .forEach(p -> {
                     try {
                         String content = Files.readString(p);
                         assertFalse(content.contains("import com.integrityfamily.hud.dto.professional"),
                                 p.getFileName() + " must not import professional components");
                     } catch (IOException e) {
                         fail(e.getMessage());
                     }
                 });
        }
    }
}
