package info.smart_tools.smartactors.http_endpoint.http_client;


import info.smart_tools.smartactors.base.exception.invalid_argument_exception.InvalidArgumentException;
import info.smart_tools.smartactors.base.strategy.create_new_instance_strategy.CreateNewInstanceStrategy;
import info.smart_tools.smartactors.http_endpoint.http_client_handler.HttpClientHandler;
import info.smart_tools.smartactors.endpoint.interfaces.iresponse_handler.IResponseHandler;
import info.smart_tools.smartactors.endpoint.interfaces.irequest_sender.exception.RequestSenderException;
import info.smart_tools.smartactors.iobject.ds_object.DSObject;
import info.smart_tools.smartactors.iobject.field_name.FieldName;
import info.smart_tools.smartactors.iobject.ifield_name.IFieldName;
import info.smart_tools.smartactors.iobject.iobject.IObject;
import info.smart_tools.smartactors.ioc.iioccontainer.exception.RegistrationException;
import info.smart_tools.smartactors.ioc.iioccontainer.exception.ResolutionException;
import info.smart_tools.smartactors.ioc.ioc.IOC;
import info.smart_tools.smartactors.ioc.named_keys_storage.Keys;
import info.smart_tools.smartactors.ioc.resolve_by_name_ioc_strategy.ResolveByNameIocStrategy;
import info.smart_tools.smartactors.ioc.strategy_container.StrategyContainer;
import info.smart_tools.smartactors.scope.iscope.IScope;
import info.smart_tools.smartactors.scope.iscope_provider_container.exception.ScopeProviderException;
import info.smart_tools.smartactors.scope.scope_provider.ScopeProvider;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Created by sevenbits on 26.09.16.
 */
public class HttpClientTest {
    HttpClientHandler handler;
    IResponseHandler responseHandler;

    @Before
    public void setUp() throws ScopeProviderException, ResolutionException, InvalidArgumentException, RegistrationException {
        responseHandler = mock(IResponseHandler.class);
        handler = new HttpClientHandler(responseHandler);
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

        IOC.register(
                IOC.getKeyForKeyStorage(),
                new ResolveByNameIocStrategy()
        );
        IOC.register(Keys.getOrAdd(IFieldName.class.getCanonicalName()), new CreateNewInstanceStrategy(
                        (args) -> {
                            try {
                                return new FieldName((String) args[0]);
                            } catch (InvalidArgumentException e) {
                                throw new RuntimeException(e);
                            }
                        }
                )
        );

    }

    @Test
    public void test() throws URISyntaxException, RequestSenderException, InvalidArgumentException, InterruptedException {
        HttpClient client = new HttpClient(new URI("http://google.com"), handler);
        client.start();
        IObject request = new DSObject("{\"uri\": \"http://google.com\", \"method\": \"GET\", \"messageMapId\": \"messageMapId\"}");
        client.sendRequest(request);
        verify(responseHandler, timeout(1000)).handle(any(), any());
        client.stop();
    }
}
