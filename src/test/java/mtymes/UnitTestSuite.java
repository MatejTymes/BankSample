package mtymes;

import org.junit.extensions.dynamicsuite.Directory;
import org.junit.extensions.dynamicsuite.Filter;
import org.junit.extensions.dynamicsuite.TestClassFilter;
import org.junit.extensions.dynamicsuite.suite.DynamicSuite;
import org.junit.runner.RunWith;

import java.lang.reflect.Modifier;

//@RunWith(Suite.class)
@RunWith(DynamicSuite.class)
@Filter(UnitTestSuite.class)
@Directory("src/test/java")
public class UnitTestSuite implements TestClassFilter {

    @Override
    public boolean include(String className) {
        return className.endsWith("Test");
    }

    @Override
    public boolean include(Class cls) {
        return !Modifier.isAbstract(cls.getModifiers());
    }
}
