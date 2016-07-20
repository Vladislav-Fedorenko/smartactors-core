package info.smart_tools.smartactors.core.handler_routing_receiver_creator;

import info.smart_tools.smartactors.core.field_name.FieldName;
import info.smart_tools.smartactors.core.handler_routing_receiver.HandlerRoutingReceiver;
import info.smart_tools.smartactors.core.ifield_name.IFieldName;
import info.smart_tools.smartactors.core.invalid_argument_exception.InvalidArgumentException;
import info.smart_tools.smartactors.core.iobject.IObject;
import info.smart_tools.smartactors.core.ioc.IOC;
import info.smart_tools.smartactors.core.ireceiver_generator.IReceiverGenerator;
import info.smart_tools.smartactors.core.iresolve_dependency_strategy.IResolveDependencyStrategy;
import info.smart_tools.smartactors.core.iroutable_object_creator.IRoutedObjectCreator;
import info.smart_tools.smartactors.core.iroutable_object_creator.exceptions.ObjectCreationException;
import info.smart_tools.smartactors.core.irouter.IRouter;
import info.smart_tools.smartactors.core.iwrapper_generator.IWrapperGenerator;
import info.smart_tools.smartactors.core.message_processing.IMessageReceiver;
import info.smart_tools.smartactors.core.named_keys_storage.Keys;
import info.smart_tools.smartactors.strategy.apply_function_to_arguments.ApplyFunctionToArgumentsStrategy;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class HandlerRoutingReceiverCreator implements IRoutedObjectCreator {

    private FieldName name;
    private FieldName dependency;
    private FieldName wrapper;

    public HandlerRoutingReceiverCreator()
            throws ObjectCreationException {
        try {
            this.name = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "name");
            this.dependency = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "dependency");;
            this.wrapper = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "wrapper");
        } catch (Throwable e) {
            throw new ObjectCreationException("Could not create instance of ActorReceiverCreator.");
        }
    }

    @Override
    public void createObject(IRouter router, IObject description)
            throws ObjectCreationException, InvalidArgumentException {

        try {
            Map<Object, IMessageReceiver> handlerReceiversMap = new HashMap<>();
            IWrapperGenerator wg = IOC.resolve(Keys.getOrAdd(IWrapperGenerator.class.getCanonicalName()));
            IReceiverGenerator rg = IOC.resolve(Keys.getOrAdd(IReceiverGenerator.class.getCanonicalName()));
            Object object = IOC.resolve(
                    Keys.getOrAdd((String) description.getValue(this.dependency)),
                    (IObject) description.getValue(this.wrapper)
            );
            for (Method m : object.getClass().getDeclaredMethods()) {
                Class wrapperInterface = m.getParameterTypes()[0];
                Object wrapper = wg.generate(wrapperInterface);
                IResolveDependencyStrategy strategy = new ApplyFunctionToArgumentsStrategy(
                        (arg) -> {
                            try {
                                return wrapper.getClass().newInstance();
                            } catch (Throwable e) {
                                throw new RuntimeException(
                                        "Could not create instance of " + wrapper.getClass().getCanonicalName() + "."
                                        , e
                                );
                            }
                        }
                );
                IMessageReceiver handlerReceiver = rg.generate(object, strategy, m.getName());
                handlerReceiversMap.put(m.getName(), handlerReceiver);
            }
            IMessageReceiver handlerRoutingReceiver = new HandlerRoutingReceiver(handlerReceiversMap);
            router.register(description.getValue(this.name), handlerRoutingReceiver);
        } catch (Throwable e) {
            throw new ObjectCreationException("Could not create receiver chain.", e);
        }
    }
}
