package xdi2.core.impl.json.ipfs;

import xdi2.core.LiteralNode;
import xdi2.core.impl.AbstractLiteralNode;

public class IPFSLiteralNode extends AbstractLiteralNode implements LiteralNode {

	private static final long serialVersionUID = -7857969624385707741L;

	private Object literalData;

	IPFSLiteralNode(IPFSContextNode contextNode, Object literalData) {

		super(contextNode);

		this.literalData = literalData;
	}

	@Override
	public Object getLiteralData() {

		return this.literalData;
	}

	@Override
	public void setLiteralData(Object literalData) {

		this.literalData = literalData;
	}
}
