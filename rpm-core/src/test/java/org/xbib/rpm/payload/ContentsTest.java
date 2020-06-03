package org.xbib.rpm.payload;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xbib.rpm.payload.CpioHeader.DIR;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ContentsTest {

    @Test
    public void testListParents() {
        List<String> list = new ArrayList<>();
        new Contents().listParents(list, Paths.get("/one/two/three/four"));
        assertEquals(3, list.size());
        assertEquals("/one/two/three", list.get(0));
        assertEquals("/one/two", list.get(1));
        assertEquals("/one", list.get(2));
    }

    @Test
    public void testListParentsBuiltin() {
        List<String> list = new ArrayList<>();
        new Contents().listParents(list, Paths.get("/bin/one/two/three/four"));
        assertEquals(3, list.size());
        assertEquals("/bin/one/two/three", list.get(0));
        assertEquals("/bin/one/two", list.get(1));
        assertEquals("/bin/one", list.get(2));
    }

    @Test
    public void testListParentsNewLocalBuiltin() {
        List<String> list = new ArrayList<>();
        Contents contents = new Contents();
        contents.addLocalBuiltinDirectory("/home");
        contents.listParents(list, Paths.get("/home/one/two/three/four"));
        assertEquals(3, list.size());
        assertEquals("/home/one/two/three", list.get(0));
        assertEquals("/home/one/two", list.get(1));
        assertEquals("/home/one", list.get(2));
    }

    @Test
    public void testAddFileSetsDirModeOnHeader() throws IOException {
        Contents contents = new Contents();
        contents.addFile("/test/file.txt", Paths.get("src/test/resources/test.txt"),
                511, 73,null,
                "testuser", "testgroup", 0, 0, true, -1);
        Iterable<CpioHeader> headers = contents.headers();
        Map<String, Integer> filemodes = new HashMap<>();
        for (CpioHeader header : headers) {
            filemodes.put(header.getName(), header.getPermissions());
        }
        assertThat(filemodes.get("/test"), is(73));
        assertThat(filemodes.get("/test/file.txt"), is(511));
    }

    @Test
    public void testAddDirectory() {
        Contents contents = new Contents();
        addDirectoryWithParents(contents, "test");
        Iterable<CpioHeader> headers = contents.headers();
        CpioHeader header = headers.iterator().next();
        assertThat("test", is(header.name));
        assertThat(DIR, is(header.type));
    }

    @Test
    public void testAddNestedDirectory() {
        Contents contents = new Contents();
        addDirectoryWithParents(contents, "test1/test2/test3");
        Iterable<CpioHeader> headers = contents.headers();
        Iterator<CpioHeader> iterator = headers.iterator();
        CpioHeader header = iterator.next();
        assertThat("test1", is(header.name));
        assertThat(DIR, is(header.type));
        header = iterator.next();
        assertThat("test1/test2", is(header.name));
        assertThat(DIR, is(header.type));
        header = iterator.next();
        assertThat("test1/test2/test3", is(header.name));
        assertThat(DIR, is(header.type));
    }

    @Test
    public void testAddNestedDirectoryWithoutParents() {
        Contents contents = new Contents();
        addDirectoryWithoutParents(contents, "test1/test2/test3");
        Iterable<CpioHeader> headers = contents.headers();
        Iterator<CpioHeader> iterator = headers.iterator();
        CpioHeader header = iterator.next();
        assertThat("test1/test2/test3", is(header.name));
        assertThat(DIR, is(header.type));
    }

    private void addDirectoryWithParents(Contents contents, String path) {
        contents.addDirectory(path, 0,
                EnumSet.of(Directive.NONE), null, null, 0,0, true);
    }

    private void addDirectoryWithoutParents(Contents contents, String path) {
        contents.addDirectory(path, 0,
                EnumSet.of(Directive.NONE), null, null, 0,0, false);
    }
}
