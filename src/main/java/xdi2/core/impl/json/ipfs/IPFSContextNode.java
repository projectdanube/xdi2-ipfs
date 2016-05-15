package xdi2.core.impl.json.ipfs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ipfs.api.MerkleNode;
import org.ipfs.api.Multihash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import xdi2.core.ContextNode;
import xdi2.core.LiteralNode;
import xdi2.core.Node;
import xdi2.core.Relation;
import xdi2.core.exceptions.Xdi2RuntimeException;
import xdi2.core.impl.AbstractContextNode;
import xdi2.core.impl.AbstractLiteralNode;
import xdi2.core.syntax.XDIAddress;
import xdi2.core.syntax.XDIArc;
import xdi2.core.util.iterators.ReadOnlyIterator;

public class IPFSContextNode extends AbstractContextNode implements ContextNode {

	private static final long serialVersionUID = 4930852359817860369L;

	private static final Logger log = LoggerFactory.getLogger(IPFSContextNode.class);

	private static final Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

	public static final Multihash MULTIHASH_EMPTY = Multihash.fromBase58("QmStX2p9x3AV9Gdp1ArLk7bLNzZft5WCBxSLCp4NdbU3z4");

	private XDIArc XDIarc;

	MerkleNode ipfsMerkleNode;
	Map<XDIArc, MerkleNode> ipfsLinks;
	JsonObject ipfsData;

	IPFSContextNode(IPFSGraph graph, IPFSContextNode contextNode, XDIArc XDIarc, MerkleNode ipfsMerkleNode, Map<XDIArc, MerkleNode> ipfsLinks, JsonObject ipfsData) {

		super(graph, contextNode);

		this.XDIarc = XDIarc;

		this.ipfsMerkleNode = ipfsMerkleNode;
		this.ipfsLinks = ipfsLinks;
		this.ipfsData = ipfsData;
	}

	@Override
	public XDIArc getXDIArc() {

		return this.XDIarc;
	}

	/*
	 * Methods related to context nodes of this context node
	 */

	@Override
	public synchronized ContextNode setContextNode(XDIArc XDIarc) {

		// check validity

		this.setContextNodeCheckValid(XDIarc);

		// set the context node

		MerkleNode ipfsMerkleNode = this.ipfsLinks.get(XDIarc);
		if (ipfsMerkleNode != null) return load((IPFSGraph) this.getGraph(), this, XDIarc, ipfsMerkleNode.hash);

		IPFSContextNode contextNode = empty((IPFSGraph) this.getGraph(), this, XDIarc);
		this.ipfsLinks.put(XDIarc, contextNode.ipfsMerkleNode);

		// store context node

		this.store();

		// set inner root

		this.setContextNodeSetInnerRoot(XDIarc, contextNode);

		// done

		return contextNode;
	}

	@Override
	public ReadOnlyIterator<ContextNode> getContextNodes() {

		List<ContextNode> list = new ArrayList<ContextNode> ();

		// get context nodes

		for (Entry<XDIArc, MerkleNode> entry : this.ipfsLinks.entrySet()) {

			XDIArc key = entry.getKey();
			MerkleNode value = entry.getValue();

			ContextNode contextNode = IPFSContextNode.load((IPFSGraph) this.getGraph(), this, key, value.hash);
			list.add(contextNode);
		}

		// done

		return new ReadOnlyIterator<ContextNode> (list.iterator());
	}

	@Override
	public synchronized void delContextNode(XDIArc XDIarc) {

		// delete the context node

		this.ipfsLinks.remove(XDIarc);

		// store context node

		this.store();
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

		JsonArray jsonArray = this.ipfsData.getAsJsonArray("/" + XDIaddress.toString());

		boolean found = false;

		if (jsonArray == null) {

			jsonArray = new JsonArray();
			this.ipfsData.add("/" + XDIaddress.toString(), jsonArray);
		} else {

			for (int i=0; i<jsonArray.size(); i++) {

				if (jsonArray.get(i).equals("/" + XDIaddress.toString())) {

					found = true;
					break;
				}
			}
		}

		if (! found) jsonArray.add(new JsonPrimitive(targetXDIAddress.toString()));

		// store context node

		this.store();

		// done

		IPFSRelation relation = new IPFSRelation(this, XDIaddress, targetXDIAddress);
		return relation;
	}

	@Override
	public ReadOnlyIterator<Relation> getRelations() {

		List<Relation> list = new ArrayList<Relation> ();

		// get relations

		for (Entry<String, JsonElement> entry : this.ipfsData.entrySet()) {

			String key = entry.getKey();
			JsonElement value = entry.getValue();

			if (! key.startsWith("/")) continue;

			if (! (value instanceof JsonArray)) {

				log.warn("Unexpected value " + value.toString() + " for key " + key + " in object " + this.ipfsMerkleNode.hash.toBase58());
				continue;
			}

			XDIAddress XDIaddress = XDIAddress.create(key.substring(1));

			for (JsonElement element : ((JsonArray) value)) {

				if ((! (element instanceof JsonPrimitive)) || (! ((JsonPrimitive) element).isString())) {

					log.warn("Unexpected element " + value.toString() + " for key " + key + " in object " + this.ipfsMerkleNode.hash.toBase58());
					continue;
				}

				XDIAddress targetXDIAddress = XDIAddress.create(((JsonPrimitive) element).getAsString());

				list.add(new IPFSRelation(this, XDIaddress, targetXDIAddress));
			}
		}

		// done

		return new ReadOnlyIterator<Relation> (list.iterator());
	}

