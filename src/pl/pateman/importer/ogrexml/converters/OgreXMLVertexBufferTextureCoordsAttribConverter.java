package pl.pateman.importer.ogrexml.converters;

import com.thoughtworks.xstream.converters.SingleValueConverter;

/**
 * Created by pateman on 2016-03-17.
 */
public class OgreXMLVertexBufferTextureCoordsAttribConverter implements SingleValueConverter {
    @Override
    public boolean canConvert(Class type) {
        return type.equals(Boolean.class);
    }

    @Override
    public String toString(Object obj) {
        return "";
    }

    @Override
    public Object fromString(String str) {
        if (str == null) {
            return Boolean.FALSE;
        }

        return str.equals("1") ? Boolean.TRUE : Boolean.FALSE;
    }
}
