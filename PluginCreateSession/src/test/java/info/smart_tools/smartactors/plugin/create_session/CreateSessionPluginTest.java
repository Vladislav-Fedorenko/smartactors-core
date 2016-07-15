package info.smart_tools.smartactors.plugin.create_session;

import info.smart_tools.smartactors.actors.create_session.CreateSessionActor;
import info.smart_tools.smartactors.actors.create_session.wrapper.CreateSessionConfig;
import info.smart_tools.smartactors.core.bootstrap_item.BootstrapItem;
import info.smart_tools.smartactors.core.create_new_instance_strategy.CreateNewInstanceStrategy;
import info.smart_tools.smartactors.core.iaction.IPoorAction;
import info.smart_tools.smartactors.core.ibootstrap.IBootstrap;
import info.smart_tools.smartactors.core.ikey.IKey;
import info.smart_tools.smartactors.core.iobject.IObject;
import info.smart_tools.smartactors.core.ioc.IOC;
import info.smart_tools.smartactors.core.ipool.IPool;
import info.smart_tools.smartactors.core.named_keys_storage.Keys;
import jdk.nashorn.internal.runtime.linker.Bootstrap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@PrepareForTest({IOC.class, Keys.class, CreateSessionActor.class})
@RunWith(PowerMockRunner.class)
public class CreateSessionPluginTest {

    private CreateSessionPlugin plugin;
    private IBootstrap bootstrap;

    private IKey key;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(IOC.class);
        PowerMockito.mockStatic(Keys.class);

        key = Mockito.mock(IKey.class);
        when(IOC.getKeyForKeyStorage()).thenReturn(key);

        bootstrap = Mockito.mock(IBootstrap.class);
        plugin = new CreateSessionPlugin(bootstrap);
    }

    @Test
    public void ShouldAddNewItemDuringLoad() throws Exception {
        IKey actorKey = Mockito.mock(IKey.class);
        when(Keys.getOrAdd(Mockito.eq(CreateSessionActor.class.toString()))).thenReturn(actorKey);

        BootstrapItem bootstrapItem = Mockito.mock(BootstrapItem.class);
        PowerMockito.whenNew(BootstrapItem.class).withAnyArguments().thenReturn(bootstrapItem);
        when(bootstrapItem.after(anyString())).thenReturn(bootstrapItem);

        plugin.load();

        PowerMockito.verifyNew(BootstrapItem.class).withArguments("CreateCreateSessionActorPlugin");

        ArgumentCaptor<IPoorAction> actionArgumentCaptor = ArgumentCaptor.forClass(IPoorAction.class);
        Mockito.verify(bootstrapItem).process(actionArgumentCaptor.capture());

        ArgumentCaptor<CreateNewInstanceStrategy> createNewInstanceStrategyArgumentCaptor =
                ArgumentCaptor.forClass(CreateNewInstanceStrategy.class);
        actionArgumentCaptor.getValue().execute();

        PowerMockito.verifyStatic();
        IOC.register(Mockito.eq(actorKey), Mockito.eq(createNewInstanceStrategyArgumentCaptor.capture()));

        CreateSessionActor actor = Mockito.mock(CreateSessionActor.class);
        PowerMockito.whenNew(CreateSessionActor.class).withAnyArguments().thenReturn(actor);

        Mockito.verify(bootstrap).add(Mockito.any());
    }
}