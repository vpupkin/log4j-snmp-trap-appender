package org.apache.log4j.ext;

import org.apache.log4j.*;
import org.apache.log4j.helpers.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

class SnmpTrapAppenderTest {

    private static final Category trace = Category.getInstance(SnmpTrapAppenderTest.class.getName());
    private static boolean log4jInitialized = false;

    public SnmpTrapAppenderTest() {
        final String sFile = "/etc/snmpTrapAppenderTestConfig.xml";
        initLogging(sFile);
        if (!log4jInitialized)
            LogLog.error("[SnmpTrapAppenderTest],  Logging initialization error.  Config file=" + sFile + " -- Aborting.");
        else {
            trace.debug("Here's a debug message");
            trace.error("Whoa, had an error there");
            trace.info("Here's some info about that");
            trace.fatal("Help! I'm dying out here!");
            trace.fatal("Here's an Exception!", new Exception("Exception message"));
        }
    }

    public static void initLogging(final String configFile) {
        if (!log4jInitialized) {
            try {
                org.apache.log4j.xml.DOMConfigurator.configure(configFile);
                log4jInitialized = true;
            } catch (Exception ex) {
                LogLog.error("[SnmpTrapAppenderTest], [initLogging], Error initializing logging!", ex);
            }
        }//if
    }

    public static void main(final String[] args) {
        new SnmpTrapAppenderTest();
        LogManager.shutdown();
    }
}
