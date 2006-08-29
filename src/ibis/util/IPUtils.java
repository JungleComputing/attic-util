/* $Id$ */

package ibis.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import org.apache.log4j.Logger;

/**
 * Some utilities that deal with IP addresses.
 */
public class IPUtils {
    private static final String prefix = "ibis.util.ip.";

    private static final String addr = prefix + "address";

    private static final String alt_addr = prefix + "alt-address";

    private static final String networkInterface = prefix + "interface";

    private static final String altNetworkInterface = prefix + "alt-interface";

    private static final String[] sysprops =
            { addr, alt_addr, networkInterface, altNetworkInterface };

    static {
        TypedProperties.checkProperties(prefix, sysprops, null);
    }

    private static InetAddress localaddress = null;

    private static InetAddress alt_localaddress = null;

    private static InetAddress detected = null;

    static Logger logger =
            ibis.util.GetLogger.getLogger(IPUtils.class.getName());

    private IPUtils() {
        /* do nothing */
    }

    /**
     * Returns true if the specified address is an external address.
     * External means not a site local, link local, or loopback address.
     * @param address the specified address.
     * @return <code>true</code> if <code>addr</code> is an external address.
     */
    public static boolean isExternalAddress(InetAddress address) {
        if (address.isLoopbackAddress()) {
            return false;
        }
        if (address.isLinkLocalAddress()) {
            return false;
        }
        if (address.isSiteLocalAddress()) {
            return false;
        }

        return true;
    }

    /**
     * Returns the {@link java.net.InetAddress} associated with the local host.
     *  
     * If the ibis.util.ip.address property is specified and set to a specific 
     * IP address (e.g., "192.168.1.150"), that address is tried first. 
     * 
     * If the ibis.util.ip.address property is specified and set to a subnet and 
     * netmask (e.g., "192.168.0.0/255.255.0.0"), getLocalHostAddress will try 
     * to find a matching address. 
     * 
     * If the ibis.util.ip.interface property is set (e.g., "eth1") 
     * getLocalHostAddress will try to find an address bound to that network 
     * interface. 
     * 
     * If no properties are set or the above lookups fail, getLocalHostAddress 
     * will try to find a (preferrably external) IP address on any available 
     * network interface. 
     * 
     * @return the resulting {@link java.net.InetAddress}.
     */
    public static InetAddress getLocalHostAddress() {
        if (localaddress == null) {

            // Check if an IP address is specified by the user.
            String tmp = System.getProperties().getProperty(addr);

            if (tmp != null) {
                localaddress = doWorkGetSpecificHostAddress(tmp);

                if (localaddress == null) {
                    logger.error("The specified IP address " + tmp
                        + " could not be found!");
                }
            }

            if (localaddress == null) {
                // Check if a  network interface is specified by the user.
                tmp = System.getProperties().getProperty(networkInterface);

                if (tmp != null) {
                    localaddress = doWorkGetSpecificNetworkInterface(tmp, null);

                    if (localaddress == null) {
                        logger.error("The specified network interface " + tmp
                            + " could not be found!");
                    }
                }
            }

            if (localaddress == null) {
                // Fall back to 'normal' rountine            
                localaddress = doWorkGetLocalHostAddress();
            }

            // To make sure that a hostname is filled in:
            localaddress.getHostName();

            logger.debug("Found address: " + localaddress);
        }
        return localaddress;
    }

