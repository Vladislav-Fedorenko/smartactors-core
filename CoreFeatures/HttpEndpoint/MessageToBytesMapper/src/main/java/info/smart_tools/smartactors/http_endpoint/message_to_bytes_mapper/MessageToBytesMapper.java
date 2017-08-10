package info.smart_tools.smartactors.http_endpoint.message_to_bytes_mapper;


import com.google.common.base.Charsets;
import info.smart_tools.smartactors.endpoint.interfaces.imessage_mapper.IMessageMapper;
import info.smart_tools.smartactors.iobject.iobject.exception.SerializeException;
import info.smart_tools.smartactors.ioc.iioccontainer.exception.ResolutionException;
import info.smart_tools.smartactors.iobject.iobject.IObject;
import info.smart_tools.smartactors.ioc.ioc.IOC;
import info.smart_tools.smartactors.ioc.named_keys_storage.Keys;

/**
 * Implementation of {@link IMessageMapper} which map message to byte array
 */
public class MessageToBytesMapper implements IMessageMapper<byte[]> {

    /**
     * Deserialize bytes array of json string to IObject
     *
     * @param serializedInput Array of bytes, that should be deserialize
     * @return {@link IObject} interpretation of serializedInput
     * @throws ResolutionException
     */
    @Override
    public IObject deserialize(final byte[] serializedInput) throws ResolutionException {
        if (serializedInput.length == 0) {
            return IOC.resolve(Keys.getOrAdd("EmptyIObject"));
        }
        String string = new String(serializedInput);
        string = string.substring(string.indexOf('{'), string.lastIndexOf('}') + 1);
        return IOC.resolve(Keys.getOrAdd("info.smart_tools.smartactors.iobject.iobject.IObject"), string);
    }

    @Override
    public byte[] serialize(final IObject message) {
        try {
            String serializedMessage = message.serialize();
            return serializedMessage.getBytes(Charsets.UTF_8);
        } catch (SerializeException e) {
            System.err.println(new java.util.Date());
            e.printStackTrace();
        }
        return null;
    }
}
