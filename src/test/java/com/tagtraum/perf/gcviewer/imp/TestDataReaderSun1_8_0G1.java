package com.tagtraum.perf.gcviewer.imp;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;

import com.tagtraum.perf.gcviewer.model.GcResourceFile;
import org.junit.Test;

import com.tagtraum.perf.gcviewer.model.GCEvent;
import com.tagtraum.perf.gcviewer.UnittestHelper;
import com.tagtraum.perf.gcviewer.model.GCModel;
import com.tagtraum.perf.gcviewer.model.GCResource;

/**
 * Test logs generated specifically by JDK 1.8 G1 algorithm.
 *
 * @author <a href="mailto:gcviewer@gmx.ch">Joerg Wuethrich</a>
 * <p>created on: 22.07.2014</p>
 */
public class TestDataReaderSun1_8_0G1 {

    private InputStream getInputStream(String fileName) throws IOException {
        return UnittestHelper.getResourceAsStream(UnittestHelper.FOLDER_OPENJDK, fileName);
    }

    private DataReader getDataReader(GCResource gcResource) throws UnsupportedEncodingException, IOException {
        return new DataReaderSun1_6_0G1(gcResource, getInputStream(gcResource.getResourceName()), GcLogType.SUN1_8G1);
    }

    @Test
    public void fullConcurrentCycle() throws Exception {
        TestLogHandler handler = new TestLogHandler();
        handler.setLevel(Level.WARNING);
        GCResource gcResource = new GcResourceFile("SampleSun1_8_0G1_ConcurrentCycle.txt");
        gcResource.getLogger().addHandler(handler);

        DataReader reader = getDataReader(gcResource);
        GCModel model = reader.read();

        assertThat("size", model.size(), is(10));

        assertThat("tenured size after concurrent cycle", model.getPostConcurrentCycleTenuredUsedSizes().getMax(), is(31949 - 10*1024 - 3072));
        assertThat("heap size after concurrent cycle", model.getPostConcurrentCycleHeapUsedSizes().getMax(), is(31949));

        assertThat("initiatingOccupancyFraction", model.getCmsInitiatingOccupancyFraction().getMax(), closeTo(0.69, 0.001));

        assertThat("number of errors", handler.getCount(), is(0));
    }

    /**
     * In java 8, suddenly the full gc events in G1 got detailed information about the generation
     * sizes again. Test, that they are parsed correctly.
     */
    @Test
    public void fullGcWithDetailedSizes() throws Exception {
        TestLogHandler handler = new TestLogHandler();
        handler.setLevel(Level.WARNING);
        GCResource gcResource = new GcResourceFile("byteArray");

        ByteArrayInputStream in = new ByteArrayInputStream(
                ("2014-07-24T13:49:45.090+0400: 92457.841: [Full GC (Allocation Failure)  5811M->3097M(12G), 8.9862292 secs]"
                        + "\n  [Eden: 4096.0K(532.0M)->0.0B(612.0M) Survivors: 80.0M->0.0B Heap: 5811.9M(12.0G)->3097.8M(12.0G)], [Metaspace: 95902K->95450K(1140736K)]"
                        + "\n [Times: user=12.34 sys=0.22, real=8.99 secs]")
                        .getBytes());

        DataReader reader = new DataReaderSun1_6_0G1(gcResource, in, GcLogType.SUN1_8);
        GCModel model = reader.read();

        GCEvent event = (GCEvent) model.get(0);
        assertThat("footprint", event.getTotal(), is(12*1024*1024));
        assertThat("yound before", event.getYoung().getPreUsed(), is(4096 + 80*1024));
        assertThat("tenured", event.getTenured().getTotal(), is(12*1024*1024 - 612*1024));
        assertThat("metaspace", event.getPerm().getTotal(), is(1140736));

        assertThat("perm", model.getPermAllocatedSizes().getN(), is(1));

        assertThat("warning count", handler.getCount(), is(0));
    }

    @Test
    public void printGCCauseTenuringDistribution() throws Exception {
        TestLogHandler handler = new TestLogHandler();
        handler.setLevel(Level.WARNING);
        GCResource gcResource = new GcResourceFile("SampleSun1_8_0G1PrintGCCausePrintTenuringDistribution.txt");
        gcResource.getLogger().addHandler(handler);

        DataReader reader = getDataReader(gcResource);
        GCModel model = reader.read();

        assertEquals("gc pause sum", 16.7578613, model.getPause().getSum(), 0.000000001);

        assertEquals("number of errors", 0, handler.getCount());
    }

    @Test
    public void printHeapAtGC() throws Exception {
        TestLogHandler handler = new TestLogHandler();
        handler.setLevel(Level.WARNING);
        GCResource gcResource = new GcResourceFile("SampleSun1_8_0G1PrintHeapAtGc.txt");
        gcResource.getLogger().addHandler(handler);

        DataReader reader = getDataReader(gcResource);
        GCModel model = reader.read();

        assertEquals("gc pause sum", 0.0055924, model.getPause().getSum(), 0.000000001);

        assertEquals("number of errors", 0, handler.getCount());
    }

    @Test
    public void humongousMixed() throws Exception {
        TestLogHandler handler = new TestLogHandler();
        handler.setLevel(Level.WARNING);
        GCResource gcResource = new GcResourceFile("SampleSun1_8_0G1HumongousMixed.txt");
        gcResource.getLogger().addHandler(handler);

        DataReader reader = getDataReader(gcResource);
        GCModel model = reader.read();

        assertThat("number of events", model.size(), is(1));
        assertThat("number of errors", handler.getCount(), is(2));
    }

    @Test
    public void extendedRemark() throws Exception {
        TestLogHandler handler = new TestLogHandler();
        handler.setLevel(Level.WARNING);
        GCResource gcResource = new GcResourceFile("SampleSun1_8_0G1extended-remark.txt");
        gcResource.getLogger().addHandler(handler);

        DataReader reader = getDataReader(gcResource);
        GCModel model = reader.read();

        assertThat("number of events", model.size(), is(1));
        assertThat("number of errors", handler.getCount(), is(0));
        assertThat("pause duration", model.get(0).getPause(), closeTo(0.1005220, 0.00000001));
    }
}
