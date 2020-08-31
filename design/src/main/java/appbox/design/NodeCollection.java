package appbox.design;

import java.util.*;

/** 
 子节点，添加时自动排序
*/
public final class NodeCollection
{

	private DesignNode owner;
	private List<DesignNode> nodes;

	public NodeCollection(DesignNode owner)
	{
		this.owner = owner;
		nodes = new ArrayList<DesignNode>();
	}

	public int Add(DesignNode item)
	{
		item.setParent(owner);
		//特定owner找到插入点
		if (owner != null && (owner.getNodeType() == DesignNodeType.ModelRootNode || owner.getNodeType() == DesignNodeType.FolderNode))
		{
			int index = -1;
			for (var i = 0; i < nodes.size(); i++)
			{
				if (!item.equals(nodes.get(i)) )
				{
					index = i;
					break;
				}
			}
			if (index != -1)
			{
				nodes.add(index, item);
				return index;
			}

			nodes.add(item);
			return nodes.size() - 1;
		}

		nodes.add(item);
		return nodes.size() - 1;
	}

	public void Remove(DesignNode item)
	{
		int index = nodes.indexOf(item);
		if (index >= 0)
		{
			item.setParent(null);
			nodes.remove(index);
		}
	}

	public void Clear()
	{
		for (int i = 0; i < nodes.size(); i++)
		{
			nodes.get(i).setParent(null);
		}
		nodes.clear();
	}

	public DesignNode Find(java.util.function.Predicate<DesignNode> match)
	{
		for(DesignNode node:nodes){
			if(match.test(node)){
				return node;
			}
		}
		return null;
	}

	public boolean Exists(java.util.function.Predicate<DesignNode> match)
	{
		for(DesignNode node:nodes){
			if(match.test(node)){
				return true;
			}
		}
		return false;
	}

	public int getCount()
	{
		return nodes.size();
	}

	public DesignNode getItem(int index)
	{
		return nodes.get(index);
	}

	public DesignNode[] ToArray()
	{
		return nodes.toArray(new DesignNode[0]);
	}

}