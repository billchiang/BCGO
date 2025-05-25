package com.example.afpgen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File; // Not strictly needed due to usage of Path and Files
import java.nio.file.Path;
import java.nio.file.Files;

class AfpGeneratorTest {

    @Test
    void testGenerateAfp(@TempDir Path tempDir) throws Exception {
        AfpGenerator generator = new AfpGenerator();
        
        // Create dummy template.fo in tempDir for test
        // This should be a valid XSLT that can transform the dummy XML.
        // For simplicity, we'll use a minimal structure that matches the new data's root.
        Path templateFo = tempDir.resolve("template.fo");
        String foContent = "<fo:root xmlns:fo=\"http://www.w3.org/1999/XSL/Format\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">" +
                           "<xsl:template match=\"/statementData\">" + // Match the new root element
                           "<fo:layout-master-set><fo:simple-page-master master-name=\"A4\">" +
                           "<fo:region-body margin=\"1in\"/></fo:simple-page-master></fo:layout-master-set>" +
                           "<fo:page-sequence master-reference=\"A4\"><fo:flow flow-name=\"xsl-region-body\">" +
                           "<fo:block>Hello <xsl:value-of select=\"customer/name\"/></fo:block>" +
                           "</fo:flow></fo:page-sequence>" +
                           "</xsl:template></fo:root>";
        Files.write(templateFo, foContent.getBytes());

        // Create dummy data.xml in tempDir for test, reflecting the new structure
        Path dataXml = tempDir.resolve("data.xml");
        String xmlContent = "<statementData><customer><name>Test User</name></customer></statementData>"; // XML with root <statementData>
        Files.write(dataXml, xmlContent.getBytes());

        Path outputAfp = tempDir.resolve("output.afp");
        
        generator.generateAfp(templateFo.toString(), dataXml.toString(), outputAfp.toString());

        assertTrue(Files.exists(outputAfp), "AFP file should be generated.");
        assertTrue(Files.size(outputAfp) > 0, "AFP file should not be empty.");
        // Visual/content validation of the complex AFP is out of scope for this unit test.
    }
}
