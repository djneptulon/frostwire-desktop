package com.limegroup.gnutella.messagehandlers;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.Executor;

import org.limewire.io.IP;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.setting.LongSetting;
import org.limewire.setting.StringArraySetting;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.UDPReplyHandlerCache;
import com.limegroup.gnutella.UDPReplyHandlerFactory;
import com.limegroup.gnutella.filters.IPList;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.RoutableGGEPMessage;

/**
 * A message handler that responds to messages only to hosts
 * contained in a simppable whitelist.
 */
abstract class RestrictedResponder implements MessageHandler {
    /** list of hosts that we can send responses to */
    private volatile IPList allowed;
    /** setting to check for updates to the host list */
    private final StringArraySetting setting;
    /** The last version of the routable message that was routed */
    private final LongSetting lastRoutedVersion;
    
    private final NetworkManager networkManager;
    private final UDPReplyHandlerFactory udpReplyHandlerFactory;
    private final UDPReplyHandlerCache udpReplyHandlerCache;
    private final Executor messageExecutorService; 
    private final NetworkInstanceUtils networkInstanceUtils;
    
    public RestrictedResponder(StringArraySetting setting, NetworkManager networkManager,
             UDPReplyHandlerFactory udpReplyHandlerFactory,
            UDPReplyHandlerCache udpReplyHandlerCache, Executor messageExecutor,
            NetworkInstanceUtils networkInstanceUtils) {
        this(setting, null, networkManager,  udpReplyHandlerFactory,
                udpReplyHandlerCache, messageExecutor, networkInstanceUtils);
    }
    
    /**
     * @param setting the setting containing the list of allowed
     * hosts to respond to.
     * @param verifier the <tt>SignatureVerifier</tt> to use.  Null if we
     * want to process all messages.
     */
    // TODO cleanup: SimmpManager registration should be done in extra initialize method
    // and also cleaned up
    public RestrictedResponder(StringArraySetting setting, 
            LongSetting lastRoutedVersion, NetworkManager networkManager,
             UDPReplyHandlerFactory udpReplyHandlerFactory,
            UDPReplyHandlerCache udpReplyHandlerCache, Executor messageExecutorService,
            NetworkInstanceUtils networkInstanceUtils) {
        this.setting = setting;
        this.lastRoutedVersion = lastRoutedVersion;
        this.networkManager = networkManager;
        this.udpReplyHandlerFactory = udpReplyHandlerFactory;
        this.udpReplyHandlerCache = udpReplyHandlerCache;
        this.messageExecutorService = messageExecutorService;
        this.networkInstanceUtils = networkInstanceUtils;
        allowed = new IPList();
        allowed.add("*.*.*.*");
        
        updateAllowed();
    }
    
    private void updateAllowed() {
        IPList newCrawlers = new IPList();
        try {
            for (String ip : setting.getValue())
                newCrawlers.add(new IP(ip));
            if (newCrawlers.isValidFilter(false, networkInstanceUtils))
                allowed = newCrawlers;
        } catch (IllegalArgumentException badSimpp) {}
    }
    
    public void simppUpdated(int newVersion) {
        updateAllowed();
    }
    
    public final void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
//	System.out.print("Handling message: " + msg);
//        if (msg instanceof RoutableGGEPMessage) {
//            // if we have a verifier, verify
//            if (verifier != null && msg instanceof SecureMessage)
//                verifier.verify((SecureMessage)msg, new SecureCallback(addr, handler));
//            else
//                processRoutableMessage((RoutableGGEPMessage)msg, addr, handler);
//        } else {
//            // just check the return address.
//            if (!allowed.contains(new IP(handler.getAddress())))
//                return;
//            processAllowedMessage(msg, addr, handler);
//        }
    }
    
    /** 
     * Processes a routable message.
     * 
     * If the message has a return address, it must have a routable version.
     * If not, it must have either a routable version or a destination address.
     */
    private void processRoutableMessage(RoutableGGEPMessage msg, InetSocketAddress addr, ReplyHandler handler) {
        
        // if the message specifies a return address, use that 
        if (msg.getReturnAddress() != null) {
            // messages with return address MUST have routable version
            if (msg.getRoutableVersion() < 0)
                return;
            handler = udpReplyHandlerFactory.createUDPReplyHandler(msg.getReturnAddress().getInetAddress(),
                    msg.getReturnAddress().getPort(), udpReplyHandlerCache.getPersonalFilter());
        } else if (msg.getDestinationAddress() != null) {
            // if there is a destination address, it must match our external address
            if (!Arrays.equals(networkManager.getExternalAddress(),
                    msg.getDestinationAddress().getInetAddress().getAddress()))
                return;
        } else if (msg.getRoutableVersion() < 0) // no routable version either? drop.
            return;

        
        if (!allowed.contains(new IP(handler.getAddress())))
            return;
        
        // check if its a newer version than the last we routed.
        long routableVersion = msg.getRoutableVersion();
        if (lastRoutedVersion != null && routableVersion > 0) {
            synchronized(lastRoutedVersion) {
                if (routableVersion <= lastRoutedVersion.getValue())
                    return;
                lastRoutedVersion.setValue(routableVersion);
            }
        }
        
        processAllowedMessage(msg, addr, handler);
        
    }
    
   

    /**
     * Process the specified message because it has been approved.
     */
    protected abstract void processAllowedMessage(Message msg, InetSocketAddress addr, ReplyHandler handler);
}
