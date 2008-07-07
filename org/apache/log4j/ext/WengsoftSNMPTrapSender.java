/*
 *
 *
============================================================================
 *     This license is based on the Apache Software License, Version 1.1
 *
============================================================================
 *
 *    Copyright (C) 2001-2003 Mark Masterson. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of  source code must  retain the above copyright  notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include  the following  acknowledgment:  "This product includes  software
 *    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
 *    Alternately, this  acknowledgment may  appear in the software itself,  if
 *    and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names Mark Masterson, M2 Technologies, SNMPTrapAppender or log4j must
 *    not be used to endorse or promote products derived  from this  software
 *    without  prior written permission. For written permission, please contact
 *    m.masterson@computer.org.
 *
 * 5. Products  derived from this software may not  be called "Apache", nor may
 *    "Apache" appear  in their name,  without prior written permission  of the
 *    Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 * APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 * ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 * (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package org.apache.log4j.ext;

import org.apache.log4j.helpers.*;
import ca.wengsoft.snmp.Core.*;

/**
 * Title: WengsoftSNMPTrapSender<br>
 * Description: This class makes use of the Wengsoft SNMP library to implement
 * the underlying SNMP protocol(s).<br>
 *
 * You can get a copy of Wengsoft SNMP <a href="http://www.simpleweb.org/
 * software/select_obj.php?orderBy=package&oldorderBy=package&orderDir=
 * asc&free=free">here</a>.<br>
 *
 * You will need a copy of the library ("snmpv3.jar" at a minimum) to use
 * and/or compile this class.<br>
 * <p>
 * The source code of the Wengsoft SNMP library includes the following comment:<br>
 * <pre>
 * COPYRIGHT (c) 1999 by WENG-SOFT Inc. Brosard QC CA<br>
 * All Rights Reserved.<br>
 *<br>
 * The copyright to the computer program(s) herein is the property of WENG-SOFT<br>
 * Inc. It's a free software, The program(s) may be used and/or copied freely.<br>
 *<br>
 * Contact : wweng@videotron.ca<br>
 * </pre>
 * <p>
 * @author Mark Masterson (<a href="mailto:m.masterson@computer.org">m.masterson@computer.org</a>)<br>
 * <a href="http://www.m2technologies.net/">http://www.m2technologies.net/</a><br>
 * @version 1.0.2<br>
 * 2002-10-03<br>
 * changes ---<br>
 *
 * 2002-10-15: mwm : changed the sysUpTime value to a long, to cope with the SysUpTimeResolver mechanism.<br>
 *
 * 2002-12-10: mwm : minor tweaks and prettying up of code.<br>
 *
 * 2003-05-24: mwm : minor changes to accomodate the changes in the SnmpTrapSenderFacade interface.<br>
 */

public class WengsoftSNMPTrapSender implements SnmpTrapSenderFacade {

    private String managementHost = "127.0.0.1";
    private int managementHostTrapListenPort = 162;
    private String enterpriseOID = "1.3.6.1.2.1.1.2.0";
    private long sysUpTime = 0;
    private int trapVersion = 2;
    private boolean isInitialized = false;
    private SnmpMessage snmpMessage;
    private SnmpGetSetPdu trapPdu;

    private static final String SYSTEM_UPTIME_KEY = "1.3.6.1.2.1.1.3";
    private static final String TRAP_OID_KEY = "1.3.6.1.6.3.1.1.4.1";
    private static final String ENTERPRISE_OID_KEY = "1.3.6.1.6.3.1.1.4.3";

    /**
     * Default constructor.
     */
    public WengsoftSNMPTrapSender() {
    }

    public void initialize(SNMPTrapAppender appender) {
        this.managementHost = appender.getManagementHost();
        this.managementHostTrapListenPort = appender.getManagementHostTrapListenPort();
        this.enterpriseOID = appender.getEnterpriseOID();
        this.sysUpTime = appender.getSysUpTime();
        this.trapVersion = appender.getTrapVersion();
        this.snmpMessage = new SnmpMessage();
        this.trapPdu = new SnmpGetSetPdu();
        this.isInitialized = true;
    }

    public void addTrapMessageVariable(final String applicationTrapOIDValue, final String value) {
        //check pre-condition
        if (!this.isInitialized) {
            LogLog.error("The initialize() method must be called before calling addTrapMessageVariable()");
            return;
        }
        this.trapPdu.addNameValuePair(new AsnNameValuePair(TRAP_OID_KEY, new AsnOID(applicationTrapOIDValue)));
        this.trapPdu.addNameValuePair(new AsnNameValuePair(applicationTrapOIDValue, new AsnOctets(value)));
    }

    public void sendTrap() {
        //check pre-condition
        if (!this.isInitialized) {
            LogLog.error("The initialize() method must be called before calling sendTrap()");
            return;
        }
        this.trapPdu.setMsgType(AsnObject.SNMPV2_TRAP);
        this.trapPdu.setReqId(1);
        // Add enterprise OID
        this.trapPdu.addNameValuePair(new AsnNameValuePair(ENTERPRISE_OID_KEY, new AsnOID(this.enterpriseOID)));
        // Add system up time.
        this.trapPdu.addNameValuePair(new AsnNameValuePair(SYSTEM_UPTIME_KEY, new AsnInteger(this.sysUpTime)));
        this.snmpMessage.setPdu(trapPdu);
        this.snmpMessage.setSnmpVersion(this.trapVersion);
        //fire it off
        final SnmpClient client = new SnmpClient();
        try {
            client.sendSnmpMessage(this.managementHost, this.managementHostTrapListenPort, this.snmpMessage);
        } catch (Exception ex) {
            LogLog.error("There was an unexpected error while sending the trap.", ex);
        }
    }
}