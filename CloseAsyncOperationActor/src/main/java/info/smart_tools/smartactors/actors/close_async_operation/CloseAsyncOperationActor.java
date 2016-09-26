package info.smart_tools.smartactors.actors.close_async_operation;

import info.smart_tools.smartactors.actors.close_async_operation.wrapper.CloseAsyncOpMessage;
import info.smart_tools.smartactors.core.async_operation_collection.IAsyncOperationCollection;
import info.smart_tools.smartactors.core.async_operation_collection.exception.CompleteAsyncOperationException;
import info.smart_tools.smartactors.iobject.ifield.IField;
import info.smart_tools.smartactors.ioc.iioccontainer.exception.ResolutionException;
import info.smart_tools.smartactors.base.exception.invalid_argument_exception.InvalidArgumentException;
import info.smart_tools.smartactors.iobject.iobject.IObject;
import info.smart_tools.smartactors.iobject.iobject.exception.ReadValueException;
import info.smart_tools.smartactors.ioc.ioc.IOC;
import info.smart_tools.smartactors.ioc.named_keys_storage.Keys;

/**
 * Actor that close async operation
 */
public class CloseAsyncOperationActor {
    private static IField collectionNameField;
    private IAsyncOperationCollection collection;

    /**
     * Constructor
     * @param params the params for constructor
     * @throws InvalidArgumentException Throw when can't read some value from message or resolving key or dependency is throw exception
     */
    public CloseAsyncOperationActor(final IObject params) throws InvalidArgumentException {
        try {
            collectionNameField = IOC.resolve(Keys.getOrAdd(IField.class.getCanonicalName()), "collectionName");
            collection = IOC.resolve(Keys.getOrAdd(IAsyncOperationCollection.class.getCanonicalName()), (String) collectionNameField.in(params));
        } catch (ReadValueException e) {
            throw new InvalidArgumentException("Can't read collection name from message", e);
        } catch (ResolutionException e) {
            throw new InvalidArgumentException("Can't get key or resolve dependency", e);
        }
    }

    /**
     * Remove token from session and mark operation as complete
     * @param message the message
     *                <pre>
     *                {
     *                    "token" : "some token value"
     *                }
     *                </pre>
     * @throws InvalidArgumentException Throw when can't read some value from message or have invalid parameters
     */
    public void completeAsyncOp(final CloseAsyncOpMessage message) throws InvalidArgumentException {
        try {
            message.getOperationTokens().remove(message.getToken());
            collection.complete(message.getOperation());
        } catch (ReadValueException e) {
            throw new InvalidArgumentException("Can't read some of values in message", e);
        } catch (CompleteAsyncOperationException e) {
            throw new InvalidArgumentException("Can't close async operation with this parameters: " + message, e);
        }
    }
}
