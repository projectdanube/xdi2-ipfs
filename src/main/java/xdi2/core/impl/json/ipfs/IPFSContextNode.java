package xdi2.core.impl.json.ipfs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.ipfs.api.MerkleNode;
import org.ipfs.api.Multihash;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import xdi2.core.ContextNode;
import xdi2.core.LiteralNode;
import xdi2.core.Node;
import xdi2.core.Relation;
import xdi2.core.exceptions.Xdi2RuntimeException;
import xdi2.core.impl.AbstractContextNode;
import xdi2.core.impl.AbstractLiteralNode;
import xdi2.core.impl.memory.MemoryGraphFactory;
import xdi2.core.syntax.XDIAddress;
import xdi2.core.syntax.XDIArc;
import xdi2.core.util.iterators.ReadOnlyIterator;

public class IPFSContextNode extends AbstractContextNode implements ContextNode {

	private static final long serialVersionUID = 4930852359817860369L;
	private static final Multihash MULTIHASH_EMPTY = Multihash.fromBase58("QmStX2p9x3AV9Gdp1ArLk7bLNzZft5WCBxSLCp4NdbU3z4");

	private static final Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

	private XDIArc XDIarc;
	private MerkleNode merkleNode;
	private JsonObject ipfsData;

	IPFSContextNode(IPFSGraph graph, IPFSContextNode contextNode, XDIArc XDIarc, MerkleNode merkleNode, JsonObject ipfsData) {

		super(graph, contextNode);

		this.XDIarc = XDIarc;
		this.merkleNode = merkleNode;
		this.ipfsData = ipfsData;
	}

	@Override
	public XDIArc getXDIArc() {

		return this.XDIarc;
	}

	JsonObject makeJsonObject() {

		JsonArray ipfsLinks = new JsonArray();

		for (MerkleNode merkleNode : this.merkleNode.links) {

			JsonObject ipfsLink = new JsonObject();
			ipfsLink.addProperty("Name", merkleNode.name.get());
			ipfsLink.addProperty("Hash", merkleNode.hash.toBase58());
			ipfsLink.addProperty("Size", merkleNode.size.isPresent() ? merkleNode.size.get() : Integer.valueOf(0));

			ipfsLinks.add(ipfsLink);
		}

		JsonObject ipfsObject = new JsonObject();

		ipfsObject.addProperty("Data", new String(this.merkleNode.data.get(), StandardCharsets.UTF_8));
		ipfsObject.add("Links", ipfsLinks);

		return ipfsObject;
	}

	/*
	 * Methods related to context nodes of this context node
	 */

	@Override
	public synchronized ContextNode setContextNode(XDIArc XDIarc) {

		// check validity

		this.setContextNodeCheckValid(XDIarc);

		// set the context node

		JsonObject ipfsLink = new JsonObject();
		ipfsLink.addProperty("Name", XDIarc.toString());
		ipfsLink.addProperty("Hash", merkleNode.hash.toBase58());
		ipfsLink.addProperty("Size", merkleNode.size.isPresent() ? merkleNode.size.get() : Integer.valueOf(0));


		ContextNode contextNode = this.contextNodes.get(XDIarc);

		if (contextNode != null) {

			return contextNode;
		}

		contextNode = new IPFSContextNode((IPFSGraph) this.getGraph(), this, XDIarc);

		this.contextNodes.put(XDIarc, (IPFSContextNode) contextNode);

		// set inner root

		this.setContextNodeSetInnerRoot(XDIarc, contextNode);

		// done

		return contextNode;
	}

	@Override
	public ReadOnlyIterator<ContextNode> getContextNodes() {

		List<ContextNode> list = new ArrayList<ContextNode> (this.contextNodes.values());

		return new ReadOnlyIterator<ContextNode> (list.iterator());
	}

	@Override
	public synchronized void delContextNode(XDIArc XDIarc) {

		ContextNode contextNode = this.getContextNode(XDIarc, true);
		if (contextNode == null) return;

		// delete all inner roots and incoming relations

		((IPFSContextNode) contextNode).delContextNodeDelAllInnerRoots();
		((IPFSContextNode) contextNode).delContextNodeDelAllIncomingRelations();

		// delete this context node

		this.contextNodes.remove(XDIarc);
	}

	/*
	 * Methods related to relations of this context node
	 */

