package io.tackle.dgi.code2graph.utils;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.JarFileEntry;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.types.ClassLoaderReference;

public class AnalysisUtils {
  public static Boolean isApplicationClass(IClass _class) {
    if (_class.getClassLoader().getReference().equals(ClassLoaderReference.Primordial)) {
      return false;
    }
    if (_class instanceof ShrikeClass) {
      ShrikeClass sc = (ShrikeClass) _class;
      ModuleEntry m = sc.getModuleEntry();
      System.out.println(m.getClassName());
      if (!(m instanceof JarFileEntry)) {
        return true;
      } else {
        return false;
      }
    }
    return true;
  }
}
