package appbox.design.common;

import appbox.design.tree.DesignNodeType;

import java.util.*;

/** 
 用于包装设计器向服务端发送的签出请求
*/
public final class CheckoutInfo
{
	private DesignNodeType nodeType = DesignNodeType.values()[0];
	private String         targetID;
	private int            version;
	private String         developerName;
	private UUID           developerOuid;

	public CheckoutInfo() {
	}

	public CheckoutInfo(DesignNodeType nodeType, String targetID, int version, String developerName, UUID developerOuID)
	{
		setNodeType(nodeType);
		setTargetID(targetID);
		setVersion(version);
		setDeveloperName(developerName);
		setDeveloperOuid(developerOuID);
		setCheckoutTime(java.time.LocalDateTime.now());
	}

	public String GetKey()
	{
		return MakeKey(getNodeType(), getTargetID());
	}

	public static String MakeKey(DesignNodeType nodeType, String targetId)
	{
		return String.format("%1$s|%2$s", (byte)nodeType.getValue(), targetId);
	}
	//region ====get set====
	public DesignNodeType getNodeType()
	{
		return nodeType;
	}
	private void setNodeType(DesignNodeType value)
	{
		nodeType = value;
	}
	public boolean getIsSingleModel()
	{
		return getNodeType().getValue() >= DesignNodeType.EntityModelNode.getValue();
	}
	public String getTargetID()
	{
		return targetID;
	}
	private void setTargetID(String value)
	{
		targetID = value;
	}
	public int getVersion()
	{
		return version;
	}
	private void setVersion(int value)
	{
		version = value;
	}
	public String getDeveloperName()
	{
		return developerName;
	}
	private void setDeveloperName(String value)
	{
		developerName = value;
	}
	public UUID getDeveloperOuid()
	{
		return developerOuid;
	}
	private void setDeveloperOuid(UUID value)
	{
		developerOuid = value;
	}
	private java.time.LocalDateTime CheckoutTime = java.time.LocalDateTime.MIN;
	public java.time.LocalDateTime getCheckoutTime()
	{
		return CheckoutTime;
	}
	public void setCheckoutTime(java.time.LocalDateTime value)
	{
		CheckoutTime = value;
	}
	//endregion
}