package xdi2.core.impl.json.ipfs;

import java.util.Collections;

import org.ipfs.api.IPFS;
import org.ipfs.api.MerkleNode;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import xdi2.core.ContextNode;
import xdi2.core.Graph;
import xdi2.core.Relation;
import xdi2.core.impl.AbstractGraph;
import xdi2.core.impl.AbstractLiteralNode;

public class IPFSGraph extends AbstractGraph implements Graph {

	private static final long serialVersionUID = 8979035878235290607L;

	private IPFS ipfs;

	private IPFSContextNode rootContextNode;

	IPFSGraph(IPFSGraphFactory graphFactory, String identifier, IPFS ipfs) {

		super(graphFactory, identifier);

		this.ipfs = ipfs;

		this.rootContextNode = new IPFSContextNode(this, null, null);
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
