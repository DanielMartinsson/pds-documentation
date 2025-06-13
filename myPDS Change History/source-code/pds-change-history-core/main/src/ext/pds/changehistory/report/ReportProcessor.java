/*
 *  Copyright 2013 PDS Vision AB.
 *  All rights reserved
 *
 *  This software contains confidential proprietary information
 *  belonging to PDS Vision AB. No part of this information may be
 *  used, reproduced, or stored without prior written consent
 *  from PDS Vision AB.
 *
 */
package ext.pds.changehistory.report;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.ptc.core.components.forms.DefaultObjectFormProcessor;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.OperationIdentifier;
import ext.pds.changehistory.ChangeHistoryInfoProcessorConfig;
import ext.pds.changehistory.utils.LogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wt.change2.ChangeOrder2;
import wt.fc.QueryResult;
import wt.fc.ReferenceFactory;
import wt.fc.WTObject;
import wt.fc.collections.WTArrayList;
import wt.maturity.MaturityHelper;
import wt.maturity.PromotionNotice;
import wt.part.WTPart;
import wt.session.SessionHelper;
import wt.util.WTException;
import wt.util.WTProperties;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static ext.pds.changehistory.ChangeHistoryInfoProcessorConfig.PROP_IBA_IMPACT_TO_EXISTING_PARTS;
import static ext.pds.changehistory.ChangeHistoryInfoProcessorConfig.PROP_IBA_INFO_MARKETING;
import static ext.pds.changehistory.ChangeHistoryInfoProcessorConfig.PROP_IBA_INFO_SERVICE;
import static ext.pds.changehistory.ChangeHistoryInfoProcessorConfig.PROP_IBA_REASON;
import static ext.pds.changehistory.ChangeHistoryInfoProcessorConfig.PROP_IBA_REASON_SOURCE;
import static ext.pds.changehistory.ChangeHistoryInfoProcessorConfig.PROP_IBA_RELEASED_BY;
import static ext.pds.changehistory.ChangeHistoryInfoProcessorConfig.PROP_IBA_RELEASE_DATE;
import static ext.pds.changehistory.ChangeHistoryInfoProcessorConfig.PROP_IBA_SHORT_REASON;

/**
 * @author Daniel Södling
 */
