package pl.pateman.importer.ogrexml.converters;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * Created by pateman on 2016-03-17.
 */
public class OgreXMLMeshSkeletonLinkConverter implements Converter {
    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        //  Do nothing.
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        return reader.getAttribute("name");
    }

    @Override
    public boolean canConvert(Class type) {
        return type.equals(String.class);
    }
}
