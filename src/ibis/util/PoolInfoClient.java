/* $Id$ */

package ibis.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

import smartsockets.direct.IPAddressSet;

/**
 * The <code>PoolInfoClient</code> class provides a utility for finding out
 * information about the nodes involved in a closed-world run.
 * It is a client for a
 * {@link ibis.util.PoolInfoServer PoolInfoServer}.
 * The best way to access pool information is to obtain a
 * {@link ibis.util.PoolInfo PoolInfo} by means of the
 * {@link ibis.util.PoolInfo#createPoolInfo() PoolInfo.createPoolInfo} static
 * method. This is the most flexible, only creating a
 * <code>PoolInfoClient</code> when a more knowledgeable <code>PoolInfo</code>
 * cannot be created.
 * <br>
 * The <code>PoolInfoClient</code> class depends on the following
 * system properties:
 * <br>
 * <pre>ibis.pool.total_hosts</pre>
 * must be present, and contain the total number of hosts involved in the run.
 * <br>
 * <pre>ibis.pool.cluster</pre>
 * must contain the cluster name of the current node. If not present,
 * "unknown" is used.
 * <br>
 * <pre>ibis.pool.pool</pre>
 * must contain the poolId identifying the current run. If not present,
 * the <code>ibis.registry.pool</code> is tried. If that is not present
 * either, "unknown" is used.
 * <br>
 * <pre>ibis.pool.server.port</pre>
 * must contain the port number on which the <code>PoolInfoServer</code>
 * is accepting connections. If not present, <code>ibis.registry.port</code>
 * is tried. If present, 1 is added and that is used as port number. If not,
 * the default is used.
 * <br>
 * <pre>ibis.pool.server.host</pre>
 * must contain the hostname of the host on which the
 * <code>PoolInfoServer</code> runs. If not present,
 * <code>ibis.registry.host</code> is tried.
 * One of the two system properties must be defined.
 * <br>
 */
public class PoolInfoClient extends PoolInfo {
    private String[] host_clusters;

    private static PoolInfoClient instance;

    /**
     * For testing purposes: a main program.
     */
    public static void main(String[] argv) {
        try {
            PoolInfoClient test = create();
            System.out.println(test.toString());
        } catch (Exception e) {
            System.out.println("Got exception: ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates a <code>PoolInfoClient</code> if not already present.
     * @return the <code>PoolInfoClient</code>.
     */
    public static PoolInfoClient create() {
        if (instance == null) {
            instance = new PoolInfoClient(System.getProperties());
        }
        return instance;
    }

    private PoolInfoClient(Properties prop) {
        super(0);

        // Dirty hack for Mathijs --- Jason
        completeLocalAddress = IPAddressSet.getLocalHost();
        // End hack...
        
        InetAddress serverAddress;
        InetAddress myAddress = IPUtils.getAlternateLocalHostAddress();
        TypedProperties props = new TypedProperties(prop);

        total_hosts = props.getIntProperty(s_total);
        int remove_doubles = props.booleanProperty(s_single) ? 1 : 0;

        int serverPort = props.getIntProperty(s_port, -1);
        if (serverPort == -1) {
            serverPort = props.getIntProperty("ibis.registry.port",
                    -1);
            if (serverPort == -1) {
                serverPort = PoolInfoServer.POOL_INFO_PORT;
            } else {
                serverPort++;IPAddressSet.getLocalHost();
                
            }
        }
        String serverName = props.getProperty(s_host);
        if (serverName == null) {
            serverName
                    = props.getProperty("ibis.registry.host");
            if (serverName == null) {
                throw new RuntimeException("property " + s_host
                        + " is not specified");
            }
        }

        if (serverName.equals("localhost")) {
            serverName = myAddress.getHostName();
        }

        String pool = props.getProperty(s_pool);
        if (pool == null) {
            pool = props.getProperty("ibis.registry.pool");
            if (pool == null) {
                pool = "unknown";
            }
        }
        try {
            serverAddress = InetAddress.getByName(serverName);
        } catch (UnknownHostException e) {
            throw new RuntimeException("cannot get ip of pool server");
        }

        if (serverAddress.equals(myAddress)) {
            try {
                PoolInfoServer p = new PoolInfoServer(serverPort, true);
                p.setDaemon(true);
                p.start();
            } catch (Throwable e) {
                // Ignored. Assume already present ...
            }
        }

        Socket socket = null;
        int cnt = 0;
        while (socket == null) {
            try {
                cnt++;
                socket = new Socket(serverAddress, serverPort);
            } catch (Exception e) {
                if (cnt == 60) {
                    System.err.println("Could not connect to PoolInfoServer "
                            + "on " + serverAddress + ":" + serverPort
                            + " after 60 seconds; giving up ...");
                    throw new RuntimeException("Got exception: " + e);
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
        
        DataOutputStream out = null;
        ObjectInputStream in = null;
        try {
            out = new DataOutputStream(
                    new BufferedOutputStream(socket.getOutputStream()));
           
            out.writeUTF(pool);
            out.writeInt(total_hosts);
            out.writeInt(remove_doubles);
            out.writeUTF(clusterName);
            
            // Dirty hack for Mathijs --- Jason
            out.writeUTF(completeLocalAddress.toString());
            // End hack...
            
            out.flush();

            in = new ObjectInputStream(
                    new BufferedInputStream(socket.getInputStream()));
            host_number = in.readInt();
            if (host_number == -1) {
                throw new RuntimeException("This node is already registered");
            }
            total_hosts = in.readInt();
            host_clusters = (String[]) in.readObject();
            hosts = (InetAddress[]) in.readObject();
            completeHosts = (IPAddressSet []) in.readObject();
            
            host_names = new String[total_hosts];
            
            
            for (int i = 0; i < total_hosts; i++) {
                host_names[i] = hosts[i].getHostName();
            }

        } catch (Exception e) {
            throw new RuntimeException("Got exception: " + e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                socket.close();
            } catch(Exception e) {
                // ignored
            }
        }

        if (host_number >= total_hosts || host_number < 0 || total_hosts < 1) {
            throw new RuntimeException("Sanity check on host numbers failed!");
        }
    }

    public String clusterName() {
        return host_clusters[host_number];
    }

    public String clusterName(int rank) {
        return host_clusters[rank];
    }

    public String[] clusterNames() {
        return (String[]) host_clusters.clone();
    }

    public String toString() {
        String result = "pool info: size = " + total_hosts + "; my rank is "
                + host_number + "; host list:\n";
        for (int i = 0; i < total_hosts; i++) {
            result += i + ": address=" + hosts[i] + " complete=" + 
                completeHosts[i].toString() + " cluster="
                    + host_clusters[i] + "\n";
        }
        return result;
    }
}
