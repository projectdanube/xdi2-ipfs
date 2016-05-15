package xdi2.core.impl.json.ipfs;

import java.io.IOException;

import org.ipfs.api.IPFS;
import org.ipfs.api.Multihash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.core.ContextNode;
import xdi2.core.Graph;
import xdi2.core.exceptions.Xdi2RuntimeException;
import xdi2.core.impl.AbstractGraph;

public class IPFSGraph extends AbstractGraph implements Graph {

	private static final long serialVersionUID = 8979035878235290607L;

	private static final Logger log = LoggerFactory.getLogger(IPFSContextNode.class);

	private IPFS ipfs;
	private Multihash ipnsMultihash;
	private IPFSContextNode rootContextNode;

	IPFSGraph(IPFSGraphFactory graphFactory, String identifier, IPFS ipfs) {

		super(graphFactory, identifier);

		this.ipfs = ipfs;

		if (identifier.startsWith("/ipfs/")) {

			Multihash ipfsMultihash = Multihash.fromBase58(identifier.substring(6));

			if (log.isInfoEnabled()) log.info("Loading IPFS multihash " + ipfsMultihash);

			this.rootContextNode = IPFSContextNode.load(this, null, null, ipfsMultihash);
			if (this.rootContextNode == null) this.rootContextNode = IPFSContextNode.empty(this, null, null);
		} else if (identifier.startsWith("/ipns/")) {

			this.ipnsMultihash = Multihash.fromBase58(identifier.substring(6));

			String resolveIpfsMultihash = null;

			try {

				resolveIpfsMultihash = ipfs.name.resolve(this.ipnsMultihash);
			} catch (IOException ex) {

				throw new Xdi2RuntimeException("Unable to resolve IPNS multihash " + this.ipnsMultihash.toBase58() + ": " + ex, ex);
			}

			if (log.isInfoEnabled()) log.info("Resolved IPNS multihash " + this.ipnsMultihash + " to " + resolveIpfsMultihash);

			if (resolveIpfsMultihash == null) {

				this.rootContextNode = IPFSContextNode.empty(this, null, null);
			} else {

				Multihash ipfsMultihash = Multihash.fromBase58(resolveIpfsMultihash.substring(6));

				if (log.isInfoEnabled()) log.info("Loading IPFS multihash " + ipfsMultihash);

				this.rootContextNode = IPFSContextNode.load(this, null, null, ipfsMultihash);
				if (this.rootContextNode == null) this.rootContextNode = IPFSContextNode.empty(this, null, null);
			}
		} else {

			// TODO
		}
	}

	@Override
	public ContextNode getRootContextNode(boolean subgraph) {

		return this.rootContextNode;
	}

	@Override
	public void close() {

		this.rootContextNode = null;
	}

	/*
	 * Retrieve/store methods
	 */

	void store() {

		if (this.ipnsMultihash != null) {

			try {

				this.getIpfs().name.publish(this.rootContextNode.ipfsMerkleNode.hash);

				if (log.isInfoEnabled()) log.info("Published IPNS multihash " + this.ipnsMultihash + " to IPFS multihash " + this.rootContextNode.ipfsMerkleNode.hash);
			} catch (IOException ex) {

				throw new Xdi2RuntimeException("Cannot publish name: " + ex.getMessage(), ex);
			}
		}
	}

	/*
	 * Getters and setters
	 */

	public IPFS getIpfs() {

		return this.ipfs;
	}
}














