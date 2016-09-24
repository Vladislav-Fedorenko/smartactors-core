package info.smart_tools.smartactors.core.message_processing_sequence;

import info.smart_tools.smartactors.base.interfaces.iaction.IAction;
import info.smart_tools.smartactors.base.interfaces.iaction.exception.ActionExecuteException;
import info.smart_tools.smartactors.core.ifield_name.IFieldName;
import info.smart_tools.smartactors.core.ikey.IKey;
import info.smart_tools.smartactors.base.exception.invalid_argument_exception.InvalidArgumentException;
import info.smart_tools.smartactors.core.iobject.IObject;
import info.smart_tools.smartactors.core.ioc.IOC;
import info.smart_tools.smartactors.core.message_processing.IMessageProcessingSequence;
import info.smart_tools.smartactors.core.message_processing.IMessageReceiver;
import info.smart_tools.smartactors.core.message_processing.IReceiverChain;
import info.smart_tools.smartactors.core.message_processing.exceptions.NestedChainStackOverflowException;
import info.smart_tools.smartactors.core.message_processing.exceptions.NoExceptionHandleChainException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Tests for {@link MessageProcessingSequence}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({IOC.class})
public class MessageProcessingSequenceTest {
    private IReceiverChain mainChainMock;
    private IMessageReceiver[] messageReceiverMocks;
    private IObject[] receiverArgsMocks;
    private IObject contextMock;
    private IKey fieldNameKey;

    private IFieldName chainFieldName = mock(IFieldName.class);
    private IFieldName afterActionFieldName = mock(IFieldName.class);

    @Before
    public void setUp()
            throws Exception {
        mainChainMock = mock(IReceiverChain.class);
        messageReceiverMocks = new IMessageReceiver[10];
        receiverArgsMocks = new IObject[10];

        for (int i = 0; i < messageReceiverMocks.length; i++) {
            messageReceiverMocks[i] = mock(IMessageReceiver.class);
        }

        for (int i = 0; i < receiverArgsMocks.length; i++) {
            receiverArgsMocks[i] = mock(IObject.class);
        }

        contextMock = mock(IObject.class);

        mockStatic(IOC.class);

        fieldNameKey = mock(IKey.class);
        PowerMockito.when(IOC.getKeyForKeyStorage()).thenReturn(mock(IKey.class));
        PowerMockito.when(IOC.resolve(same(IOC.getKeyForKeyStorage()), eq(IFieldName.class.getCanonicalName()))).thenReturn((IKey) fieldNameKey);
        PowerMockito.when(IOC.resolve(same(fieldNameKey), eq("causeLevel"))).thenReturn(mock(IFieldName.class));
        PowerMockito.when(IOC.resolve(same(fieldNameKey), eq("causeStep"))).thenReturn(mock(IFieldName.class));
        PowerMockito.when(IOC.resolve(same(fieldNameKey), eq("catchLevel"))).thenReturn(mock(IFieldName.class));
        PowerMockito.when(IOC.resolve(same(fieldNameKey), eq("catchStep"))).thenReturn(mock(IFieldName.class));
        PowerMockito.when(IOC.resolve(same(fieldNameKey), eq("chain"))).thenReturn(this.chainFieldName);
        PowerMockito.when(IOC.resolve(same(fieldNameKey), eq("after"))).thenReturn(this.afterActionFieldName);
    }

    @Test(expected = InvalidArgumentException.class)
    public void Should_constructorThrow_When_invalidStackDepthGiven()
            throws Exception {
        assertNotNull(new MessageProcessingSequence(0, mock(IReceiverChain.class)));
    }

    @Test(expected = InvalidArgumentException.class)
    public void Should_constructorThrow_When_nullMainChainGiven()
            throws Exception {
        assertNotNull(new MessageProcessingSequence(1, null));
    }

    @Test(expected = InvalidArgumentException.class)
    public void Should_constructorThrow_When_mainChainContainsNoReceivers()
            throws Exception {
        when(mainChainMock.get(eq(0))).thenReturn(null);

        assertNotNull(new MessageProcessingSequence(1, mainChainMock));
    }

    @Test(expected = NestedChainStackOverflowException.class)
    public void Should_throw_When_stackOverflowOccurs()
            throws Exception {
        when(mainChainMock.get(eq(0))).thenReturn(messageReceiverMocks[0]);

        IMessageProcessingSequence messageProcessingSequence = new MessageProcessingSequence(4, mainChainMock);

        try {
            messageProcessingSequence.callChain(mainChainMock);
            messageProcessingSequence.callChain(mainChainMock);
            messageProcessingSequence.callChain(mainChainMock);
        } catch (NestedChainStackOverflowException e) {
            fail();
        }

        messageProcessingSequence.callChain(mainChainMock);
    }

