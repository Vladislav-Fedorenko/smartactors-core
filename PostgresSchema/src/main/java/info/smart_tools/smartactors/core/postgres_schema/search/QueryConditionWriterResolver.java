package info.smart_tools.smartactors.core.postgres_schema.search;


import info.smart_tools.smartactors.core.db_storage.exceptions.QueryBuildException;

/**
 * Resolves a {@link QueryConditionWriter} for specific condition object key.
 */
public interface QueryConditionWriterResolver {

    /**
     *
     * @param conditionKey key in key-value pair of condition object. May be an operator name,
     *                     field path or null for default operator.
     * @return QueryConditionWriter found for this condition key.
     * @throws QueryBuildException if something goes wrong
     */
    QueryConditionWriter resolve(String conditionKey) throws QueryBuildException;
}
