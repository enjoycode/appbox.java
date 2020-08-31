package appbox.design;

import appbox.*;
import java.util.*;

/** 
 用于包装设计器向服务端发送的签出请求
*/
public final class CheckoutInfo
{
	private DesignNodeType NodeType = DesignNodeType.values()[0];
	public DesignNodeType getNodeType()
	{
		return NodeType;
	}
	private void setNodeType(DesignNodeType value)
	{
		NodeType = value;
	}
	public boolean getIsSingleModel()
	{
		return getNodeType().getValue() >= DesignNodeType.EntityModelNode.getValue();
	}
	private String TargetID;
	public String getTargetID()
	{
		return TargetID;
	}
	private void setTargetID(String value)
	{
		TargetID = value;
	}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private uint Version;
	private int Version;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public uint getVersion()
	public int getVersion()
	{
		return Version;
	}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private void setVersion(uint value)
	private void setVersion(int value)
	{
		Version = value;
	}
	private String DeveloperName;
	public String getDeveloperName()
	{
		return DeveloperName;
	}
	private void setDeveloperName(String value)
	{
		DeveloperName = value;
	}
	private UUID DeveloperOuid;
	public UUID getDeveloperOuid()
	{
		return DeveloperOuid;
	}
	private void setDeveloperOuid(UUID value)
	{
		DeveloperOuid = value;
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

	public CheckoutInfo() {
	}

	//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public CheckoutInfo(DesignNodeType nodeType, string targetID, uint version, string developerName, Guid developerOuID)
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
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: return string.Format("{0}|{1}", (byte)nodeType, targetId);
		return String.format("%1$s|%2$s", (byte)nodeType.getValue(), targetId);
	}

}