    /**
     * Returns the {@link java.net.InetAddress} associated with the local host.
     *       
     * If the ibis.util.ip.alt-address property is specified and set to a 
     * specific IP address (e.g., "192.168.1.150"), that address is tried first. 
     * 
     * If the ibis.util.ip.alt-address property is specified and set to a subnet
     * and netmask (e.g., "192.168.0.0/255.255.0.0"), 
     * getAlternateLocalHostAddress will try to find a matching address. 
     * 
     * If the ibis.util.ip.alt-interface property is set (e.g., "eth1") 
     * getAlternateLocalHostAddress will try to find an address bound to that
     * network interface. 
     * 
     * If no properties are set or the above lookups fail, 
     * getAlternateLocalHostAddress will try to find a (preferrably external) IP
     * address on any available network interface. 
     * 
     * @return the resulting {@link java.net.InetAddress}.
     */
    public static InetAddress getAlternateLocalHostAddress() {
        if (alt_localaddress == null) {

            // Check if an IP address is specified by the user.
            String tmp = System.getProperties().getProperty(alt_addr);

            if (tmp != null) {
                alt_localaddress = doWorkGetSpecificHostAddress(tmp);

                if (alt_localaddress == null) {
                    logger.error("The specified (alt) IP address " + tmp
                        + " could not be found!");
                }
            }

            if (alt_localaddress == null) {
                // Check if a  network interface is specified by the user.
                tmp = System.getProperties().getProperty(altNetworkInterface);

                if (tmp != null) {
                    alt_localaddress =
                            doWorkGetSpecificNetworkInterface(tmp, null);

                    if (alt_localaddress == null) {
                        logger.error("The specified (alt) network interface "
                            + tmp + " could not be found!");
                    }
                }
            }

            if (alt_localaddress == null) {
                alt_localaddress = doWorkGetLocalHostAddress();
            }

            // To make sure that a hostname is filled in:
            alt_localaddress.getHostName();

            logger.debug("Found alt address: " + alt_localaddress);
        }
        return alt_localaddress;
    }

    private static byte[] addressToBytes(String address) {

        try {
            InetAddress tmp = InetAddress.getByName(address);
            return tmp.getAddress();
        } catch (UnknownHostException e) {
            logger.debug("Failed to convert " + address + " to bytes!");
            return new byte[0];
        }
    }

    private static boolean matchAddress(byte[] sub, byte[] mask, byte[] ad) {

        if (sub.length != ad.length) {
            // Not sure how to mix IPv4 and IPv6 yet ...
            return false;
        }

        for (int i = 0; i < sub.length; i++) {

            if ((ad[i] & mask[i]) != (sub[i] & mask[i])) {
                return false;
            }
        }

        return true;
    }

    private static InetAddress doWorkGetSpecificHostAddress(String ha) {

        InetAddress address = null;
        byte[] subnet = null;
        byte[] netmask = null;

        int index = ha.indexOf("/");

        if (index == -1) {
            logger.debug("Simple network address specified: " + ha);
            try {
                address = InetAddress.getByName(ha);
            } catch (UnknownHostException e) {
                logger.debug("Simple network address " + ha
                    + " could not be resolved!");
            }
        } else {
            logger.debug("Subnet/netmask specified: " + ha);

            subnet = addressToBytes(ha.substring(0, index));
            netmask = addressToBytes(ha.substring(index + 1));

            InetAddress[] all = null;

            try {
                String hostname = InetAddress.getLocalHost().getHostName();
                all = InetAddress.getAllByName(hostname);
            } catch (UnknownHostException e) {
                logger.debug("Unable to retrieve any IP addresses!");
            }

            if (all != null) {
                for (int i = 0; i < all.length; i++) {
                    if (matchAddress(subnet, netmask, all[i].getAddress())) {
                        logger.debug("Address " + all[i] + " matches " + ha);
                        address = all[i];
                        break;
                    } else {
                        logger.debug("Address " + all[i] + " does not match "
                            + ha);
                    }
                }
            }

            logger.debug("No address matching " + ha + " found");
        }

        if (address != null) {
            logger.info("Specified IP address found: " + address);
            return address;
        }

        Enumeration e = null;

        try {
            e = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            logger.debug("Could not get network interfaces.");
            return null;
        }

        /*
         * Preference order:
         * 1. external and IPv4.
         * 2. external
         * 3. sitelocal and IPv4
         * 4. sitelocal
         * 5. Ipv4
         * 6. other
         */

        while (e.hasMoreElements()) {

            NetworkInterface nw = (NetworkInterface) e.nextElement();

            logger.debug("Trying interface: " + nw.getName());

            Enumeration e2 = nw.getInetAddresses();

            while (e2.hasMoreElements()) {
                InetAddress tmp = (InetAddress) e2.nextElement();

                if (matchAddress(subnet, netmask, tmp.getAddress())) {
                    logger.debug("Address " + tmp + " matches " + ha);
                    return tmp;
                } else {
                    logger.debug("Address " + tmp + " does mot match " + ha);
                }
            }
        }

        logger.debug("No IP address matching " + ha + " was found");

        return null;
    }