    @Test
    public void Should_moveOverAllReceiversInAllNestedChains()
            throws Exception {
        IReceiverChain chainMock1 = mock(IReceiverChain.class);
        IReceiverChain chainMock2 = mock(IReceiverChain.class);
        IReceiverChain chainMock3 = mock(IReceiverChain.class);

        when(mainChainMock.get(eq(0))).thenReturn(messageReceiverMocks[0]);
        when(mainChainMock.get(eq(1))).thenReturn(messageReceiverMocks[1]);
        when(mainChainMock.get(eq(2))).thenReturn(messageReceiverMocks[2]);

        when(chainMock1.get(eq(0))).thenReturn(messageReceiverMocks[3]);
        when(chainMock1.get(eq(1))).thenReturn(messageReceiverMocks[4]);

        when(chainMock2.get(eq(0))).thenReturn(messageReceiverMocks[5]);
        when(chainMock2.get(eq(1))).thenReturn(messageReceiverMocks[6]);

        when(chainMock3.get(eq(0))).thenReturn(messageReceiverMocks[7]);
        when(chainMock3.get(eq(1))).thenReturn(messageReceiverMocks[8]);

        when(mainChainMock.getArguments(eq(0))).thenReturn(receiverArgsMocks[0]);
        when(mainChainMock.getArguments(eq(1))).thenReturn(receiverArgsMocks[1]);
        when(mainChainMock.getArguments(eq(2))).thenReturn(receiverArgsMocks[2]);

        when(chainMock1.getArguments(eq(0))).thenReturn(receiverArgsMocks[3]);
        when(chainMock1.getArguments(eq(1))).thenReturn(receiverArgsMocks[4]);

        when(chainMock2.getArguments(eq(0))).thenReturn(receiverArgsMocks[5]);
        when(chainMock2.getArguments(eq(1))).thenReturn(receiverArgsMocks[6]);

        when(chainMock3.getArguments(eq(0))).thenReturn(receiverArgsMocks[7]);
        when(chainMock3.getArguments(eq(1))).thenReturn(receiverArgsMocks[8]);

        IMessageProcessingSequence messageProcessingSequence = new MessageProcessingSequence(4, mainChainMock);

        assertSame(messageReceiverMocks[0], messageProcessingSequence.getCurrentReceiver());
        assertSame(receiverArgsMocks[0], messageProcessingSequence.getCurrentReceiverArguments());
        assertSame(messageReceiverMocks[0], messageProcessingSequence.getCurrentReceiver());
        assertSame(receiverArgsMocks[0], messageProcessingSequence.getCurrentReceiverArguments());
        assertTrue(messageProcessingSequence.next());
        assertSame(messageReceiverMocks[1], messageProcessingSequence.getCurrentReceiver());

        messageProcessingSequence.callChain(chainMock1);

        assertTrue(messageProcessingSequence.next());
        assertSame(messageReceiverMocks[3], messageProcessingSequence.getCurrentReceiver());

        messageProcessingSequence.callChain(chainMock2);

        assertTrue(messageProcessingSequence.next());
        assertSame(messageReceiverMocks[5], messageProcessingSequence.getCurrentReceiver());
        assertTrue(messageProcessingSequence.next());
        assertSame(messageReceiverMocks[6], messageProcessingSequence.getCurrentReceiver());

        messageProcessingSequence.callChain(chainMock3);

        assertTrue(messageProcessingSequence.next());
        assertSame(messageReceiverMocks[7], messageProcessingSequence.getCurrentReceiver());
        assertTrue(messageProcessingSequence.next());
        assertSame(messageReceiverMocks[8], messageProcessingSequence.getCurrentReceiver());

        assertTrue(messageProcessingSequence.next());
        assertSame(messageReceiverMocks[4], messageProcessingSequence.getCurrentReceiver());

        assertTrue(messageProcessingSequence.next());
        assertSame(messageReceiverMocks[2], messageProcessingSequence.getCurrentReceiver());

        assertFalse(messageProcessingSequence.next());
        assertNull(messageProcessingSequence.getCurrentReceiver());
    }

