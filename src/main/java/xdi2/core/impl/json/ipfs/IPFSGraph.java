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
	 * Retrieve/store methods
	 */
	
	void store(IPFSContextNode contextNode) {

		JsonArray ipfsLinks = new JsonArray();
		JsonObject ipfsObjects = new JsonObject();

		for (ContextNode contextNode : parentContextNode.getContextNodes()) {

			MerkleNode merkleNode = contextNodeToIPFS(ipfs, contextNode);

			JsonObject ipfsLink = new JsonObject();
			ipfsLink.addProperty("Name", contextNode.getXDIArc().toString());
			ipfsLink.addProperty("Hash", merkleNode.hash.toBase58());
			ipfsLink.addProperty("Size", merkleNode.size.isPresent() ? merkleNode.size.get() : Integer.valueOf(0));

			ipfsLinks.add(ipfsLink);
		}

		JsonObject ipfsData = new JsonObject();

		for (Relation relation : parentContextNode.getRelations()) {

			ipfsData.addProperty("/" + relation.getXDIAddress().toString(), relation.getTargetXDIAddress().toString());
		}

		if (parentContextNode.containsLiteralNode()) { 

			ipfsData.add("&", AbstractLiteralNode.literalDataToJsonElement(parentContextNode.getLiteralData()));
		}

		ipfsObjects.addProperty("Data", gson.toJson(ipfsData));
		ipfsObjects.add("Links", ipfsLinks);

		System.out.println(gson.toJson(ipfsObjects));

		MerkleNode parentMerkleNode = ipfs.object.put(Collections.singletonList(gson.toJson(ipfsObjects).getBytes("UTF-8"))).get(0);
		System.out.println(parentContextNode.getXDIAddress() + " --> " + parentMerkleNode.hash.toBase58());
		return parentMerkleNode;
	}
	
	/*
	 * Misc methods
	 */

	public IPFS getIpfs() {

		return this.ipfs;
	}
}
