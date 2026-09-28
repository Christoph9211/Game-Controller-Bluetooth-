package com.example.bluetooth;

import org.junit.Test;

/** Run the pinned source's standalone checks as part of the normal Gradle test task. */
public class SourceCoreTest {
    @Test public void codecAndQueue() { CoreTests.main(new String[0]); }
    @Test public void senderSchedulingAndSafety() throws Exception { SenderTests.main(new String[0]); }
}
