package appbox.store.query;

public interface ISqlSelectQuery extends ISqlQuery {
    enum QueryPurpose {
        None, Count, ToScalar, ToTreeList, ToSingle, ToList, ToTreeNodePath;
    }

    QueryPurpose getPurpose();
}
