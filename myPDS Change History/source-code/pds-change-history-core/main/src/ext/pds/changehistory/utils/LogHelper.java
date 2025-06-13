/*
 * Copyright 2019 PDSVISION AB.
 * All rights reserved
 *
 * This software contains confidential proprietary information
 * belonging to PDSVISION AB. No part of this information may be
 * used, reproduced, or stored without prior written consent
 * from PDSVISION AB.
 */
package ext.pds.changehistory.utils;

import com.ptc.core.meta.type.common.TypeInstance;
import wt.epm.EPMDocument;
import wt.epm.EPMDocumentMaster;
import wt.fc.WTObject;
import wt.folder.Folder;
import wt.org.WTUser;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Contains a number of static method for printing common PDMLink object. The intended use is for logging in order to
 * produce readable strings instead of obids.
 *
 * @author Daniel Martinsson
 */
public class LogHelper {

    private LogHelper() {
        // Hides implicit no-args constructor
    }

    public static String toString(Collection collection) {
        return "[" + collection.stream().map(LogHelper::toString).collect(Collectors.joining(" , ")).toString() + "]";
    }

    public static String toString(Object obj) {

        String str = null;

        if (obj != null) {
            if (obj instanceof EPMDocument) {
                str = toString((EPMDocument) obj);

            } else if (obj instanceof EPMDocumentMaster) {
                str = toString((EPMDocumentMaster) obj);

            } else if (obj instanceof WTObject) {
                str = toString((WTObject) obj);

            } else {
                str = obj.toString();
            }
        }

        return str;

    }

    /**
     * Logs EPMDocuments.
     *
     * @param epmDocument
     * @return
     */
    public static String toString(EPMDocument epmDocument) {
        String returnValue = "";
        if (epmDocument != null) {
            returnValue = epmDocument.getNumber() + ", " + epmDocument.getIterationDisplayIdentifier().toString() + " - " + epmDocument.getCADName();
        }
        return returnValue;
    }

    /**
     * Logs EPMDocumentMasters.
     *
     * @param epmDocumentMaster
     * @return
     */
    public static String toString(EPMDocumentMaster epmDocumentMaster) {
        String returnValue = "";
        if (epmDocumentMaster != null) {
            returnValue = epmDocumentMaster.getNumber() + " - " + epmDocumentMaster.getCADName();
        }
        return returnValue;
    }

    public static String toString(TypeInstance ti) {
        return ti != null ? ti.getIdentifier().toString() : null;
    }

    public static String toString(Folder folder) {
        return folder != null ? folder.getName() : null;
    }

    public static String toString(WTUser user) {
        return user != null ? user.getName() : null;
    }

    private static String toString(WTObject wtobj) {
        String str = "(unknown)";
        try {
            str = wtobj != null ? wtobj.getDisplayIdentifier().toString() : null;
        } catch (Exception ex) {
            // We ignore this but keep the catch to avoid causing problems simply because the logging fails.
        }
        return str;
    }

}
