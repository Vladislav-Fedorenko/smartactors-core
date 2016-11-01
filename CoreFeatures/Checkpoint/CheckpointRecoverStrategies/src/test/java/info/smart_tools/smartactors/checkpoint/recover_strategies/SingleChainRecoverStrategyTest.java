package info.smart_tools.smartactors.checkpoint.recover_strategies;

import info.smart_tools.smartactors.base.interfaces.iresolve_dependency_strategy.IResolveDependencyStrategy;
import info.smart_tools.smartactors.base.interfaces.iresolve_dependency_strategy.exception.ResolveDependencyStrategyException;
import info.smart_tools.smartactors.checkpoint.interfaces.IRecoverStrategy;
import info.smart_tools.smartactors.checkpoint.interfaces.exceptions.RecoverStrategyInitializationException;
import info.smart_tools.smartactors.helpers.plugins_loading_test_base.PluginsLoadingTestBase;
import info.smart_tools.smartactors.iobject.iobject.IObject;
import info.smart_tools.smartactors.iobject_plugins.dsobject_plugin.PluginDSObject;
import info.smart_tools.smartactors.iobject_plugins.ifieldname_plugin.IFieldNamePlugin;
import info.smart_tools.smartactors.ioc.ioc.IOC;
import info.smart_tools.smartactors.ioc.named_keys_storage.Keys;
import info.smart_tools.smartactors.ioc_plugins.ioc_keys_plugin.PluginIOCKeys;
import info.smart_tools.smartactors.scope_plugins.scope_provider_plugin.PluginScopeProvider;
import info.smart_tools.smartactors.scope_plugins.scoped_ioc_plugin.ScopedIOCPlugin;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test for {@link SingleChainRecoverStrategy}.
 */
public class SingleChainRecoverStrategyTest extends PluginsLoadingTestBase {

    @Override
    protected void loadPlugins() throws Exception {
        load(ScopedIOCPlugin.class);
        load(PluginScopeProvider.class);
        load(PluginIOCKeys.class);
        load(PluginDSObject.class);
        load(IFieldNamePlugin.class);
    }

    @Override
    protected void registerMocks() throws Exception {
        IOC.register(Keys.getOrAdd("chain_id_from_map_name"), new IResolveDependencyStrategy() {
            @Override
            public <T> T resolve(Object... args) throws ResolveDependencyStrategyException {
                return (T) args[0].toString().concat("__0");
            }
        });
    }

    @Test
    public void Should_alwaysReturnTheSameChainForAStateObject()
            throws Exception {
        IRecoverStrategy strategy = new SingleChainRecoverStrategy();

        IObject state = IOC.resolve(Keys.getOrAdd(IObject.class.getCanonicalName()));
        IObject args = IOC.resolve(Keys.getOrAdd(IObject.class.getCanonicalName()),
                "{'chain':'theChain'}".replace('\'','"'));

        strategy.init(state, args);

        assertEquals("theChain__0", strategy.chooseRecoveryChain(state));
        assertEquals("theChain__0", strategy.chooseRecoveryChain(state));
        assertEquals("theChain__0", strategy.chooseRecoveryChain(state));
    }

    @Test(expected = RecoverStrategyInitializationException.class)
    public void Should_throwWhenArgumentsContainNoChainName()
            throws Exception {
        IRecoverStrategy strategy = new SingleChainRecoverStrategy();

        IObject state = IOC.resolve(Keys.getOrAdd(IObject.class.getCanonicalName()));
        IObject args = IOC.resolve(Keys.getOrAdd(IObject.class.getCanonicalName()));

        strategy.init(state, args);
    }
}
