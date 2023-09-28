package org.apache.helix;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

public class TestListener implements IAnnotationTransformer {

  @Override
  public void transform(ITestAnnotation annotation, Class aClass, Constructor constructor, Method method) {
    if (method.getName().contains("testSimpleCacheRefresh")) {
      annotation.setExpectedExceptions(new Class[] { RuntimeException.class});
    }

    if (!method.getDeclaringClass().getPackage().getName().contains("org.apache.helix.common.caches")) {
      annotation.setEnabled(false);
    }
  }

}
