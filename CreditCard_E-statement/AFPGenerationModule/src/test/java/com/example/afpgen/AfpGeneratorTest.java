package com.example.afpgen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;

class AfpGeneratorTest {

    @Test
    void testGenerateAfp(@TempDir Path tempDir) throws Exception {
        AfpGenerator generator = new AfpGenerator();
        
        // Create dummy template.fo in tempDir for test
        // Note: The XSL-FO content here is simplified for the test.
        // It directly uses xsl:value-of which implies it's an XSLT.
        Path templateFo = tempDir.resolve("template.fo");
        String foContent = "<root xmlns=\"http://www.w3.org/1999/XSL/Format\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">" +
                           "<layout-master-set><simple-page-master master-name=\"A4\">" +
                           "<region-body margin=\"1in\"/></simple-page-master></layout-master-set>" +
                           "<page-sequence master-reference=\"A4\"><flow flow-name=\"xsl-region-body\">" +
                           "<block>Hello <xsl:value-of select=\"/data/name\"/></block>" + // Removed explicit fo:inline for simplicity in test string
                           "</flow></page-sequence></root>";
        Files.write(templateFo, foContent.getBytes());

        // Create dummy data.xml in tempDir for test
        Path dataXml = tempDir.resolve("data.xml");
        String xmlContent = "<data><name>Test User</name></data>";
        Files.write(dataXml, xmlContent.getBytes());

        Path outputAfp = tempDir.resolve("output.afp");

        // The AfpGenerator's constructor might throw an exception if FOPFactory can't be initialized
        // (e.g., if FOP configuration files are missing and it strictly requires them, though default usually works).
        // The main AfpGenerator class initializes FopFactory with a base URI.
        // For this test, it should be fine as it uses a basic FopFactory instance.
        
        generator.generateAfp(templateFo.toString(), dataXml.toString(), outputAfp.toString());

        assertTrue(Files.exists(outputAfp), "AFP file should be generated.");
        assertTrue(Files.size(outputAfp) > 0, "AFP file should not be empty.");
        // A more robust test might try to parse the AFP or check for specific content,
        // but that requires specialized AFP libraries and is beyond a simple unit test for generation.
    }
}
