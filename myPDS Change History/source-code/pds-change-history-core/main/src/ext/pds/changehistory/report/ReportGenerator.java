/*
 * Copyright 2017 PDSVision AB.
 * All rights reserved
 *
 * This software contains confidential proprietary information
 * belonging to PDSVision AB. No part of this information may be
 * used, reproduced, or stored without prior written consent
 * from PDSVision AB.
 */
package ext.pds.changehistory.report;

import ext.pds.changehistory.ChangeHistoryInfoProcessorConfig;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;
import org.apache.xmlgraphics.util.MimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Daniel Södling
 */
public class ReportGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);

    public static final String TEMPLATE = "pn-report.vm";
    public static final String STYLESHEET = "pn-report.xsl";

    protected VelocityEngine ve;
    protected final FopFactory fopFactory;
    protected String resourceFolderPath;

    public static ReportGenerator newInstance(String reportFolderPath) {
        FopFactory fopFactory;
        try {
            String url = Paths.get(reportFolderPath).toUri().toURL().toString();
            fopFactory = FOPHelper.newInstance().getFOPFactory(url);
            return new ReportGenerator(fopFactory, reportFolderPath);
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Invalid config folder argument: " + reportFolderPath, ex);
        }
    }

    public ReportGenerator(FopFactory fopFactory, String resourceFolderPath) {
        if (logger.isTraceEnabled()) {
            logger.trace("ReportGenerator(FopFactory fopFactory, String resourceFolderPath)");
            logger.trace("resourceFolderPath: {}", resourceFolderPath);
        }
        this.fopFactory = fopFactory;
        this.resourceFolderPath = resourceFolderPath;

        ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
        ve.setProperty("file.resource.loader.class", FileResourceLoader.class.getName());
        ve.setProperty("file.resource.loader.path", resourceFolderPath);
        ve.setProperty("file.resource.loader.cache", "false");
        ve.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");
        ve.init();

    }

    public void setVelocityEngine(VelocityEngine ve) {
        this.ve = ve;
    }

    public void generate(ReportModel report, ChangeHistoryInfoProcessorConfig config, OutputStream out) throws IOException {
        if (logger.isTraceEnabled()) {
            logger.trace("generate(BOMReport report, OutputStream out)");
            logger.trace("report: {}", report);
        }

        byte[] xmlBytes = generateXML(report, config);
        InputStream in = new ByteArrayInputStream(xmlBytes);

        transformToPDF(in, out);

    }

    /**
     * Uses Velocity to generate a UTF-8-encoded XML byte array for the provided model and config.
     *
     * @param model
     * @param config
     * @return Returns the XML as a byte array.
     */
    public byte[] generateXML(Object model, ChangeHistoryInfoProcessorConfig config) throws IOException {
        if (logger.isTraceEnabled()) {
            logger.trace("generateXML(Object model, ChangeHistoryInfoProcessorConfig config)");
            logger.trace("model: {}", model);
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try (Writer writer = new OutputStreamWriter(os, UTF_8)) {

            VelocityContext context = new VelocityContext();
            Template template = ve.getTemplate(TEMPLATE);

            context.put("model", model);
            context.put("config", config);
            context.put("writer", this);

            template.setEncoding("UTF-8");
            template.merge(context, writer);
            writer.flush();

            if (logger.isTraceEnabled()) {
                logger.trace("generateXML#result:\n{}", os.toString(UTF_8));
            }
            return os.toByteArray();
        }
    }

    /**
     * Transforms the given XML to PDF and writes it into the provided
     * OutputStream.
     *
     * @param inputXML
     * @param out
     * @throws java.io.IOException
     */
    public void transformToPDF(InputStream inputXML, OutputStream out) throws IOException {
        logger.trace("transformToPDF(String inputXML, File outputFile)");
        Fop fop;
        FOUserAgent foUserAgent;
        Source source;
        Result result;
        StreamSource stylesheetSource;
        File stylesheet = new File(resourceFolderPath, STYLESHEET);

        if (!stylesheet.exists()) {
            throw new FileNotFoundException(stylesheet.getAbsolutePath());
        }

        try (BufferedOutputStream bos = new BufferedOutputStream(out)) {
            stylesheetSource = new StreamSource(new FileInputStream(stylesheet));
            foUserAgent = fopFactory.newFOUserAgent();
            fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, bos);
            source = new StreamSource(inputXML);
            result = new SAXResult(fop.getDefaultHandler());
            transform(source, stylesheetSource, result);
            bos.flush();
        } catch (FOPException ex) {
            throw new IOException("Failed to generate the report.", ex);
        }
    }

    /**
     * @param source
     * @param stylesheet
     * @param result
     */
    protected void transform(Source source, StreamSource stylesheet, Result result) {
        if (logger.isTraceEnabled()) {
            logger.trace("transform(Source source, StreamSource stylesheet, Result result)");
            logger.trace("source: " + source);
            logger.trace("stylesheet: " + stylesheet);
            logger.trace("result: " + result);
        }
        Transformer transformer;
        TransformerFactory transformerFactory;
        try {
            transformerFactory = TransformerFactory.newInstance();
            transformer = transformerFactory.newTransformer(stylesheet);
            transformer.transform(source, result);
        } catch (TransformerConfigurationException ex) {
            logger.error("Unable to create a transformer from the stylesheet provided.", ex);
        } catch (TransformerException ex) {
            logger.error("Unable to transform.", ex);
        }
    }

    public String encodeXML(String value) {
        String escaped = null;
        String trimmed;
        if (value != null) {
            trimmed = value.trim();
            if (!trimmed.isEmpty() && !trimmed.equalsIgnoreCase("NULL")) {
                escaped = StringEscapeUtils.escapeXml11(value);
            }
        }
        return escaped;
    }

}
