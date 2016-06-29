package info.smart_tools.smartactors.core.db_task.insert.psql;

import info.smart_tools.smartactors.core.create_new_instance_strategy.CreateNewInstanceStrategy;
import info.smart_tools.smartactors.core.db_storage.exceptions.StorageException;
import info.smart_tools.smartactors.core.db_storage.interfaces.CompiledQuery;
import info.smart_tools.smartactors.core.db_storage.interfaces.PreparedQuery;
import info.smart_tools.smartactors.core.db_storage.interfaces.StorageConnection;
import info.smart_tools.smartactors.core.db_storage.utils.CollectionName;
import info.smart_tools.smartactors.core.db_task.insert.psql.wrapper.IInsertQueryMessage;
import info.smart_tools.smartactors.core.idatabase_task.exception.TaskPrepareException;
import info.smart_tools.smartactors.core.ikey.IKey;
import info.smart_tools.smartactors.core.invalid_argument_exception.InvalidArgumentException;
import info.smart_tools.smartactors.core.iobject.IFieldName;
import info.smart_tools.smartactors.core.iobject.IObject;
import info.smart_tools.smartactors.core.ioc.IOC;
import info.smart_tools.smartactors.core.iscope.IScope;
import info.smart_tools.smartactors.core.iscope_provider_container.exception.ScopeProviderException;
import info.smart_tools.smartactors.core.itask.exception.TaskExecutionException;
import info.smart_tools.smartactors.core.named_keys_storage.Keys;
import info.smart_tools.smartactors.core.resolve_by_name_ioc_with_lambda_strategy.ResolveByNameIocStrategy;
import info.smart_tools.smartactors.core.scope_provider.ScopeProvider;
import info.smart_tools.smartactors.core.singleton_strategy.SingletonStrategy;
import info.smart_tools.smartactors.core.sql_commons.JDBCCompiledQuery;
import info.smart_tools.smartactors.core.sql_commons.QueryStatement;
import info.smart_tools.smartactors.core.strategy_container.StrategyContainer;
import info.smart_tools.smartactors.core.string_ioc_key.Key;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.StringWriter;
import java.sql.ResultSet;

import static org.mockito.Mockito.*;
import static org.powermock.api.support.membermodification.MemberMatcher.field;

@RunWith(PowerMockRunner.class)
@PrepareForTest()
public class PSQLInsertTaskTest {

    private JDBCCompiledQuery compiledQuery;
    private PSQLInsertTask task = new PSQLInsertTask();

    @BeforeClass
    public static void before() throws ScopeProviderException {
        ScopeProvider.subscribeOnCreationNewScope(
                scope -> {
                    try {
                        scope.setValue(IOC.getIocKey(), new StrategyContainer());
                    } catch (Exception e) {
                        throw new Error(e);
                    }
                }
        );

        Object keyOfMainScope = ScopeProvider.createScope(null);
        IScope mainScope = ScopeProvider.getScope(keyOfMainScope);
        ScopeProvider.setCurrentScope(mainScope);
    }

    @Before
    public void setUp() throws Exception {
        compiledQuery = mock(JDBCCompiledQuery.class);
        CollectionName collectionName = mock(CollectionName.class);
        when(collectionName.toString()).thenReturn("collection");
        IInsertQueryMessage IInsertQueryMessage = mock(IInsertQueryMessage.class);
        when(IInsertQueryMessage.getCollectionName()).thenReturn(collectionName);

        IOC.register(
                IOC.getKeyForKeyStorage(),
                new ResolveByNameIocStrategy(
                        (a) -> {
                            try {
                                return new Key<IKey>((String) a[0]);
                            } catch (InvalidArgumentException e) {
                                throw new RuntimeException(e);
                            }
                        }
                )
        );

        IKey<PSQLInsertTask> keyDBInsertTask = Keys.getOrAdd(PSQLInsertTask.class.toString());
        IKey<IInsertQueryMessage> keyInsertMessage = Keys.getOrAdd(IInsertQueryMessage.class.toString());
        IKey<QueryStatement> keyQueryStatement = Keys.getOrAdd(QueryStatement.class.toString());
        IKey<IFieldName> keyFieldName = Keys.getOrAdd(IFieldName.class.toString());
        IKey<CompiledQuery> keyCompiledQuery = Keys.getOrAdd(CompiledQuery.class.toString());
        IOC.register(
                keyDBInsertTask,
                new SingletonStrategy(mock(PSQLInsertTask.class))
        );
        IOC.register(
                keyInsertMessage,
                new SingletonStrategy(IInsertQueryMessage)
        );
        QueryStatement queryStatement = mock(QueryStatement.class);
        when(queryStatement.getBodyWriter()).thenReturn(new StringWriter());
        IOC.register(
                keyQueryStatement,
                new SingletonStrategy(queryStatement)
        );

        IOC.register(
                keyFieldName,
                new CreateNewInstanceStrategy(
                        (arg) ->{
                            StorageConnection connection = (StorageConnection) arg[0];
                            try {
                                return connection.compileQuery(new QueryStatement());
                            } catch (StorageException ignored) {}
                            return null;
                        }
                )
        );

        IOC.register(
                keyCompiledQuery,
                new CreateNewInstanceStrategy(
                        (arg) -> {
                            try{
                                StorageConnection connection = (StorageConnection) arg[0];
                                return connection.compileQuery(new QueryStatement());
                            } catch (Exception e){}
                            return null;
                        }
                )
        );

        task = new PSQLInsertTask();
    }

    @Test(expected = TaskPrepareException.class)
    public void ShouldThrowTaskPrepareException_When_IdIsNull() throws Exception {
        IKey<String> keyString = Keys.getOrAdd(String.class.toString());
        IOC.register(
                keyString,
                new CreateNewInstanceStrategy(
                        (arg) -> {return null;}
                )
        );

        IObject insertMessage = mock(IObject.class);
        StorageConnection connection = mock(StorageConnection.class);
        when(connection.compileQuery(any(PreparedQuery.class))).thenReturn(compiledQuery);

        task.setStorageConnection(connection);
        task.prepare(insertMessage);
    }

    @Test(expected = TaskExecutionException.class)
    public void ShouldThrowException_When_NoDocumentsHaveBeenInserted() throws Exception {
        StorageConnection connection = mock(StorageConnection.class);
        when(connection.compileQuery(any(PreparedQuery.class))).thenReturn(compiledQuery);

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.first()).thenReturn(true);
        when(compiledQuery.executeQuery()).thenReturn(resultSet);
        field(PSQLInsertTask.class, "compiledQuery").set(task, compiledQuery);

        task.setStorageConnection(connection);
        task.execute();
    }



}