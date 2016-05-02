package xdi2.core.impl.json.ipfs;

import xdi2.core.Relation;
import xdi2.core.impl.AbstractRelation;
import xdi2.core.syntax.XDIAddress;

public class IPFSRelation extends AbstractRelation implements Relation {

	private static final long serialVersionUID = -2979718490345210876L;

	private XDIAddress XDIaddress;
	private XDIAddress targetXDIAddress;

	IPFSRelation(IPFSContextNode contextNode, XDIAddress XDIaddress, XDIAddress targetXDIAddress) {

		super(contextNode);

		this.XDIaddress = XDIaddress;
		this.targetXDIAddress = targetXDIAddress;
	}

	@Override
	public XDIAddress getXDIAddress() {

		return this.XDIaddress;
	}

	@Override
	public XDIAddress getTargetXDIAddress() {

		return this.targetXDIAddress;
	}
}
