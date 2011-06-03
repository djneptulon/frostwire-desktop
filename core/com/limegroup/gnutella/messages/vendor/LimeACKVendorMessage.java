package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.limewire.service.ErrorService;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.GGEP;

/** In Vendor Message parlance, the "message type" of this VMP is "LIME/11".
 *  This message acknowledges (ACKS) the guid contained in the message (i.e. A 
 *  sends B a message with GUID g, B can acknowledge this message by sending a 
 *  LimeACKVendorMessage to A with GUID g).  It also contains the amount of
 *  results the client wants.
 *
 *  This message must maintain backwards compatibility between successive
 *  versions.  This entails that any new features would grow the message
 *  outward but shouldn't change the meaning of older fields.  This could lead
 *  to some issues (i.e. abandoning fields does not allow for older fields to
 *  be reused) but since we don't expect major changes this is probably OK.
 *  EXCEPTION: Version 1 is NEVER accepted.  Only version's 2 and above are
 *  recognized.
 *
 *  Note that this behavior of maintaining backwards compatiblity is really
 *  only necessary for UDP messages since in the UDP case there is probably no
 *  MessagesSupportedVM exchange.
 *  
 *  @version 3
 *  
 *  * Adds a security token to prevent clients from spoofing their ip and just sending
 *  results back after a little while
 */
public final class LimeACKVendorMessage extends AbstractVendorMessage {

    public static final int VERSION = 3;
    
    public static final int OLD_VERSION = 2;
    
    private static final int PAYLOAD_MIN_LENGTH_V3 = derivePayloadV3(255, new byte[1]).length;

    /**
     * Constructs a new LimeACKVendorMessage with data from the network.
     */
    LimeACKVendorMessage(byte[] guid, byte ttl, byte hops, int version, 
                          byte[] payload, Network network) 
        throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_LIME_ACK, version,
              payload, network);
        if (getVersion() == 1)
            throw new BadPacketException("UNSUPPORTED OLD VERSION");
        if (getPayload().length < 1)
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         getPayload().length);
        if ((getVersion() == OLD_VERSION) && (getPayload().length != 1))
            throw new BadPacketException("VERSION 2 UNSUPPORTED PAYLOAD LEN: " +
                                         getPayload().length);
        if ((getVersion() == VERSION) && (getPayload().length < PAYLOAD_MIN_LENGTH_V3))
            throw new BadPacketException("VERSION 3 should have a GGEP");
    }

    /**
     * Constructs a new LimeACKVendorMessage to be sent out.
     *  @param numResults The number of results (0-255 inclusive) that you want
     *  for this query.  If you want more than 255 just send 255.
     *  @param replyGUID The guid of the original query/reply that you want to
     *  send reply info for.
     *  @param addressSecurityToken the query key that is sent along to make sure the 
     *  opposite side is not spoofing their ip address
     */
    public LimeACKVendorMessage(GUID replyGUID, 
                                int numResults) {
        super(F_LIME_VENDOR_ID, F_LIME_ACK, OLD_VERSION,
                derivePayload(numResults));
        setGUID(replyGUID);
    }
    
    /** @return an int (0-255) representing the amount of results that a host
     *  wants for a given query (as specified by the guid of this message).
     */
    public int getNumResults() {
        return ByteOrder.ubyte2int(getPayload()[0]);
    }
    
    
    /**
     * Constructs the payload for a LimeACKVendorMessage with the given
     * number of results.
     */
    private static byte[] derivePayload(int numResults) {
        if ((numResults < 0) || (numResults > 255))
            throw new IllegalArgumentException("Number of results too big: " +
                                               numResults);
        byte[] payload = new byte[1];
        byte[] bytes = new byte[2];
        ByteOrder.short2leb((short) numResults, bytes, 0);
        payload[0] = bytes[0];
        return payload;
    }
    
    private static byte[] derivePayloadV3(int numResults, byte[] securityTokenBytes) {
        if ((numResults <= 0) || (numResults > 255))
            throw new IllegalArgumentException("Number of results too big: " +
                                               numResults);
        byte[] bytes = new byte[2];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteOrder.short2leb((short) numResults, bytes, 0);
        out.write(bytes[0]); 

        GGEP ggep = new GGEP();
        ggep.put(GGEP.GGEP_HEADER_SECURE_OOB, securityTokenBytes);
        try {
            ggep.write(out);
        }
        catch(IOException iox) {
            ErrorService.error(iox); // impossible.
        }
        return out.toByteArray();
    }

    public boolean equals(Object other) {
//        if (other instanceof LimeACKVendorMessage) {
//            LimeACKVendorMessage o = (LimeACKVendorMessage)other;
//            GUID myGuid = new GUID(getGUID());
//            GUID otherGuid = new GUID(o.getGUID());
//            int otherResults = o.getNumResults();
//            return ((myGuid.equals(otherGuid)) && 
//                    (getNumResults() == otherResults) &&
//                    areEqualTokens(getSecurityToken(), o.getSecurityToken()) &&
//                    super.equals(other));
//        }
        return false;
    }
    /** Overridden purely for stats handling.
     */
    protected void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(super.toString());
        builder.append(", num results: ").append(getNumResults());
        //builder.append(", security token: ").append(getSecurityToken());
        return builder.toString();
    }

   
    
}
