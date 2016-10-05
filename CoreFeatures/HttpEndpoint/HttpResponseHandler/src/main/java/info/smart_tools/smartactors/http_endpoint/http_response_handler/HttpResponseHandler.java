package info.smart_tools.smartactors.http_endpoint.http_response_handler;

import info.smart_tools.smartactors.base.exception.invalid_argument_exception.InvalidArgumentException;
import info.smart_tools.smartactors.endpoint.interfaces.ideserialize_strategy.IDeserializeStrategy;
import info.smart_tools.smartactors.endpoint.interfaces.ideserialize_strategy.exceptions.DeserializationException;
import info.smart_tools.smartactors.endpoint.interfaces.iresponse_handler.IResponseHandler;
import info.smart_tools.smartactors.endpoint.interfaces.iresponse_handler.exception.ResponseHandlerException;
import info.smart_tools.smartactors.iobject.ifield_name.IFieldName;
import info.smart_tools.smartactors.iobject.iobject.IObject;
import info.smart_tools.smartactors.iobject.iobject.exception.ChangeValueException;
import info.smart_tools.smartactors.iobject.iobject.exception.ReadValueException;
import info.smart_tools.smartactors.ioc.iioccontainer.exception.ResolutionException;
import info.smart_tools.smartactors.ioc.ioc.IOC;
import info.smart_tools.smartactors.ioc.named_keys_storage.Keys;
import info.smart_tools.smartactors.message_processing_interfaces.message_processing.IMessageProcessingSequence;
import info.smart_tools.smartactors.message_processing_interfaces.message_processing.IMessageProcessor;
import info.smart_tools.smartactors.message_processing_interfaces.message_processing.IReceiverChain;
import info.smart_tools.smartactors.task.interfaces.iqueue.IQueue;
import info.smart_tools.smartactors.task.interfaces.itask.ITask;
import info.smart_tools.smartactors.task.interfaces.itask.exception.TaskExecutionException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;

import java.util.ArrayList;

/**
 * Handler for http response
 */
public class HttpResponseHandler implements IResponseHandler<ChannelHandlerContext, FullHttpResponse> {
    private IQueue<ITask> taskQueue;
    private int stackDepth;
    private IReceiverChain receiverChain;

    private IFieldName messageFieldName;
    private IFieldName contextFieldName;
    private IFieldName httpResponseStatusCodeFieldName;
    private IFieldName responseFieldName;
    private IFieldName headersFieldName;
    private IFieldName cookiesFieldName;
    private IFieldName messageMapIdFieldName;
    private String messageMapId;

    private boolean isReceived;

    /**
     * Constructor
     *
     * @param taskQueue     main queue of the {@link ITask}
     * @param stackDepth    depth of the stack for {@link io.netty.channel.ChannelOutboundBuffer.MessageProcessor}
     * @param receiverChain chain, that should receive message
     */
    public HttpResponseHandler(final IQueue<ITask> taskQueue, final int stackDepth, final IReceiverChain receiverChain,
                               final String messageMapId) {
        this.taskQueue = taskQueue;
        this.stackDepth = stackDepth;
        this.receiverChain = receiverChain;
        try {
            messageFieldName = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "message");
            contextFieldName = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "context");
            httpResponseStatusCodeFieldName = IOC.resolve(
                    Keys.getOrAdd(IFieldName.class.getCanonicalName()),
                    "httpResponseStatusCode"
            );
            responseFieldName = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "response");
            headersFieldName = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "headers");
            cookiesFieldName = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "cookies");
            messageMapIdFieldName = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "messageMapId");
            this.messageMapId = messageMapId;
            isReceived = false;
        } catch (ResolutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handle(final ChannelHandlerContext ctx, final FullHttpResponse response) {
        isReceived = true;
        ITask task = () -> {
            try {
                IObject environment = getEnvironment(response);
                IMessageProcessingSequence processingSequence =
                        IOC.resolve(Keys.getOrAdd(IMessageProcessingSequence.class.getCanonicalName()), stackDepth, receiverChain);
                IMessageProcessor messageProcessor =
                        IOC.resolve(Keys.getOrAdd(IMessageProcessor.class.getCanonicalName()), taskQueue, processingSequence);
                IFieldName messageFieldName = null;
                messageFieldName = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "message");
                IFieldName contextFieldName = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "context");
                IObject message = (IObject) environment.getValue(messageFieldName);
                message.setValue(messageMapIdFieldName, messageMapId);
                IObject context = (IObject) environment.getValue(contextFieldName);
                messageProcessor.process(message, context);
            } catch (ChangeValueException | ReadValueException | InvalidArgumentException | ResponseHandlerException |
                    ResolutionException e) {
                throw new TaskExecutionException(e);
            }
        };
        try {
            taskQueue.put(task);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isReceived() {
        return isReceived;
    }

    private IObject getEnvironment(final FullHttpResponse response) throws ResponseHandlerException {
        try {
            IDeserializeStrategy deserializeStrategy = IOC.resolve(Keys.getOrAdd("httpResponseResolver"), response);
            IObject message = deserializeStrategy.deserialize(response);
            IObject environment = IOC.resolve(Keys.getOrAdd("EmptyIObject"));
            IObject context = IOC.resolve(Keys.getOrAdd("EmptyIObject"));
            context.setValue(cookiesFieldName, new ArrayList<IObject>());
            context.setValue(headersFieldName, new ArrayList<IObject>());
            context.setValue(responseFieldName, response);
            context.setValue(httpResponseStatusCodeFieldName, response.status().code());
            environment.setValue(messageFieldName, message);
            environment.setValue(contextFieldName, context);
            return environment;
        } catch (ResolutionException | DeserializationException | ChangeValueException | InvalidArgumentException e) {
            throw new ResponseHandlerException(e);
        }
    }
}
