package ext.pds.changehistory;

import ext.pds.changehistory.utils.IBAUtil;
import ext.pds.changehistory.utils.LogHelper;
import ext.pds.changehistory.utils.PersistableAdapterDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wt.change2.ChangeHelper2;
import wt.change2.WTChangeActivity2;
import wt.change2.WTChangeOrder2;
import wt.epm.EPMDocument;
import wt.fc.Persistable;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.lifecycle.State;
import wt.lifecycle._State;
import wt.maturity.MaturityHelper;
import wt.maturity.PromotionNotice;
import wt.util.WTException;
import wt.util.WTStandardDateFormat;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import static ext.pds.changehistory.ChangeHistoryInfoProcessorConfig.PROP_DATE_FORMAT;
import static ext.pds.changehistory.ChangeHistoryInfoProcessorConfig.PROP_DOCTYPE;
import static ext.pds.changehistory.ChangeHistoryInfoProcessorConfig.PROP_FILTER_ON_CATEGORY;
import static ext.pds.changehistory.ChangeHistoryInfoProcessorConfig.PROP_GET_REASON_FROM_CHANGE;
import static ext.pds.changehistory.ChangeHistoryInfoProcessorConfig.PROP_IBA_CHANGE_HISTORY_INFO;
import static ext.pds.changehistory.ChangeHistoryInfoProcessorConfig.PROP_IBA_RELEASED_BY;
import static ext.pds.changehistory.ChangeHistoryInfoProcessorConfig.PROP_IBA_RELEASE_DATE;
import static ext.pds.changehistory.ChangeHistoryInfoProcessorConfig.PROP_IBA_RELEASE_DESC;
import static ext.pds.changehistory.ChangeHistoryInfoProcessorConfig.PROP_MAX_ENTRIES;
import static ext.pds.changehistory.ChangeHistoryInfoProcessorConfig.PROP_RESOLVED_CN_STATE;

/**
 * @author Akila
 */
