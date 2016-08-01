package info.smart_tools.smartactors.plugin.cached_collection;

import info.smart_tools.smartactors.core.bootstrap_item.BootstrapItem;
import info.smart_tools.smartactors.core.cached_collection.CachedCollection;
import info.smart_tools.smartactors.core.cached_collection.ICachedCollection;
import info.smart_tools.smartactors.core.iaction.exception.ActionExecuteException;
import info.smart_tools.smartactors.core.ibootstrap.IBootstrap;
import info.smart_tools.smartactors.core.ibootstrap_item.IBootstrapItem;
import info.smart_tools.smartactors.core.ifield.IField;
import info.smart_tools.smartactors.core.iioccontainer.exception.RegistrationException;
import info.smart_tools.smartactors.core.iioccontainer.exception.ResolutionException;
import info.smart_tools.smartactors.core.ikey.IKey;
import info.smart_tools.smartactors.core.invalid_argument_exception.InvalidArgumentException;
import info.smart_tools.smartactors.core.iobject.IObject;
import info.smart_tools.smartactors.core.iobject.exception.ChangeValueException;
import info.smart_tools.smartactors.core.iobject.exception.ReadValueException;
import info.smart_tools.smartactors.core.ioc.IOC;
import info.smart_tools.smartactors.core.iplugin.IPlugin;
import info.smart_tools.smartactors.core.iplugin.exception.PluginException;
import info.smart_tools.smartactors.core.ipool.IPool;
import info.smart_tools.smartactors.core.named_keys_storage.Keys;
import info.smart_tools.smartactors.core.postgres_connection.wrapper.ConnectionOptions;
import info.smart_tools.smartactors.core.resolve_by_composite_name_ioc_with_lambda_strategy.ResolveByCompositeNameIOCStrategy;


/**
 * Plugin for registration strategy of create cached collection with IOC.
 * IOC resolve method waits collectionName as a first parameter and keyName as a second parameter.
 */
public class CachedCollectionPlugin implements IPlugin {

    private final IBootstrap<IBootstrapItem<String>> bootstrap;

    /**
     * Constructor
     * @param bootstrap bootstrap
     */
    public CachedCollectionPlugin(final IBootstrap<IBootstrapItem<String>> bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public void load() throws PluginException {

        try {
            IBootstrapItem<String> item = new BootstrapItem("CachedCollectionPlugin");

            item
                .after("IOC")
                .after("IFieldPlugin")
                .process(() -> {
                    try {
                        IKey cachedCollectionKey = Keys.getOrAdd(ICachedCollection.class.getCanonicalName());
                        IField connectionPoolField = IOC.resolve(Keys.getOrAdd(IField.class.getCanonicalName()), "connectionPool");
                        IField collectionNameField = IOC.resolve(Keys.getOrAdd(IField.class.getCanonicalName()), "collectionName");
                        IField keyNameField = IOC.resolve(Keys.getOrAdd(IField.class.getCanonicalName()), "keyName");
                        IOC.register(cachedCollectionKey, new ResolveByCompositeNameIOCStrategy(
                            (args) -> {
                                try {
                                    String collectionName = (String) args[0];
                                    if (collectionName == null) {
                                        throw new RuntimeException("Can't resolve cached collection: collectionName is null");
                                    }
                                    String keyName = String.valueOf(args[1]);
                                    //TODO:: clarify about generators
                                    //TODO:: wrapperGenerator should be resolved by IOC
                                    ConnectionOptions connectionOptionsWrapper = new ConnectionOptions() {
                                        @Override
                                        public String getUrl() throws ReadValueException {
                                            return "jdbc:postgresql://localhost:5432/test_async";
                                        }

                                        @Override
                                        public String getUsername() throws ReadValueException {
                                            return "test_user";
                                        }

                                        @Override
                                        public String getPassword() throws ReadValueException {
                                            return "qwerty";
                                        }

                                        @Override
                                        public Integer getMaxConnections() throws ReadValueException {
                                            return 10;
                                        }

                                        @Override
                                        public void setUrl(String url) throws ChangeValueException {

                                        }

                                        @Override
                                        public void setUsername(String username) throws ChangeValueException {

                                        }

                                        @Override
                                        public void setPassword(String password) throws ChangeValueException {

                                        }

                                        @Override
                                        public void setMaxConnections(Integer maxConnections) throws ChangeValueException {

                                        }
                                    };
                                    IPool connectionPool = IOC.resolve(Keys.getOrAdd("PostgresConnectionPool"), connectionOptionsWrapper);
                                    IObject config = IOC.resolve(Keys.getOrAdd(IObject.class.getCanonicalName()));
                                    connectionPoolField.out(config, connectionPool);
                                    collectionNameField.out(config, collectionName);
                                    keyNameField.out(config, keyName);

                                    return new CachedCollection(config);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }));
                } catch (RegistrationException | InvalidArgumentException | ResolutionException e) {
                    throw new ActionExecuteException("Error during registration strategy for cached collection.", e);
                }
            });
            bootstrap.add(item);
        } catch (InvalidArgumentException e) {
            throw new PluginException("Can't load CachedCollectionPlugin plugin", e);
        }
    }
}
