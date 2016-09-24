package info.smart_tools.smartactors.test.test_assertions;

import info.smart_tools.smartactors.core.iobject.IObject;
import info.smart_tools.smartactors.core.iobject.exception.ReadValueException;
import info.smart_tools.smartactors.test.iassertion.exception.AssertionFailureException;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Test for {@link EqualAssertion}.
 */
public class EqualAssertionTest extends AssertionTestBase {
    @Test
    public void Should_passWhenValueIsEqualToReference()
            throws Exception {
        apply(EqualAssertion.class, "{'to':'reference string'}".replace('\'', '"'), "reference string");
    }

    @Test(expected = AssertionFailureException.class)
    public void Should_failWhenValueIsNotEqualToReferenceValue()
            throws Exception {
        apply(EqualAssertion.class, "{'to':'reference string'}".replace('\'', '"'), "wrong string");
    }

    @Test(expected = AssertionFailureException.class)
    public void Should_throwWhenCannotReadReferenceValue()
            throws Exception {
        IObject desc = mock(IObject.class);

        when(desc.getValue(any())).thenThrow(ReadValueException.class);

        apply(EqualAssertion.class, desc, "value");
    }
}
