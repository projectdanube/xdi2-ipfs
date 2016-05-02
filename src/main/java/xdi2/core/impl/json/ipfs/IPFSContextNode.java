package xdi2.core.impl.json.ipfs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.ipfs.api.MerkleNode;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import xdi2.core.ContextNode;
import xdi2.core.LiteralNode;
import xdi2.core.Node;
import xdi2.core.Relation;
import xdi2.core.impl.AbstractContextNode;
import xdi2.core.impl.AbstractLiteralNode;
import xdi2.core.impl.memory.MemoryGraphFactory;
import xdi2.core.syntax.XDIAddress;
import xdi2.core.syntax.XDIArc;
import xdi2.core.util.iterators.ReadOnlyIterator;

public class IPFSContextNode extends AbstractContextNode implements ContextNode {

	private static final long serialVersionUID = 4930852359817860369L;

	private MerkleNode merkleNode;
	private JsonObject jsonObject;

	IPFSContextNode(IPFSGraph graph, IPFSContextNode contextNode, XDIArc XDIarc) {

		super(graph, contextNode);

		this.XDIarc = XDIarc;

		if (graph.getSortMode() == MemoryGraphFactory.SORTMODE_ALPHA) {

			this.contextNodes = new TreeMap<XDIArc, IPFSContextNode> ();
			this.relations = new TreeMap<XDIAddress, Map<XDIAddress, IPFSRelation>> ();
			this.literalNode = null;
		} else if (graph.getSortMode() == MemoryGraphFactory.SORTMODE_ORDER) {

			this.contextNodes = new LinkedHashMap<XDIArc, IPFSContextNode> ();
			this.relations = new LinkedHashMap<XDIAddress, Map<XDIAddress, IPFSRelation>> ();
			this.literalNode = null;
		} else {

			this.contextNodes = new HashMap<XDIArc, IPFSContextNode> ();
			this.relations = new HashMap<XDIAddress, Map<XDIAddress, IPFSRelation>> ();
			this.literalNode = null;
		}
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
}