public class ChangeHistoryProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ChangeHistoryProcessor.class);

    private final int maxEntries;
    private final String dateFormat;
    private final String ibaReleasedDate;
    private final String ibaReleasedBy;
    private final String ibaReleaseInfo;
    private final String ibaReleaseDesc;
    private final String docType;
    private final boolean filterOnCategory;
    private final String resolvedCNState;
    private final boolean getReasonFromChange;

    public static ChangeHistoryProcessor newProcessor() {
        return new ChangeHistoryProcessor(ChangeHistoryInfoProcessorConfig.newServerSideConfig());
    }

    public ChangeHistoryProcessor(ChangeHistoryInfoProcessorConfig config) {
        this.dateFormat = config.get(PROP_DATE_FORMAT);
        this.maxEntries = Integer.parseInt(config.get(PROP_MAX_ENTRIES));
        this.ibaReleaseInfo = config.get(PROP_IBA_CHANGE_HISTORY_INFO);
        this.ibaReleasedDate = config.get(PROP_IBA_RELEASE_DATE);
        this.ibaReleasedBy = config.get(PROP_IBA_RELEASED_BY);
        this.docType = config.get(PROP_DOCTYPE);
        this.filterOnCategory = Boolean.parseBoolean(config.get(PROP_FILTER_ON_CATEGORY));
        this.resolvedCNState = config.get(PROP_RESOLVED_CN_STATE);
        this.ibaReleaseDesc = config.get(PROP_IBA_RELEASE_DESC);
        this.getReasonFromChange = Boolean.parseBoolean(config.get(PROP_GET_REASON_FROM_CHANGE));
    }

    /**
     * Parses the given value and splits it into a list of ChangeHistoryInfo object where each entry corresponds to one stored
     * release event.
     * <p>
     * The entries in the returned list retains the order from the stored value.
     *
     * @param epm
     * @return a list of ChangeHistoryInfo objects
     */
    public List<ChangeHistoryInfo> getStoredChangeHistory(EPMDocument epm) {
        if (logger.isTraceEnabled()) {
            logger.trace("getReleaseInfo(epm={})", epm.getDisplayIdentifier());
        }

        List<ChangeHistoryInfo> list = new ArrayList<>();
        String values = IBAUtil.newIBAUtil(epm, true).getIBAValue(ibaReleaseInfo);

        logger.debug("releaseIBA: {}", ibaReleaseInfo);
        logger.debug("values: {}", values);

        if (values != null) {
            String[] storedValues = values.split("\\{\\}");
            for (String value : storedValues) {
                if (isNotEmpty(value)) {
                    try {
                        list.add(ChangeHistoryInfo.parse(value));
                    } catch (IllegalArgumentException ex) {
                        logger.error(ex.getLocalizedMessage());
                    }
                }
            }
        }

        logger.trace("getReleaseInfo#return: {}", list);
        return list;

    }

    /**
     * Parses the given value and splits it into a list of ChangeHistoryInfo object where each entry corresponds to one stored
     * release event.
     * <p>
     * The entries in the returned list retains the order from the stored value.
     *
     * @param epm
     * @return a list of ChangeHistoryInfo objects
     */
    public List<ChangeHistoryInfo> getOngoingReleaseInfo(EPMDocument epm) {
        if (logger.isTraceEnabled()) {
            logger.trace("getReleaseInfo(epm={})", epm.getDisplayIdentifier());
        }

        List<ChangeHistoryInfo> list = new ArrayList<>();
        QueryResult changeOrders;
        WTChangeOrder2 cn;
        State stateResolved = _State.toState(resolvedCNState);
        String shortReason;
        String revision;
        String releasedBy;
        String releasedDate;
        PersistableAdapterDelegate pad;

        try {
            changeOrders = ChangeHelper2.service.getLatestUniqueImplementedChangeOrders(epm);
            if (changeOrders != null) {
                while (changeOrders.hasMoreElements()) {
                    cn = (WTChangeOrder2) changeOrders.nextElement();
                    if (!stateResolved.equals(cn.getLifeCycleState())) {
                        shortReason = getChangObjectAttributes(cn);
                        pad = PersistableAdapterDelegate.newRetrieveDelegate(epm);
                        pad.load(ibaReleasedBy, ibaReleasedDate);
                        revision = epm.getVersionDisplayIdentifier().getLocalizedMessage(Locale.getDefault());
                        releasedBy = pad.getAsString(ibaReleasedBy);
                        releasedDate = formatDate((Date) pad.get(ibaReleasedDate));
                        list.add(ChangeHistoryInfo.newChangeHistoryInfo(revision, shortReason, releasedDate, releasedBy));
                        break;
                    }
                }
            }

        } catch (WTException ex) {
            logger.error("Error when searching for related ECNs for {}", LogHelper.toString(epm), ex);
        }

        logger.trace("getReleaseInfo#return: {}", list);
        return list;

    }

    /**
     * Retrieves release info from the promotion request and sets this along with additional drawing specific info on
     * each drawing that is part of the given promotion request.
     * <p>
     * The info for the eight most recent releases is formatted stored as a string attribute on each drawing.
     *
     * @param obj
     * @return
     */
    public boolean setChangeHistory(Object obj) {
        if (logger.isTraceEnabled()) {
            logger.trace("setReleaseInfo(obj={})", LogHelper.toString(obj));
        }

        boolean success = false;
        PromotionNotice pn;
        WTChangeOrder2 cn;
        QueryResult changeobjects = null;
        String shortReason = "";

        try {
            if (obj instanceof PromotionNotice) {
                pn = (PromotionNotice) obj;
                changeobjects = MaturityHelper.service.getPromotionTargets(pn);
                if (getReasonFromChange) {
                    shortReason = getChangObjectAttributes(pn);
                }
            } else if (obj instanceof WTChangeOrder2) {
                cn = (WTChangeOrder2) obj;
                changeobjects = ChangeHelper2.service.getChangeablesAfter(cn, true);
                if (getReasonFromChange) {
                    shortReason = getChangObjectAttributes(cn);
                }
            } else if (obj instanceof WTChangeActivity2) {
                WTChangeActivity2 ca = (WTChangeActivity2) obj;
                changeobjects = ChangeHelper2.service.getChangeablesAfter((WTChangeActivity2) obj, true);

                QueryResult qr = ChangeHelper2.service.getChangeOrder(ca);
                if (qr.hasMoreElements()) {
                    cn = (WTChangeOrder2) qr.nextElement();
                    if (getReasonFromChange) {
                        shortReason = getChangObjectAttributes(cn);
                    }
                }

            } else {
                throw new IllegalArgumentException("setRelaseInfo not supported for type " + obj.getClass().getName());
            }
            success = setChangeHistory(changeobjects, shortReason);
        } catch (Exception ex) {
            logger.error("Was unable to find change objects", ex);
        }

        logger.trace("setReleaseInfo#return: {}", success);
        return success;

    }

    /**
     * gets the reason attribute from the change object
     *
     * @param obj
     * @return
     */
    public String getChangObjectAttributes(Persistable obj) {
        String result = null;
        try {
            PersistableAdapterDelegate pad;
            pad = PersistableAdapterDelegate.newRetrieveDelegate(obj);
            pad.load(ibaReleaseDesc);
            result = pad.getAsString(ibaReleaseDesc);
        } catch (WTException ex) {
            logger.error("Was unable to get Change object attributes {}", LogHelper.toString(obj), ex);
        }
        return result;
    }


    /**
     * Sets the release info on the given change objects.
     * <p>
     * The reason can be null and will be null in normal runtime scenarios. When it is null the value for reason
     * will be read from the shortReason IBA. The reason for having it as a parameter is that when doing testing
     * from the ext.jbt.--.test.Controller class we need to be able to pass in a value.
     *
     * @param changeobjects
     * @param reason
     * @return
     */
    public boolean setChangeHistory(QueryResult changeobjects, String reason) {
        logger.trace("setReleaseInfo(changeobjects={}, reason={})", changeobjects, reason);

        boolean success = false;
        Object target;
        ChangeHistoryInfo changeHistoryInfo;
        List<ChangeHistoryInfo> list;
        String revision;
        String releasedBy;
        String releasedDate;
        String releaseEvents;

        if (changeobjects == null) {
            return false;
        }

        while (changeobjects.hasMoreElements()) {
            target = changeobjects.nextElement();

            if (!isApplicable(target)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Skipping non-drawing {} ", ((WTObject) target).getDisplayIdentifier());
                }
                continue;
            }

            EPMDocument doc = (EPMDocument) target;

            try {

                IBAUtil util = IBAUtil.newIBAUtil(doc, true);
                reason = getReasonFromChange ? reason : util.getIBAStringValue(ibaReleaseDesc);
                revision = doc.getVersionDisplayIdentifier().getLocalizedMessage(Locale.getDefault());
                releasedBy = util.getIBAStringValue(ibaReleasedBy);
                releasedDate = getReleasedDate(doc);

                changeHistoryInfo = ChangeHistoryInfo.newChangeHistoryInfo(revision, reason, releasedDate, releasedBy);

                list = getStoredChangeHistory(doc);
                list.add(changeHistoryInfo);

                releaseEvents = String.join("{}", formatValues(list, new RevisionComparator(doc.getMaster())));

                util.clearIBA(doc, ibaReleaseInfo);
                util.updateIBAHolder(util.updateIBAPart(doc));

                util.setIBAValue(ibaReleaseInfo, releaseEvents);
                util.updateIBAHolder(util.updateIBAPart(doc));
                success = true;
            } catch (Exception ex) {
                logger.error("Was unable to set release info on {}", doc.getDisplayIdentifier(), ex);
            }

        }

        logger.trace("setReleaseInfo#return: {}", success);
        return success;

    }

    private String getReleasedDate(Persistable doc) throws WTException {

        PersistableAdapterDelegate pad = PersistableAdapterDelegate.newRetrieveDelegate(doc);
        pad.load(ibaReleasedDate);
        String releasedDate;
        switch (pad.getAttributeDescriptor(ibaReleasedDate).getDataType()) {
            case "java.sql.Timestamp":
                releasedDate = formatDate((Date) pad.get(ibaReleasedDate));
                break;
            case "java.lang.String":
                releasedDate = pad.getAsString(ibaReleasedDate);
                break;
            default:
                if (logger.isErrorEnabled()) {
                    logger.error("The {} must be of String or Date type", ibaReleasedDate);
                }
                releasedDate = "IBA Type error";
                break;
        }
        return releasedDate;
    }

    /**
     * Validates that the object is a drawing by checking its type and document type if the object is an EPMDocument.
     * <p>
     * The docType to validate against can be configured in wt.properties by setting the property {@link PROP_DOCTYPE}
     *
     * @param target
     * @return true if and only if the objects is a drawing
     */
    private boolean isApplicable(Object target) {
        if (logger.isTraceEnabled()) {
            logger.trace("isApplicable(Object target)");
            logger.trace("target: {}", target);
            logger.trace("filterOnCategory: {}", filterOnCategory);
        }

        EPMDocument epm;
        String epmDocType;
        boolean isApplicable = false;

        if (target instanceof EPMDocument) {
            if (filterOnCategory) {
                epm = (EPMDocument) target;
                epmDocType = epm.getDocType().toString();
                isApplicable = docType.equals(epmDocType);

                if (logger.isDebugEnabled()) {
                    logger.debug("epm: {}", epm.getDisplayIdentifier());
                    logger.debug("docType: {}", epmDocType);
                }

            } else {
                isApplicable = true;
            }
        }

        logger.trace("isApplicable#return: {}", isApplicable);
        return isApplicable;
    }

    private boolean isNotEmpty(String storedValue) {
        return storedValue != null && !storedValue.trim().isEmpty();
    }

    /**
     * Formats and truncates the messages given a list of entries where each entry represents a release event.
     * <p>
     * The method sorts the values by their revision and retains at most eight entries which are then added to the list that
     * is returned.
     *
     * @param entries the list if info entries
     * @param comp a comparator
     * @return a formatted message
     */
    protected List<String> formatValues(List<ChangeHistoryInfo> entries, RevisionComparator comp) {
        if (logger.isTraceEnabled()) {
            logger.trace("formatValues(List<ChangeHistoryInfo> entries)");
            logger.trace("entries: {}", entries);
        }

        List<String> values = new ArrayList<>();
        List<ChangeHistoryInfo> infos;
        Iterator<ChangeHistoryInfo> itr = entries.iterator();
        ChangeHistoryInfo entry;
        TreeMap<String, ChangeHistoryInfo> sortedValues = new TreeMap<>(comp);

        while (itr.hasNext()) {
            entry = itr.next();
            sortedValues.put(entry.getRevision(), entry);
        }
        infos = new ArrayList<>(sortedValues.values());
        int start = sortedValues.size() > maxEntries ? sortedValues.size() - maxEntries : 0;
        infos = infos.subList(start, sortedValues.size());
        infos.forEach(value -> values.add(value.format()));

        logger.trace("formatMessage#return: {}", values);
        return values;
    }

    public String formatDate(Timestamp ts) {
        Date date = new Date(ts.getTime());
        return formatDate(date);
    }

    public String formatDate(Date date) {
        return date != null ? WTStandardDateFormat.format(date, dateFormat) : "";
    }

    public Date parseDate(String str) {
        try {
            return WTStandardDateFormat.parse(str, dateFormat);
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Invalid date: " + str, ex);
        }
    }
}