public class ReportProcessor extends DefaultObjectFormProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ReportProcessor.class);
    private static final ReferenceFactory rf = new ReferenceFactory();

    private static final String NUMBER = "number";
    private static final String NAME = "name";
    private static final String REPORT_FOLDER_PATH = "ext" + File.separator + "ext/pds" + File.separator + "changehistory" + File.separator + "report";
    private static final String REPORT_URL_PATH = "ext/pds/changehistory/report/cache";

    private LoadingCache<String, File> cache;
    private File folder;
    private File cacheFolder;
    private String baseURL;

    public static ReportProcessor getInstance() {
        logger.debug("Create new ChangeReportProcessor instance");
        try {
            WTProperties props = WTProperties.getLocalProperties();
            String codebase = props.getProperty("wt.codebase.location");
            String baseURL = props.getProperty("wt.server.codebase");
            ReportProcessor instance = new ReportProcessor();
            instance.init(codebase, baseURL);
            return instance;
        } catch (IOException ex) {
            throw new IllegalStateException("Was unable to read wt.properties", ex);
        }

    }

    private void init(String codebase, String baseURL) {
        if (logger.isTraceEnabled()) {
            logger.trace("init(String codebase, String baseURL)");
            logger.trace("codebase: {}", codebase);
            logger.trace("baseURL: {}", baseURL);
            logger.trace("REPORT_URL_PATH: {}", REPORT_URL_PATH);
        }
        ReportCacheDelegate delegate = new ReportCacheDelegate(this);
        this.baseURL = baseURL;
        this.folder = new File(codebase, REPORT_FOLDER_PATH);
        this.cacheFolder = new File(codebase, REPORT_FOLDER_PATH + File.separator + "cache");
        this.cacheFolder.mkdirs();
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .removalListener(delegate)
                .build(delegate);
    }

    public URL getReportURL(String obid) throws WTException {
        if (logger.isTraceEnabled()) {
            logger.trace("getReportURL(String obid)");
            logger.trace("obid {}", obid);
        }

        URL url = null;
        File file;
        try {
            file = cache.get(obid);
            if (file != null) {
                url = new URL(baseURL + "/" + REPORT_URL_PATH + "/" + file.getName());
            }
        } catch (ExecutionException | MalformedURLException ex) {
            throw new WTException(ex);
        }

        logger.trace("getReportURL#return: {}", url);
        return url;

    }

    protected File generateReport(String obid) throws WTException {
        logger.trace("generateReport(obid={})", obid);

        Object obj = rf.getReference(obid).getObject();
        String number;

        if (obj instanceof PromotionNotice) {
            number = ((PromotionNotice) obj).getNumber();
        } else if (obj instanceof ChangeOrder2) {
            number = ((ChangeOrder2) obj).getNumber();
        } else {
            throw new WTException("Invalid type. Expected PromotionNotice or ChangeOrder2 but was " + obj.getClass().getName());
        }

        String id = String.valueOf(System.currentTimeMillis());
        String fileName = number.toLowerCase().replace(" ", "-").concat("-").concat(id).concat(".pdf");
        File file = new File(cacheFolder, fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                ChangeHistoryInfoProcessorConfig config = ChangeHistoryInfoProcessorConfig.newServerSideConfig();
                ReportModel model = generateModel(obj, config);
                ReportGenerator.newInstance(folder.getAbsolutePath()).generate(model, config, out);
                out.writeTo(fos);
                out.flush();
            }
        } catch (IOException ex) {
            throw new WTException(ex, "Report error");
        }

        return file;

    }

    private ReportModel generateModel(Object obj, ChangeHistoryInfoProcessorConfig config) throws WTException {
        ReportModel model = null;
        if (obj instanceof PromotionNotice) {
            model = generateModel((PromotionNotice) obj, config);
        } else if (obj instanceof ChangeOrder2) {
            model = generateModel((ChangeOrder2) obj, config);
        }
        return model;
    }

    /**
     * Creates a new report model object for the given promotion request
     *
     * @param co
     * @return
     */
    private ReportModel generateModel(ChangeOrder2 co, ChangeHistoryInfoProcessorConfig config) throws WTException {
        if (logger.isTraceEnabled()) {
            logger.trace("generateModel(ChangeOrder2 co, ChangeHistoryInfoProcessorConfig config)");
            logger.trace("co: {}", co.getNumber());
            logger.trace("config: {}", config);
        }

        ReportModel model = new ReportModel(createPNEntry(co, config));

        wt.fc.QueryResult qr = wt.change2.ChangeHelper2.service.getChangeablesAfter(co, true);

        WTArrayList list = qr != null ? new WTArrayList(qr) : new WTArrayList();
        if (logger.isTraceEnabled()) {
            logger.trace("generateModel QueryResult Size: {}", list.size());
        }
        Iterator itr = list.subCollection(WTPart.class).persistableIterator();
        WTPart part;

        while (itr.hasNext()) {
            part = (WTPart) itr.next();
            if (logger.isTraceEnabled()) {
                logger.trace("Part: {}", LogHelper.toString(part));
                logger.trace("adding part");

            }
            model.addPart(createPartEntry(part, config));
        }

        return model;

    }

    /**
     * Creates a new report model object for the given promotion request
     *
     * @param pn
     * @return
     */
    private ReportModel generateModel(PromotionNotice pn, ChangeHistoryInfoProcessorConfig config) throws WTException {
        if (logger.isTraceEnabled()) {
            logger.trace("generateModel(PromotionNotice pn, ChangeHistoryInfoProcessorConfig config)");
            logger.trace("pn: {}", pn.getNumber());
            logger.trace("config: {}", config);
        }

        ReportModel model = new ReportModel(createPNEntry(pn, config));
        QueryResult qr = MaturityHelper.service.getPromotionTargets(pn);

        WTArrayList list = qr != null ? new WTArrayList(qr) : new WTArrayList();
        if (logger.isTraceEnabled()) {
            logger.trace("generateModel QueryResult Size: {}", list.size());
        }
        if (logger.isTraceEnabled()) {
            Iterator itrtemp = list.persistableIterator();
            while (itrtemp.hasNext()) {
                Object o = itrtemp.next();
                logger.trace("Object in temp iterator: {}", LogHelper.toString(o));
            }
        }

        Iterator itr = list.subCollection(WTPart.class).persistableIterator();
        WTPart part;

        while (itr.hasNext()) {
            part = (WTPart) itr.next();
            if (logger.isTraceEnabled()) {
                logger.trace("Part: {}", LogHelper.toString(part));
                logger.trace("adding part");

            }
            model.addPart(createPartEntry(part, config));
        }

        return model;

    }

    private ReportEntry createPNEntry(PromotionNotice pn, ChangeHistoryInfoProcessorConfig config) throws WTException {
        String[] keys = new String[]{NUMBER, PROP_IBA_INFO_SERVICE, PROP_IBA_INFO_MARKETING,
                PROP_IBA_REASON, PROP_IBA_SHORT_REASON, PROP_IBA_REASON_SOURCE, PROP_IBA_IMPACT_TO_EXISTING_PARTS};
        return new ReportEntry(getAttributes(keys, pn, config));
    }

    private ReportEntry createPNEntry(ChangeOrder2 co, ChangeHistoryInfoProcessorConfig config) throws WTException {
        String[] keys = new String[]{NUMBER, PROP_IBA_INFO_SERVICE, PROP_IBA_INFO_MARKETING,
                PROP_IBA_REASON, PROP_IBA_SHORT_REASON, PROP_IBA_REASON_SOURCE, PROP_IBA_IMPACT_TO_EXISTING_PARTS};
        return new ReportEntry(getAttributes(keys, co, config));
    }

    private ReportEntry createPartEntry(WTPart part, ChangeHistoryInfoProcessorConfig config) throws WTException {
        String[] keys = new String[]{NUMBER, NAME, PROP_IBA_RELEASED_BY, PROP_IBA_RELEASE_DATE};
        HashMap<String, Object> attr = getAttributes(keys, part, config);
        attr.put("revision", part.getVersionDisplayIdentifier().toString());
        return new ReportEntry(attr);
    }

    private HashMap<String, Object> getAttributes(String[] keys, WTObject obj, ChangeHistoryInfoProcessorConfig config) throws WTException {
        HashMap<String, Object> attr = new HashMap<>();
        OperationIdentifier op = null;
        PersistableAdapter adapter = new PersistableAdapter(obj, null, SessionHelper.getLocale(), op);
        for (String key : keys) {
            attr.put(key, getIBAValue(adapter, obj, config.getAttr(key)));
        }
        return attr;
    }

    private String getIBAValue(PersistableAdapter adapter, WTObject obj, String iba) {
        String value = "-";
        try {
            adapter.load(iba);
            value = (String) adapter.getAsString(iba);
        } catch (WTException ex) {
            logger.error("Could not retrieve value for IBA {} on {}", iba, LogHelper.toString(obj));
        }
        return value;
    }
}
