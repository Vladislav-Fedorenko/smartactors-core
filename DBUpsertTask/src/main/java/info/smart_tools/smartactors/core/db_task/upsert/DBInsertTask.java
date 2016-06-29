package info.smart_tools.smartactors.core.db_task.upsert;

import info.smart_tools.smartactors.core.db_storage.exceptions.QueryExecutionException;
import info.smart_tools.smartactors.core.db_storage.interfaces.CompiledQuery;
import info.smart_tools.smartactors.core.db_task.upsert.psql.wrapper.IUpsertQueryMessage;
import info.smart_tools.smartactors.core.iobject.exception.ChangeValueException;
import info.smart_tools.smartactors.core.iobject.exception.ReadValueException;
import info.smart_tools.smartactors.core.itask.exception.TaskExecutionException;

import javax.annotation.Nonnull;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Common insert task executor.
 */
public abstract class DBInsertTask extends DBUpsert {

    protected DBInsertTask() {}

    /**
     * Executes insertion documents query to database.
     *
     * @param query - a compiled executable query to database.
     * @param message - source message with parameters for query.
     *
     * @throws TaskExecutionException when the result set has more than one document.
     */
    protected void execute(@Nonnull final CompiledQuery query, @Nonnull final IUpsertQueryMessage message)
            throws TaskExecutionException {
        try {
            ResultSet resultSet = query.executeQuery();
            if (resultSet == null || !resultSet.first()) {
                throw new TaskExecutionException("Query execution has been failed: " +
                        "Database returned not enough generated ids");
            }

            addIdsInResult(resultSet, message);
        } catch (QueryExecutionException | SQLException e) {
            throw new TaskExecutionException("'Insert query' execution has been failed: ", e);
        }
    }

    private static void addIdsInResult(
            final ResultSet resultSet,
            final IUpsertQueryMessage message
    ) throws TaskExecutionException {
        try {
            int docCounter = 0;
            while (resultSet.next()) {
                if (docCounter >= message.countDocuments()) {
                    throw new TaskExecutionException("Database returned too much of generated ids.");
                }
                try {
                    message.getDocuments(docCounter).setId(resultSet.getLong("id"));
                } catch (ChangeValueException e) {
                    throw new TaskExecutionException("Could not set new id on inserted document.");
                } catch (ReadValueException e) {
                    throw new TaskExecutionException("Could not read document with index " + docCounter + ".");
                }

                docCounter++;
            }
            if (docCounter < message.countDocuments()) {
                throw new TaskExecutionException("Database returned not enough of generated ids.");
            }
        } catch (SQLException e) {
            throw new TaskExecutionException(e.getMessage(), e);
        }
    }
}
