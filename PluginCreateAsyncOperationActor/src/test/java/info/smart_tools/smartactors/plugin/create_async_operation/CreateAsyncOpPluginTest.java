package info.smart_tools.smartactors.plugin.create_async_operation;

import info.smart_tools.smartactors.actor.create_async_operation.CreateAsyncOperationActor;
import info.smart_tools.smartactors.core.async_operation_collection.task.CreateAsyncOperationTask;
import info.smart_tools.smartactors.core.bootstrap_item.BootstrapItem;
import info.smart_tools.smartactors.core.create_new_instance_strategy.CreateNewInstanceStrategy;
import info.smart_tools.smartactors.core.iaction.IPoorAction;
import info.smart_tools.smartactors.core.ibootstrap.IBootstrap;
import info.smart_tools.smartactors.core.iioccontainer.exception.ResolutionException;
import info.smart_tools.smartactors.core.ikey.IKey;
import info.smart_tools.smartactors.core.invalid_argument_exception.InvalidArgumentException;
import info.smart_tools.smartactors.core.iobject.IObject;
import info.smart_tools.smartactors.core.ioc.IOC;
import info.smart_tools.smartactors.core.iplugin.exception.PluginException;
import info.smart_tools.smartactors.core.named_keys_storage.Keys;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.*;

@PrepareForTest({IOC.class, Keys.class, CreateAsyncOpPlugin.class, CreateNewInstanceStrategy.class})
@RunWith(PowerMockRunner.class)
public class CreateAsyncOpPluginTest {
    private CreateAsyncOpPlugin plugin;
    private IBootstrap bootstrap;

    @Before
    public void setUp() throws Exception {

        mockStatic(IOC.class);
        mockStatic(Keys.class);

        IKey key1 = mock(IKey.class);
        IKey keyPool = mock(IKey.class);
        when(IOC.getKeyForKeyStorage()).thenReturn(key1);
        when(IOC.resolve(eq(key1), eq("CreateAsyncOperationActorPlugin"))).thenReturn(keyPool);

        bootstrap = mock(IBootstrap.class);
        plugin = new CreateAsyncOpPlugin(bootstrap);
    }

    @Test
    public void MustCorrectLoadPlugin() throws Exception {

        IKey createAsyncOpKey = mock(IKey.class);
        when(Keys.getOrAdd(CreateAsyncOperationActor.class.toString())).thenReturn(createAsyncOpKey);

        BootstrapItem bootstrapItem = mock(BootstrapItem.class);
        whenNew(BootstrapItem.class).withArguments("CreateAsyncOperationActorPlugin").thenReturn(bootstrapItem);

        plugin.load();

        verifyStatic();
        Keys.getOrAdd(CreateAsyncOperationActor.class.toString());

        verifyNew(BootstrapItem.class).withArguments("CreateAsyncOperationActorPlugin");

        ArgumentCaptor<IPoorAction> actionArgumentCaptor = ArgumentCaptor.forClass(IPoorAction.class);

        verify(bootstrapItem).after("IOC");
        verify(bootstrapItem).process(actionArgumentCaptor.capture());

        actionArgumentCaptor.getValue().execute();

        ArgumentCaptor<CreateNewInstanceStrategy> createNewInstanceStrategyArgumentCaptor = ArgumentCaptor.forClass(CreateNewInstanceStrategy.class);

        verifyStatic();
        IOC.register(eq(createAsyncOpKey), createNewInstanceStrategyArgumentCaptor.capture());

        IObject arg = mock(IObject.class);

        CreateAsyncOperationActor actor = mock(CreateAsyncOperationActor.class);
        whenNew(CreateAsyncOperationActor.class).withArguments(arg).thenReturn(actor);

        assertTrue("Objects must return correct object", createNewInstanceStrategyArgumentCaptor.getValue().resolve(arg) == actor);

        verifyNew(BootstrapItem.class).withArguments("CreateAsyncOperationActorPlugin");
        verify(bootstrap).add(bootstrapItem);
    }

    @Test
    public void MustInCorrectLoadWhenKeysThrowException() throws ResolutionException {
        when(Keys.getOrAdd(CreateAsyncOperationActor.class.toString())).thenThrow(new ResolutionException(""));

        try {
            plugin.load();
        } catch (PluginException e) {

            verifyStatic();
            Keys.getOrAdd(CreateAsyncOperationActor.class.toString());
            return;
        }
        assertTrue("Must throw exception, but was not", false);
    }

    @Test
    public void MustInCorrectLoadNewIBootstrapItemThrowException() throws Exception {
        IKey cachedCollectionKey = mock(IKey.class);
        when(Keys.getOrAdd(CreateAsyncOperationActor.class.toString())).thenReturn(cachedCollectionKey);

        whenNew(BootstrapItem.class).withArguments("CreateAsyncOperationActorPlugin").thenThrow(new InvalidArgumentException(""));

        try {
            plugin.load();
        } catch (PluginException e) {

            verifyStatic();
            Keys.getOrAdd(CreateAsyncOperationActor.class.toString());

            verifyNew(BootstrapItem.class).withArguments("CreateAsyncOperationActorPlugin");
            return;
        }
        assertTrue("Must throw exception, but was not", false);
    }
}
