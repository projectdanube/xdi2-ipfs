package xdi2.core.impl.json.ipfs;

import org.ipfs.api.IPFS;
import org.ipfs.api.Multihash;

import xdi2.core.ContextNode;
import xdi2.core.Graph;
import xdi2.core.impl.AbstractGraph;

public class IPFSGraph extends AbstractGraph implements Graph {

	private static final long serialVersionUID = 8979035878235290607L;

	private IPFS ipfs;

	private IPFSContextNode rootContextNode;

	IPFSGraph(IPFSGraphFactory graphFactory, String identifier, IPFS ipfs) {

		super(graphFactory, identifier);

		this.ipfs = ipfs;

		this.rootContextNode = IPFSContextNode.load(this, null, null, Multihash.fromBase58(identifier));
		if (this.rootContextNode == null) this.rootContextNode = IPFSContextNode.empty(this, null, null);
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
	 * Misc methods
	 */

	public IPFS getIpfs() {

		return this.ipfs;
	}
}