    @Test
    public void Should_catchException_searchForExceptionalChainAndStartItsExecutionIfFound()
            throws Exception {
        IReceiverChain chainMock1 = mock(IReceiverChain.class);
        IReceiverChain chainMock2 = mock(IReceiverChain.class);
        IObject exceptionalChainAndEnvMock = mock(IObject.class);
        IReceiverChain exceptionalChainMock = mock(IReceiverChain.class);
        IAction afterAction = mock(IAction.class);
        Throwable exception = mock(Throwable.class);

        when(chainMock2.getExceptionalChainAndEnvironments(same(exception))).thenReturn(exceptionalChainAndEnvMock);
        when(exceptionalChainAndEnvMock.getValue(this.chainFieldName)).thenReturn(exceptionalChainMock);
        when(exceptionalChainAndEnvMock.getValue(this.afterActionFieldName)).thenReturn(afterAction);

        when(exceptionalChainMock.get(eq(0))).thenReturn(messageReceiverMocks[0]);

        when(chainMock1.get(eq(0))).thenReturn(messageReceiverMocks[1]);
        when(chainMock2.get(eq(0))).thenReturn(messageReceiverMocks[2]);

        when(mainChainMock.get(0)).thenReturn(messageReceiverMocks[3]);
        when(mainChainMock.get(1)).thenReturn(messageReceiverMocks[4]);

        IMessageProcessingSequence messageProcessingSequence = new MessageProcessingSequence(4, mainChainMock);

        assertSame(messageReceiverMocks[3], messageProcessingSequence.getCurrentReceiver());

        messageProcessingSequence.callChain(chainMock1);
        messageProcessingSequence.callChain(chainMock2);

        assertTrue(messageProcessingSequence.next());

        assertSame(messageReceiverMocks[2], messageProcessingSequence.getCurrentReceiver());

        messageProcessingSequence.catchException(exception, contextMock);

        assertTrue(messageProcessingSequence.next());
        assertSame(messageReceiverMocks[0], messageProcessingSequence.getCurrentReceiver());

        assertTrue(messageProcessingSequence.next());
        assertSame(messageReceiverMocks[1], messageProcessingSequence.getCurrentReceiver());

        assertTrue(messageProcessingSequence.next());
        assertSame(messageReceiverMocks[4], messageProcessingSequence.getCurrentReceiver());
    }

    @Test(expected = NoExceptionHandleChainException.class)
    public void Should_throwWhenNoChainFoundForException()
            throws Exception {
        Throwable exception = mock(Throwable.class);

        when(mainChainMock.getExceptionalChainAndEnvironments(same(exception))).thenReturn(mock(IObject.class));
        when(mainChainMock.get(eq(0))).thenReturn(messageReceiverMocks[0]);

        IMessageProcessingSequence sequence = new MessageProcessingSequence(1, mainChainMock);

        sequence.catchException(exception, contextMock);
    }

    @Test
    public void Should_writeCauseAndCatchPositionsToContext()
            throws Exception {
        Throwable exception = mock(Throwable.class);

        IReceiverChain exceptionalChain = mock(IReceiverChain.class);
        IObject exceptionalChainAndEnv = mock(IObject.class);
        IAction afterAction = mock(IAction.class);
        IReceiverChain secondaryChain = mock(IReceiverChain.class);

        when(mainChainMock.getExceptionalChainAndEnvironments(same(exception))).thenReturn(exceptionalChainAndEnv);
        when(secondaryChain.getExceptionalChainAndEnvironments(same(exception))).thenReturn(mock(IObject.class));
        when(exceptionalChainAndEnv.getValue(this.afterActionFieldName)).thenReturn(afterAction);
        when(exceptionalChainAndEnv.getValue(this.chainFieldName)).thenReturn(exceptionalChain);

        when(mainChainMock.get(eq(0))).thenReturn(messageReceiverMocks[0]);
        when(secondaryChain.get(eq(0))).thenReturn(messageReceiverMocks[0]);
        when(secondaryChain.get(eq(1))).thenReturn(messageReceiverMocks[1]);
        when(exceptionalChain.get(eq(0))).thenReturn(messageReceiverMocks[0]);

        MessageProcessingSequence messageProcessingSequence = new MessageProcessingSequence(5, mainChainMock);

        messageProcessingSequence.next();
        messageProcessingSequence.callChain(mainChainMock);
        messageProcessingSequence.next();
        messageProcessingSequence.callChain(secondaryChain);
        messageProcessingSequence.next();
        messageProcessingSequence.callChain(secondaryChain);
        messageProcessingSequence.next();
        messageProcessingSequence.next();

        messageProcessingSequence.catchException(exception, contextMock);

        verify(contextMock).setValue(same(IOC.resolve(fieldNameKey, "causeLevel")), eq(3));
        verify(contextMock).setValue(same(IOC.resolve(fieldNameKey, "causeStep")), eq(1));
        verify(contextMock).setValue(same(IOC.resolve(fieldNameKey, "catchLevel")), eq(1));
        verify(contextMock).setValue(same(IOC.resolve(fieldNameKey, "catchStep")), eq(0));
    }

