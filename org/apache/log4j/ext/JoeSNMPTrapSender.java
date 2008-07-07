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

import java.lang.*;
import java.net.*;

import org.apache.log4j.helpers.*;
import org.opennms.protocols.snmp.*;

/**
 * This class makes use of the JoeSNMP library to implement the underlying SNMP
 * protocol(s).  The JoeSNMP library is a part of the OpenNMS project, and is
 * available/distributed under the GNU Lesser General Public License (LGPL).<p>
 *
 * To quote the JoeSNMP FAQ:<br><pre>
 * "What license is JoeSNMP released under?<br>
 *  <br>
 *	JoeSNMP is released under the GNU Lesser General Public License (LGPL),<br>
 *	documented fully at http://www.fsf.org/copyleft/lesser.html.<br>
 *	Effectively, this means that JoeSNMP is free to distribute and modify as<br>
 *	long as you provide your modified code back to the community.  And if<br>
 *	you'd like to use JoeSNMP as a library within your commercial product,<br>
 *	you are welcome to do so as well, but again, any changes to the<br>
 *	library itself need to be contributed back."</pre><p>
 *
 * You can get more information about OpenNMS <a href="http://www.opennms.org
 * /">here</a>.  You can get a copy of JoeSNMP <a href="http://www.opennms.org
 * /files/releases/joeSNMP/">here</a>.<br>
 *
 * You will need a copy of the library ("joesnmp-0.2.6.jar" at a minimum) to use
 * and/or compile this class.<br>
 * WARNING: The JoeSNMP library <b>requires a minimum of JDK 1.2</b>.
 * <p>
 * @version 2.0.3<br>
 * 2002-10-03<br>
 * changes ---<br>
 *
 * 2001-09-31: mwm : cleaned up the coding style errors, modified the
 * addTrapMessageVariable(String value) to addTrapMessageVariable(String value,
 * String applicationTrapOIDValue) so that the Facade is more flexible.<br>
 *
 * 2001-10-03: mwm : swapped the SNMP library out, replacing the AdventNet
 * commercial library with the one from OpenNMS.  Thanks to <a href="mailto:JZhao@Qcorps.com">
 * Jin Zhao</a> for pointing me towards this!<br>
 *
 * 2001-11-04: mwm : fixed a minor bug with use of the SnmpTrapSession object.  General tidying up.<br>
 *
 * 2002-10-03: mwm : changed the name of the class to "JoeSNMPTrapSender", to reflect the fact that
 * this is now simply the concrete implementation of the new "SnmpTrapSenderFacade" interface that
 * uses JoeSNMP as it's underlying library.  Made changes to deal with the
 * new architecture.<br>
 *
 * 2002-10-15: mwm : fixed a bug that caused some NMS software to receive a the IP address of the
 * sending host incorrectly formatted.<br>
 *
 * 2002-10-15: mwm : changed the sysUpTime value to a long, to cope with the SysUpTimeResolver mechanism.<br>
 *
 * 2002-12-10: mwm : minor tweaks and prettying up of code.<br>
 *
 * 2003-03-21: mwm : fixed a big, nasty, RTFM bug in #sendTrap<br>
 *
 * 2003-05-24: mwm : minor changes to accomodate the changes in the SnmpTrapSenderFacade interface.<br>
 *
 * @author Mark Masterson (<a href="mailto:m.masterson@computer.org">m.masterson@computer.org</a>)<br>
 * <a href="http://www.m2technologies.net/">http://www.m2technologies.net/</a><br>
 */

public class JoeSNMPTrapSender implements SnmpTrapHandler, SnmpTrapSenderFacade {

    private String managementHost = "127.0.0.1";
    private int managementHostTrapListenPort = 162;
    private String enterpriseOID = "1.3.6.1.2.1.1.2.0";
    private String localIPAddress = "127.0.0.1";
    private int localTrapSendPort = 161;
    private int genericTrapType = 0;
    private int specificTrapType = 6;
    private String applicationTrapOID = "1.3.6.1.2.1.1.2.0.0.0.0";
    private String communityString = "public";
    private long sysUpTime = 0;
    private SnmpPduTrap pdu = null;
    private SnmpTrapSession session = null;
    private boolean isInitialized = false;
    private int trapVersion = 1;

    /**
     * Default constructor.
     */
    public JoeSNMPTrapSender() {
    }

    /**
     * Skeleton method, implemented only to satisfy the requirements of the
     * JoeSNMP API.  Does nothing except spit out an error message via
     * LogLog.
     */
    public void snmpReceivedTrap(final SnmpTrapSession parm1,
                                 final InetAddress parm2,
                                 final int parm3,
                                 final SnmpOctetString parm4,
                                 final SnmpPduPacket parm5) {
        LogLog.error("This appender does not support receiving traps",
                     new java.lang.UnsupportedOperationException("Method snmpReceivedTrap() not implemented."));
    }

