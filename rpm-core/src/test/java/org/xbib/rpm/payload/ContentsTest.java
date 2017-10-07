package org.xbib.rpm.payload;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ContentsTest {

    @Test
    public void testListParents() throws Exception {
        ArrayList<String> list = new ArrayList<>();
        new Contents().listParents(list, Paths.get("/one/two/three/four"));
        assertEquals(3, list.size());
        assertEquals("/one/two/three", list.get(0));
        assertEquals("/one/two", list.get(1));
        assertEquals("/one", list.get(2));
    }

    @Test
    public void testListParentsBuiltin() throws Exception {
        ArrayList<String> list = new ArrayList<>();
        new Contents().listParents(list, Paths.get("/bin/one/two/three/four"));
        assertEquals(3, list.size());
        assertEquals("/bin/one/two/three", list.get(0));
        assertEquals("/bin/one/two", list.get(1));
        assertEquals("/bin/one", list.get(2));
    }

    @Test
    public void testListParentsNewLocalBuiltin() throws Exception {
        ArrayList<String> list = new ArrayList<>();
        Contents contents = new Contents();
        contents.addLocalBuiltinDirectory("/home");
        contents.listParents(list, Paths.get("/home/one/two/three/four"));
        assertEquals(3, list.size());
        assertEquals("/home/one/two/three", list.get(0));
        assertEquals("/home/one/two", list.get(1));
        assertEquals("/home/one", list.get(2));
    }

    @Test
    public void testAddFileSetsDirModeOnHeader() throws Exception {
        Contents contents = new Contents();
        contents.addFile("/test/file.txt", Paths.get("src/test/resources/test.txt"),
                511, null, "testuser", "testgroup", 73);
        Iterable<CpioHeader> headers = contents.headers();
        Map<String, Integer> filemodes = new HashMap<>();
        for (CpioHeader header : headers) {
            filemodes.put(header.getName(), header.getPermissions());
        }
        assertThat(filemodes.get("/test"), is(73));
        assertThat(filemodes.get("/test/file.txt"), is(511));
    }
}