	@Override
	public synchronized void delRelation(XDIAddress XDIaddress, XDIAddress targetXDIAddress) {

		// delete relation

		JsonArray jsonArray = this.ipfsData.getAsJsonArray("/" + XDIaddress.toString());
		if (jsonArray == null) return;

		for (Iterator<JsonElement> i = jsonArray.iterator(); i.hasNext(); ) {

			JsonElement jsonElement = i.next();

			if (jsonElement.equals("/" + XDIaddress.toString())) {

				i.remove();
			}
		}

		if (jsonArray.size() < 1) this.ipfsData.remove("/" + XDIaddress.toString());

		// store context node

		this.store();

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

		JsonElement jsonElement = AbstractLiteralNode.literalDataToJsonElement(literalData);
		this.ipfsData.add("&", jsonElement);

		// store context node

		this.store();

		// done

		return new IPFSLiteralNode(this);
	}

	@Override
	public LiteralNode getLiteralNode() {

		// get literal node

		JsonElement jsonElement = this.ipfsData.get("&");
		if (jsonElement == null) return null;

		// done

		return new IPFSLiteralNode(this);
	}

	@Override
	public synchronized void delLiteralNode() {

		// delete literal node

		this.ipfsData.remove("&");

		// store context node

		this.store();
	}

	/*
	 * Retrieve/store methods
	 */

	private static XDIArc parseIpfsLinkXDIArc(MerkleNode link) {

		JsonObject jsonObject;

		String tempString = "{\"temp\":\"" + link.name.get() + "\"}";

		try {

			jsonObject = gson.getAdapter(JsonObject.class).fromJson(tempString);
		} catch (IOException ex) {

			throw new Xdi2RuntimeException("Cannot parse link: " + link.name.get() + ": " + ex.getMessage());
		}

		return XDIArc.create(jsonObject.get("temp").getAsString());
	}

	private static JsonObject parseIpfsDataJsonObject(MerkleNode node) {

		JsonObject jsonObject;

		String tempString = "{\"temp\":\"" + new String(node.data.get(), StandardCharsets.UTF_8) + "\"}";

		try {

			jsonObject = gson.getAdapter(JsonObject.class).fromJson(tempString);
			tempString = jsonObject.get("temp").getAsString();
			jsonObject = gson.getAdapter(JsonObject.class).fromJson(tempString);
		} catch (IOException ex) {

			throw new Xdi2RuntimeException("Cannot parse node: " + node.hash.toBase58() + ": " + ex.getMessage());
		}

		return jsonObject;
	}

	static IPFSContextNode empty(IPFSGraph graph, IPFSContextNode contextNode, XDIArc XDIarc) {

		return load(graph, contextNode, XDIarc, MULTIHASH_EMPTY);
	}

	static IPFSContextNode load(IPFSGraph graph, IPFSContextNode contextNode, XDIArc XDIarc, Multihash multihash) {

		MerkleNode ipfsMerkleNode;
		Map<XDIArc, MerkleNode> ipfsLinks = new HashMap<XDIArc, MerkleNode> ();
		JsonObject ipfsData;

		try {

			ipfsMerkleNode = graph.getIpfs().object.get(multihash);
			if (ipfsMerkleNode == null) return null;

			if (log.isDebugEnabled()) log.debug("Loaded merkle node " + multihash + ": " + ipfsMerkleNode.toJSONString());

			for (MerkleNode ipfsLink : ipfsMerkleNode.links) {

				ipfsLinks.put(parseIpfsLinkXDIArc(ipfsLink), ipfsLink);
			}

			ipfsData = parseIpfsDataJsonObject(ipfsMerkleNode);
		} catch (IOException ex) {

			throw new Xdi2RuntimeException("Cannot load merkle node for multihash " + multihash.toBase58() + ": " + ex.getMessage(), ex);
		}

		return new IPFSContextNode(graph, contextNode, XDIarc, ipfsMerkleNode, ipfsLinks, ipfsData);
	}

	void store() {

		// assemble IPFS links array

		JsonArray ipfsLinks = new JsonArray();

		for (Entry<XDIArc, MerkleNode> entry : this.ipfsLinks.entrySet()) {

			XDIArc key = entry.getKey();
			MerkleNode value = entry.getValue();

			JsonObject ipfsLink = new JsonObject();
			ipfsLink.addProperty("Name", key.toString());
			ipfsLink.addProperty("Hash", value.hash.toBase58());
			ipfsLink.addProperty("Size", value.size.isPresent() ? value.size.get() : Integer.valueOf(0));

			ipfsLinks.add(ipfsLink);
		}

		// assemble IPFS object

		JsonObject ipfsObject = new JsonObject();

		ipfsObject.addProperty("Data", gson.toJson(this.ipfsData));
		ipfsObject.add("Links", ipfsLinks);

		// store IPFS object

		MerkleNode ipfsMerkleNode;

		try {

			ipfsMerkleNode = ((IPFSGraph) this.getGraph()).getIpfs().object.put(Collections.singletonList(gson.toJson(ipfsObject).getBytes(StandardCharsets.UTF_8))).get(0);

			if (log.isDebugEnabled()) log.debug("Stored merkle node for " + this.getXDIAddress() + ": " + ipfsMerkleNode.hash);
		} catch (IOException ex) {

			throw new Xdi2RuntimeException("Cannot store merkle node: " + ex.getMessage(), ex);
		}

		// anything changed?

		if (ipfsMerkleNode.equals(this.ipfsMerkleNode)) return;

		this.ipfsMerkleNode = ipfsMerkleNode;

		// recursion

		IPFSContextNode parentContextNode = (IPFSContextNode) this.getContextNode();

		if (parentContextNode != null) {

			parentContextNode.ipfsLinks.put(this.XDIarc, this.ipfsMerkleNode);
			parentContextNode.store();
		} else {

			((IPFSGraph) this.getGraph()).store();
		}
	}
}