    /**
     * Skeleton method, implemented only to satisfy the requirements of the
     * JoeSNMP API.  Does nothing except spit out an error message via
     * LogLog.
     */
    public void snmpReceivedTrap(final SnmpTrapSession parm1,
                                 final InetAddress parm2,
                                 final int parm3,
                                 final SnmpOctetString parm4,
                                 final SnmpPduTrap parm5) {
        LogLog.error("This appender does not support receiving traps",
                     new java.lang.UnsupportedOperationException("Method snmpReceivedTrap() not implemented."));
    }

    /**
     * Skeleton method, implemented only to satisfy the requirements of the
     * JoeSNMP API.  Does nothing except spit out an error message via
     * LogLog.
     */
    public void snmpTrapSessionError(final SnmpTrapSession parm1,
                                     final int parm2,
                                     final Object parm3) {
        LogLog.error("There was a fatal error at the SNMP session layer.");
    }

    public void initialize(SNMPTrapAppender appender) {
        this.managementHost = appender.getManagementHost();
        this.managementHostTrapListenPort = appender.getManagementHostTrapListenPort();
        this.enterpriseOID = appender.getEnterpriseOID();
        this.localIPAddress = appender.getLocalIPAddress();
        this.localTrapSendPort = appender.getLocalTrapSendPort();
        this.communityString = appender.getCommunityString();
        this.sysUpTime = appender.getSysUpTime();
        this.genericTrapType = appender.getGenericTrapType();
        this.specificTrapType = appender.getSpecificTrapType();
        this.trapVersion = appender.getTrapVersion();
        this.pdu = new SnmpPduTrap();
        this.isInitialized = true;
    }

    public void addTrapMessageVariable(final String applicationTrapOIDValue,
                                       final String value) {
        //check pre-condition
        if (!this.isInitialized) {
            LogLog.error("The initialize() method must be called before calling addTrapMessageVariable()");
            return;
        }
        // add OID
        if (applicationTrapOIDValue != null) {
            this.applicationTrapOID = applicationTrapOIDValue;
        }
        final SnmpObjectId oid = new SnmpObjectId(this.applicationTrapOID);
        // set the type
        final SnmpOctetString msg = new SnmpOctetString();
        msg.setString(value);
        // create SnmpVar instance for the value and the type
        try {
            //create varbind
            final SnmpVarBind varbind = new SnmpVarBind(oid, msg);
            // add variable binding
            this.pdu.addVarBind(varbind);
        } catch (Exception e) {
            LogLog.error("Unexpected error creating SNMP bind variable: " + oid + " with value: " + value, e);
        }
    }

    public void sendTrap() {
        //check pre-condition
        if (!this.isInitialized) {
            LogLog.error("The initialize() method must be called before calling sendTrap()");
            return;
        }
        //open the session, set the PDU's values and send the packet
        try {
            this.session = new SnmpTrapSession(this, this.localTrapSendPort);
            final SnmpPeer peer = new SnmpPeer(InetAddress.getByName(this.managementHost));
            peer.setPort(this.managementHostTrapListenPort);
            final SnmpParameters snmpParms = new SnmpParameters();
            snmpParms.setReadCommunity(this.communityString);
            if (2 == this.trapVersion) {
                snmpParms.setVersion(SnmpSMI.SNMPV2);
            } else {
                snmpParms.setVersion(SnmpSMI.SNMPV1);
            }
            peer.setParameters(snmpParms);
            if (this.pdu != null) {
                this.pdu.setEnterprise(this.enterpriseOID);
                final SnmpOctetString addr = new SnmpOctetString();
                addr.setString(InetAddress.getByName(this.localIPAddress).getAddress());
                final SnmpIPAddress ipAddr = new SnmpIPAddress(addr);
                this.pdu.setAgentAddress(ipAddr);
                this.pdu.setGeneric(this.genericTrapType);
                this.pdu.setSpecific(this.specificTrapType);
                this.pdu.setTimeStamp(this.sysUpTime);
                if (this.pdu.getLength() > 0) {
                    this.session.send(peer, this.pdu);
                }
            }
        } catch (SnmpPduEncodingException ex) {
            LogLog.error("There were problems with the SNMP parameters -- could not create and send trap", ex);
        } catch (Exception e) {
            LogLog.error("There was an unexpected error", e);
        } finally {
            //this is running on a seperate thread, so make sure it gets
            //cleaned up...
            if (null != this.session && !this.session.isClosed()) this.session.close();
        }
    }
}