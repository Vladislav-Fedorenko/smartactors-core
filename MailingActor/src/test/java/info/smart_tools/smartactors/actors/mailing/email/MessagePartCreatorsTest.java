package info.smart_tools.smartactors.actors.mailing.email;


import info.smart_tools.smartactors.core.ds_object.DSObject;
import info.smart_tools.smartactors.core.field.Field;
import info.smart_tools.smartactors.core.field_name.FieldName;
import info.smart_tools.smartactors.core.ifield.IField;
import info.smart_tools.smartactors.core.iioccontainer.exception.RegistrationException;
import info.smart_tools.smartactors.core.iioccontainer.exception.ResolutionException;
import info.smart_tools.smartactors.core.ikey.IKey;
import info.smart_tools.smartactors.base.exception.invalid_argument_exception.InvalidArgumentException;
import info.smart_tools.smartactors.core.iobject.IObject;
import info.smart_tools.smartactors.core.ioc.IOC;
import info.smart_tools.smartactors.core.iscope.IScope;
import info.smart_tools.smartactors.core.iscope_provider_container.exception.ScopeProviderException;
import info.smart_tools.smartactors.core.named_keys_storage.Keys;
import info.smart_tools.smartactors.core.resolve_by_name_ioc_strategy.ResolveByNameIocStrategy;
import info.smart_tools.smartactors.core.scope_provider.ScopeProvider;
import info.smart_tools.smartactors.core.strategy_container.StrategyContainer;
import info.smart_tools.smartactors.base.strategy.apply_function_to_arguments.ApplyFunctionToArgumentsStrategy;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class MessagePartCreatorsTest {
    @Before
    public void setUp() throws ScopeProviderException, RegistrationException, ResolutionException, InvalidArgumentException {
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
        IKey keyIField = Keys.getOrAdd(IField.class.getCanonicalName());
        IOC.register(keyIField, new ApplyFunctionToArgumentsStrategy(
                (args) -> {
                    String fieldName = String.valueOf(args[0]);
                    try {
                        return new Field(new FieldName(fieldName));
                    } catch (InvalidArgumentException e) {
                        throw new RuntimeException("Can't resolve IField: ", e);
                    }
                }
        ));
    }

    @Test
    public void Should_MessagePartCreatorsCallAddedCreator()
            throws Exception {
        IObject context = new DSObject();
        IObject params = new DSObject("{\"type\":\"some-test-part\"}");
        SMTPMessageAdaptor messageAdaptor = new SMTPMessageAdaptor(SMTPMessageAdaptor.createMimeMessage());
        MessagePartCreator mockCreator = mock(MessagePartCreator.class);
        List<IObject> partsList = Collections.singletonList(params);

        MessagePartCreators.add("some-test-part", mockCreator);

        MessagePartCreators.addAllPartsTo(messageAdaptor, context, partsList);

        verify(mockCreator).addPartTo(messageAdaptor, context, params);
    }

    @Test
    public void Should_MessagePartCreatorsCreateTextAndFileParts()
            throws Exception {
        IObject context = new DSObject();
        SMTPMessageAdaptor messageAdaptor = mock(SMTPMessageAdaptor.class);
        List<IObject> partsParameters = Collections.singletonList(
                new DSObject("{\"type\":\"text\",\"mime\":\"text/html\",\"text\":\"TeXt\"}"));

        ArgumentMatcher<MimeBodyPart> partMatcher = new ArgumentMatcher<MimeBodyPart>() {
            @Override
            public boolean matches(Object o) {
                MimeBodyPart part = (MimeBodyPart)o;
                try {
                    return part.getDataHandler().getContentType().equals("text/html") && part.getContent().equals("TeXt");
                } catch (MessagingException | IOException e) {
                    return false;
                }
            }
        };

        MessagePartCreators.addAllPartsTo(messageAdaptor, context, partsParameters);

        verify(messageAdaptor).addPart(argThat(partMatcher));
    }
}