    private static InetAddress doWorkGetSpecificNetworkInterface(String ni,
            InetAddress known) {

        InetAddress external = known;
        InetAddress internal = null;

        String externalNI = null;
        String internalNI = null;

        boolean first = true;

        Enumeration e = null;

        try {
            e = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            logger.debug("Could not get network interfaces.");
            return null;
        }

        /*
         * Preference order:
         * 1. external and IPv4.
         * 2. external
         * 3. sitelocal and IPv4
         * 4. sitelocal
         * 5. Ipv4
         * 6. other
         */

        while (e.hasMoreElements()) {

            NetworkInterface nw = (NetworkInterface) e.nextElement();

            if (ni == null || nw.getName().equals(ni)) {

                logger.debug("Trying interface: " + nw.getName());

                Enumeration e2 = nw.getInetAddresses();

                while (e2.hasMoreElements()) {
                    InetAddress tmp = (InetAddress) e2.nextElement();

                    if (isExternalAddress(tmp)) {
                        if (external == null) {
                            external = tmp;
                            externalNI = nw.getName();
                        } else if (external.equals(tmp)) {
                            if (externalNI == null) {
                                externalNI = nw.getName();
                            }
                        } else if (!(external instanceof Inet4Address)
                            && tmp instanceof Inet4Address) {
                            // Preference for IPv4
                            external = tmp;
                            externalNI = nw.getName();
                        } else {
                            if (first) {
                                first = false;
                                logger.info("WARNING, this machine has "
                                    + "more than one external IP "
                                    + "address, using " + external
                                    + " but found " + tmp + " as well");
                            } else {
                                logger
                                    .info("... and found " + tmp + " as well");
                            }
                        }
                    } else if (internal == null) {
                        internal = tmp;
                        internalNI = nw.getName();
                    } else if (tmp.isSiteLocalAddress()) {
                        if (!internal.isSiteLocalAddress()
                            || !(internal instanceof Inet4Address)) {
                            internal = tmp;
                            internalNI = nw.getName();
                        }
                    } else {
                        if (!internal.isSiteLocalAddress()
                            && !(internal instanceof Inet4Address)) {
                            internal = tmp;
                            internalNI = nw.getName();
                        }
                    }
                }
            }
        }

        if (external != null) {
            logger.debug("Found external address " + external + " on network "
                + "interface " + externalNI);
            return external;
        }

        if (internal != null) {
            logger.debug("Found internal address " + internal + " on network "
                + "interface " + internalNI);

            return internal;
        }

        logger.debug("Did not find any suitable address"
            + (ni == null ? "" : (" on network interface " + ni)));
        return null;
    }

    private static InetAddress doWorkGetLocalHostAddress() {
        if (detected != null) {
            return detected;
        }

        InetAddress external = null;

        InetAddress[] all = null;
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            all = InetAddress.getAllByName(hostname);
        } catch (java.net.UnknownHostException e) {
            logger.debug("InetAddress.getLocalHost().getHostName() failed");
        }
        
        if (all != null) {
            for (int i = 0; i < all.length; i++) {
                if (isExternalAddress(all[i])) {
                    external = all[i];
                    logger.debug("trying address: " + external + " EXTERNAL");
                    external = doWorkGetSpecificNetworkInterface(null, external);

                    if(external != null) break;
                }
            }
        }

        if (external == null) {
            try {
                InetAddress a = InetAddress.getLocalHost();

                if (a != null) {
                    String name = a.getHostName();
                    external =
                            InetAddress.getByName(InetAddress.getByName(name)
                                .getHostAddress());
                }
            } catch (UnknownHostException ex) {
                // ignore
            }
        }

        if (external == null) {
            logger.error("Could not find local IP address, you "
                + "should specify the "
                + "-Dibis.util.ip.address=A.B.C.D option");
            return null;
        }

        detected = external;
        return external;
    }
}