	@Override
	public synchronized Relation setRelation(XDIAddress XDIaddress, Node targetNode) {

		XDIAddress targetXDIAddress = targetNode.getXDIAddress();

		// check validity

		this.setRelationCheckValid(XDIaddress, targetXDIAddress);

		// set the relation

		Relation relation = this.getRelation(XDIaddress, targetXDIAddress);
		if (relation != null) return relation;

		Map<XDIAddress, IPFSRelation> relations = this.relations.get(XDIaddress);
		if (relations == null) {

			if (((IPFSGraph) this.getGraph()).getSortMode() == MemoryGraphFactory.SORTMODE_ALPHA) {

				relations = new TreeMap<XDIAddress, IPFSRelation> ();
			} else if (((IPFSGraph) this.getGraph()).getSortMode() == MemoryGraphFactory.SORTMODE_ORDER) {

				relations = new LinkedHashMap<XDIAddress, IPFSRelation> ();
			} else {

				relations = new HashMap<XDIAddress, IPFSRelation> ();
			}

			this.relations.put(XDIaddress, relations);
		}

		relation = new IPFSRelation(this, XDIaddress, targetXDIAddress);

		relations.put(targetXDIAddress, (IPFSRelation) relation);

		// done

		return relation;
	}

	@Override
	public ReadOnlyIterator<Relation> getRelations() {

		List<Relation> list = new ArrayList<Relation> ();

		for (Entry<XDIAddress, Map<XDIAddress, IPFSRelation>> relations : this.relations.entrySet()) {

			list.addAll(relations.getValue().values());
		}

		return new ReadOnlyIterator<Relation> (list.iterator());
	}

	@Override
	public synchronized void delRelation(XDIAddress XDIaddress, XDIAddress targetXDIAddress) {

		// delete the relation

		Map<XDIAddress, IPFSRelation> relations = this.relations.get(XDIaddress);
		if (relations == null) return;

		IPFSRelation relation = relations.remove(targetXDIAddress);
		if (relation == null) return;

		if (relations.isEmpty()) {

			this.relations.remove(XDIaddress);
		}

		// delete inner root

		this.delRelationDelInnerRoot(XDIaddress, targetXDIAddress);
	}

	/*
	 * Methods related to literals of this context node
	 */

	@Override
	public synchronized LiteralNode setLiteralNode(Object literalData) {

		// check validity

		this.setLiteralCheckValid(literalData);

		// set the literal

		this.literalNode = new IPFSLiteralNode(this, literalData);

		// done

		return this.literalNode;
	}

	@Override
	public LiteralNode getLiteralNode() {

		JsonElement jsonElement = this.jsonObject.get("&");
		if (jsonElement == null) return null;

		return new IPFSLiteralNode(this, AbstractLiteralNode.jsonElementToLiteralData(jsonElement));
	}

	@Override
	public synchronized void delLiteralNode() {

		this.jsonObject.remove("&");
	}

	/*
	 * Retrieve/store methods
	 */

	static IPFSContextNode load(IPFSGraph graph, IPFSContextNode contextNode, XDIArc XDIarc, Multihash multihash) {

		MerkleNode merkleNode;
		JsonObject jsonObject;

		try {

			merkleNode = graph.getIpfs().object.get(multihash);
			jsonObject = gson.getAdapter(JsonObject.class).fromJson(new InputStreamReader(new ByteArrayInputStream(merkleNode.data.get()), "UTF-8"));
		} catch (IOException ex) {

			throw new Xdi2RuntimeException("Cannot load merkle node for multihash " + multihash.toBase58() + ": " + ex.getMessage(), ex);
		}

		return new IPFSContextNode(graph, contextNode, XDIarc, merkleNode, jsonObject);
	}

	static IPFSContextNode empty(IPFSGraph graph, IPFSContextNode contextNode, XDIArc XDIarc) {

		return load(graph, contextNode, XDIarc, MULTIHASH_EMPTY);
	}

	void store(IPFSContextNode childContextNode) {

		JsonArray ipfsLinks = new JsonArray();
		JsonObject ipfsObjects = new JsonObject();

		for (ContextNode contextNode : parentContextNode.getContextNodes()) {

			MerkleNode merkleNode = contextNodeToIPFS(ipfs, contextNode);


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
}