    @Test(expected = InvalidArgumentException.class)
    public void Should_goToThrow_When_positionIsOutOfRange()
            throws Exception {
        new MessageProcessingSequence(5, mainChainMock).goTo(-1, 0);
    }

    @Test
    public void Should_permitAccessToCurrentStackLevelAndStepIndexesOnAllLevel()
            throws Exception {
        when(mainChainMock.get(eq(0))).thenReturn(messageReceiverMocks[0]);

        MessageProcessingSequence messageProcessingSequence = new MessageProcessingSequence(5, mainChainMock);

        messageProcessingSequence.next();
        messageProcessingSequence.callChain(mainChainMock);
        messageProcessingSequence.next();
        messageProcessingSequence.callChain(mainChainMock);
        messageProcessingSequence.next();

        assertEquals(2, messageProcessingSequence.getCurrentLevel());
        assertEquals(0, messageProcessingSequence.getStepAtLevel(2));

        messageProcessingSequence.goTo(1,3);

        assertEquals(1, messageProcessingSequence.getCurrentLevel());
        assertEquals(2, messageProcessingSequence.getStepAtLevel(1));
    }

    @Test(expected = InvalidArgumentException.class)
    public void Should_getStepAtLevelThrow_When_LevelIndexIsNegative()
            throws Exception {
        new MessageProcessingSequence(5, mainChainMock).getStepAtLevel(-1);
    }

    @Test(expected = InvalidArgumentException.class)
    public void Should_getStepAtLevelThrow_When_LevelIndexIsGreaterThanIndexOfCurrentLevel()
            throws Exception {
        new MessageProcessingSequence(5, mainChainMock).getStepAtLevel(1);
    }

    @Test
    public void Should_returnFalse_When_CallEndMethod()
            throws Exception {

        when(mainChainMock.get(eq(0))).thenReturn(messageReceiverMocks[0]);
        when(mainChainMock.get(eq(1))).thenReturn(messageReceiverMocks[1]);
        when(mainChainMock.get(eq(2))).thenReturn(messageReceiverMocks[2]);

        IMessageProcessingSequence messageProcessingSequence = new MessageProcessingSequence(4, mainChainMock);
        messageProcessingSequence.end();
        assertFalse(messageProcessingSequence.next());
    }

    @Test
    public void Should_returnFalse_When_AfterActionCallEndMethod()
            throws Exception {
        Throwable exception = mock(Throwable.class);
        IReceiverChain exceptionalChain = mock(IReceiverChain.class);
        IObject exceptionalChainAndEnv = mock(IObject.class);
        IAction<IMessageProcessingSequence> afterAction = IMessageProcessingSequence::end;

        when(mainChainMock.getExceptionalChainAndEnvironments(same(exception))).thenReturn(exceptionalChainAndEnv);
        when(exceptionalChainAndEnv.getValue(this.afterActionFieldName)).thenReturn(afterAction);
        when(exceptionalChainAndEnv.getValue(this.chainFieldName)).thenReturn(exceptionalChain);

        when(mainChainMock.get(eq(0))).thenReturn(messageReceiverMocks[0]);
        when(mainChainMock.get(eq(1))).thenReturn(messageReceiverMocks[1]);
        when(mainChainMock.get(eq(2))).thenReturn(messageReceiverMocks[2]);

        IMessageProcessingSequence messageProcessingSequence = new MessageProcessingSequence(4, mainChainMock);
        messageProcessingSequence.catchException(exception, contextMock);

        assertFalse(messageProcessingSequence.next());
    }

    @Test
    public void Should_returnFalse_WhenAfterActionThrowsException() throws Exception {
        Throwable exception = mock(Throwable.class);
        IReceiverChain exceptionalChain = mock(IReceiverChain.class);
        IObject exceptionalChainAndEnv = mock(IObject.class);
        IAction<IMessageProcessingSequence> afterAction = (mps) -> {throw new ActionExecuteException("exception");};

        when(mainChainMock.getExceptionalChainAndEnvironments(same(exception))).thenReturn(exceptionalChainAndEnv);
        when(exceptionalChainAndEnv.getValue(this.afterActionFieldName)).thenReturn(afterAction);
        when(exceptionalChainAndEnv.getValue(this.chainFieldName)).thenReturn(exceptionalChain);

        when(mainChainMock.get(eq(0))).thenReturn(messageReceiverMocks[0]);
        when(mainChainMock.get(eq(1))).thenReturn(messageReceiverMocks[1]);
        when(mainChainMock.get(eq(2))).thenReturn(messageReceiverMocks[2]);

        IMessageProcessingSequence messageProcessingSequence = new MessageProcessingSequence(4, mainChainMock);
        messageProcessingSequence.catchException(exception, contextMock);

        assertFalse(messageProcessingSequence.next());
    }
}
