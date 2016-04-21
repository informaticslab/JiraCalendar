package ut.org.phiresearchlab.jira.plugins;

import org.junit.Test;
import org.phiresearchlab.jira.plugins.api.MyPluginComponent;
import org.phiresearchlab.jira.plugins.impl.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest
{
    @Test
    public void testMyName()
    {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent",component.getName());
    }
}