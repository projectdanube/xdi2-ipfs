package xdi2.core.impl.json.ipfs;

import java.io.IOException;

import org.ipfs.api.IPFS;
import org.ipfs.api.MultiAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.core.GraphFactory;
import xdi2.core.impl.AbstractGraphFactory;

/**
 * GraphFactory that creates JSON graphs in IPFS.
 * 
 * @author markus
 */
public class IPFSGraphFactory extends AbstractGraphFactory implements GraphFactory {

	private static final Logger log = LoggerFactory.getLogger(IPFSGraphFactory.class);

	public static final String  DEFAULT_IPFS_MULTIADDRESS = "/ip4/127.0.0.1/tcp/5001";

	private String ipfsMultiaddress;

	public IPFSGraphFactory() { 

		super();

		this.ipfsMultiaddress = DEFAULT_IPFS_MULTIADDRESS;
	}

	@Override
	public IPFSGraph openGraph(String identifier) throws IOException {

		IPFS ipfs = new IPFS(new MultiAddress(this.getIpfsMultiaddress()));

		IPFSGraph graph = new IPFSGraph(this, identifier, ipfs);

		return graph;
	}

	/*
	 * Getters and setters
	 */

	public String getIpfsMultiaddress() {

		return this.ipfsMultiaddress;
	}

	public void setIpfsMultiaddress(String ipfsMultiaddress) {

		this.ipfsMultiaddress = ipfsMultiaddress;
	}
}
