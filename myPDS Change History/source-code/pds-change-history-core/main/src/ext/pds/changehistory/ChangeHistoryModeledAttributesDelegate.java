/*
 * Copyright 2017 PDSVision AB.
 * All rights reserved
 *
 * This software contains confidential proprietary information
 * belonging to PDSVision AB. No part of this information may be
 * used, reproduced, or stored without prior written consent
 * from PDSVision AB.
 */
package ext.pds.changehistory;

import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.windchill.uwgm.proesrv.c11n.ModeledAttributesDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wt.epm.EPMDocument;
import wt.type.ClientTypedUtility;
import wt.util.WTException;
import wt.util.WTProperties;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Adds modeled attributes for subtype of EPMDocuments as properties in content
 * files during add to workspace
 *
 * @author AKullenberg
 */
public class ChangeHistoryModeledAttributesDelegate implements ModeledAttributesDelegate {

    private static final Logger logger = LoggerFactory.getLogger(ChangeHistoryModeledAttributesDelegate.class);

    private String epmSubType = null;
    private final HashMap<String, Class<Object>> availableAttributes = new HashMap<>();
    private final ChangeHistoryAttributeHelper changeHistoryAttributeHelper;

    public ChangeHistoryModeledAttributesDelegate() {
        if (logger.isTraceEnabled()) {
            logger.trace("ChangeHistoryModeledAttributesDelegate()");
        }
        WTProperties props;
        try {
            props = WTProperties.getLocalProperties();
            epmSubType = props.getProperty("ext.changeHistory.epmdoctype");
        } catch (IOException ex) {
            logger.warn("Not able to read wt.properties, using default EPMDocument type", ex);
        }
        changeHistoryAttributeHelper = ChangeHistoryAttributeHelper.newHelper();
    }

    /**
     * Returns a map of the available attributes.
     * <p>
     * The map contains attribute names as the keys and attribute types as the
     * values.
     *
     * @return a HashMap
     */
    @Override
    public HashMap getAvailableAttributes() {
        if (logger.isTraceEnabled()) {
            logger.trace("getAvailableAttributes()");
            logger.trace("MODELEDATTRLIST: {}", availableAttributes);
        }

        return availableAttributes;
    }

    /**
     * Filters EPMDocuments based on subtype and adds attributes as properties.
     * <p>
     * Following attributes are added: Number without prefix and extension
     * Created by Created On formated according to property Modified by Modified
     * On formated according to property.
     *
     * @param docs Collection of EPMDocument added to workspace
     * @return HashMap
     * @throws wt.util.WTException
     */
    @Override
    public HashMap<EPMDocument, HashMap<String, String>> getModeledAttributes(Collection docs) throws WTException {
        logger.trace("getModeledAttributes(Collection docs)");
        HashMap<EPMDocument, HashMap<String, String>> result = new HashMap<>();
        Iterator itr = docs.iterator();
        TypeIdentifier tiCheck = null;

        if (epmSubType != null && epmSubType.length() > 1) {
            tiCheck = ClientTypedUtility.getTypeIdentifier(epmSubType);
        }

        while (itr.hasNext()) {
            HashMap<String, String> attributes = new HashMap<>();
            EPMDocument epmDoc = (EPMDocument) itr.next();

            if (tiCheck != null) {
                TypeIdentifier tiObj = ClientTypedUtility.getTypeIdentifier(epmDoc);
                if (!(tiObj.equals(tiCheck) || tiObj.isDescendedFrom(tiCheck))) {
                    logger.debug("{} does not meet type criteria. Type: {}, Criteria: {}", epmDoc.getNumber(), tiObj.getTypename(), tiCheck.getTypename());
                    continue;
                }
            }

            attributes.putAll(changeHistoryAttributeHelper.getChangeHistoryInfo(epmDoc));
            result.put(epmDoc, attributes);

        }
        return result;
    }
}
