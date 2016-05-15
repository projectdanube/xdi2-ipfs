package xdi2.core.impl.json.ipfs;

import com.google.gson.JsonElement;

import xdi2.core.LiteralNode;
import xdi2.core.impl.AbstractLiteralNode;

public class IPFSLiteralNode extends AbstractLiteralNode implements LiteralNode {

	private static final long serialVersionUID = -7857969624385707741L;

	IPFSLiteralNode(IPFSContextNode contextNode) {

		super(contextNode);
	}

	@Override
	public Object getLiteralData() {

		JsonElement jsonElement = ((IPFSContextNode) this.getContextNode()).ipfsData.get("&");

		return AbstractLiteralNode.jsonElementToLiteralData(jsonElement);
	}

	@Override
	public void setLiteralData(Object literalData) {

		// set new literal value

		JsonElement jsonElement = AbstractLiteralNode.literalDataToJsonElement(literalData);
		((IPFSContextNode) this.getContextNode()).ipfsData.add("&", jsonElement);

		// store context node

		((IPFSContextNode) this.getContextNode()).store();
	}
}
