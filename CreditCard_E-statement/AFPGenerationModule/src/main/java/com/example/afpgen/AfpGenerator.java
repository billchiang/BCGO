package com.example.afpgen;

import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.MimeConstants;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.sax.SAXResult;

import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.URI;

public class AfpGenerator {

    private FopFactory fopFactory;

    public AfpGenerator() throws Exception {
        // Initialize FopFactory. It's good practice to configure it, 
        // especially if you have a fop.xconf file for fonts, etc.
        // For this example, we use a basic instance.
        // The base URI can be important for resolving relative paths in XSL-FO,
        // like for images or other resources.
        URI baseUri = new File(".").toURI(); 
        fopFactory = FopFactory.newInstance(baseUri);
    }

    public void generateAfp(String foFilePath, String xmlFilePath, String outputAfpPath) throws Exception {
        OutputStream out = null;
        InputStream xmlStream = null; // Only needed if foFilePath is an XSLT
        // foStream is not directly used if foFilePath is an XSLT; StreamSource handles it.

        try {
            File outputFile = new File(outputAfpPath);
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }
            out = new FileOutputStream(outputFile);
            
            // Configure FOP to output AFP
            Fop fop = fopFactory.newFop(MimeConstants.MIME_AFP, out);

            TransformerFactory factory = TransformerFactory.newInstance();
            
            // The provided template.fo uses <xsl:value-of>, which means it's an XSLT stylesheet
            // that transforms the XML data into an XSL-FO structure.
            // So, foFilePath is actually the path to our XSLT stylesheet.
            File xsltFile = new File(foFilePath);
            File xmlFile = new File(xmlFilePath);

            StreamSource xsltSource = new StreamSource(xsltFile);
            StreamSource xmlSource = new StreamSource(xmlFile); // Input XML data

            // Create a transformer for the XSLT stylesheet
            Transformer transformer = factory.newTransformer(xsltSource);

            // The result of the XSLT transformation will be XSL-FO,
            // which is then piped directly to FOP's handler.
            Result res = new SAXResult(fop.getDefaultHandler());

            // Perform the transformation (XML + XSLT -> XSL-FO -> FOP Handler)
            transformer.transform(xmlSource, res);

        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    // log or handle
                }
            }
            if (xmlStream != null) { // xmlStream was for xmlFile, handled by StreamSource
                 try {
                    xmlStream.close();
                } catch (Exception e) {
                    // log or handle
                }
            }
            // foStream was also for xsltFile, handled by StreamSource
        }
    }

    public static void main(String[] args) {
        try {
            AfpGenerator generator = new AfpGenerator();
            String baseDir = "src/main/resources/";
            
            // Ensure output directory exists
            File outputDir = new File("output");
            if (!outputDir.exists()){
                outputDir.mkdirs();
            }
            
            String foTemplatePath = baseDir + "template.fo";
            String xmlDataPath = baseDir + "data.xml";
            String outputFilePath = "output/statement.afp";

            generator.generateAfp(foTemplatePath, xmlDataPath, outputFilePath);
            System.out.println("AFP generated successfully at " + outputFilePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
